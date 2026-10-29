package com.example.projetoPBD.pontoCerto.service;

import com.example.projetoPBD.pontoCerto.domain.Equipamento;
import com.example.projetoPBD.pontoCerto.domain.Batida;
import com.example.projetoPBD.pontoCerto.domain.Funcionario;
import com.example.projetoPBD.pontoCerto.domain.FuncionarioUserDetails;
import com.example.projetoPBD.pontoCerto.domain.HistoricoImportacao;
import com.example.projetoPBD.pontoCerto.domain.Relogio;
import com.example.projetoPBD.pontoCerto.domain.enums.StatusBatida;
import com.example.projetoPBD.pontoCerto.domain.enums.StatusImportacao;
import com.example.projetoPBD.pontoCerto.domain.enums.TipoOrigem;
import com.example.projetoPBD.pontoCerto.domain.enums.TipoRegistro;
import com.example.projetoPBD.pontoCerto.dto.domaindtos.ImportacaoDTO;
import com.example.projetoPBD.pontoCerto.repository.BatidaRepository;
import com.example.projetoPBD.pontoCerto.repository.EquipamentoRepository;
import com.example.projetoPBD.pontoCerto.repository.FuncionarioRepository;
import com.example.projetoPBD.pontoCerto.repository.HistoricoImportacaoRepository;
import com.example.projetoPBD.pontoCerto.security.SecurityTenantContext;
import com.example.projetoPBD.pontoCerto.service.exceptions.ArquivoImportacaoInvalidoException;
import com.example.projetoPBD.pontoCerto.service.exceptions.EquipamentoNaoEncontradoException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Service
public class ImportacaoService {

    private final ProcessadorArquivoPosicional processador;
    private final EquipamentoRepository equipamentoRepository;
    private final FuncionarioRepository funcionarioRepository;
    private final BatidaRepository batidaRepository;
    private final HistoricoImportacaoRepository historicoRepository;
    private final SecurityTenantContext securityTenantContext;

    public ImportacaoService(
            ProcessadorArquivoPosicional processador,
            EquipamentoRepository equipamentoRepository,
            FuncionarioRepository funcionarioRepository,
            BatidaRepository batidaRepository,
            HistoricoImportacaoRepository historicoRepository,
            SecurityTenantContext securityTenantContext
    ) {
        this.processador = processador;
        this.equipamentoRepository = equipamentoRepository;
        this.funcionarioRepository = funcionarioRepository;
        this.batidaRepository = batidaRepository;
        this.historicoRepository = historicoRepository;
        this.securityTenantContext = securityTenantContext;
    }

