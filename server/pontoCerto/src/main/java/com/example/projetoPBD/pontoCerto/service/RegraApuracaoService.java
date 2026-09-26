package com.example.projetoPBD.pontoCerto.service;


import com.example.projetoPBD.pontoCerto.domain.Empresa;
import com.example.projetoPBD.pontoCerto.domain.RegraApuracao;
import com.example.projetoPBD.pontoCerto.dto.RegraApuracaoConsulta;
import com.example.projetoPBD.pontoCerto.dto.domaindtos.FaixaHoraExtraDTO;
import com.example.projetoPBD.pontoCerto.dto.domaindtos.RegraAdicionalNoturnoDTO;
import com.example.projetoPBD.pontoCerto.dto.domaindtos.RegraApuracaoDTO;
import com.example.projetoPBD.pontoCerto.repository.EmpresaRepository;
import com.example.projetoPBD.pontoCerto.repository.RegraApuracaoRepository;
import com.example.projetoPBD.pontoCerto.security.SecurityTenantContext;
import com.example.projetoPBD.pontoCerto.service.exceptions.CadastroRegraApuracaoInvalidoException;
import com.example.projetoPBD.pontoCerto.service.exceptions.EmpresaNaoEncontradaException;
import com.example.projetoPBD.pontoCerto.service.exceptions.RegraApuracaoNaoEncontradaException;
import com.example.projetoPBD.pontoCerto.service.mapper.RegraApuracaoResponseMapper;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class RegraApuracaoService {


    private final RegraApuracaoRepository regraApuracaoRepository;
    private final SecurityTenantContext securityTenantContext;
    private final EmpresaRepository empresaRepository;
    private final RegraApuracaoResponseMapper regraApuracaoResponseMapper;

    public RegraApuracaoService(RegraApuracaoRepository regraApuracaoRepository, SecurityTenantContext securityTenantContext, EmpresaRepository empresaRepository, RegraApuracaoResponseMapper regraApuracaoResponseMapper) {
        this.regraApuracaoRepository = regraApuracaoRepository;
        this.securityTenantContext = securityTenantContext;
        this.empresaRepository = empresaRepository;
        this.regraApuracaoResponseMapper = regraApuracaoResponseMapper;
    }

    @Transactional
    public RegraApuracaoDTO.Response cadastro(RegraApuracaoDTO.Criar dto) {
        validar(dto);

        LocalDate hoje = LocalDate.now();
        UUID empresaId = securityTenantContext.empresaAtual();

        LocalDate inicioVigencia = ajustarInicioVigencia(dto.inicioVigencia(), hoje);
        validarInicioVigencia(inicioVigencia);


        Empresa empresa = empresaRepository.findById(empresaId)
                .orElseThrow(() -> new EmpresaNaoEncontradaException("Empresa de usuário logado não encontrada"));


        RegraApuracao regraApuracao = regraApuracaoRepository.save(regraApuracaoResponseMapper.toEntity(dto, empresa, inicioVigencia));

        Optional<RegraApuracaoConsulta> consulta = regraApuracaoRepository.findConsultaById(regraApuracao.getId(), empresaId, hoje);

        if (consulta.isEmpty()) throw new RegraApuracaoNaoEncontradaException("Regra de Apuração não encontrada");

        return regraApuracaoResponseMapper.consultaToResponse(consulta.get());
    }

    @Transactional()
    public List<RegraApuracaoDTO.Response> listar() {
        return regraApuracaoRepository
                .listVigenciasAndRegraApuracaoStatus(
                        securityTenantContext.empresaAtual(),
                        LocalDate.now()
                )
                .stream()
                .map(regraApuracaoResponseMapper::consultaToResponse)
                .toList();
    }



    private void validarAdicionalNoturno(RegraAdicionalNoturnoDTO.Criar adicional) {
        if (adicional.horaInicio().equals(adicional.horaFim())) {
            throw new CadastroRegraApuracaoInvalidoException(
                    "Hora de início não pode ser igual à hora do fim"
            );
        }

        if (adicional.duracaoHoraNoturnaSegundos() < 1 ||
                adicional.duracaoHoraNoturnaSegundos() > 3600) {
            throw new CadastroRegraApuracaoInvalidoException(
                    "Duração de hora noturna em segundos deve estar entre 1 e 3600"
            );
        }
    }

        private void validarFaixas(List<FaixaHoraExtraDTO.Criar> faixas) {
            long faixasSemDuracao = faixas.stream()
                    .filter(f -> f.duracaoMinutos() == null)
                    .count();

            if (faixasSemDuracao > 1) {
                throw new CadastroRegraApuracaoInvalidoException(
                        "Somente a última faixa pode ter duração nula"
                );
            }
        }


    private LocalDate ajustarInicioVigencia(LocalDate inicioVigencia, LocalDate hoje) {
        return inicioVigencia.equals(hoje)
                ? hoje.plusDays(1)
                : inicioVigencia;
    }

    private void validarInicioVigencia(LocalDate inicioVigencia) {

        if (inicioVigencia.isBefore(LocalDate.now())) {
            throw new CadastroRegraApuracaoInvalidoException(
                    "A data de início de vigência não pode ser anterior à data atual!"
            );
        }

        boolean diaVigenteExiste =
                regraApuracaoRepository.existsByEmpresaIdAndInicioVigencia(
                        securityTenantContext.empresaAtual(),
                        inicioVigencia
                );

        if (diaVigenteExiste) {
            throw new CadastroRegraApuracaoInvalidoException(
                    "O dia escolhido para vigência já possui uma regra cadastrada."
            );
        }
    }

    private void validar(RegraApuracaoDTO.Criar dto) {
        if (dto.faixaHoraExtra() == null || dto.faixaHoraExtra().isEmpty()) {
            throw new CadastroRegraApuracaoInvalidoException(
                    "Você deve preencher os campos de faixa de hora extra"
            );
        }

        if (dto.regraAdicionalNoturno() == null) {
            throw new CadastroRegraApuracaoInvalidoException(
                    "Você deve preencher os campos de regras de adicional noturno"
            );
        }

        validarInicioVigencia(dto.inicioVigencia());
        validarFaixas(dto.faixaHoraExtra());
        validarAdicionalNoturno(dto.regraAdicionalNoturno());
    }



}
