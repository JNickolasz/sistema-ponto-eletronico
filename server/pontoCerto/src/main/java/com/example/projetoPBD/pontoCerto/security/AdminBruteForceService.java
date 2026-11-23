package com.example.projetoPBD.pontoCerto.security;

import com.example.projetoPBD.pontoCerto.domain.AdminIpBloqueio;
import com.example.projetoPBD.pontoCerto.repository.AdminIpBloqueioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

/**
 * Gerencia tentativas inválidas de X-Admin-Key com persistência em PostgreSQL (admin_ip_bloqueio)
 * e escalonamento progressivo:
 * - 5 erros iniciais -> 5 minutos
 * - Reincidência após desbloqueio -> 2 horas -> 3 dias -> 30 dias -> bloqueio permanente.
 * O desbloqueio manual pode ser feito diretamente no banco (ex.: ativo = false ou DELETE).
 */
@Service
public class AdminBruteForceService {

    public static final int MAX_TENTATIVAS_INICIAIS = 5;
    public static final int NIVEL_PERMANENTE = 5;
    private static final DateTimeFormatter FMT_BR = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final AdminIpBloqueioRepository repository;

    public AdminBruteForceService(AdminIpBloqueioRepository repository) {
        this.repository = repository;
    }

    public record StatusBloqueio(
            boolean bloqueado,
            boolean permanente,
            long segundosRestantes,
            int tentativasRestantes,
            int nivelPenalidade,
            String mensagem
    ) {
        public static StatusBloqueio liberado(int tentativasRestantes) {
            return new StatusBloqueio(false, false, 0L, tentativasRestantes, 0, null);
        }

        public static StatusBloqueio temporario(long segundosRestantes, int nivelPenalidade, LocalDateTime ate) {
            return new StatusBloqueio(
                    true,
                    false,
                    Math.max(1L, segundosRestantes),
                    0,
                    nivelPenalidade,
                    String.format(
                            "IP bloqueado temporariamente (nível %d/4) após tentativas inválidas de chave administrativa. Tente novamente em %s.",
                            nivelPenalidade,
                            ate.format(FMT_BR)
                    )
            );
        }

        public static StatusBloqueio permanente(int nivelPenalidade) {
            return new StatusBloqueio(
                    true,
                    true,
                    0L,
                    0,
                    nivelPenalidade,
                    "IP bloqueado permanentemente após sucessivas tentativas inválidas de chave administrativa. Libere o acesso diretamente na tabela 'admin_ip_bloqueio' do banco de dados."
            );
        }
    }

    @Transactional(readOnly = true)
    public StatusBloqueio verificarSeBloqueado(String ip) {
        return verificarSeBloqueado(ip, LocalDateTime.now());
    }

    @Transactional(readOnly = true)
    public StatusBloqueio verificarSeBloqueado(String ip, LocalDateTime agora) {
        Optional<AdminIpBloqueio> opt = repository.findByIp(normalizarIp(ip));
        if (opt.isEmpty()) {
            return StatusBloqueio.liberado(MAX_TENTATIVAS_INICIAIS);
        }

        AdminIpBloqueio registro = opt.get();
        if (!registro.isAtivo()) {
            return StatusBloqueio.liberado(MAX_TENTATIVAS_INICIAIS);
        }

        if (registro.isBloqueadoPermanente()) {
            return StatusBloqueio.permanente(registro.getNivelPenalidade());
        }

        if (registro.getBloqueadoAte() != null && agora.isBefore(registro.getBloqueadoAte())) {
            long segundos = Duration.between(agora, registro.getBloqueadoAte()).getSeconds();
            return StatusBloqueio.temporario(segundos, registro.getNivelPenalidade(), registro.getBloqueadoAte());
        }

        int nivel = registro.getNivelPenalidade() != null ? registro.getNivelPenalidade() : 0;
        if (nivel > 0) {
            return StatusBloqueio.liberado(1);
        }
        int restantes = Math.max(0, MAX_TENTATIVAS_INICIAIS - (registro.getFalhasConsecutivas() != null ? registro.getFalhasConsecutivas() : 0));
        return StatusBloqueio.liberado(restantes);
    }

    @Transactional
    public StatusBloqueio registrarFalha(String ip) {
        return registrarFalha(ip, LocalDateTime.now());
    }

