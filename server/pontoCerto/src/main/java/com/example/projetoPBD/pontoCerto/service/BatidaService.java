package com.example.projetoPBD.pontoCerto.service;

import com.example.projetoPBD.pontoCerto.domain.Batida;
import com.example.projetoPBD.pontoCerto.domain.Equipamento;
import com.example.projetoPBD.pontoCerto.domain.Estacao;
import com.example.projetoPBD.pontoCerto.domain.Funcionario;
import com.example.projetoPBD.pontoCerto.domain.FuncionarioUserDetails;
import com.example.projetoPBD.pontoCerto.domain.HistoricoImportacao;
import com.example.projetoPBD.pontoCerto.domain.enums.StatusBatida;
import com.example.projetoPBD.pontoCerto.domain.enums.StatusEquipamento;
import com.example.projetoPBD.pontoCerto.domain.enums.TipoEquipamento;
import com.example.projetoPBD.pontoCerto.domain.enums.TipoOrigem;
import com.example.projetoPBD.pontoCerto.domain.enums.TipoRegistro;
import com.example.projetoPBD.pontoCerto.dto.domaindtos.BatidaDTO;
import com.example.projetoPBD.pontoCerto.repository.BatidaRepository;
import com.example.projetoPBD.pontoCerto.repository.EquipamentoRepository;
import com.example.projetoPBD.pontoCerto.repository.EstacaoRepository;
import com.example.projetoPBD.pontoCerto.repository.FuncionarioRepository;
import com.example.projetoPBD.pontoCerto.repository.HistoricoImportacaoRepository;
import com.example.projetoPBD.pontoCerto.security.SecurityTenantContext;
import com.example.projetoPBD.pontoCerto.service.exceptions.AcessoNegadoException;
import com.example.projetoPBD.pontoCerto.service.exceptions.CredenciaisInvalidasException;
import com.example.projetoPBD.pontoCerto.service.exceptions.EquipamentoInvalidoException;
import com.example.projetoPBD.pontoCerto.service.exceptions.EquipamentoNaoEncontradoException;
import com.example.projetoPBD.pontoCerto.service.exceptions.UsuarioNaoEncontradoException;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import com.example.projetoPBD.pontoCerto.domain.Inconsistencia;
import com.example.projetoPBD.pontoCerto.domain.enums.AcaoTratamento;
import com.example.projetoPBD.pontoCerto.domain.enums.StatusInconsistencia;
import com.example.projetoPBD.pontoCerto.domain.enums.TipoInconsistencia;
import com.example.projetoPBD.pontoCerto.repository.InconsistenciaRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@Service
public class BatidaService {
    private static final Logger log = LoggerFactory.getLogger(BatidaService.class);
    private final BatidaRepository batidaRepository;
    private final FuncionarioRepository funcionarioRepository;
    private final EquipamentoRepository equipamentoReposity;
    private final EstacaoRepository estacaoRepository;
    private final HistoricoImportacaoRepository historicoImportacaoRepository;
    private final InconsistenciaRepository inconsistenciaRepository;
    private final InconsistenciaService inconsistenciaService;
    private final ApuracaoDiariaService apuracaoDiariaService;
    private final CompetenciaLockService competenciaLockService;
    private final SecurityTenantContext securityTenantContext;
    private final PasswordEncoder passwordEncoder;
    private final AnexoStorageService anexoStorageService;

    @PersistenceContext
    private EntityManager entityManager;

    public BatidaService(
            BatidaRepository batidaRepository,
            FuncionarioRepository funcionarioRepository,
            EquipamentoRepository equipamentoReposity,
            EstacaoRepository estacaoRepository,
            HistoricoImportacaoRepository historicoImportacaoRepository,
            InconsistenciaRepository inconsistenciaRepository,
            InconsistenciaService inconsistenciaService,
            ApuracaoDiariaService apuracaoDiariaService,
            CompetenciaLockService competenciaLockService,
            SecurityTenantContext securityTenantContext,
            PasswordEncoder passwordEncoder,
            AnexoStorageService anexoStorageService
    ) {
        this.batidaRepository = batidaRepository;
        this.funcionarioRepository = funcionarioRepository;
        this.equipamentoReposity = equipamentoReposity;
        this.estacaoRepository = estacaoRepository;
        this.historicoImportacaoRepository = historicoImportacaoRepository;
        this.inconsistenciaRepository = inconsistenciaRepository;
        this.inconsistenciaService = inconsistenciaService;
        this.apuracaoDiariaService = apuracaoDiariaService;
        this.competenciaLockService = competenciaLockService;
        this.securityTenantContext = securityTenantContext;
        this.passwordEncoder = passwordEncoder;
        this.anexoStorageService = anexoStorageService;
    }

