package com.example.projetoPBD.pontoCerto.security;

import com.example.projetoPBD.pontoCerto.service.exceptions.TooManyRequestsException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Collections;
import java.util.Deque;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;

@Service
public class RateLimiterService {

    private static final Logger log = LoggerFactory.getLogger(RateLimiterService.class);
    private static final int MAX_KEYS = 10_000;
    private static final String REDIS_KEY_PREFIX = "rl:";
    private static final String MSG_LIMITE_EXCEDIDO =
            "Muitas tentativas em sequência. Aguarde alguns instantes antes de tentar novamente.";

    private static final DefaultRedisScript<Long> REDIS_INCR_EXPIRE_SCRIPT = new DefaultRedisScript<>(
            """
            local current = redis.call('INCR', KEYS[1])
            if current == 1 then
                redis.call('EXPIRE', KEYS[1], tonumber(ARGV[1]))
            end
            return current
            """,
            Long.class
    );

    private final ConcurrentHashMap<String, Deque<Long>> buckets = new ConcurrentHashMap<>();
    private final StringRedisTemplate redisTemplate;
    private final boolean redisEnabled;

    public RateLimiterService() {
        this.redisTemplate = null;
        this.redisEnabled = false;
    }

    @Autowired
    public RateLimiterService(
            ObjectProvider<StringRedisTemplate> redisTemplateProvider,
            @Value("${app.security.rate-limit.redis-enabled:false}") boolean redisEnabled
    ) {
        this.redisTemplate = redisTemplateProvider != null ? redisTemplateProvider.getIfAvailable() : null;
        this.redisEnabled = redisEnabled;
    }

    public RateLimiterService(StringRedisTemplate redisTemplate, boolean redisEnabled) {
        this.redisTemplate = redisTemplate;
        this.redisEnabled = redisEnabled;
    }

    /**
     * Valida limite de requisições usando Redis distribuído (quando habilitado e disponível)
     * com fallback automático para janela deslizante em memória.
     */
    public void verificarLimite(String chave, int maxRequisicoes, long janelaSegundos) {
        if (redisEnabled && redisTemplate != null) {
            try {
                Long contagem = redisTemplate.execute(
                        REDIS_INCR_EXPIRE_SCRIPT,
                        Collections.singletonList(REDIS_KEY_PREFIX + chave),
                        String.valueOf(janelaSegundos)
                );
                if (contagem != null) {
                    if (contagem > maxRequisicoes) {
                        throw new TooManyRequestsException(MSG_LIMITE_EXCEDIDO);
                    }
                    return;
                }
            } catch (TooManyRequestsException ex) {
                throw ex;
            } catch (DataAccessException ex) {
                log.warn("Falha ao consultar Redis para rate limit (usando fallback em memória): {}", ex.getMessage());
            }
        }

        verificarLimiteEmMemoria(chave, maxRequisicoes, janelaSegundos);
    }

    private void verificarLimiteEmMemoria(String chave, int maxRequisicoes, long janelaSegundos) {
        long agora = Instant.now().getEpochSecond();
        long limiteInferior = agora - janelaSegundos;

        if (buckets.size() >= MAX_KEYS && !buckets.containsKey(chave)) {
            limparBucketsExpirados(limiteInferior);
        }

        Deque<Long> timestamps = buckets.computeIfAbsent(chave, k -> new ConcurrentLinkedDeque<>());
        synchronized (timestamps) {
            while (!timestamps.isEmpty() && timestamps.peekFirst() < limiteInferior) {
                timestamps.pollFirst();
            }
            if (timestamps.size() >= maxRequisicoes) {
                throw new TooManyRequestsException(MSG_LIMITE_EXCEDIDO);
            }
            timestamps.addLast(agora);
        }
    }

    private void limparBucketsExpirados(long limiteInferior) {
        buckets.entrySet().removeIf(entry -> {
            Deque<Long> deque = entry.getValue();
            synchronized (deque) {
                while (!deque.isEmpty() && deque.peekFirst() < limiteInferior) {
                    deque.pollFirst();
                }
                return deque.isEmpty();
            }
        });

        // Se ainda estiver no teto, remove apenas o excedente (10%) sem zerar todos os contadores ativos.
        if (buckets.size() >= MAX_KEYS) {
            int alvoRemocao = Math.max(1, MAX_KEYS / 10);
            int removidos = 0;
            Iterator<Map.Entry<String, Deque<Long>>> it = buckets.entrySet().iterator();
            while (it.hasNext() && removidos < alvoRemocao) {
                it.next();
                it.remove();
                removidos++;
            }
        }
    }
}
