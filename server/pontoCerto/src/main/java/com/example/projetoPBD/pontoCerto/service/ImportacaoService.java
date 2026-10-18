package com.example.projetoPBD.pontoCerto.service;

import com.example.projetoPBD.pontoCerto.domain.Equipamento;
import com.example.projetoPBD.pontoCerto.domain.Batida;
import com.example.projetoPBD.pontoCerto.domain.Funcionario;
import com.example.projetoPBD.pontoCerto.domain.FuncionarioUserDetails;
import com.example.projetoPBD.pontoCerto.domain.HistoricoImportacao;
import com.example.projetoPBD.pontoCerto.domain.Relogio;
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

        for (ProcessadorArquivoPosicional.RegistroBatidaLinha reg : resultado.registro()) {
            Optional<Funcionario> funcOpt = buscarFuncionario(reg.pis(), empresaId);

            if (funcOpt.isEmpty()) {
                recusadas.add(new ImportacaoDTO.ItemPreview(
                        reg.numeroLinha(),
                        reg.pis(),
                        null,
                        reg.dataHora(),
                        "Funcionário não encontrado com este PIS/PASEP na empresa"
                ));
                continue;
            }

            Funcionario funcionario = funcOpt.get();
            String chaveUnica = funcionario.getId() + "_" + reg.dataHora();
            boolean jaExisteNoBanco = batidaRepository.existsByFuncionarioIdAndDataHora(funcionario.getId(), reg.dataHora());

            if (jaExisteNoBanco || !chavesNoArquivo.add(chaveUnica)) {
                repetidas.add(new ImportacaoDTO.ItemPreview(
                        reg.numeroLinha(),
                        reg.pis(),
                        funcionario.getNomeCompleto(),
                        reg.dataHora(),
                        "Batida já cadastrada no sistema para esta data e hora"
                ));
            } else {
                validas.add(new ImportacaoDTO.ItemPreview(
                        reg.numeroLinha(),
                        reg.pis(),
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
        Equipamento equipamento = validarEquipamento(equipamentoId);
        UUID empresaId = equipamento.getEmpresa().getId();

        List<String> linhas = lerLinhasEstrutura(arquivo);

        HistoricoImportacao historico = new HistoricoImportacao();
        historico.setNomeArquivo(arquivo.getOriginalFilename() != null ? arquivo.getOriginalFilename() : "importacao.txt");
        historico.setEquipamento(equipamento);
        historico.setAutor(obterFuncionarioLogado(empresaId));
        historico.setStatus(StatusImportacao.PENDENTE);
        historico = historicoRepository.saveAndFlush(historico);

        List<Batida> batidasSalvas = new ArrayList<>();
        Set<String> chavesNoArquivo = new HashSet<>();
        int repetidasCount = 0;
        int recusadasCount = 0;
        int totalLidas = 0;

        for (int i = 1; i < linhas.size() - 1; i++) {
            String linha = linhas.get(i);
            int numeroLinha = i + 1;
            totalLidas++;

            ProcessadorArquivoPosicional.RegistroBatidaLinha reg = processador.extrairRegistroBatida(numeroLinha, linha);
            Optional<Funcionario> funcOpt = buscarFuncionario(reg.pis(), empresaId);

            if (funcOpt.isEmpty()) {
                recusadasCount++;
                continue;
            }

            Funcionario funcionario = funcOpt.get();
            String chaveUnica = funcionario.getId() + "_" + reg.dataHora();
            boolean jaExisteNoBanco = batidaRepository.existsByFuncionarioIdAndDataHora(funcionario.getId(), reg.dataHora());

            if (jaExisteNoBanco || !chavesNoArquivo.add(chaveUnica)) {
                repetidasCount++;
            } else {
                Batida batida = new Batida();
                batida.setFuncionario(funcionario);
                batida.setEquipamento(equipamento);
                batida.setDataHora(reg.dataHora());
                batida.setTipoRegistro(TipoRegistro.ENTRADA);
                batida.setTipoOrigem(TipoOrigem.ARQUIVO_IMPORTADO);
                batida.setArquivoImportadoId(historico.getId());

                List<Batida> salvas = batidaRepository.saveAll(List.of(batida));
                batidaRepository.flush();
                batidasSalvas.addAll(salvas);
            }
        }

        historico.setTotalLidas(totalLidas);
        historico.setTotalValidas(batidasSalvas.size());
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
        if (identificador == null) {
            return Optional.empty();
        }
        String limpo = identificador.trim();
        Optional<Funcionario> porPis = funcionarioRepository.findByPisPasepAndEmpresaId(limpo, empresaId);
        if (porPis.isPresent()) {
            return porPis;
        }
        return funcionarioRepository.findByMatriculaAndEmpresaId(limpo, empresaId);
    }

    private Equipamento validarEquipamento(UUID equipamentoId) {
        if (equipamentoId == null) {
            throw new EquipamentoNaoEncontradoException("Equipamento não encontrado");
        }
        Equipamento equipamento = equipamentoRepository.findById(equipamentoId)
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