    @Transactional(readOnly = true)
    public ImportacaoDTO.PreviewResponse gerarPreview(MultipartFile arquivo, UUID equipamentoId) {
        Equipamento equipamento = validarEquipamento(equipamentoId);
        UUID empresaId = equipamento.getEmpresa().getId();

        ProcessadorArquivoPosicional.ResultadoLeitura resultado = lerArquivo(arquivo);
        List<ImportacaoDTO.ItemPreview> validas = new ArrayList<>();
        List<ImportacaoDTO.ItemPreview> repetidas = new ArrayList<>();
        List<ImportacaoDTO.ItemPreview> recusadas = new ArrayList<>();
        Set<String> chavesNoArquivo = new HashSet<>();

        List<Long> nsrsDoArquivo = resultado.registro().stream()
                .map(ProcessadorArquivoPosicional.RegistroBatidaLinha::nsr)
                .toList();
        Set<Long> nsrsJaExistentesNoBanco = nsrsDoArquivo.isEmpty()
                ? Set.of()
                : new HashSet<>(batidaRepository.findNsrsByEquipamentoIdAndNsrIn(equipamento.getId(), nsrsDoArquivo));
        Set<Long> nsrsNoArquivo = new HashSet<>();

        for (ProcessadorArquivoPosicional.RegistroBatidaLinha reg : resultado.registro()) {
            String identificadorLimpo = reg.pis() != null ? reg.pis().trim() : "";
            Optional<Funcionario> funcOpt = buscarFuncionario(identificadorLimpo, empresaId);

            boolean nsrRepetido = nsrsJaExistentesNoBanco.contains(reg.nsr()) || !nsrsNoArquivo.add(reg.nsr());
            if (nsrRepetido) {
                repetidas.add(new ImportacaoDTO.ItemPreview(
                        reg.numeroLinha(),
                        reg.nsr(),
                        identificadorLimpo,
                        funcOpt.map(Funcionario::getNomeCompleto).orElse(null),
                        reg.dataHora(),
                        "Marcação já existente para este equipamento e NSR (" + reg.nsr() + ")"
                ));
                continue;
            }

            if (funcOpt.isEmpty()) {
                String chaveOrfa = "ORFA_" + identificadorLimpo + "_" + reg.dataHora();
                boolean orfaJaExiste = batidaRepository.existsOrfaByIdentificadorAndDataHoraAndEmpresaId(
                        identificadorLimpo, reg.dataHora(), empresaId
                );

                if (orfaJaExiste || !chavesNoArquivo.add(chaveOrfa)) {
                    repetidas.add(new ImportacaoDTO.ItemPreview(
                            reg.numeroLinha(),
                            reg.nsr(),
                            identificadorLimpo,
                            null,
                            reg.dataHora(),
                            "Batida órfã já registrada anteriormente para este identificador e horário"
                    ));
                } else {
                    recusadas.add(new ImportacaoDTO.ItemPreview(
                            reg.numeroLinha(),
                            reg.nsr(),
                            identificadorLimpo,
                            null,
                            reg.dataHora(),
                            "Colaborador não encontrado — será gravada como Batida Órfã (Pendente de Vínculo)"
                    ));
                }
                continue;
            }

            Funcionario funcionario = funcOpt.get();
            String chaveUnica = funcionario.getId() + "_" + reg.dataHora();
            boolean jaExisteNoBanco = batidaRepository.existsByFuncionarioIdAndDataHora(funcionario.getId(), reg.dataHora());

            if (jaExisteNoBanco || !chavesNoArquivo.add(chaveUnica)) {
                repetidas.add(new ImportacaoDTO.ItemPreview(
                        reg.numeroLinha(),
                        reg.nsr(),
                        identificadorLimpo,
                        funcionario.getNomeCompleto(),
                        reg.dataHora(),
                        "Batida já cadastrada no sistema para esta data e hora"
                ));
            } else {
                validas.add(new ImportacaoDTO.ItemPreview(
                        reg.numeroLinha(),
                        reg.nsr(),
                        identificadorLimpo,
                        funcionario.getNomeCompleto(),
                        reg.dataHora(),
                        "Válida para gravação"
                ));
            }
        }

        return new ImportacaoDTO.PreviewResponse(
                arquivo.getOriginalFilename(),
                equipamento.getId(),
                equipamento.getIdentificacao(),
                resultado.totalLinhasBatidas(),
                validas.size(),
                repetidas.size(),
                recusadas.size(),
                validas,
                repetidas,
                recusadas
        );
    }

