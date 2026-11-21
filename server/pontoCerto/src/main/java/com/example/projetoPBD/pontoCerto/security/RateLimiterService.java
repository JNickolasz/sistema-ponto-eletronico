package com.example.projetoPBD.pontoCerto.security;

import com.example.projetoPBD.pontoCerto.service.exceptions.TooManyRequestsException;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Deque;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;

@Service
public class RateLimiterService {

    private static final int MAX_KEYS = 10_000;
    private final ConcurrentHashMap<String, Deque<Long>> buckets = new ConcurrentHashMap<>();

    /**
     * Valida limite de requisições em janela deslizante (sliding window) com limite de memória.
     */
    public void verificarLimite(String chave, int maxRequisicoes, long janelaSegundos) {
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
                throw new TooManyRequestsException(
                        "Muitas tentativas em sequência. Aguarde alguns instantes antes de tentar novamente."
                );
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
        if (buckets.size() >= MAX_KEYS) {
            buckets.clear();
        }
    }
}
