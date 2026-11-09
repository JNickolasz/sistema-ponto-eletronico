package com.example.projetoPBD.pontoCerto.service;

import com.example.projetoPBD.pontoCerto.domain.*;
import com.example.projetoPBD.pontoCerto.domain.enums.*;
import com.example.projetoPBD.pontoCerto.dto.HorarioPrevistoDTO;
import com.example.projetoPBD.pontoCerto.dto.domaindtos.InconsistenciaDTO;
import com.example.projetoPBD.pontoCerto.repository.*;
import com.example.projetoPBD.pontoCerto.security.SecurityTenantContext;
import com.example.projetoPBD.pontoCerto.service.exceptions.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class InconsistenciaService {

    private final InconsistenciaRepository inconsistenciaRepository;
    private final FechamentoPeriodoRepository fechamentoPeriodoRepository;
    private final BatidaRepository batidaRepository;
    private final FuncionarioRepository funcionarioRepository;
    private final EquipamentoRepository equipamentoRepository;
    private final HistoricoImportacaoRepository historicoImportacaoRepository;
    private final FeriadoRepository feriadoRepository;
    private final RegimeTrabalhoService regimeTrabalhoService;
    private final InconsistenciaRegistroAutonomoService registroAutonomoService;
    private final SecurityTenantContext securityTenantContext;

    public InconsistenciaService(
            InconsistenciaRepository inconsistenciaRepository,
            FechamentoPeriodoRepository fechamentoPeriodoRepository,
            BatidaRepository batidaRepository,
            FuncionarioRepository funcionarioRepository,
            EquipamentoRepository equipamentoRepository,
            HistoricoImportacaoRepository historicoImportacaoRepository,
            FeriadoRepository feriadoRepository,
            RegimeTrabalhoService regimeTrabalhoService,
            InconsistenciaRegistroAutonomoService registroAutonomoService,
            SecurityTenantContext securityTenantContext
    ) {
        this.inconsistenciaRepository = inconsistenciaRepository;
        this.fechamentoPeriodoRepository = fechamentoPeriodoRepository;
        this.batidaRepository = batidaRepository;
        this.funcionarioRepository = funcionarioRepository;
        this.equipamentoRepository = equipamentoRepository;
        this.historicoImportacaoRepository = historicoImportacaoRepository;
        this.feriadoRepository = feriadoRepository;
        this.regimeTrabalhoService = regimeTrabalhoService;
        this.registroAutonomoService = registroAutonomoService;
        this.securityTenantContext = securityTenantContext;
    }

    @Transactional
    public void registrarInconsistenciaOrfa(Batida batidaOrfa, String nomeArquivo) {
        if (batidaOrfa == null || batidaOrfa.getEquipamento() == null) {
            return;
        }
        Empresa empresa = batidaOrfa.getEquipamento().getEmpresa();
        String chave = "ORFA_BATIDA_" + batidaOrfa.getId();

        if(inconsistenciaRepository.existsByEmpresaIdAndChaveDeduplicacao(empresa.getId(), chave)) {
            return;
        }

        Inconsistencia inc = new Inconsistencia();
        inc.setEmpresa(empresa);
        inc.setTipo(TipoInconsistencia.ORFA);
        inc.setSituacao(StatusInconsistencia.ABERTA);
        inc.setEquipamento(batidaOrfa.getEquipamento());
        inc.setBatidaOrigemId(batidaOrfa.getId());
        inc.setArquivoImportadoId(batidaOrfa.getArquivoImportadoId());
        inc.setNomeArquivo(nomeArquivo);
        inc.setIdentificadorRecebido(batidaOrfa.getIdentificadorOriginal());
        inc.setDataReferencia(batidaOrfa.getDataHora().toLocalDate());
        inc.setDataHoraOrigem(batidaOrfa.getDataHora());
        inc.setChaveDeduplicacao(chave);
        inc.setDescricaoOrigem(
                "Batida órfã (NSR " + batidaOrfa.getNsr() + ") com identificador '"
                        + batidaOrfa.getIdentificadorOriginal() + "' não vinculado a nenhum colaborador no equipamento '"
                        + batidaOrfa.getEquipamento().getIdentificacao() + "'."
        );

        inconsistenciaRepository.saveAndFlush(inc);
    }

    public void registrarEquipamentoDesconhecido(
            UUID empresaId,
            String nomeArquivo,
            String identificadorEquipamento
    ) {
        try {
            registroAutonomoService.registrarEquipamentoDesconhecidoAutonomo(
                    empresaId,
                    nomeArquivo,
                    identificadorEquipamento
            );
        } catch (Exception ignored) {
        }
    }

    @Transactional
    public void registrarDuplicadaNoMinuto(
            Funcionario funcionario,
            LocalDateTime dataHora,
            Long nsr,
            Equipamento equipamento,
            String nomeArquivo,
            UUID batidaOrigemId,
            UUID batidaSecundariaId
    ) {
        if (funcionario == null || dataHora == null) {
            return;
        }
        UUID empresaId = funcionario.getEmpresa().getId();
        LocalDateTime minuto= dataHora.truncatedTo(ChronoUnit.MINUTES);
        String chave = "DUP_MIN_" + funcionario.getId() + "_" + minuto;

        if (inconsistenciaRepository.existsByEmpresaIdAndChaveDeduplicacao(empresaId, chave)) {
            return;
        }

        Inconsistencia inc = new Inconsistencia();
        inc.setEmpresa(funcionario.getEmpresa());
        inc.setTipo(TipoInconsistencia.DUPLICADA_MINUTO);
        inc.setSituacao(StatusInconsistencia.ABERTA);
        inc.setFuncionario(funcionario);
        inc.setEquipamento(equipamento);
        inc.setBatidaOrigemId(batidaOrigemId);
        inc.setBatidaSecundariaId(batidaSecundariaId);
        inc.setNomeArquivo(nomeArquivo);
        inc.setDataReferencia(dataHora.toLocalDate());
        inc.setDataHoraOrigem(dataHora);
        inc.setChaveDeduplicacao(chave);
        inc.setDescricaoOrigem(
                "Batida duplicada no mesmo minuto (" + minuto.toLocalTime() + ") para o colaborador "
                        + funcionario.getNomeCompleto() + (nsr != null ? " (NSR " + nsr + ")" : "") + "."
        );

        inconsistenciaRepository.saveAndFlush(inc);
    }

    @Transactional
    public void analisarDiaColaborador(Funcionario funcionario, LocalDate data) {
        if (funcionario == null || data == null) {
            return;
        }
        UUID empresaId = funcionario.getEmpresa().getId();
        LocalDateTime inicioDia = data.atStartOfDay();
        LocalDateTime fimDia = data.atTime(LocalTime.MAX);

        List<Batida> batidasAtivas = batidaRepository
                .findAtivasByFuncionarioIdAndDataHoraBetweenOrderByDataHoraAsc(
                        funcionario.getId(),
                        inicioDia,
                        fimDia
                );

        if (batidasAtivas.isEmpty()) {
            limparDiaImparAberto(empresaId, funcionario.getId(), data);
            return;
        }

        for (int i = 1; i < batidasAtivas.size(); i++) {
            Batida anterior = batidasAtivas.get(i - 1);
            Batida atual = batidasAtivas.get(i);
            LocalDateTime minAnt = anterior.getDataHora().truncatedTo(ChronoUnit.MINUTES);
            LocalDateTime minAtu = atual.getDataHora().truncatedTo(ChronoUnit.MINUTES);

            if (minAnt.equals(minAtu)) {
                registrarDuplicadaNoMinuto(
                        funcionario,
                        atual.getDataHora(),
                        atual.getNsr(),
                        atual.getEquipamento(),
                        null,
                        anterior.getId(),
                        atual.getId()
                );
            }
        }

        // 2. SEQUENCIA_INVALIDA
        Batida primeira = batidasAtivas.get(0);
        if (primeira.getTipoRegistro() == TipoRegistro.SAIDA) {
            String chaveSeq = "SEQ_INV_" + funcionario.getId() + "_" + primeira.getId();
            if (!inconsistenciaRepository.existsByEmpresaIdAndChaveDeduplicacao(empresaId, chaveSeq)) {
                Inconsistencia inc = new Inconsistencia();
                inc.setEmpresa(funcionario.getEmpresa());
                inc.setTipo(TipoInconsistencia.SEQUENCIA_INVALIDA);
                inc.setSituacao(StatusInconsistencia.ABERTA);
                inc.setFuncionario(funcionario);
                inc.setEquipamento(primeira.getEquipamento());
                inc.setBatidaOrigemId(primeira.getId());
                inc.setDataReferencia(data);
                inc.setDataHoraOrigem(primeira.getDataHora());
                inc.setChaveDeduplicacao(chaveSeq);
                inc.setDescricaoOrigem(
                        "Sequência inválida: primeira marcação do dia (" + primeira.getDataHora().toLocalTime()
                                + ") registrada como SAÍDA sem ENTRADA anterior."
                );
                inconsistenciaRepository.saveAndFlush(inc);
            }
        }

        for (int i = 1; i < batidasAtivas.size(); i++) {
            Batida anterior = batidasAtivas.get(i - 1);
            Batida atual = batidasAtivas.get(i);
            boolean mesmoMinuto = anterior.getDataHora().truncatedTo(ChronoUnit.MINUTES)
                    .equals(atual.getDataHora().truncatedTo(ChronoUnit.MINUTES));

            if (!mesmoMinuto && anterior.getTipoRegistro() == atual.getTipoRegistro()) {
                String chaveSeq = "SEQ_INV_" + funcionario.getId() + "_" + atual.getId();
                if (!inconsistenciaRepository.existsByEmpresaIdAndChaveDeduplicacao(empresaId, chaveSeq)) {
                    Inconsistencia inc = new Inconsistencia();
                    inc.setEmpresa(funcionario.getEmpresa());
                    inc.setTipo(TipoInconsistencia.SEQUENCIA_INVALIDA);
                    inc.setSituacao(StatusInconsistencia.ABERTA);
                    inc.setFuncionario(funcionario);
                    inc.setEquipamento(atual.getEquipamento());
                    inc.setBatidaOrigemId(atual.getId());
                    inc.setBatidaSecundariaId(anterior.getId());
                    inc.setDataReferencia(data);
                    inc.setDataHoraOrigem(atual.getDataHora());
                    inc.setChaveDeduplicacao(chaveSeq);
                    inc.setDescricaoOrigem(
                            "Sequência inválida: duas marcações seguidas do tipo " + atual.getTipoRegistro()
                                    + " (" + anterior.getDataHora().toLocalTime() + " e " + atual.getDataHora().toLocalTime() + ")."
                    );
                    inconsistenciaRepository.saveAndFlush(inc);
                }
            }
        }

        // 3. DIA_IMPAR
        if (batidasAtivas.size() % 2 != 0) {
            Batida ultima = batidasAtivas.get(batidasAtivas.size() - 1);
            String chaveImpar = "DIA_IMPAR_" + funcionario.getId() + "_" + data;
            if (!inconsistenciaRepository.existsByEmpresaIdAndChaveDeduplicacaoAndSituacao(
                    empresaId, chaveImpar, StatusInconsistencia.ABERTA
            ) && !inconsistenciaRepository.existsByEmpresaIdAndChaveDeduplicacao(empresaId, chaveImpar)) {
                Inconsistencia inc = new Inconsistencia();
                inc.setEmpresa(funcionario.getEmpresa());
                inc.setTipo(TipoInconsistencia.DIA_IMPAR);
                inc.setSituacao(StatusInconsistencia.ABERTA);
                inc.setFuncionario(funcionario);
                inc.setEquipamento(ultima.getEquipamento());
                inc.setBatidaOrigemId(ultima.getId());
                inc.setDataReferencia(data);
                inc.setDataHoraOrigem(ultima.getDataHora());
                inc.setChaveDeduplicacao(chaveImpar);
                inc.setDescricaoOrigem(
                        "Dia com quantidade ímpar de batidas (" + batidasAtivas.size()
                                + " marcação(ões) ativa(s)) — falta marcação para fechar o par do dia."
                );
                inconsistenciaRepository.saveAndFlush(inc);
            }
        } else {
            limparDiaImparAberto(empresaId, funcionario.getId(), data);
        }

        // 4. FORA_DA_JORNADA
        String motivoForaJornada = verificarMotivoForaDaJornada(funcionario, data);
        if (motivoForaJornada != null) {
            Batida primeiraBatida = batidasAtivas.get(0);
            String chaveFora = "FORA_JORNADA_" + funcionario.getId() + "_" + data;
            if (!inconsistenciaRepository.existsByEmpresaIdAndChaveDeduplicacao(empresaId, chaveFora)) {
                Inconsistencia inc = new Inconsistencia();
                inc.setEmpresa(funcionario.getEmpresa());
                inc.setTipo(TipoInconsistencia.FORA_DA_JORNADA);
                inc.setSituacao(StatusInconsistencia.ABERTA);
                inc.setFuncionario(funcionario);
                inc.setEquipamento(primeiraBatida.getEquipamento());
                inc.setBatidaOrigemId(primeiraBatida.getId());
                inc.setDataReferencia(data);
                inc.setDataHoraOrigem(primeiraBatida.getDataHora());
                inc.setChaveDeduplicacao(chaveFora);
                inc.setDescricaoOrigem(
                        "Marcação realizada fora da jornada prevista em " + data + ": " + motivoForaJornada
                );
                inconsistenciaRepository.saveAndFlush(inc);
            }
        }
    }

    private void limparDiaImparAberto(UUID empresaId, UUID funcionarioId, LocalDate data) {
        List<Inconsistencia> abertas = inconsistenciaRepository
                .findByEmpresaIdAndFuncionarioIdAndDataReferenciaAndTipoAndSituacao(
                        empresaId,
                        funcionarioId,
                        data,
                        TipoInconsistencia.DIA_IMPAR,
                        StatusInconsistencia.ABERTA
                );
        if (!abertas.isEmpty()) {
            inconsistenciaRepository.deleteAll(abertas);
            inconsistenciaRepository.flush();
        }
    }

    private String verificarMotivoForaDaJornada(Funcionario funcionario, LocalDate data) {
        Empresa empresa = funcionario.getEmpresa();
        String uf = funcionario.getLocalDeTrabalho() != null ? funcionario.getLocalDeTrabalho().getUf() : null;
        String municipio = funcionario.getLocalDeTrabalho() != null ? funcionario.getLocalDeTrabalho().getMunicipio() : null;

        List<Feriado> feriados = feriadoRepository.buscarCalendarioAnual(
                data.getYear(),
                uf != null ? uf : "",
                municipio != null ? municipio : "",
                empresa.getId()
        );
        Optional<Feriado> feriadoNaData = feriados.stream()
                .filter(f -> data.equals(f.getData()))
                .findFirst();

        if (feriadoNaData.isPresent()) {
            return "Feriado ativo (" + feriadoNaData.get().getDescricao() + ").";
        }

        HorarioPrevistoDTO.Response horario = regimeTrabalhoService.obterHorarioPrevisto(funcionario.getId(), data);
        if (horario != null && horario.tipoRegime() != null && !horario.diaTrabalho()) {
            return horario.mensagem() != null ? horario.mensagem() : "Dia de folga no regime de trabalho.";
        }

        return null;
    }

    @Transactional(readOnly = true)
    public List<InconsistenciaDTO.Response> listar(
            TipoInconsistencia tipo,
            String situacaoStr,
            UUID funcionarioId,
            LocalDate dataInicio,
            LocalDate dataFim,
            String mesReferencia
    ) {
        UUID empresaId = obterEmpresaAtualSegura();
        LocalDate[] intervalo = resolverIntervaloDatas(dataInicio, dataFim, mesReferencia);
        LocalDate inicioFiltro = intervalo[0];
        LocalDate fimFiltro = intervalo[1];

        StatusInconsistencia situacaoFiltro = resolverSituacaoFiltro(situacaoStr);

        return inconsistenciaRepository.findByEmpresaIdOrderByDataHoraOrigemDesc(empresaId)
                .stream()
                .filter(i -> tipo == null || i.getTipo() == tipo)
                .filter(i -> situacaoFiltro == null || i.getSituacao() == situacaoFiltro)
                .filter(i -> funcionarioId == null || (i.getFuncionario() != null && funcionarioId.equals(i.getFuncionario().getId())))
                .filter(i -> inicioFiltro == null || (i.getDataReferencia() != null && !i.getDataReferencia().isBefore(inicioFiltro)))
                .filter(i -> fimFiltro == null || (i.getDataReferencia() != null && !i.getDataReferencia().isAfter(fimFiltro)))
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public InconsistenciaDTO.PainelContagemResponse obterPainelContagem(
            LocalDate dataInicio,
            LocalDate dataFim,
            String mesReferencia
    ) {
        UUID empresaId = obterEmpresaAtualSegura();
        LocalDate[] intervalo = resolverIntervaloDatas(dataInicio, dataFim, mesReferencia);
        LocalDate inicioFiltro = intervalo[0];
        LocalDate fimFiltro = intervalo[1];

        List<Inconsistencia> todas = inconsistenciaRepository.findByEmpresaIdOrderByDataHoraOrigemDesc(empresaId)
                .stream()
                .filter(i -> inicioFiltro == null || (i.getDataReferencia() != null && !i.getDataReferencia().isBefore(inicioFiltro)))
                .filter(i -> fimFiltro == null || (i.getDataReferencia() != null && !i.getDataReferencia().isAfter(fimFiltro)))
                .toList();

        long totalAbertas = todas.stream()
                .filter(i -> i.getSituacao() == StatusInconsistencia.ABERTA)
                .count();

        long totalResolvidas = todas.stream()
                .filter(i -> i.getSituacao() == StatusInconsistencia.RESOLVIDA)
                .count();

        Map<TipoInconsistencia, Long> abertasPorTipo = new EnumMap<>(TipoInconsistencia.class);
        for (TipoInconsistencia t : TipoInconsistencia.values()) {
            long qtd = todas.stream()
                    .filter(i -> i.getSituacao() == StatusInconsistencia.ABERTA && i.getTipo() == t)
                    .count();
            abertasPorTipo.put(t, qtd);
        }

        return new InconsistenciaDTO.PainelContagemResponse(
                totalAbertas,
                totalResolvidas,
                abertasPorTipo
        );
    }

    @Transactional
    public InconsistenciaDTO.Response tratar(UUID inconsistenciaId, InconsistenciaDTO.TratarRequest dto) {
        if (dto == null || dto.acao() == null) {
            throw new IllegalArgumentException("A ação de tratamento é obrigatória.");
        }
        if (dto.motivo() == null || dto.motivo().trim().isEmpty()) {
            throw new IllegalArgumentException("O motivo do tratamento é obrigatório.");
        }

        Inconsistencia inc = inconsistenciaRepository.findById(inconsistenciaId)
                .orElseThrow(() -> new IllegalArgumentException("Inconsistência não encontrada."));

        UUID empresaId = obterEmpresaContextoOuInconsistencia(inc);
        if (!inc.getEmpresa().getId().equals(empresaId)) {
            throw new AcessoNegadoException("Esta inconsistência não pertence à sua empresa.");
        }

        if (inc.getSituacao() == StatusInconsistencia.RESOLVIDA) {
            throw new IllegalArgumentException("Esta inconsistência já foi resolvida.");
        }

        String autor = obterIdentificacaoUsuarioLogado();
        UUID autorId = obterIdUsuarioLogado();
        String motivoLimpo = dto.motivo().trim();
        String detalheResolucao;

        switch (dto.acao()) {
            case VINCULAR -> detalheResolucao = executarAcaoVincular(inc, dto, empresaId, autor, motivoLimpo);
            case INCLUIR_MARCACAO_AJUSTE -> detalheResolucao = executarAcaoIncluirAjuste(inc, dto);
            case DESCARTAR_DUPLICADA -> detalheResolucao = executarAcaoDescartarDuplicada(inc);
            case ABONAR_DIA -> detalheResolucao = executarAcaoAbonarDia(inc);
            default -> throw new IllegalArgumentException("Ação de tratamento inválida.");
        }

        inc.setSituacao(StatusInconsistencia.RESOLVIDA);
        inc.setAcaoTomada(dto.acao());
        inc.setTratadoPor(autor);
        inc.setTratadoPorId(autorId);
        inc.setTratadoEm(LocalDateTime.now());
        inc.setMotivoTratamento(motivoLimpo);
        inc.setDetalheResolucao(detalheResolucao);

        Inconsistencia salva = inconsistenciaRepository.saveAndFlush(inc);
        return toResponse(salva);
    }

    private String executarAcaoVincular(
            Inconsistencia inc,
            InconsistenciaDTO.TratarRequest dto,
            UUID empresaId,
            String autor,
            String motivoLimpo
    ) {
        if (inc.getTipo() == TipoInconsistencia.EQUIPAMENTO_DESCONHECIDO) {
            if (dto.equipamentoId() == null) {
                throw new IllegalArgumentException("Para vincular equipamento desconhecido, informe o equipamentoId.");
            }
            Equipamento equipamento = equipamentoRepository.findById(dto.equipamentoId())
                    .orElseThrow(() -> new EquipamentoNaoEncontradoException("Equipamento não encontrado."));
            if (!equipamento.getEmpresa().getId().equals(empresaId)) {
                throw new AcessoNegadoException("O equipamento selecionado não pertence à sua empresa.");
            }
            inc.setEquipamento(equipamento);
            return "Arquivo '" + inc.getNomeArquivo() + "' vinculado ao equipamento '" + equipamento.getIdentificacao() + "'.";
        }

        if (dto.funcionarioId() == null) {
            throw new IllegalArgumentException("Para a ação VINCULAR, o ID do colaborador é obrigatório.");
        }

        Funcionario funcionario = funcionarioRepository.findById(dto.funcionarioId())
                .orElseThrow(() -> new UsuarioNaoEncontradoException("Colaborador não encontrado."));

        if (!funcionario.getEmpresa().getId().equals(empresaId)) {
            throw new AcessoNegadoException("O colaborador selecionado não pertence à sua empresa.");
        }

        inc.setFuncionario(funcionario);

        if (inc.getBatidaOrigemId() != null) {
            Batida batida = batidaRepository.findById(inc.getBatidaOrigemId()).orElse(null);
            if (batida != null && batida.getStatusBatida() == StatusBatida.ORFA_PENDENTE) {
                if (batidaRepository.existsByFuncionarioIdAndDataHora(funcionario.getId(), batida.getDataHora())) {
                    throw new IllegalArgumentException("Este colaborador já possui uma batida registrada nesta mesma data e hora.");
                }
                batida.setFuncionario(funcionario);
                batida.setStatusBatida(StatusBatida.VINCULADA);
                batida.setVinculadoPor(autor);
                batida.setDataVinculo(LocalDateTime.now());
                batida.setMotivoVinculo(motivoLimpo);
                batidaRepository.saveAndFlush(batida);
            }
        }

        return "Batida vinculada ao colaborador " + funcionario.getNomeCompleto() + " (Matrícula: " + funcionario.getMatricula() + ").";
    }

    private String executarAcaoIncluirAjuste(Inconsistencia inc, InconsistenciaDTO.TratarRequest dto) {
        Funcionario funcionario = inc.getFuncionario();
        if (funcionario == null && dto.funcionarioId() != null) {
            funcionario = funcionarioRepository.findById(dto.funcionarioId())
                    .orElseThrow(() -> new UsuarioNaoEncontradoException("Colaborador não encontrado."));
            inc.setFuncionario(funcionario);
        }
        if (funcionario == null) {
            throw new IllegalArgumentException("Não há colaborador associado para incluir marcação de ajuste.");
        }

        LocalTime horarioAjuste = dto.horarioAjuste() != null ? dto.horarioAjuste() : LocalTime.of(18, 0);
        TipoRegistro tipoAjuste = dto.tipoRegistroAjuste() != null ? dto.tipoRegistroAjuste() : TipoRegistro.SAIDA;
        LocalDateTime dataHoraAjuste = LocalDateTime.of(inc.getDataReferencia(), horarioAjuste);

        Batida ajuste = new Batida();
        ajuste.setNsr(batidaRepository.proximoNsrSequence());
        ajuste.setFuncionario(funcionario);
        ajuste.setEquipamento(inc.getEquipamento());
        ajuste.setDataHora(dataHoraAjuste);
        ajuste.setTipoRegistro(tipoAjuste);
        ajuste.setTipoOrigem(TipoOrigem.AJUSTE_MANUAL);
        ajuste.setStatusBatida(StatusBatida.NORMAL);

        ajuste = batidaRepository.saveAndFlush(ajuste);
        inc.setBatidaSecundariaId(ajuste.getId());

        return "Marcação de ajuste (" + tipoAjuste + " às " + horarioAjuste + ", NSR " + ajuste.getNsr()
                + ") incluída para o colaborador " + funcionario.getNomeCompleto() + ".";
    }

    private String executarAcaoDescartarDuplicada(Inconsistencia inc) {
        UUID idParaDescartar = inc.getBatidaSecundariaId() != null
                ? inc.getBatidaSecundariaId()
                : inc.getBatidaOrigemId();

        if (idParaDescartar != null) {
            Batida batida = batidaRepository.findById(idParaDescartar).orElse(null);
            if (batida != null) {
                batida.setStatusBatida(StatusBatida.DESCARTADA);
                batidaRepository.saveAndFlush(batida);
                return "Batida NSR " + batida.getNsr() + " (" + batida.getDataHora() + ") marcada como DESCARTADA.";
            }
        }
        return "Marcação duplicada/indevida descartada da apuração.";
    }

    private String executarAcaoAbonarDia(Inconsistencia inc) {
        String nomeFunc = inc.getFuncionario() != null ? inc.getFuncionario().getNomeCompleto() : "colaborador";
        return "Dia " + inc.getDataReferencia() + " abonado para " + nomeFunc + ".";
    }

    @Transactional
    public InconsistenciaDTO.FechamentoResponse fecharPeriodo(InconsistenciaDTO.FechamentoRequest dto) {
        if (dto == null || dto.mesReferencia() == null || dto.mesReferencia().trim().isEmpty()) {
            throw new IllegalArgumentException("O mês de referência (YYYY-MM) é obrigatório.");
        }

        UUID empresaId = obterEmpresaAtualSegura();
        String mesRef = dto.mesReferencia().trim();
        LocalDate[] intervalo = resolverIntervaloDatas(dto.dataInicio(), dto.dataFim(), mesRef);
        LocalDate inicio = intervalo[0];
        LocalDate fim = intervalo[1];

        if (inicio == null || fim == null) {
            throw new IllegalArgumentException("Não foi possível determinar o período de fechamento. Informe mesReferencia no formato YYYY-MM.");
        }

        List<Inconsistencia> abertasNoPeriodo = inconsistenciaRepository
                .findByEmpresaIdAndSituacaoAndDataReferenciaBetweenOrderByDataHoraOrigemDesc(
                        empresaId,
                        StatusInconsistencia.ABERTA,
                        inicio,
                        fim
                );

        int qtdAbertas = abertasNoPeriodo.size();
        boolean confirmar = Boolean.TRUE.equals(dto.confirmarComPendencias());

        String resumoPorTipo = abertasNoPeriodo.stream()
                .collect(Collectors.groupingBy(Inconsistencia::getTipo, Collectors.counting()))
                .entrySet()
                .stream()
                .map(e -> e.getKey().name() + ": " + e.getValue())
                .collect(Collectors.joining(", "));

        if (qtdAbertas > 0 && !confirmar) {
            throw new FechamentoComPendenciasException(
                    "Não é possível fechar o período " + mesRef + " (" + inicio + " a " + fim + "): existem "
                            + qtdAbertas + " inconsistência(s) aberta(s) na fila [" + resumoPorTipo
                            + "]. Trate as pendências antes do fechamento ou confirme o fechamento com ressalva."
            );
        }

        Empresa empresa = obterEmpresaEntidade(empresaId);
        String autor = obterIdentificacaoUsuarioLogado();
        UUID autorId = obterIdUsuarioLogado();

        FechamentoPeriodo fechamento = new FechamentoPeriodo();
        fechamento.setEmpresa(empresa);
        fechamento.setMesReferencia(mesRef);
        fechamento.setDataInicio(inicio);
        fechamento.setDataFim(fim);
        fechamento.setDataFechamento(LocalDateTime.now());
        fechamento.setFechadoPor(autor);
        fechamento.setFechadoPorId(autorId);
        fechamento.setPendenciasAbertasNoFechamento(qtdAbertas);

        if (qtdAbertas > 0) {
            fechamento.setStatus(StatusFechamentoPeriodo.CONCLUIDO_COM_PENDENCIAS);
            fechamento.setAvisoPendencias(
                    "Fechamento realizado com " + qtdAbertas + " pendência(s) em aberto no período (" + resumoPorTipo + ")."
            );
        } else {
            fechamento.setStatus(StatusFechamentoPeriodo.CONCLUIDO);
            fechamento.setAvisoPendencias("Período fechado sem pendências abertas.");
        }

        FechamentoPeriodo salvo = fechamentoPeriodoRepository.saveAndFlush(fechamento);
        return toFechamentoResponse(salvo);
    }

    @Transactional(readOnly = true)
    public List<InconsistenciaDTO.FechamentoResponse> listarFechamentos() {
        UUID empresaId = obterEmpresaAtualSegura();
        return fechamentoPeriodoRepository.findByEmpresaIdOrderByDataFechamentoDesc(empresaId)
                .stream()
                .map(this::toFechamentoResponse)
                .toList();
    }

    private UUID obterEmpresaAtualSegura() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof FuncionarioUserDetails) {
            return securityTenantContext.empresaAtual();
        }
        return funcionarioRepository.findAll().stream()
                .map(f -> f.getEmpresa().getId())
                .findFirst()
                .orElseThrow(() -> new AcessoNegadoException("Usuário não autenticado!"));
    }

    private StatusInconsistencia resolverSituacaoFiltro(String situacaoStr) {
        if (situacaoStr == null || situacaoStr.isBlank() || "TODAS".equalsIgnoreCase(situacaoStr.trim())) {
            return null;
        }
        String s = situacaoStr.trim().toUpperCase();
        if ("PENDENTE".equals(s) || "ABERTA".equals(s) || "ABERTAS".equals(s)) {
            return StatusInconsistencia.ABERTA;
        }
        if ("RESOLVIDA".equals(s) || "RESOLVIDAS".equals(s) || "HISTORICO".equals(s)) {
            return StatusInconsistencia.RESOLVIDA;
        }
        return StatusInconsistencia.valueOf(s);
    }

    private LocalDate[] resolverIntervaloDatas(LocalDate dataInicio, LocalDate dataFim, String mesReferencia) {
        LocalDate inicio = dataInicio;
        LocalDate fim = dataFim;

        if ((inicio == null || fim == null) && mesReferencia != null && !mesReferencia.isBlank()) {
            try {
                YearMonth ym = YearMonth.parse(mesReferencia.trim());
                if (inicio == null) inicio = ym.atDay(1);
                if (fim == null) fim = ym.atEndOfMonth();
            } catch (Exception ignored) {
            }
        }
        return new LocalDate[]{inicio, fim};
    }

    private UUID obterEmpresaContextoOuInconsistencia(Inconsistencia inc) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof FuncionarioUserDetails) {
            return securityTenantContext.empresaAtual();
        }
        return inc.getEmpresa().getId();
    }

    private Empresa obterEmpresaEntidade(UUID empresaId) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof FuncionarioUserDetails userDetails) {
            return userDetails.getFuncionario().getEmpresa();
        }
        return funcionarioRepository.findByEmpresaId(empresaId).stream()
                .map(Funcionario::getEmpresa)
                .findFirst()
                .orElseGet(() -> {
                    Empresa e = new Empresa();
                    e.setId(empresaId);
                    return e;
                });
    }

    private String obterIdentificacaoUsuarioLogado() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof FuncionarioUserDetails userDetails) {
            Funcionario f = userDetails.getFuncionario();
            return f.getNomeCompleto() + " (" + f.getUsuario() + ")";
        }
        return auth != null ? auth.getName() : "RH";
    }

    private UUID obterIdUsuarioLogado() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof FuncionarioUserDetails userDetails) {
            return userDetails.getFuncionario().getId();
        }
        return null;
    }

    private InconsistenciaDTO.Response toResponse(Inconsistencia i) {
        Funcionario f = i.getFuncionario();
        Equipamento eq = i.getEquipamento();
        return new InconsistenciaDTO.Response(
                i.getId(),
                i.getTipo(),
                i.getSituacao(),
                i.getDataReferencia(),
                i.getDataHoraOrigem(),
                i.getDescricaoOrigem(),
                f != null ? f.getId() : null,
                f != null ? f.getNomeCompleto() : null,
                f != null ? f.getMatricula() : null,
                eq != null ? eq.getId() : null,
                eq != null ? eq.getIdentificacao() : null,
                eq != null ? eq.getCodigo() : null,
                i.getIdentificadorRecebido(),
                i.getNomeArquivo(),
                i.getBatidaOrigemId(),
                i.getBatidaSecundariaId(),
                i.getAcaoTomada(),
                i.getTratadoPor(),
                i.getTratadoEm(),
                i.getMotivoTratamento(),
                i.getDetalheResolucao()
        );
    }

    private InconsistenciaDTO.FechamentoResponse toFechamentoResponse(FechamentoPeriodo fp) {
        return new InconsistenciaDTO.FechamentoResponse(
                fp.getId(),
                fp.getMesReferencia(),
                fp.getDataInicio(),
                fp.getDataFim(),
                fp.getDataFechamento(),
                fp.getFechadoPor(),
                fp.getStatus(),
                fp.getPendenciasAbertasNoFechamento() != null ? fp.getPendenciasAbertasNoFechamento() : 0,
                fp.getAvisoPendencias(),
                fp.getStatus() == StatusFechamentoPeriodo.CONCLUIDO_COM_PENDENCIAS
        );
    }
}


