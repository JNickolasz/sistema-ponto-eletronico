package com.example.projetoPBD.pontoCerto.service;

import com.example.projetoPBD.pontoCerto.domain.*;
import com.example.projetoPBD.pontoCerto.domain.enums.*;
import com.example.projetoPBD.pontoCerto.dto.domaindtos.BancoHorasDTO;
import com.example.projetoPBD.pontoCerto.repository.*;
import com.example.projetoPBD.pontoCerto.security.SecurityTenantContext;
import com.example.projetoPBD.pontoCerto.service.exceptions.AcessoNegadoException;
import com.example.projetoPBD.pontoCerto.service.exceptions.PeriodoFechadoException;
import com.example.projetoPBD.pontoCerto.service.exceptions.UsuarioNaoEncontradoException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class BancoHorasService {

    private static final DateTimeFormatter MES_REF_FMT = DateTimeFormatter.ofPattern("yyyy-MM");

    private final MovimentacaoBancoHorasRepository movimentacaoRepository;
    private final FechamentoBancoHorasRepository fechamentoBancoHorasRepository;
    private final FechamentoPeriodoRepository fechamentoPeriodoRepository;
    private final FuncionarioRepository funcionarioRepository;
    private final SecurityTenantContext securityTenantContext;

    public BancoHorasService(
            MovimentacaoBancoHorasRepository movimentacaoRepository,
            FechamentoBancoHorasRepository fechamentoBancoHorasRepository,
            FechamentoPeriodoRepository fechamentoPeriodoRepository,
            FuncionarioRepository funcionarioRepository,
            SecurityTenantContext securityTenantContext
    ) {
        this.movimentacaoRepository = movimentacaoRepository;
        this.fechamentoBancoHorasRepository = fechamentoBancoHorasRepository;
        this.fechamentoPeriodoRepository = fechamentoPeriodoRepository;
        this.funcionarioRepository = funcionarioRepository;
        this.securityTenantContext = securityTenantContext;
    }

    /**
     * Derivação automática e idempotente chamada pelo motor de apuração (ApuracaoDiariaService).
     * Nenhum endpoint externo pode criar CREDITO_APURACAO ou DEBITO_APURACAO manualmente.
     */
    @Transactional
    public void derivarMovimentacoesDaApuracao(ApuracaoDiaria apuracao) {
        if (apuracao == null || apuracao.getId() == null || apuracao.getFuncionario() == null) {
            return;
        }

        Funcionario funcionario = apuracao.getFuncionario();
        LocalDate dataRef = apuracao.getDataReferencia();
        UUID empresaId = funcionario.getEmpresa().getId();

        // Imutabilidade: se o período estiver fechado ou congelado, não toca nas movimentações
        if (estaPeriodoFechado(empresaId, funcionario.getId(), dataRef)) {
            return;
        }

        // Se o dia voltou para PENDENTE_INCONSISTENCIA, remove movimentações automáticas anteriores daquele dia
        if (apuracao.getStatus() == StatusApuracaoDiaria.PENDENTE_INCONSISTENCIA) {
            removerMovimentacaoAutomaticaSeAberta(apuracao.getId(), TipoMovimentacaoBancoHoras.CREDITO_APURACAO);
            removerMovimentacaoAutomaticaSeAberta(apuracao.getId(), TipoMovimentacaoBancoHoras.DEBITO_APURACAO);
            return;
        }

        int credito = apuracao.getMinutosHoraExtraTotal() != null ? apuracao.getMinutosHoraExtraTotal() : 0;
        int atraso = apuracao.getMinutosAtraso() != null ? apuracao.getMinutosAtraso() : 0;
        int saidaAntecipada = apuracao.getMinutosSaidaAntecipada() != null ? apuracao.getMinutosSaidaAntecipada() : 0;
        int falta = apuracao.getMinutosFalta() != null ? apuracao.getMinutosFalta() : 0;
        int debito = atraso + saidaAntecipada + falta;

        // 1. Processa CREDITO_APURACAO (idempotente por apuracaoDiariaId + tipo)
        if (credito > 0) {
            MovimentacaoBancoHoras movCredito = movimentacaoRepository
                    .findByApuracaoDiariaIdAndTipo(apuracao.getId(), TipoMovimentacaoBancoHoras.CREDITO_APURACAO)
                    .orElseGet(MovimentacaoBancoHoras::new);

            if (!movCredito.isPeriodoFechado()) {
                movCredito.setFuncionario(funcionario);
                movCredito.setDataReferencia(dataRef);
                movCredito.setTipo(TipoMovimentacaoBancoHoras.CREDITO_APURACAO);
                movCredito.setMinutos(credito); // Positivo (+)
                movCredito.setApuracaoDiariaId(apuracao.getId());
                movCredito.setDescricao("Crédito diário apurado (+" + formatarMinutos(credito) + " excedentes)");
                movCredito.setPeriodoFechado(false);
                movCredito.setRegistradoPor("SISTEMA (APURAÇÃO)");
                movCredito.setRegistradoEm(LocalDateTime.now());
                movimentacaoRepository.saveAndFlush(movCredito);
            }
        } else {
            removerMovimentacaoAutomaticaSeAberta(apuracao.getId(), TipoMovimentacaoBancoHoras.CREDITO_APURACAO);
        }

        // 2. Processa DEBITO_APURACAO (idempotente por apuracaoDiariaId + tipo)
        if (debito > 0) {
            MovimentacaoBancoHoras movDebito = movimentacaoRepository
                    .findByApuracaoDiariaIdAndTipo(apuracao.getId(), TipoMovimentacaoBancoHoras.DEBITO_APURACAO)
                    .orElseGet(MovimentacaoBancoHoras::new);

            if (!movDebito.isPeriodoFechado()) {
                String detalheDebito = falta > 0
                        ? "Débito por falta não abonada (-" + formatarMinutos(debito) + ")"
                        : "Débito diário apurado por atraso/saída antecipada (-" + formatarMinutos(debito) + ")";

                movDebito.setFuncionario(funcionario);
                movDebito.setDataReferencia(dataRef);
                movDebito.setTipo(TipoMovimentacaoBancoHoras.DEBITO_APURACAO);
                movDebito.setMinutos(-debito); // Negativo (-)
                movDebito.setApuracaoDiariaId(apuracao.getId());
                movDebito.setDescricao(detalheDebito);
                movDebito.setPeriodoFechado(false);
                movDebito.setRegistradoPor("SISTEMA (APURAÇÃO)");
                movDebito.setRegistradoEm(LocalDateTime.now());
                movimentacaoRepository.saveAndFlush(movDebito);
            }
        } else {
            removerMovimentacaoAutomaticaSeAberta(apuracao.getId(), TipoMovimentacaoBancoHoras.DEBITO_APURACAO);
        }
    }

    /**
     * Registra uma compensação de horas (abatimento do saldo acumulado com motivo obrigatório).
     */
    @Transactional
    public BancoHorasDTO.ItemExtratoResponse registrarCompensacao(BancoHorasDTO.RegistrarCompensacaoRequest req) {
        return registrarCompensacao(obterFuncionarioLogadoOpcional(), req);
    }

    @Transactional
    public BancoHorasDTO.ItemExtratoResponse registrarCompensacao(
            Funcionario autorLogado,
            BancoHorasDTO.RegistrarCompensacaoRequest req
    ) {
        if (req == null || req.funcionarioId() == null) {
            throw new IllegalArgumentException("O colaborador é obrigatório para registrar compensação.");
        }
        if (req.dataReferencia() == null) {
            throw new IllegalArgumentException("A data da compensação é obrigatória.");
        }
        if (req.minutos() == null || req.minutos() <= 0) {
            throw new IllegalArgumentException("A quantidade de minutos a compensar deve ser maior que zero.");
        }
        if (req.motivo() == null || req.motivo().trim().isEmpty()) {
            throw new IllegalArgumentException("O motivo da compensação é obrigatório.");
        }

        Funcionario funcionario = funcionarioRepository.findById(req.funcionarioId())
                .orElseThrow(() -> new UsuarioNaoEncontradoException("Colaborador não encontrado."));

        if (autorLogado != null) {
            if (!funcionario.getEmpresa().getId().equals(autorLogado.getEmpresa().getId())) {
                throw new AcessoNegadoException("Colaborador não pertence à sua empresa.");
            }
            if (autorLogado.getPerfilAcesso() == PerfilAcesso.GESTOR
                    && (funcionario.getGestor() == null || !funcionario.getGestor().getId().equals(autorLogado.getId()))
                    && !funcionario.getId().equals(autorLogado.getId())) {
                throw new AcessoNegadoException("Gestor só pode registrar compensação para membros da sua equipe.");
            }
        }

        UUID empresaId = funcionario.getEmpresa().getId();
        if (estaPeriodoFechado(empresaId, funcionario.getId(), req.dataReferencia())) {
            String mesRef = req.dataReferencia().format(MES_REF_FMT);
            throw new PeriodoFechadoException("O período " + mesRef + " encontra-se fechado. Não é permitido registrar compensações.");
        }

        int minutosNegativos = -Math.abs(req.minutos());
        String autorNome = autorLogado != null
                ? autorLogado.getNomeCompleto() + " (" + autorLogado.getPerfilAcesso().name() + ")"
                : obterIdentificacaoUsuarioLogado();

        MovimentacaoBancoHoras comp = new MovimentacaoBancoHoras();
        comp.setFuncionario(funcionario);
        comp.setDataReferencia(req.dataReferencia());
        comp.setTipo(TipoMovimentacaoBancoHoras.COMPENSACAO);
        comp.setMinutos(minutosNegativos);
        comp.setApuracaoDiariaId(null);
        comp.setDescricao("Compensação de horas - Motivo: " + req.motivo().trim());
        comp.setPeriodoFechado(false);
        comp.setRegistradoPor(autorNome);
        comp.setRegistradoEm(LocalDateTime.now());

        MovimentacaoBancoHoras salva = movimentacaoRepository.saveAndFlush(comp);

        // Calcula o saldo acumulado até o fim do dia da compensação para retornar no item
        BancoHorasDTO.ExtratoPeriodoResponse extratoDia = consultarExtrato(
                funcionario.getId(),
                req.dataReferencia(),
                req.dataReferencia()
        );

        return new BancoHorasDTO.ItemExtratoResponse(
                salva.getId(),
                salva.getDataReferencia(),
                salva.getTipo(),
                salva.getDescricao(),
                salva.getMinutos(),
                extratoDia.saldoFinalMinutos(),
                salva.isPeriodoFechado(),
                salva.getRegistradoPor(),
                salva.getRegistradoEm()
        );
    }

    /**
     * Consulta o extrato detalhado do colaborador em um intervalo [dataInicio, dataFim],
     * calculando o saldo inicial acumulado, a evolução linha a linha e o saldo final.
     */
    @Transactional(readOnly = true)
    public BancoHorasDTO.ExtratoPeriodoResponse consultarExtrato(
            UUID funcionarioId,
            LocalDate dataInicio,
            LocalDate dataFim
    ) {
        if (funcionarioId == null || dataInicio == null || dataFim == null) {
            throw new IllegalArgumentException("Colaborador, data de início e data de fim são obrigatórios.");
        }
        if (dataFim.isBefore(dataInicio)) {
            throw new IllegalArgumentException("A data final não pode ser anterior à data inicial.");
        }

        Funcionario funcionario = funcionarioRepository.findById(funcionarioId)
                .orElseThrow(() -> new UsuarioNaoEncontradoException("Colaborador não encontrado."));

        UUID empresaId = funcionario.getEmpresa().getId();

        // Verifica se a consulta corresponde exatamente a um mês fechado com snapshot congelado
        String mesInicio = dataInicio.format(MES_REF_FMT);
        String mesFim = dataFim.format(MES_REF_FMT);
        YearMonth ymInicio = YearMonth.from(dataInicio);
        Optional<FechamentoBancoHoras> snapshotMesOpt = (mesInicio.equals(mesFim)
                && dataInicio.equals(ymInicio.atDay(1))
                && dataFim.equals(ymInicio.atEndOfMonth()))
                ? fechamentoBancoHorasRepository.findByFuncionarioIdAndMesReferencia(funcionarioId, mesInicio)
                : Optional.empty();

        int saldoInicial = snapshotMesOpt
                .map(FechamentoBancoHoras::getSaldoAnteriorMinutos)
                .orElseGet(() -> calcularSaldoAnterior(funcionarioId, dataInicio));

        List<MovimentacaoBancoHoras> movsPeriodo = movimentacaoRepository
                .findByFuncionarioIdAndDataReferenciaBetweenOrderByDataReferenciaAscRegistradoEmAsc(
                        funcionarioId,
                        dataInicio,
                        dataFim
                );

        int saldoCorrente = saldoInicial;
        int creditosPeriodo = 0;
        int debitosPeriodo = 0;
        int compensacoesPeriodo = 0;

        List<BancoHorasDTO.ItemExtratoResponse> itens = new ArrayList<>();

        for (MovimentacaoBancoHoras m : movsPeriodo) {
            saldoCorrente += m.getMinutos();

            if (m.getTipo() == TipoMovimentacaoBancoHoras.CREDITO_APURACAO && m.getMinutos() > 0) {
                creditosPeriodo += m.getMinutos();
            } else if (m.getTipo() == TipoMovimentacaoBancoHoras.DEBITO_APURACAO && m.getMinutos() < 0) {
                debitosPeriodo += Math.abs(m.getMinutos());
            } else if (m.getTipo() == TipoMovimentacaoBancoHoras.COMPENSACAO && m.getMinutos() < 0) {
                compensacoesPeriodo += Math.abs(m.getMinutos());
            }

            boolean fechado = m.isPeriodoFechado() || estaPeriodoFechado(empresaId, funcionarioId, m.getDataReferencia());

            itens.add(new BancoHorasDTO.ItemExtratoResponse(
                    m.getId(),
                    m.getDataReferencia(),
                    m.getTipo(),
                    m.getDescricao(),
                    m.getMinutos(),
                    saldoCorrente,
                    fechado,
                    m.getRegistradoPor(),
                    m.getRegistradoEm()
            ));
        }

        if (snapshotMesOpt.isPresent()) {
            FechamentoBancoHoras snap = snapshotMesOpt.get();
            return new BancoHorasDTO.ExtratoPeriodoResponse(
                    funcionario.getId(),
                    funcionario.getNomeCompleto(),
                    funcionario.getMatricula(),
                    dataInicio,
                    dataFim,
                    snap.getSaldoAnteriorMinutos(),
                    snap.getCreditosPeriodoMinutos(),
                    snap.getDebitosPeriodoMinutos(),
                    snap.getCompensacoesPeriodoMinutos(),
                    snap.getSaldoFinalCongeladoMinutos(),
                    itens
            );
        }

        return new BancoHorasDTO.ExtratoPeriodoResponse(
                funcionario.getId(),
                funcionario.getNomeCompleto(),
                funcionario.getMatricula(),
                dataInicio,
                dataFim,
                saldoInicial,
                creditosPeriodo,
                debitosPeriodo,
                compensacoesPeriodo,
                saldoCorrente,
                itens
        );
    }

    /**
     * Congela os saldos de banco de horas de todos os colaboradores da empresa no fechamento de período (YYYY-MM).
     */
    @Transactional
    public void congelarPeriodoEmpresa(UUID empresaId, String mesReferencia, String fechadoPor) {
        List<Funcionario> funcionarios = funcionarioRepository.findByEmpresaId(empresaId);
        for (Funcionario f : funcionarios) {
            congelarPeriodoFuncionario(f, mesReferencia, fechadoPor);
        }
    }

    /**
     * Congela o saldo de um colaborador específico em determinada competência (YYYY-MM),
     * gravando o snapshot FechamentoBancoHoras e marcando as movimentações como periodoFechado = true.
     */
    @Transactional
    public FechamentoBancoHoras congelarPeriodoFuncionario(
            Funcionario funcionario,
            String mesReferencia,
            String fechadoPor
    ) {
        YearMonth ym = YearMonth.parse(mesReferencia.trim());
        LocalDate inicioMes = ym.atDay(1);
        LocalDate fimMes = ym.atEndOfMonth();

        // Se já existir snapshot para este mês, retorna o existente (imutável)
        Optional<FechamentoBancoHoras> existenteOpt = fechamentoBancoHorasRepository
                .findByFuncionarioIdAndMesReferencia(funcionario.getId(), mesReferencia.trim());
        if (existenteOpt.isPresent()) {
            return existenteOpt.get();
        }

        BancoHorasDTO.ExtratoPeriodoResponse extrato = consultarExtrato(funcionario.getId(), inicioMes, fimMes);

        // Marca todas as movimentações do mês como congeladas
        List<MovimentacaoBancoHoras> movsMes = movimentacaoRepository
                .findByFuncionarioIdAndDataReferenciaBetweenOrderByDataReferenciaAscRegistradoEmAsc(
                        funcionario.getId(),
                        inicioMes,
                        fimMes
                );
        for (MovimentacaoBancoHoras m : movsMes) {
            m.setPeriodoFechado(true);
        }
        if (!movsMes.isEmpty()) {
            movimentacaoRepository.saveAllAndFlush(movsMes);
        }

        FechamentoBancoHoras fechamento = new FechamentoBancoHoras();
        fechamento.setFuncionario(funcionario);
        fechamento.setMesReferencia(mesReferencia.trim());
        fechamento.setSaldoAnteriorMinutos(extrato.saldoInicialMinutos());
        fechamento.setCreditosPeriodoMinutos(extrato.creditosPeriodoMinutos());
        fechamento.setDebitosPeriodoMinutos(extrato.debitosPeriodoMinutos());
        fechamento.setCompensacoesPeriodoMinutos(extrato.compensacoesPeriodoMinutos());
        fechamento.setSaldoFinalCongeladoMinutos(extrato.saldoFinalMinutos());
        fechamento.setFechadoEm(LocalDateTime.now());
        fechamento.setFechadoPor(fechadoPor != null ? fechadoPor : "RH");

        return fechamentoBancoHorasRepository.saveAndFlush(fechamento);
    }

    /**
     * Descongela os saldos de banco de horas da empresa quando uma competência é reaberta pelo RH.
     */
    @Transactional
    public void descongelarPeriodoEmpresa(UUID empresaId, String mesReferencia) {
        List<Funcionario> funcionarios = funcionarioRepository.findByEmpresaId(empresaId);
        for (Funcionario f : funcionarios) {
            descongelarPeriodoFuncionario(f, mesReferencia);
        }
    }

    @Transactional
    public void descongelarPeriodoFuncionario(Funcionario funcionario, String mesReferencia) {
        YearMonth ym = YearMonth.parse(mesReferencia.trim());
        LocalDate inicioMes = ym.atDay(1);
        LocalDate fimMes = ym.atEndOfMonth();

        fechamentoBancoHorasRepository.findByFuncionarioIdAndMesReferencia(funcionario.getId(), mesReferencia.trim())
                .ifPresent(fechamento -> {
                    fechamentoBancoHorasRepository.delete(fechamento);
                    fechamentoBancoHorasRepository.flush();
                });

        List<MovimentacaoBancoHoras> movsMes = movimentacaoRepository
                .findByFuncionarioIdAndDataReferenciaBetweenOrderByDataReferenciaAscRegistradoEmAsc(
                        funcionario.getId(),
                        inicioMes,
                        fimMes
                );
        for (MovimentacaoBancoHoras m : movsMes) {
            m.setPeriodoFechado(false);
        }
        if (!movsMes.isEmpty()) {
            movimentacaoRepository.saveAllAndFlush(movsMes);
        }
    }

    /**
     * Relatório consolidado de saldos por equipe para o Gestor (ou empresa inteira para o RH).
     */
    @Transactional(readOnly = true)
    public List<BancoHorasDTO.SaldoColaboradorEquipeResponse> consultarSaldoEquipe(
            LocalDate dataInicio,
            LocalDate dataFim
    ) {
        Funcionario logado = obterFuncionarioLogadoOpcional();
        if (logado == null) {
            throw new AcessoNegadoException("Usuário não autenticado.");
        }
        return consultarSaldoEquipe(logado, dataInicio, dataFim);
    }

    @Transactional(readOnly = true)
    public List<BancoHorasDTO.SaldoColaboradorEquipeResponse> consultarSaldoEquipe(
            Funcionario solicitante,
            LocalDate dataInicio,
            LocalDate dataFim
    ) {
        if (solicitante == null) {
            throw new AcessoNegadoException("Solicitante inválido.");
        }

        LocalDate inicio = dataInicio != null ? dataInicio : YearMonth.now().atDay(1);
        LocalDate fim = dataFim != null ? dataFim : YearMonth.now().atEndOfMonth();

        List<Funcionario> membros;
        if (solicitante.getPerfilAcesso() == PerfilAcesso.GESTOR) {
            membros = funcionarioRepository.findByGestorIdOrderByNomeCompletoAsc(solicitante.getId());
        } else if (solicitante.getPerfilAcesso() == PerfilAcesso.RH) {
            membros = funcionarioRepository.findByEmpresaIdOrderByNomeCompletoAsc(solicitante.getEmpresa().getId());
        } else {
            throw new AcessoNegadoException("Apenas gestores e RH podem consultar o relatório consolidado de equipe.");
        }

        List<BancoHorasDTO.SaldoColaboradorEquipeResponse> resultado = new ArrayList<>();
        for (Funcionario f : membros) {
            BancoHorasDTO.ExtratoPeriodoResponse ext = consultarExtrato(f.getId(), inicio, fim);
            resultado.add(new BancoHorasDTO.SaldoColaboradorEquipeResponse(
                    f.getId(),
                    f.getNomeCompleto(),
                    f.getMatricula(),
                    f.getCargo(),
                    ext.saldoInicialMinutos(),
                    ext.creditosPeriodoMinutos(),
                    ext.debitosPeriodoMinutos(),
                    ext.compensacoesPeriodoMinutos(),
                    ext.saldoFinalMinutos()
            ));
        }
        return resultado;
    }

    /**
     * Calcula o saldo acumulado estritamente anterior a dataInicio,
     * respeitando qualquer snapshot congelado (FechamentoBancoHoras) de meses anteriores.
     */
    private int calcularSaldoAnterior(UUID funcionarioId, LocalDate dataInicio) {
        String mesDataInicio = dataInicio.format(MES_REF_FMT);

        Optional<FechamentoBancoHoras> ultimoFechamentoOpt = fechamentoBancoHorasRepository
                .findFirstByFuncionarioIdAndMesReferenciaLessThanOrderByMesReferenciaDesc(funcionarioId, mesDataInicio);

        if (ultimoFechamentoOpt.isPresent()) {
            FechamentoBancoHoras ultimo = ultimoFechamentoOpt.get();
            YearMonth ymFechado = YearMonth.parse(ultimo.getMesReferencia());
            LocalDate diaAposFechamento = ymFechado.atEndOfMonth().plusDays(1);

            int somaAposSnapshot = 0;
            if (diaAposFechamento.isBefore(dataInicio)) {
                Integer parcial = movimentacaoRepository.somarSaldoEntre(funcionarioId, diaAposFechamento, dataInicio);
                somaAposSnapshot = parcial != null ? parcial : 0;
            }
            return ultimo.getSaldoFinalCongeladoMinutos() + somaAposSnapshot;
        }

        Integer somaTotalAnterior = movimentacaoRepository.somarSaldoAnterior(funcionarioId, dataInicio);
        return somaTotalAnterior != null ? somaTotalAnterior : 0;
    }

    private void removerMovimentacaoAutomaticaSeAberta(UUID apuracaoDiariaId, TipoMovimentacaoBancoHoras tipo) {
        movimentacaoRepository.findByApuracaoDiariaIdAndTipo(apuracaoDiariaId, tipo)
                .ifPresent(mov -> {
                    if (!mov.isPeriodoFechado()) {
                        movimentacaoRepository.delete(mov);
                        movimentacaoRepository.flush();
                    }
                });
    }

    public boolean estaPeriodoFechado(UUID empresaId, UUID funcionarioId, LocalDate data) {
        String mesRef = data.format(MES_REF_FMT);

        if (funcionarioId != null && fechamentoBancoHorasRepository.findByFuncionarioIdAndMesReferencia(funcionarioId, mesRef).isPresent()) {
            return true;
        }

        if (empresaId != null) {
            Optional<FechamentoPeriodo> fechamentoOpt = fechamentoPeriodoRepository
                    .findFirstByEmpresaIdAndMesReferenciaOrderByDataFechamentoDesc(empresaId, mesRef);
            if (fechamentoOpt.isPresent()) {
                StatusFechamentoPeriodo st = fechamentoOpt.get().getStatus();
                return st == StatusFechamentoPeriodo.CONCLUIDO || st == StatusFechamentoPeriodo.CONCLUIDO_COM_PENDENCIAS;
            }
        }
        return false;
    }

    private Funcionario obterFuncionarioLogadoOpcional() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof FuncionarioUserDetails ud) {
            return ud.getFuncionario();
        }
        return null;
    }

    private String obterIdentificacaoUsuarioLogado() {
        Funcionario f = obterFuncionarioLogadoOpcional();
        if (f != null) {
            return f.getNomeCompleto() + " (" + f.getMatricula() + ")";
        }
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null ? auth.getName() : "RH";
    }

    private String formatarMinutos(int minutos) {
        int abs = Math.abs(minutos);
        int h = abs / 60;
        int m = abs % 60;
        return String.format("%02d:%02d", h, m);
    }
}