    /**
     * Registra marcação pela Estação de Ponto (T07):
     * - Autentica o colaborador por usuário e senha a cada batida.
     * - Bloqueia colaborador desligado/inativo.
     * - Grava exclusivamente a hora do servidor (LocalDateTime.now()).
     * - Exige e armazena foto junto à marcação.
     * - Vincula ao equipamento tipo ESTACAO com tipoOrigem = ESTACAO.
     */
    @Transactional
    public BatidaDTO.ComprovanteEstacaoResponse registrarPontoEstacao(
            UUID estacaoId,
            String usuario,
            String senha,
            MultipartFile foto,
            String ipCliente
    ) {
        if (estacaoId == null) {
            throw new IllegalArgumentException("O identificador da estação é obrigatório.");
        }
        if (usuario == null || usuario.isBlank() || senha == null || senha.isBlank()) {
            throw new CredenciaisInvalidasException("Usuário e senha do colaborador são obrigatórios.");
        }
        if (foto == null || foto.isEmpty()) {
            throw new IllegalArgumentException("A captura da foto é obrigatória para registrar o ponto na estação.");
        }

        Estacao estacao = estacaoRepository.findById(estacaoId)
                .orElseThrow(() -> new EquipamentoNaoEncontradoException("Estação de registro não encontrada."));

        if (estacao.getTipo() != TipoEquipamento.ESTACAO || estacao.getStatus() != StatusEquipamento.ATIVO) {
            throw new EquipamentoInvalidoException("A estação selecionada encontra-se inativa ou indisponível para registro.");
        }

        UUID empresaId = estacao.getEmpresa().getId();
        Funcionario funcionario = funcionarioRepository.findByUsuarioAndEmpresaId(usuario.trim(), empresaId)
                .orElseThrow(() -> new CredenciaisInvalidasException("Usuário ou senha inválidos."));

        if (!passwordEncoder.matches(senha, funcionario.getSenhaHash())) {
            throw new CredenciaisInvalidasException("Usuário ou senha inválidos.");
        }

        LocalDateTime agoraServidor = LocalDateTime.now();
        LocalDate hoje = agoraServidor.toLocalDate();

        if (funcionario.getDataDesligamento() != null && !funcionario.getDataDesligamento().isAfter(hoje)) {
            throw new AcessoNegadoException("Colaborador desligado ou inativo. Registro de ponto não permitido.");
        }

        competenciaLockService.validarPeriodoAberto(empresaId, hoje);

        String caminhoFoto = anexoStorageService.salvarFotoBatida(foto);

        List<Batida> batidasHoje = batidaRepository.findAtivasByFuncionarioIdAndDataHoraBetweenOrderByDataHoraAsc(
                funcionario.getId(),
                hoje.atStartOfDay(),
                hoje.atTime(LocalTime.MAX)
        );
        TipoRegistro tipoCalculado = (batidasHoje.size() % 2 == 0) ? TipoRegistro.ENTRADA : TipoRegistro.SAIDA;

        Batida batida = new Batida();
        batida.setFuncionario(funcionario);
        batida.setEquipamento(estacao);
        batida.setDataHora(agoraServidor);
        batida.setTipoRegistro(tipoCalculado);
        batida.setStatusBatida(StatusBatida.NORMAL);
        batida.setTipoOrigem(TipoOrigem.ESTACAO);
        batida.setFotoUrl(caminhoFoto);
        batida.setIpRecebido(ipCliente);
        batida.setNsr(batidaRepository.proximoNsrSequence());

        batida = batidaRepository.saveAndFlush(batida);

        try {
            inconsistenciaService.analisarDiaColaborador(funcionario, hoje);
            apuracaoDiariaService.apurarDia(funcionario, hoje);
        } catch (Exception e) {
            log.error("Falha ao analisar/apurar dia {} após batida na estação para colaborador {}", hoje, funcionario.getId(), e);
        }

        String horaFormatada = agoraServidor.format(DateTimeFormatter.ofPattern("HH:mm"));
        return new BatidaDTO.ComprovanteEstacaoResponse(
                batida.getId(),
                batida.getNsr(),
                estacao.getId(),
                estacao.getIdentificacao(),
                estacao.getEmpresa().getRazaoSocial(),
                estacao.getEmpresa().getCnpj(),
                funcionario.getNomeCompleto(),
                funcionario.getCpf(),
                funcionario.getMatricula(),
                agoraServidor,
                batida.getTipoRegistro(),
                TipoOrigem.ESTACAO,
                true,
                "Ponto registrado com sucesso às " + horaFormatada
        );
    }