    @Transactional
    public StatusBloqueio registrarFalha(String ip, LocalDateTime agora) {
        String ipNorm = normalizarIp(ip);
        AdminIpBloqueio registro = repository.findByIp(ipNorm).orElseGet(() -> {
            AdminIpBloqueio novo = new AdminIpBloqueio();
            novo.setIp(ipNorm);
            novo.setCriadoEm(agora);
            return novo;
        });

        // Se foi desbloqueado manualmente no banco (ativo = false), recomeça limpo
        if (!registro.isAtivo()) {
            registro.setAtivo(true);
            registro.setFalhasConsecutivas(0);
            registro.setNivelPenalidade(0);
            registro.setBloqueadoAte(null);
            registro.setBloqueadoPermanente(false);
            registro.setMotivo(null);
        }

        if (registro.isBloqueadoPermanente()) {
            registro.setUltimaTentativaEm(agora);
            registro.setAtualizadoEm(agora);
            repository.save(registro);
            return StatusBloqueio.permanente(registro.getNivelPenalidade());
        }

        if (registro.getBloqueadoAte() != null && agora.isBefore(registro.getBloqueadoAte())) {
            long segundos = Duration.between(agora, registro.getBloqueadoAte()).getSeconds();
            registro.setUltimaTentativaEm(agora);
            registro.setAtualizadoEm(agora);
            repository.save(registro);
            return StatusBloqueio.temporario(segundos, registro.getNivelPenalidade(), registro.getBloqueadoAte());
        }

        int nivelAtual = registro.getNivelPenalidade() != null ? registro.getNivelPenalidade() : 0;
        int falhas = (registro.getFalhasConsecutivas() != null ? registro.getFalhasConsecutivas() : 0) + 1;
        registro.setUltimaTentativaEm(agora);
        registro.setAtualizadoEm(agora);

        if (nivelAtual == 0 && falhas < MAX_TENTATIVAS_INICIAIS) {
            registro.setFalhasConsecutivas(falhas);
            repository.save(registro);
            int restantes = MAX_TENTATIVAS_INICIAIS - falhas;
            return new StatusBloqueio(
                    false,
                    false,
                    0L,
                    restantes,
                    0,
                    String.format("Chave de administrador inválida. Restam %d tentativa(s) antes do bloqueio.", restantes)
            );
        }

        int proximoNivel = Math.min(nivelAtual + 1, NIVEL_PERMANENTE);
        registro.setNivelPenalidade(proximoNivel);
        registro.setFalhasConsecutivas(0);
        registro.setTotalBloqueios((registro.getTotalBloqueios() != null ? registro.getTotalBloqueios() : 0) + 1);

        if (proximoNivel >= NIVEL_PERMANENTE) {
            registro.setBloqueadoPermanente(true);
            registro.setBloqueadoAte(null);
            registro.setMotivo("Bloqueio definitivo após reincidência contínua de chave administrativa inválida.");
            repository.save(registro);
            return StatusBloqueio.permanente(proximoNivel);
        }

        Duration duracao = duracaoParaNivel(proximoNivel);
        LocalDateTime ate = agora.plus(duracao);
        registro.setBloqueadoAte(ate);
        registro.setBloqueadoPermanente(false);
        registro.setMotivo(String.format("Bloqueio temporário nível %d (%s).", proximoNivel, descricaoDuracao(proximoNivel)));
        repository.save(registro);

        return StatusBloqueio.temporario(duracao.getSeconds(), proximoNivel, ate);
    }

    @Transactional
    public void registrarSucesso(String ip) {
        String ipNorm = normalizarIp(ip);
        repository.findByIp(ipNorm).ifPresent(registro -> {
            if (!registro.isBloqueadoPermanente()
                    && (registro.getFalhasConsecutivas() > 0 || registro.getNivelPenalidade() > 0 || registro.getBloqueadoAte() != null)) {
                registro.setFalhasConsecutivas(0);
                registro.setNivelPenalidade(0);
                registro.setBloqueadoAte(null);
                registro.setMotivo(null);
                registro.setAtivo(true);
                registro.setAtualizadoEm(LocalDateTime.now());
                repository.save(registro);
            }
        });
    }

    public static Duration duracaoParaNivel(int nivel) {
        return switch (nivel) {
            case 1 -> Duration.ofMinutes(5);
            case 2 -> Duration.ofHours(2);
            case 3 -> Duration.ofDays(3);
            case 4 -> Duration.ofDays(30);
            default -> Duration.ofDays(36500);
        };
    }

    private String descricaoDuracao(int nivel) {
        return switch (nivel) {
            case 1 -> "5 minutos";
            case 2 -> "2 horas";
            case 3 -> "3 dias";
            case 4 -> "30 dias";
            default -> "permanente";
        };
    }

    private String normalizarIp(String ip) {
        if (ip == null || ip.isBlank()) return "unknown";
        String trimmed = ip.trim();
        return trimmed.length() > 64 ? trimmed.substring(0, 64) : trimmed;
    }
}
