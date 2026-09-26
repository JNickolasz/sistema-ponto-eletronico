package com.example.projetoPBD.pontoCerto.service.mapper;

import com.example.projetoPBD.pontoCerto.domain.Empresa;
import com.example.projetoPBD.pontoCerto.domain.FaixaHoraExtra;
import com.example.projetoPBD.pontoCerto.domain.RegraAdicionalNoturno;
import com.example.projetoPBD.pontoCerto.domain.RegraApuracao;
import com.example.projetoPBD.pontoCerto.domain.enums.RegraApuracaoStatus;
import com.example.projetoPBD.pontoCerto.dto.RegraApuracaoConsulta;
import com.example.projetoPBD.pontoCerto.dto.domaindtos.FaixaHoraExtraDTO;
import com.example.projetoPBD.pontoCerto.dto.domaindtos.RegraAdicionalNoturnoDTO;
import com.example.projetoPBD.pontoCerto.dto.domaindtos.RegraApuracaoDTO;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

@Component
public class RegraApuracaoResponseMapper {

    /**
     * DTO de criação -> Entity
     */
    public RegraApuracao toEntity(
            RegraApuracaoDTO.Criar dto,
            Empresa empresa,
            LocalDate inicioVigencia
    ) {
        RegraApuracao regra = new RegraApuracao();

        regra.setEmpresa(empresa);
        regra.setInicioVigencia(inicioVigencia);
        regra.setLimiteDiarioExtraMinutos(dto.limiteDiarioExtraMinutos());

        dto.faixaHoraExtra()
                .stream()
                .map(this::toFaixaHoraExtraEntity)
                .forEach(regra::setFaixaHoraExtra);

        if (dto.regraAdicionalNoturno() != null) {
            regra.setAdicionalNoturno(
                    toAdicionalNoturnoEntity(dto.regraAdicionalNoturno())
            );
        }

        return regra;
    }

    /**
     * FaixaHoraExtra DTO de criação -> Entity
     */
    private FaixaHoraExtra toFaixaHoraExtraEntity(
            FaixaHoraExtraDTO.Criar dto
    ) {
        return new FaixaHoraExtra(
                dto.ordem(),
                dto.duracaoMinutos(),
                dto.percentual()
        );
    }

    /**
     * RegraAdicionalNoturno DTO de criação -> Entity
     */
    private RegraAdicionalNoturno toAdicionalNoturnoEntity(
            RegraAdicionalNoturnoDTO.Criar dto
    ) {
        return new RegraAdicionalNoturno(
                dto.horaInicio(),
                dto.horaFim(),
                dto.duracaoHoraNoturnaSegundos(),
                dto.percentual()
        );
    }

    /**
     * Consulta -> Response
     */
    public RegraApuracaoDTO.Response consultaToResponse(
            RegraApuracaoConsulta consulta
    ) {

        LocalDate fimVigencia = consulta.proximaVigencia() != null
                ? consulta.proximaVigencia().minusDays(1)
                : null;

        RegraApuracaoStatus status = RegraApuracaoStatus.valueOf(
                consulta.status()
        );

        RegraApuracao regra = consulta.regraApuracao();

        List<FaixaHoraExtraDTO.Response> faixas =
                regra.getFaixaHoraExtra()
                        .stream()
                        .map(this::toFaixaHoraExtraResponse)
                        .toList();

        RegraAdicionalNoturnoDTO.Response adicionalNoturno =
                toAdicionalNoturnoResponse(regra.getAdicionalNoturno());

        return new RegraApuracaoDTO.Response(
                regra.getId(),
                regra.getInicioVigencia(),
                fimVigencia,
                status,
                regra.getLimiteDiarioExtraMinutos(),
                faixas,
                adicionalNoturno
        );
    }

    /**
     * FaixaHoraExtra Entity -> Response
     */
    private FaixaHoraExtraDTO.Response toFaixaHoraExtraResponse(
            FaixaHoraExtra faixa
    ) {
        return new FaixaHoraExtraDTO.Response(
                faixa.getId(),
                faixa.getOrdem(),
                faixa.getDuracaoMinutos(),
                faixa.getPercentual()
        );
    }

    /**
     * RegraAdicionalNoturno Entity -> Response
     */
    private RegraAdicionalNoturnoDTO.Response toAdicionalNoturnoResponse(
            RegraAdicionalNoturno adicional
    ) {
        if (adicional == null) {
            return null;
        }

        return new RegraAdicionalNoturnoDTO.Response(
                adicional.getId(),
                adicional.getHoraInicio(),
                adicional.getHoraFim(),
                adicional.getDuracaoHoraNoturnaSegundos(),
                adicional.getPercentual()
        );
    }
}