    @Transactional(rollbackFor = Exception.class)
    public ImportacaoDTO.HistoricoResponse confirmarImportacao(MultipartFile arquivo, UUID equipamentoId) {
        Equipamento equipamento = validarEquipamentoParaGravacao(equipamentoId);
        UUID empresaId = equipamento.getEmpresa().getId();

        List<String> linhas = lerLinhasEstrutura(arquivo);

        HistoricoImportacao historico = new HistoricoImportacao();
        historico.setNomeArquivo(arquivo.getOriginalFilename() != null ? arquivo.getOriginalFilename() : "importacao.txt");
        historico.setConteudoArquivo(String.join("\n", linhas));
        historico.setEquipamento(equipamento);
        historico.setAutor(obterFuncionarioLogado(empresaId));
        historico.setStatus(StatusImportacao.PENDENTE);
        historico = historicoRepository.saveAndFlush(historico);

        List<Batida> batidasSalvas = new ArrayList<>();
        Set<String> chavesNoArquivo = new HashSet<>();
        Set<Long> nsrsNoArquivo = new HashSet<>();
        int validasCount = 0;
        int repetidasCount = 0;
        int recusadasCount = 0;
        int totalLidas = 0;

        for (int i = 1; i < linhas.size() - 1; i++) {
            String linha = linhas.get(i);
            int numeroLinha = i + 1;
            totalLidas++;

            ProcessadorArquivoPosicional.RegistroBatidaLinha reg = processador.extrairRegistroBatida(numeroLinha, linha);
            String identificadorLimpo = reg.pis() != null ? reg.pis().trim() : "";

            boolean nsrJaExiste = batidaRepository.existsByEquipamentoIdAndNsr(equipamento.getId(), reg.nsr());
            if (nsrJaExiste || !nsrsNoArquivo.add(reg.nsr())) {
                repetidasCount++;
                continue;
            }

            Optional<Funcionario> funcOpt = buscarFuncionario(identificadorLimpo, empresaId);

            if (funcOpt.isEmpty()) {
                String chaveOrfa = "ORFA_" + identificadorLimpo + "_" + reg.dataHora();
                boolean orfaJaExiste = batidaRepository.existsOrfaByIdentificadorAndDataHoraAndEmpresaId(
                        identificadorLimpo, reg.dataHora(), empresaId
                );

                if (orfaJaExiste || !chavesNoArquivo.add(chaveOrfa)) {
                    repetidasCount++;
                } else {
                    Batida batidaOrfa = new Batida();
                    batidaOrfa.setNsr(reg.nsr());
                    batidaOrfa.setFuncionario(null);
                    batidaOrfa.setIdentificadorOriginal(identificadorLimpo);
                    batidaOrfa.setStatusBatida(StatusBatida.ORFA_PENDENTE);
                    batidaOrfa.setEquipamento(equipamento);
                    batidaOrfa.setDataHora(reg.dataHora());
                    batidaOrfa.setTipoRegistro(TipoRegistro.ENTRADA);
                    batidaOrfa.setTipoOrigem(TipoOrigem.ARQUIVO_IMPORTADO);
                    batidaOrfa.setArquivoImportadoId(historico.getId());

                    List<Batida> salvas = batidaRepository.saveAll(List.of(batidaOrfa));
                    batidaRepository.flush();
                    batidasSalvas.addAll(salvas);
                    recusadasCount++;
                }
                continue;
            }

            Funcionario funcionario = funcOpt.get();
            String chaveUnica = funcionario.getId() + "_" + reg.dataHora();
            boolean jaExisteNoBanco = batidaRepository.existsByFuncionarioIdAndDataHora(funcionario.getId(), reg.dataHora());

            if (jaExisteNoBanco || !chavesNoArquivo.add(chaveUnica)) {
                repetidasCount++;
            } else {
                Batida batida = new Batida();
                batida.setNsr(reg.nsr());
                batida.setFuncionario(funcionario);
                batida.setIdentificadorOriginal(identificadorLimpo);
                batida.setStatusBatida(StatusBatida.NORMAL);
                batida.setEquipamento(equipamento);
                batida.setDataHora(reg.dataHora());
                batida.setTipoRegistro(TipoRegistro.ENTRADA);
                batida.setTipoOrigem(TipoOrigem.ARQUIVO_IMPORTADO);
                batida.setArquivoImportadoId(historico.getId());

                List<Batida> salvas = batidaRepository.saveAll(List.of(batida));
                batidaRepository.flush();
                batidasSalvas.addAll(salvas);
                validasCount++;
            }
        }

        historico.setTotalLidas(totalLidas);
        historico.setTotalValidas(validasCount);
        historico.setTotalRepetidas(repetidasCount);
        historico.setTotalRecusadas(recusadasCount);
        historico.setStatus(StatusImportacao.CONCLUIDA);
        historico = historicoRepository.saveAndFlush(historico);

        if (equipamento instanceof Relogio relogio) {
            long totalAtual = relogio.getLinhasImportadas() != null ? relogio.getLinhasImportadas() : 0L;
            relogio.setLinhasImportadas(totalAtual + batidasSalvas.size());
            equipamentoRepository.saveAndFlush(relogio);
        }

        return new ImportacaoDTO.HistoricoResponse(historico);
    }

    @Transactional(readOnly = true)
    public List<ImportacaoDTO.HistoricoResponse> listarHistorico() {
        UUID empresaId = securityTenantContext.empresaAtual();
        return historicoRepository.findByEmpresaIdOrderByDAtaImportacaoDesc(empresaId)
                .stream()
                .map(ImportacaoDTO.HistoricoResponse::new)
                .toList();
    }

