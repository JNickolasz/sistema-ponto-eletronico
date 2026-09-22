package com.example.projetoPBD.pontoCerto.service;

import com.example.projetoPBD.pontoCerto.domain.*;
import com.example.projetoPBD.pontoCerto.domain.enums.DiaSemana;
import com.example.projetoPBD.pontoCerto.domain.enums.TipoRegime;
import com.example.projetoPBD.pontoCerto.dto.HorarioPrevistoDTO;
import com.example.projetoPBD.pontoCerto.dto.RegimeTrabalhoDTO;
import com.example.projetoPBD.pontoCerto.repository.EscalaRepository;
import com.example.projetoPBD.pontoCerto.repository.FuncionarioRepository;
import com.example.projetoPBD.pontoCerto.repository.JornadaRepository;
import com.example.projetoPBD.pontoCerto.repository.RegimeTrabalhoRepository;
import com.example.projetoPBD.pontoCerto.security.SecurityTenantContext;
import com.example.projetoPBD.pontoCerto.service.exceptions.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class RegimeTrabalhoService {

    private final RegimeTrabalhoRepository regimeRepository;
    private final FuncionarioRepository funcionarioRepository;
    private final JornadaRepository jornadaRepository;
    private final EscalaRepository escalaRepository;
    private final SecurityTenantContext securityTenantContext;

    public RegimeTrabalhoService(RegimeTrabalhoRepository regimeRepository,
                                 FuncionarioRepository funcionarioRepository,
                                 JornadaRepository jornadaRepository,
                                 EscalaRepository escalaRepository,
                                 SecurityTenantContext securityTenantContext) {
        this.regimeRepository = regimeRepository;
        this.funcionarioRepository = funcionarioRepository;
        this.jornadaRepository = jornadaRepository;
        this.escalaRepository = escalaRepository;
        this.securityTenantContext = securityTenantContext;
    }

    @Transactional
    public RegimeTrabalhoDTO.Response vincular(RegimeTrabalhoDTO.Vincular dto) {
        UUID empresaId = securityTenantContext.empresaAtual();

        Funcionario funcionario = funcionarioRepository.findById(dto.funcionarioId())
                .orElseThrow(() -> new UsuarioNaoEncontradoNaEmpresa("Colaborador não encontrado!"));

        if (!funcionario.getEmpresa().getId().equals(empresaId)) {
            throw new UsuarioNaoEncontradoNaEmpresa("Colaborador não pertence à sua empresa!");
        }

        if (dto.dataInicioVigencia() == null) {
            throw new RegimeTrabalhoInvalidoException("A data de início de vigência é obrigatória.");
        }

        if (dto.dataFimVigencia() != null && dto.dataFimVigencia().isBefore(dto.dataInicioVigencia())) {
            throw new RegimeTrabalhoInvalidoException("A data de fim de vigência não pode ser anterior à de início.");
        }

        List<RegimeTrabalho> sobrepostos = regimeRepository.findSobrepostos(
                dto.funcionarioId(),
                dto.dataInicioVigencia(),
                dto.dataFimVigencia()
        );
        if (!sobrepostos.isEmpty()) {
            throw new RegimeTrabalhoConflitoException("O colaborador já possui um regime vigente com período sobreposto.");
        }

        RegimeTrabalho regime = new RegimeTrabalho();
        regime.setFuncionario(funcionario);
        regime.setTipoRegime(dto.tipoRegime());
        regime.setDataInicioVigencia(dto.dataInicioVigencia());
        regime.setDataFimVigencia(dto.dataFimVigencia());

        if (dto.tipoRegime() == TipoRegime.JORNADA) {
            if (dto.jornadaId() == null) {
                throw new RegimeTrabalhoInvalidoException("O id da jornada é obrigatório para regime do tipo JORNADA.");
            }
            Jornada jornada = jornadaRepository.findByIdAndEmpresaId(dto.jornadaId(), empresaId)
                    .orElseThrow(() -> new JornadaNaoEncontradaException("Jornada não encontrada!"));
            regime.setJornada(jornada);
            regime.setEscala(null);
        } else if (dto.tipoRegime() == TipoRegime.ESCALA) {
            if (dto.escalaId() == null) {
                throw new RegimeTrabalhoInvalidoException("O id da escala é obrigatório para regime do tipo ESCALA.");
            }
            Escala escala = escalaRepository.findByIdAndEmpresaId(dto.escalaId(), empresaId)
                    .orElseThrow(() -> new EscalaNaoEncontradaException("Escala não encontrada!"));
            regime.setEscala(escala);
            regime.setJornada(null);
        } else {
            throw new RegimeTrabalhoInvalidoException("Tipo de regime inválido.");
        }

        RegimeTrabalho salvo = regimeRepository.save(regime);
        return toResponse(salvo);
    }

    @Transactional(readOnly = true)
    public List<RegimeTrabalhoDTO.Response> listarPorFuncionario(UUID funcionarioId) {
        return regimeRepository.findByFuncionarioIdOrderByDataInicioVigenciaDesc(funcionarioId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public HorarioPrevistoDTO.Response obterHorarioPrevisto(UUID funcionarioId, LocalDate data) {
        DiaSemana diaSemana = DiaSemana.from(data.getDayOfWeek());

        Optional<RegimeTrabalho> regimeOpt = regimeRepository.findVigenteNaData(funcionarioId, data);

        if (regimeOpt.isEmpty()) {
            return new HorarioPrevistoDTO.Response(
                    funcionarioId,
                    data,
                    diaSemana,
                    false,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    0,
                    "Nenhum regime de trabalho vigente para esta data"
            );
        }

        RegimeTrabalho regime = regimeOpt.get();

        if (regime.getTipoRegime() == TipoRegime.JORNADA) {
            Jornada jornada = regime.getJornada();
            Optional<JornadaDia> diaOpt = jornada.getDias() != null
                    ? jornada.getDias().stream().filter(d -> d.getDiaSemana() == diaSemana).findFirst()
                    : Optional.empty();

            if (diaOpt.isPresent() && diaOpt.get().isDiaTrabalho()) {
                JornadaDia dia = diaOpt.get();
                return new HorarioPrevistoDTO.Response(
                        funcionarioId,
                        data,
                        diaSemana,
                        true,
                        TipoRegime.JORNADA,
                        jornada.getNome(),
                        dia.getHoraEntrada(),
                        dia.getHoraSaida(),
                        dia.getIntervaloInicio(),
                        dia.getIntervaloFim(),
                        jornada.getToleranciaMinutos(),
                        dia.getCargaDiariaMinutos(),
                        "Dia de trabalho previsto conforme jornada semanal"
                );
            } else {
                return new HorarioPrevistoDTO.Response(
                        funcionarioId,
                        data,
                        diaSemana,
                        false,
                        TipoRegime.JORNADA,
                        jornada.getNome(),
                        null,
                        null,
                        null,
                        null,
                        jornada.getToleranciaMinutos(),
                        0,
                        "Descanso semanal remunerado (folga)"
                );
            }
        } else {
            Escala escala = regime.getEscala();
            long diasDecorridos = ChronoUnit.DAYS.between(regime.getDataInicioVigencia(), data);
            int ciclo = escala.getDiasTrabalho() + escala.getDiasFolga();
            int diaNoCiclo = ciclo > 0 ? (int) (diasDecorridos % ciclo) : 0;
            if (diaNoCiclo < 0) diaNoCiclo += ciclo;

            boolean diaTrabalho = diaNoCiclo < escala.getDiasTrabalho();

            if (diaTrabalho) {
                return new HorarioPrevistoDTO.Response(
                        funcionarioId,
                        data,
                        diaSemana,
                        true,
                        TipoRegime.ESCALA,
                        escala.getNome(),
                        escala.getHoraEntrada(),
                        escala.getHoraSaida(),
                        escala.getIntervaloInicio(),
                        escala.getIntervaloFim(),
                        escala.getToleranciaMinutos(),
                        escala.getCargaDiariaMinutos(),
                        "Dia de trabalho previsto conforme escala"
                );
            } else {
                return new HorarioPrevistoDTO.Response(
                        funcionarioId,
                        data,
                        diaSemana,
                        false,
                        TipoRegime.ESCALA,
                        escala.getNome(),
                        null,
                        null,
                        null,
                        null,
                        escala.getToleranciaMinutos(),
                        0,
                        "Folga da escala de trabalho"
                );
            }
        }
    }

    private RegimeTrabalhoDTO.Response toResponse(RegimeTrabalho regime) {
        return new RegimeTrabalhoDTO.Response(
                regime.getId(),
                regime.getFuncionario().getId(),
                regime.getFuncionario().getNomeCompleto(),
                regime.getTipoRegime(),
                regime.getJornada() != null ? regime.getJornada().getId() : null,
                regime.getJornada() != null ? regime.getJornada().getNome() : null,
                regime.getEscala() != null ? regime.getEscala().getId() : null,
                regime.getEscala() != null ? regime.getEscala().getNome() : null,
                regime.getDataInicioVigencia(),
                regime.getDataFimVigencia()
        );
    }
}
