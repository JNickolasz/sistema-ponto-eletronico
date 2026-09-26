package com.example.projetoPBD.pontoCerto.service;


import com.example.projetoPBD.pontoCerto.domain.FaixaHoraExtra;
import com.example.projetoPBD.pontoCerto.domain.RegraAdicionalNoturno;
import com.example.projetoPBD.pontoCerto.domain.RegraApuracao;
import com.example.projetoPBD.pontoCerto.domain.enums.VigenciaStatus;
import com.example.projetoPBD.pontoCerto.dto.RegraApuracaoConsultaDTO;
import com.example.projetoPBD.pontoCerto.dto.domaindtos.RegraApuracaoDTO;
import com.example.projetoPBD.pontoCerto.dto.projection.RegraApuracaoProjection;
import com.example.projetoPBD.pontoCerto.repository.EmpresaRepository;
import com.example.projetoPBD.pontoCerto.repository.FaixaHoraExtraRepository;
import com.example.projetoPBD.pontoCerto.repository.RegraAdicionalNoturnoRepository;
import com.example.projetoPBD.pontoCerto.repository.RegraApuracaoRepository;
import com.example.projetoPBD.pontoCerto.security.SecurityTenantContext;
import com.example.projetoPBD.pontoCerto.service.exceptions.CadastroRegraApuracaoInvalidoException;
import com.example.projetoPBD.pontoCerto.service.exceptions.DataRegraApuracaoInvalidaException;
import jakarta.transaction.Transactional;
import org.springframework.http.ResponseEntity;
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
    private final FaixaHoraExtraRepository faixaHoraExtraRepository;
    private final RegraAdicionalNoturnoRepository regraAdicionalNoturnoRepository;

    public RegraApuracaoService(RegraApuracaoRepository regraApuracaoRepository, SecurityTenantContext securityTenantContext, EmpresaRepository empresaRepository, FaixaHoraExtraRepository faixaHoraExtraRepository, RegraAdicionalNoturnoRepository regraAdicionalNoturnoRepository) {
        this.regraApuracaoRepository = regraApuracaoRepository;
        this.securityTenantContext = securityTenantContext;
        this.empresaRepository = empresaRepository;
        this.faixaHoraExtraRepository = faixaHoraExtraRepository;
        this.regraAdicionalNoturnoRepository = regraAdicionalNoturnoRepository;
    }

    @Transactional
    public ResponseEntity<RegraApuracaoDTO> cadastro(RegraApuracaoDTO.Criar regraDto){


        if (regraDto.inicioVigencia().isBefore(LocalDate.now())) throw new CadastroRegraApuracaoInvalidoException("A data de inicio de vigência não pode ser anterior a data atual!");

        // Pega Ultima Regra de Apuração pela empresa (Respeitando o Multi-Tenant) e pela data de referencia (Nesse caso a data atual).
        Optional<RegraApuracao> regraApuracaoAtual = regraApuracaoRepository.findFirstByEmpresaIdAndInicioVigenciaLessThanEqualOrderByInicioVigenciaDesc(
                securityTenantContext.empresaAtual(),
                LocalDate.now());



        FaixaHoraExtra faixaHoraExtraNovo = new FaixaHoraExtra();
        RegraAdicionalNoturno regraAdicionalNoturnoNovo = new RegraAdicionalNoturno();



        RegraApuracao regraApuracao = new RegraApuracao();
        regraApuracao.setInicioVigencia(regraDto.inicioVigencia());
        regraApuracao.setEmpresa(empresaRepository.findById(securityTenantContext.empresaAtual()).get());
        // regraApuracao.setFaixaHoraExtras();


    }


    @Transactional
    public List<RegraApuracaoDTO.Response> listar() {
        return regraApuracaoRepository
                .findAllByEmpresaId(securityTenantContext.empresaAtual())
                .stream()
                .map(regra -> {
                    // RegraApuracaoConsultaDTO regra = regraApuracaoRepository.listVigenciasAndRegraApuracaoStatus();

                    return new RegraApuracaoDTO.Response(
                            regra.getId(),
                            regra.getInicioVigencia(),
                            regra.getFimVigencia(),
                            regra.getLimiteDiarioExtraMinutos(),
                            consulta.getStatus(),
                            regra.getFaixaHoraExtra()
                    );
                })
                .toList();
    }

}