    @Transactional(readOnly = true)
    public Resource carregarFotoBatida(UUID batidaId) {
        UUID empresaId = securityTenantContext.empresaAtual();
        Batida batida = batidaRepository.findById(batidaId)
                .orElseThrow(() -> new IllegalArgumentException("Marcação não encontrada."));

        UUID empresaBatida = batida.getFuncionario() != null
                ? batida.getFuncionario().getEmpresa().getId()
                : (batida.getEquipamento() != null ? batida.getEquipamento().getEmpresa().getId() : null);
        if (empresaBatida == null || !empresaBatida.equals(empresaId)) {
            throw new AcessoNegadoException("Acesso negado à marcação de outra empresa.");
        }

        Funcionario logado = obterFuncionarioAutenticadoOpcional();
        if (logado != null && logado.getPerfilAcesso() == com.example.projetoPBD.pontoCerto.domain.enums.PerfilAcesso.GESTOR) {
            Funcionario fBatida = batida.getFuncionario();
            boolean ehProprioOuSubordinado = fBatida != null && (fBatida.getId().equals(logado.getId())
                    || (fBatida.getGestor() != null && fBatida.getGestor().getId().equals(logado.getId())));
            if (!ehProprioOuSubordinado) {
                throw new AcessoNegadoException("Gestor só pode visualizar fotos de colaboradores da sua equipe.");
            }
        }

        Resource recurso = anexoStorageService.carregarComoRecurso(batida.getFotoUrl());
        if (recurso == null) {
            throw new IllegalArgumentException("Foto não encontrada para esta marcação.");
        }
        return recurso;
    }

    @Transactional
    public BatidaDTO.ComprovanteResponse registrarBatida(BatidaDTO.Request dto) {
        Funcionario funcionario = funcionarioRepository.findById(dto.funcionarioId())
                .orElseThrow(() -> new UsuarioNaoEncontradoException("Funcionário não encontrado"));

        Funcionario logado = obterFuncionarioAutenticadoOpcional();
        boolean forcarHoraServidor = false;
        if (logado != null) {
            if (!funcionario.getEmpresa().getId().equals(logado.getEmpresa().getId())) {
                throw new AcessoNegadoException("Não é permitido registrar ponto para colaborador de outra empresa.");
            }
            if (logado.getPerfilAcesso() == com.example.projetoPBD.pontoCerto.domain.enums.PerfilAcesso.COLABORADOR) {
                if (!funcionario.getId().equals(logado.getId())) {
                    throw new AcessoNegadoException("Colaborador não pode registrar ponto em nome de outro colaborador.");
                }
                forcarHoraServidor = true;
            }
        }

        LocalDateTime dataHoraEfetiva = (!forcarHoraServidor && dto.dataHoraOpcional() != null)
                ? dto.dataHoraOpcional()
                : LocalDateTime.now();
        competenciaLockService.validarPeriodoAberto(
                funcionario.getEmpresa().getId(),
                dataHoraEfetiva.toLocalDate()
        );

        Equipamento equipamento = null;
        if (dto.equipamentoId() != null) {
            equipamento = equipamentoReposity.findById(dto.equipamentoId())
                    .orElseThrow(() -> new EquipamentoNaoEncontradoException("Equipamento não encontrado"));
        }

        Batida batida = new Batida();
        batida.setFuncionario(funcionario);
        batida.setEquipamento(equipamento);
        batida.setDataHora(dataHoraEfetiva);
        batida.setTipoRegistro(dto.tipoRegistro());
        batida.setStatusBatida(StatusBatida.NORMAL);
        batida.setTipoOrigem(equipamento != null ? TipoOrigem.ESTACAO : TipoOrigem.APLICATIVO_WEB);
        batida.setLatitudeRecebida(dto.latitude());
        batida.setLongitudeRecebida(dto.longitude());
        batida.setIpRecebido(dto.ip());

        // só por segurança, antes de dar o return faz uma verificação
        // e caso o nsr seja == null ele tenta criar novamente
        if (batida.getNsr() == null) {
            Long novoNsr = batidaRepository.proximoNsrSequence();
            batida.setNsr(novoNsr);
        }
        batida = batidaRepository.saveAndFlush(batida);
        try {
            inconsistenciaService.analisarDiaColaborador(funcionario, batida.getDataHora().toLocalDate());
            apuracaoDiariaService.apurarDia(funcionario, batida.getDataHora().toLocalDate());
        } catch (Exception e) {
            log.error("Falha ao analisar/apurar dia {} após registro de batida para colaborador {}",
                    batida.getDataHora().toLocalDate(), funcionario.getId(), e);
        }

        return new BatidaDTO.ComprovanteResponse(batida);
    }

