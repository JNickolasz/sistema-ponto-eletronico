package com.example.projetoPBD.pontoCerto.service;


import com.example.projetoPBD.pontoCerto.domain.FaixaHoraExtra;
import com.example.projetoPBD.pontoCerto.domain.RegraApuracao;
import com.example.projetoPBD.pontoCerto.domain.enums.VigenciaStatus;
import com.example.projetoPBD.pontoCerto.dto.domaindtos.RegraApuracaoDTO;
import com.example.projetoPBD.pontoCerto.repository.EmpresaRepository;
import com.example.projetoPBD.pontoCerto.repository.RegraApuracaoRepository;
import com.example.projetoPBD.pontoCerto.security.SecurityTenantContext;
import com.example.projetoPBD.pontoCerto.service.exceptions.DataRegraApuracaoInvalidaException;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Optional;

@Service
public class RegraApuracaoService {


    private final RegraApuracaoRepository regraApuracaoRepository;
    private final SecurityTenantContext securityTenantContext;
    private final EmpresaRepository empresaRepository;

    public RegraApuracaoService(RegraApuracaoRepository regraApuracaoRepository, SecurityTenantContext securityTenantContext, EmpresaRepository empresaRepository) {
        this.regraApuracaoRepository = regraApuracaoRepository;
        this.securityTenantContext = securityTenantContext;
        this.empresaRepository = empresaRepository;
    }

    public ResponseEntity<RegraApuracaoDTO> cadastro(RegraApuracaoDTO.Criar regraDto){


        if (regraDto.inicioVigencia().isBefore(LocalDate.now())) {
            throw new DataRegraApuracaoInvalidaException("A data de inicio de vigência não pode ser antes da data atual!");
        }

        // Pega Ultima Regra de Apuração pela empresa (Respeitando o Multi-Tenant) e pela data de referencia (Nesse caso a data atual).
        Optional<RegraApuracao> regraApuracaoAtual = regraApuracaoRepository.findFirstByEmpresaIdAndInicioVigenciaLessThanEqualOrderByInicioVigenciaDesc(
                securityTenantContext.empresaAtual(),
                LocalDate.now());

        regraApuracaoAtual.ifPresent(regraApuracao -> regraApuracaoRepository.save(revogarRegraAtual(regraApuracao)));


        FaixaHoraExtra faixaHoraExtra = new FaixaHoraExtra();

        RegraApuracao regraApuracao = new RegraApuracao();
        regraApuracao.setInicioVigencia(regraDto.inicioVigencia());
        regraApuracao.setEmpresa(empresaRepository.findById(securityTenantContext.empresaAtual()).get());
        // regraApuracao.setFaixaHoraExtras();
        return null;

    }

    private RegraApuracao revogarRegraAtual(RegraApuracao regra){
        regra.setVigenciaStatus(VigenciaStatus.REVOGADO);
        return regra;
    }

}