    private Optional<Funcionario> buscarFuncionario(String identificador, UUID empresaId) {
        if (identificador == null || identificador.isBlank()) {
            return Optional.empty();
        }
        String limpo = identificador.trim();
        String apenasDigitos = limpo.replaceAll("\\D", "");

        Optional<Funcionario> porPis = funcionarioRepository.findByPisPasepAndEmpresaId(limpo, empresaId);
        if (porPis.isPresent()) {
            return porPis;
        }
        if (!apenasDigitos.isEmpty() && !apenasDigitos.equals(limpo)) {
            porPis = funcionarioRepository.findByPisPasepAndEmpresaId(apenasDigitos, empresaId);
            if (porPis.isPresent()) {
                return porPis;
            }
        }

        Optional<Funcionario> porCpf = funcionarioRepository.findByCpfAndEmpresaId(limpo, empresaId);
        if (porCpf.isPresent()) {
            return porCpf;
        }
        if (!apenasDigitos.isEmpty() && !apenasDigitos.equals(limpo)) {
            porCpf = funcionarioRepository.findByCpfAndEmpresaId(apenasDigitos, empresaId);
            if (porCpf.isPresent()) {
                return porCpf;
            }
        }

        Optional<Funcionario> porMatricula = funcionarioRepository.findByMatriculaAndEmpresaId(limpo, empresaId);
        if (porMatricula.isPresent()) {
            return porMatricula;
        }

        String semZerosEsquerda = limpo.replaceFirst("^0+(?!$)", "");
        if (!semZerosEsquerda.equals(limpo)) {
            return funcionarioRepository.findByMatriculaAndEmpresaId(semZerosEsquerda, empresaId);
        }

        return Optional.empty();
    }

    private Equipamento validarEquipamento(UUID equipamentoId) {
        return buscarEValidarEquipamento(equipamentoId, false);
    }

    private Equipamento validarEquipamentoParaGravacao(UUID equipamentoId) {
        return buscarEValidarEquipamento(equipamentoId, true);
    }

    private Equipamento buscarEValidarEquipamento(UUID equipamentoId, boolean bloquearParaAtualizacao) {
        if (equipamentoId == null) {
            throw new EquipamentoNaoEncontradoException("Equipamento não encontrado");
        }
        Optional<Equipamento> equipamentoOpt = bloquearParaAtualizacao
                ? equipamentoRepository.findByIdForUpdate(equipamentoId)
                : equipamentoRepository.findById(equipamentoId);
        Equipamento equipamento = equipamentoOpt
                .orElseThrow(() -> new EquipamentoNaoEncontradoException("Equipamento não encontrado"));

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof FuncionarioUserDetails) {
            UUID empresaLogada = securityTenantContext.empresaAtual();
            if (!equipamento.getEmpresa().getId().equals(empresaLogada)) {
                throw new ArquivoImportacaoInvalidoException("O equipamento não pertence à empresa do usuário logado.");
            }
        }

        return equipamento;
    }

    private ProcessadorArquivoPosicional.ResultadoLeitura lerArquivo(MultipartFile arquivo) {
        if (arquivo == null || arquivo.isEmpty()) {
            throw new ArquivoImportacaoInvalidoException("Nenhum arquivo enviado.");
        }
        try {
            return processador.processar(arquivo.getInputStream());
        } catch (IOException e) {
            throw new ArquivoImportacaoInvalidoException("Erro ao abrir arquivo para leitura: " + e.getMessage());
        }
    }

    private List<String> lerLinhasEstrutura(MultipartFile arquivo) {
        if (arquivo == null || arquivo.isEmpty()) {
            throw new ArquivoImportacaoInvalidoException("Nenhum arquivo enviado.");
        }
        try {
            return processador.lerEValidarEstrutura(arquivo.getInputStream());
        } catch (IOException e) {
            throw new ArquivoImportacaoInvalidoException("Erro ao abrir arquivo para leitura: " + e.getMessage());
        }
    }

    private Funcionario obterFuncionarioLogado(UUID empresaId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof FuncionarioUserDetails userDetails) {
            return userDetails.getFuncionario();
        }
        return funcionarioRepository.findByEmpresaId(empresaId).stream()
                .findFirst()
                .orElse(null);
    }
}