    @Transactional(readOnly = true)
    public List<BatidaDTO.ComprovanteResponse> listarPorFuncionario(UUID funcionarioId) {
        Funcionario alvo = funcionarioRepository.findById(funcionarioId)
                .orElseThrow(() -> new UsuarioNaoEncontradoException("Funcionário não encontrado"));

        Funcionario logado = obterFuncionarioAutenticadoOpcional();
        if (logado != null) {
            if (!alvo.getEmpresa().getId().equals(logado.getEmpresa().getId())) {
                throw new AcessoNegadoException("Colaborador não pertence à sua empresa.");
            }
            if (logado.getPerfilAcesso() == com.example.projetoPBD.pontoCerto.domain.enums.PerfilAcesso.COLABORADOR
                    && !alvo.getId().equals(logado.getId())) {
                throw new AcessoNegadoException("Colaborador só pode listar as próprias marcações.");
            }
            if (logado.getPerfilAcesso() == com.example.projetoPBD.pontoCerto.domain.enums.PerfilAcesso.GESTOR) {
                boolean ehProprioOuSubordinado = alvo.getId().equals(logado.getId())
                        || (alvo.getGestor() != null && alvo.getGestor().getId().equals(logado.getId()));
                if (!ehProprioOuSubordinado) {
                    throw new AcessoNegadoException("Gestor só pode listar marcações da sua equipe.");
                }
            }
        }

        return batidaRepository.findByFuncionarioIdOrderByDataHoraDesc(funcionarioId)
                .stream()
                .map(BatidaDTO.ComprovanteResponse::new)
                .toList();
    }

