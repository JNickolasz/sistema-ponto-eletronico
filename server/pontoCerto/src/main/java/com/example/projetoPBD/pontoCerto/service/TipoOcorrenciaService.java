package com.example.projetoPBD.pontoCerto.service;

import com.example.projetoPBD.pontoCerto.domain.Empresa;
import com.example.projetoPBD.pontoCerto.domain.TipoOcorrencia;
import com.example.projetoPBD.pontoCerto.domain.enums.TipoAbonoOcorrencia;
import com.example.projetoPBD.pontoCerto.dto.domaindtos.TipoOcorrenciaDTO;
import com.example.projetoPBD.pontoCerto.repository.EmpresaRepository;
import com.example.projetoPBD.pontoCerto.repository.TipoOcorrenciaRepository;
import com.example.projetoPBD.pontoCerto.security.SecurityTenantContext;
import com.example.projetoPBD.pontoCerto.service.exceptions.AcessoNegadoException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class TipoOcorrenciaService {

    private final TipoOcorrenciaRepository tipoOcorrenciaRepository;
    private final EmpresaRepository empresaRepository;
    private final SecurityTenantContext securityTenantContext;

    public TipoOcorrenciaService(
            TipoOcorrenciaRepository tipoOcorrenciaRepository,
            EmpresaRepository empresaRepository,
            SecurityTenantContext securityTenantContext
    ) {
        this.tipoOcorrenciaRepository = tipoOcorrenciaRepository;
        this.empresaRepository = empresaRepository;
        this.securityTenantContext = securityTenantContext;
    }

    @Transactional
    public TipoOcorrenciaDTO.Response criar(TipoOcorrenciaDTO.CriarRequest req) {
        UUID empresaId = securityTenantContext.empresaAtual();
        validarRequest(empresaId, req.nome(), req.tipoAbono(), req.minutosAbonoPadrao(), null);

        Empresa empresa = empresaRepository.findById(empresaId)
                .orElseThrow(() -> new AcessoNegadoException("Empresa não encontrada."));

        TipoOcorrencia t = new TipoOcorrencia();
        t.setEmpresa(empresa);
        t.setNome(req.nome().trim());
        t.setDescricao(req.descricao());
        t.setTipoAbono(req.tipoAbono());

        // AQUI O IF QUE COMBINAMOS:
        if (req.tipoAbono() == TipoAbonoOcorrencia.PARCIAL) {
            t.setMinutosAbonoPadrao(req.minutosAbonoPadrao());
        } else {
            t.setMinutosAbonoPadrao(null); // Não suja o banco se for integral ou registro
        }

        t.setExigeAnexo(Boolean.TRUE.equals(req.exigeAnexo()));
        t.setAtivo(true);

        return new TipoOcorrenciaDTO.Response(tipoOcorrenciaRepository.save(t));
    }

    @Transactional
    public TipoOcorrenciaDTO.Response atualizar(UUID id, TipoOcorrenciaDTO.AtualizarRequest req) {
        UUID empresaId = securityTenantContext.empresaAtual();
        TipoOcorrencia t = tipoOcorrenciaRepository.findByIdAndEmpresaId(id, empresaId)
                .orElseThrow(() -> new IllegalArgumentException("Tipo de ocorrência não encontrado."));

        validarRequest(empresaId, req.nome(), req.tipoAbono(), req.minutosAbonoPadrao(), id);

        t.setNome(req.nome().trim());
        t.setDescricao(req.descricao());
        t.setTipoAbono(req.tipoAbono());

        if (req.tipoAbono() == TipoAbonoOcorrencia.PARCIAL) {
            t.setMinutosAbonoPadrao(req.minutosAbonoPadrao());
        } else {
            t.setMinutosAbonoPadrao(null);
        }

        if (req.exigeAnexo() != null) t.setExigeAnexo(req.exigeAnexo());
        if (req.ativo() != null) t.setAtivo(req.ativo());

        return new TipoOcorrenciaDTO.Response(tipoOcorrenciaRepository.save(t));
    }

    @Transactional(readOnly = true)
    public List<TipoOcorrenciaDTO.Response> listar(boolean apenasAtivos) {
        UUID empresaId = securityTenantContext.empresaAtual();
        List<TipoOcorrencia> lista = apenasAtivos
                ? tipoOcorrenciaRepository.findByEmpresaIdAndAtivoTrueOrderByNomeAsc(empresaId)
                : tipoOcorrenciaRepository.findByEmpresaIdOrderByNomeAsc(empresaId);

        return lista.stream().map(TipoOcorrenciaDTO.Response::new).toList();
    }

    private void validarRequest(UUID empresaId, String nome, TipoAbonoOcorrencia tipoAbono, Integer minutos, UUID idAtual) {
        if (nome == null || nome.trim().isEmpty()) {
            throw new IllegalArgumentException("O nome do tipo de ocorrência é obrigatório.");
        }
        if (tipoAbono == null) {
            throw new IllegalArgumentException("O tipo de abono é obrigatório.");
        }
        if (tipoAbono == TipoAbonoOcorrencia.PARCIAL && (minutos == null || minutos <= 0)) {
            throw new IllegalArgumentException("Para abono parcial, informe uma quantidade padrão de minutos maior que zero.");
        }

        boolean duplicado = idAtual == null
                ? tipoOcorrenciaRepository.existsByEmpresaIdAndNomeIgnoreCase(empresaId, nome.trim())
                : tipoOcorrenciaRepository.existsByEmpresaIdAndNomeIgnoreCaseAndIdNot(empresaId, nome.trim(), idAtual);

        if (duplicado) {
            throw new IllegalArgumentException("Já existe um tipo de ocorrência com esse nome na sua empresa.");
        }
    }
}