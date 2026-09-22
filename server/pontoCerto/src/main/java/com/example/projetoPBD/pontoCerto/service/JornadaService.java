package com.example.projetoPBD.pontoCerto.service;

import com.example.projetoPBD.pontoCerto.domain.Empresa;
import com.example.projetoPBD.pontoCerto.domain.Jornada;
import com.example.projetoPBD.pontoCerto.domain.JornadaDia;
import com.example.projetoPBD.pontoCerto.dto.domaindtos.JornadaDTO;
import com.example.projetoPBD.pontoCerto.dto.domaindtos.JornadaDiaDTO;
import com.example.projetoPBD.pontoCerto.repository.EmpresaRepository;
import com.example.projetoPBD.pontoCerto.repository.JornadaRepository;
import com.example.projetoPBD.pontoCerto.security.SecurityTenantContext;
import com.example.projetoPBD.pontoCerto.service.exceptions.EmpresaNaoEncontradaException;
import com.example.projetoPBD.pontoCerto.service.exceptions.JornadaNaoEncontradaException;
import com.example.projetoPBD.pontoCerto.service.utils.CalcularCargaDiariaMinutos;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class JornadaService {

    private final JornadaRepository jornadaRepository;
    private final EmpresaRepository empresaRepository;
    private final SecurityTenantContext securityTenantContext;

    public JornadaService(JornadaRepository jornadaRepository,
                          EmpresaRepository empresaRepository,
                          SecurityTenantContext securityTenantContext) {
        this.jornadaRepository = jornadaRepository;
        this.empresaRepository = empresaRepository;
        this.securityTenantContext = securityTenantContext;
    }

    @Transactional
    public JornadaDTO.Response cadastrar(JornadaDTO.Criar dto) {
        UUID empresaId = securityTenantContext.empresaAtual();
        Empresa empresa = empresaRepository.findById(empresaId)
                .orElseThrow(() -> new EmpresaNaoEncontradaException("Empresa não encontrada!"));

        Jornada jornada = new Jornada();
        jornada.setEmpresa(empresa);
        jornada.setNome(dto.nome());
        jornada.setToleranciaMinutos(dto.toleranciaMinutos() != null ? dto.toleranciaMinutos() : 10);

        List<JornadaDia> diasEntidade = new ArrayList<>();
        int cargaSemanalTotal = 0;

        if (dto.dias() != null) {
            for (JornadaDiaDTO.Criar diaDto : dto.dias()) {
                JornadaDia dia = new JornadaDia();
                dia.setJornada(jornada);
                dia.setDiaSemana(diaDto.diaSemana());
                dia.setDiaTrabalho(diaDto.diaTrabalho());
                dia.setHoraEntrada(diaDto.horaEntrada());
                dia.setHoraSaida(diaDto.horaSaida());
                dia.setIntervaloInicio(diaDto.intervaloInicio());
                dia.setIntervaloFim(diaDto.intervaloFim());

                int cargaDiaria = 0;
                if (diaDto.diaTrabalho()) {
                    cargaDiaria = CalcularCargaDiariaMinutos.calcularCargaDiariaMinutos(
                            diaDto.horaEntrada(),
                            diaDto.horaSaida(),
                            diaDto.intervaloInicio(),
                            diaDto.intervaloFim()
                    );
                    cargaSemanalTotal += cargaDiaria;
                }
                dia.setCargaDiariaMinutos(cargaDiaria);
                diasEntidade.add(dia);
            }
        }

        jornada.setCargaHorariaSemanalMinutos(cargaSemanalTotal);
        jornada.setDias(diasEntidade);

        Jornada jornadaSalva = jornadaRepository.save(jornada);
        return toResponse(jornadaSalva);
    }

    @Transactional(readOnly = true)
    public List<JornadaDTO.Response> listar() {
        UUID empresaId = securityTenantContext.empresaAtual();
        return jornadaRepository.findByEmpresaId(empresaId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public JornadaDTO.Response buscarPorId(UUID id) {
        UUID empresaId = securityTenantContext.empresaAtual();
        Jornada jornada = jornadaRepository.findByIdAndEmpresaId(id, empresaId)
                .orElseThrow(() -> new JornadaNaoEncontradaException("Jornada não encontrada com ID: " + id));
        return toResponse(jornada);
    }

    private JornadaDTO.Response toResponse(Jornada jornada) {
        List<JornadaDiaDTO.Response> diasDto = jornada.getDias() != null
                ? jornada.getDias().stream()
                .map(d -> new JornadaDiaDTO.Response(
                        d.getId(),
                        d.getDiaSemana(),
                        d.isDiaTrabalho(),
                        d.getHoraEntrada(),
                        d.getHoraSaida(),
                        d.getIntervaloInicio(),
                        d.getIntervaloFim(),
                        d.getCargaDiariaMinutos()
                ))
                .toList()
                : List.of();

        return new JornadaDTO.Response(
                jornada.getId(),
                jornada.getEmpresa() != null ? jornada.getEmpresa().getId() : null,
                jornada.getNome(),
                jornada.getToleranciaMinutos(),
                jornada.getCargaHorariaSemanalMinutos(),
                diasDto
        );
    }
}