    private Funcionario obterFuncionarioAutenticadoOpcional() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof FuncionarioUserDetails fud) {
            return funcionarioRepository.findById(fud.getFuncionario().getId()).orElse(null);
        }
        return null;
    }

    @Transactional(readOnly = true)
    public List<BatidaDTO.BatidaOrfaResponse> listarOrfas(StatusBatida statusFiltro) {
        UUID empresaId = securityTenantContext.empresaAtual();
        StatusBatida status = statusFiltro != null ? statusFiltro : StatusBatida.ORFA_PENDENTE;

        List<Batida> orfas = batidaRepository.findByEmpresaIdAndStatusBatidaOrderByDataHoraDesc(empresaId, status);

        Map<UUID, HistoricoImportacao> historicosMap = new HashMap<>();
        orfas.stream()
                .map(Batida::getArquivoImportadoId)
                .filter(Objects::nonNull)
                .distinct()
                .forEach(arqId -> historicoImportacaoRepository.findById(arqId)
                        .ifPresent(h -> historicosMap.put(arqId, h)));

        return orfas.stream()
                .map(b -> {
                    HistoricoImportacao h = b.getArquivoImportadoId() != null ? historicosMap.get(b.getArquivoImportadoId()) : null;
                    String nomeArq = h != null ? h.getNomeArquivo() : (b.getArquivoImportadoId() != null ? "Arquivo importado" : "Registro direto");
                    String conteudoArq = h != null ? h.getConteudoArquivo() : null;
                    return new BatidaDTO.BatidaOrfaResponse(b, nomeArq, conteudoArq);
                })
                .toList();
    }

    @Transactional
    public BatidaDTO.BatidaOrfaResponse vincularOrfa(UUID batidaId, BatidaDTO.VincularOrfaRequest dto) {
        Batida batida = batidaRepository.findById(batidaId)
                .orElseThrow(() -> new IllegalArgumentException("Batida não encontrada."));

        UUID empresaId = obterEmpresaContextoOuEquipamento(batida);

        if (batida.getEquipamento() != null && !batida.getEquipamento().getEmpresa().getId().equals(empresaId)) {
            throw new AcessoNegadoException("Esta batida não pertence à sua empresa.");
        }

        if (batida.getStatusBatida() != StatusBatida.ORFA_PENDENTE) {
            throw new IllegalArgumentException("Apenas batidas com status ORFA_PENDENTE podem ser vinculadas.");
        }

        if (dto.motivo() == null || dto.motivo().trim().isEmpty()) {
            throw new IllegalArgumentException("O motivo do vínculo é obrigatório.");
        }

        Funcionario funcionario = funcionarioRepository.findById(dto.funcionarioId())
                .orElseThrow(() -> new UsuarioNaoEncontradoException("Colaborador não encontrado."));

        if (empresaId != null && !funcionario.getEmpresa().getId().equals(empresaId)) {
            throw new AcessoNegadoException("O colaborador selecionado não pertence à sua empresa.");
        }

        if (empresaId != null && batida.getDataHora() != null) {
            competenciaLockService.validarPeriodoAberto(empresaId, batida.getDataHora().toLocalDate());
        }

        if (batidaRepository.existsByFuncionarioIdAndDataHora(funcionario.getId(), batida.getDataHora())) {
            throw new IllegalArgumentException("Este colaborador já possui uma batida registrada nesta mesma data e hora.");
        }

        String autorVinculo = obterIdentificacaoUsuarioLogado();

        batida.setFuncionario(funcionario);
        batida.setStatusBatida(StatusBatida.VINCULADA);
        batida.setVinculadoPor(autorVinculo);
        batida.setDataVinculo(LocalDateTime.now());
        batida.setMotivoVinculo(dto.motivo().trim());

        batida = batidaRepository.saveAndFlush(batida);
        Batida batidaSalva = batida;
        inconsistenciaRepository.findFirstByBatidaOrigemIdAndTipoAndSituacao(
                batidaSalva.getId(),
                TipoInconsistencia.ORFA,
                StatusInconsistencia.ABERTA
        ).ifPresent(inc -> {
            inc.setFuncionario(funcionario);
            inc.setSituacao(StatusInconsistencia.RESOLVIDA);
            inc.setAcaoTomada(AcaoTratamento.VINCULAR);
            inc.setTratadoPor(autorVinculo);
            inc.setTratadoEm(batidaSalva.getDataVinculo());
            inc.setMotivoTratamento(batidaSalva.getMotivoVinculo());
            inc.setDetalheResolucao("Batida órfã vinculada ao colaborador " + funcionario.getNomeCompleto() + ".");
            inconsistenciaRepository.saveAndFlush(inc);
        });

        HistoricoImportacao historico = batida.getArquivoImportadoId() != null
                ? historicoImportacaoRepository.findById(batida.getArquivoImportadoId()).orElse(null)
                : null;
        String nomeArquivo = historico != null
                ? historico.getNomeArquivo()
                : (batida.getArquivoImportadoId() != null ? "Arquivo importado" : "Registro direto");
        String conteudoArquivo = historico != null ? historico.getConteudoArquivo() : null;

        return new BatidaDTO.BatidaOrfaResponse(batida, nomeArquivo, conteudoArquivo);
    }

    private UUID obterEmpresaContextoOuEquipamento(Batida batida) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof FuncionarioUserDetails) {
            return securityTenantContext.empresaAtual();
        }
        return batida.getEquipamento() != null ? batida.getEquipamento().getEmpresa().getId() : null;
    }

    private String obterIdentificacaoUsuarioLogado() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof FuncionarioUserDetails userDetails) {
            Funcionario f = userDetails.getFuncionario();
            return f.getNomeCompleto() + " (" + f.getUsuario() + ")";
        }
        return auth != null ? auth.getName() : "RH";
    }
}
