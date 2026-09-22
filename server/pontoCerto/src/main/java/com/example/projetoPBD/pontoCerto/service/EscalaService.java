package com.example.projetoPBD.pontoCerto.service;

import com.example.projetoPBD.pontoCerto.domain.Empresa;
import com.example.projetoPBD.pontoCerto.domain.Escala;
import com.example.projetoPBD.pontoCerto.dto.domaindtos.EscalaDTO;
import com.example.projetoPBD.pontoCerto.repository.EmpresaRepository;
import com.example.projetoPBD.pontoCerto.repository.EscalaRepository;
import com.example.projetoPBD.pontoCerto.security.SecurityTenantContext;
import com.example.projetoPBD.pontoCerto.service.exceptions.EmpresaNaoEncontradaException;
import com.example.projetoPBD.pontoCerto.service.exceptions.EscalaNaoEncontradaException;
import com.example.projetoPBD.pontoCerto.service.utils.CalcularCargaDiariaMinutos;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class EscalaService {

    private final EscalaRepository escalaRepository;
    private final EmpresaRepository empresaRepository;
    private final SecurityTenantContext securityTenantContext;

    public EscalaService(EscalaRepository escalaRepository,
                         EmpresaRepository empresaRepository,
                         SecurityTenantContext securityTenantContext) {
        this.escalaRepository = escalaRepository;
        this.empresaRepository = empresaRepository;
        this.securityTenantContext = securityTenantContext;
    }

    @Transactional
    public EscalaDTO.Response cadastrar(EscalaDTO.Criar dto) {
        UUID empresaId = securityTenantContext.empresaAtual();
        Empresa empresa = empresaRepository.findById(empresaId)
                .orElseThrow(() -> new EmpresaNaoEncontradaException("Empresa não encontrada!"));

        Escala escala = new Escala();
        escala.setEmpresa(empresa);
        escala.setNome(dto.nome());
        escala.setDiasTrabalho(dto.diasTrabalho());
        escala.setDiasFolga(dto.diasFolga());
        escala.setHoraEntrada(dto.horaEntrada());
        escala.setHoraSaida(dto.horaSaida());
        escala.setIntervaloInicio(dto.intervaloInicio());
        escala.setIntervaloFim(dto.intervaloFim());
        escala.setToleranciaMinutos(dto.toleranciaMinutos() != null ? dto.toleranciaMinutos() : 10);

        int cargaDiaria = CalcularCargaDiariaMinutos.calcularCargaDiariaMinutos(
                dto.horaEntrada(),
                dto.horaSaida(),
                dto.intervaloInicio(),
                dto.intervaloFim()
        );
        escala.setCargaDiariaMinutos(cargaDiaria);

        Escala escalaSalva = escalaRepository.save(escala);
        return toResponse(escalaSalva);
    }

    @Transactional(readOnly = true)
    public List<EscalaDTO.Response> listar() {
        UUID empresaId = securityTenantContext.empresaAtual();
        return escalaRepository.findByEmpresaId(empresaId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public EscalaDTO.Response buscarPorId(UUID id) {
        UUID empresaId = securityTenantContext.empresaAtual();
        Escala escala = escalaRepository.findByIdAndEmpresaId(id, empresaId)
                .orElseThrow(() -> new EscalaNaoEncontradaException("Escala não encontrada com ID: " + id));
        return toResponse(escala);
    }

    private EscalaDTO.Response toResponse(Escala escala) {
        return new EscalaDTO.Response(
                escala.getId(),
                escala.getEmpresa() != null ? escala.getEmpresa().getId() : null,
                escala.getNome(),
                escala.getDiasTrabalho(),
                escala.getDiasFolga(),
                escala.getHoraEntrada(),
                escala.getHoraSaida(),
                escala.getIntervaloInicio(),
                escala.getIntervaloFim(),
                escala.getToleranciaMinutos(),
                escala.getCargaDiariaMinutos()
        );
    }
}
