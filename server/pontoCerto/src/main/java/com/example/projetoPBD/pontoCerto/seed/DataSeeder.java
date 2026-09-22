package com.example.projetoPBD.pontoCerto.seed;

import com.example.projetoPBD.pontoCerto.domain.*;
import com.example.projetoPBD.pontoCerto.domain.enums.DiaSemana;
import com.example.projetoPBD.pontoCerto.domain.enums.PerfilAcesso;
import com.example.projetoPBD.pontoCerto.domain.enums.TipoRegime;
import com.example.projetoPBD.pontoCerto.repository.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Component
public class DataSeeder implements CommandLineRunner {

    private final FuncionarioRepository funcionarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmpresaRepository empresaRepository;
    private final JornadaRepository jornadaRepository;
    private final EscalaRepository escalaRepository;
    private final RegimeTrabalhoRepository regimeTrabalhoRepository;
    private final FeriadoRepository feriadoRepository;
    private final PontoFacultativoRepository pontoFacultativoRepository;

    public DataSeeder(FuncionarioRepository funcionarioRepository,
                      PasswordEncoder passwordEncoder,
                      EmpresaRepository empresaRepository,
                      JornadaRepository jornadaRepository,
                      EscalaRepository escalaRepository,
                      RegimeTrabalhoRepository regimeTrabalhoRepository,
                      FeriadoRepository feriadoRepository,
                      PontoFacultativoRepository pontoFacultativoRepository) {
        this.funcionarioRepository = funcionarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.empresaRepository = empresaRepository;
        this.jornadaRepository = jornadaRepository;
        this.escalaRepository = escalaRepository;
        this.regimeTrabalhoRepository = regimeTrabalhoRepository;
        this.feriadoRepository = feriadoRepository;
        this.pontoFacultativoRepository = pontoFacultativoRepository;
    }

    @Override
    public void run(String... args) throws Exception {
        if (funcionarioRepository.count() == 0) {

            Empresa empresa = new Empresa();
            empresa.setCnpj("00.000.000/0000-00");
            empresa.setRazaoSocial("Empresa Ponto Certo");
            empresa.setEndereco("Rua ABC, Numero 123");
            empresa.setSubDominio("pontocertoWeb");
            empresa.setLogoUrl("www.logoPontoCerto.com");
            empresa = empresaRepository.save(empresa);

            Funcionario adminRh = new Funcionario();
            adminRh.setNomeCompleto("Administrador RH");
            adminRh.setUsuario("admin.rh");
            adminRh.setEmail("rh@empresa.com");
            adminRh.setMatricula("RH001");
            adminRh.setCpf("00000000000");
            adminRh.setPisPasep("00000000000");
            adminRh.setCargo("Analista de RH");
            adminRh.setPerfilAcesso(PerfilAcesso.RH);
            adminRh.setDataAdmissao(LocalDate.now());
            adminRh.setEmpresa(empresa);
            adminRh.setSenhaHash(passwordEncoder.encode("admin123"));
            funcionarioRepository.save(adminRh);

            Funcionario gestor = new Funcionario();
            gestor.setNomeCompleto("Mariana Gestora");
            gestor.setUsuario("gestor.teste");
            gestor.setEmail("gestor@empresa.com");
            gestor.setMatricula("GES001");
            gestor.setCpf("22222222222");
            gestor.setPisPasep("22222222222");
            gestor.setCargo("Gerente de Operações");
            gestor.setPerfilAcesso(PerfilAcesso.GESTOR);
            gestor.setDataAdmissao(LocalDate.now());
            gestor.setEmpresa(empresa);
            gestor.setSenhaHash(passwordEncoder.encode("gestor123"));
            funcionarioRepository.save(gestor);

            Funcionario colaborador = new Funcionario();
            colaborador.setNomeCompleto("João Colaborador");
            colaborador.setUsuario("colaborador.teste");
            colaborador.setEmail("colaborador@empresa.com");
            colaborador.setMatricula("COL001");
            colaborador.setCpf("11111111111");
            colaborador.setPisPasep("11111111111");
            colaborador.setCargo("Desenvolvedor");
            colaborador.setPerfilAcesso(PerfilAcesso.COLABORADOR);
            colaborador.setDataAdmissao(LocalDate.now());
            colaborador.setEmpresa(empresa);
            colaborador.setGestor(gestor);
            colaborador.setSenhaHash(passwordEncoder.encode("user123"));
            funcionarioRepository.save(colaborador);

            Funcionario plantonista = new Funcionario();
            plantonista.setNomeCompleto("Carlos Plantonista");
            plantonista.setUsuario("plantonista.teste");
            plantonista.setEmail("plantonista@empresa.com");
            plantonista.setMatricula("COL002");
            plantonista.setCpf("33333333333");
            plantonista.setPisPasep("33333333333");
            plantonista.setCargo("Operador de Suporte");
            plantonista.setPerfilAcesso(PerfilAcesso.COLABORADOR);
            plantonista.setDataAdmissao(LocalDate.now());
            plantonista.setEmpresa(empresa);
            plantonista.setGestor(gestor);
            plantonista.setSenhaHash(passwordEncoder.encode("user123"));
            funcionarioRepository.save(plantonista);

            Jornada jornada = new Jornada();
            jornada.setEmpresa(empresa);
            jornada.setNome("Jornada Administrativa 40h");
            jornada.setToleranciaMinutos(10);
            jornada.setCargaHorariaSemanalMinutos(2400);

            List<JornadaDia> dias = new ArrayList<>();
            for (DiaSemana diaSemana : DiaSemana.values()) {
                JornadaDia dia = new JornadaDia();
                dia.setJornada(jornada);
                dia.setDiaSemana(diaSemana);
                if (diaSemana == DiaSemana.DOMINGO || diaSemana == DiaSemana.SÁBADO) {
                    dia.setDiaTrabalho(false);
                    dia.setCargaDiariaMinutos(0);
                } else {
                    dia.setDiaTrabalho(true);
                    dia.setHoraEntrada(LocalTime.of(8, 0));
                    dia.setHoraSaida(LocalTime.of(17, 0));
                    dia.setIntervaloInicio(LocalTime.of(12, 0));
                    dia.setIntervaloFim(LocalTime.of(13, 0));
                    dia.setCargaDiariaMinutos(480);
                }
                dias.add(dia);
            }
            jornada.setDias(dias);
            jornada = jornadaRepository.save(jornada);

            Escala escala = new Escala();
            escala.setEmpresa(empresa);
            escala.setNome("Escala 12x36 Diurna");
            escala.setDiasTrabalho(1);
            escala.setDiasFolga(1);
            escala.setHoraEntrada(LocalTime.of(7, 0));
            escala.setHoraSaida(LocalTime.of(19, 0));
            escala.setIntervaloInicio(LocalTime.of(12, 0));
            escala.setIntervaloFim(LocalTime.of(13, 0));
            escala.setToleranciaMinutos(10);
            escala.setCargaDiariaMinutos(660);
            escala = escalaRepository.save(escala);

            RegimeTrabalho regimeJornada = new RegimeTrabalho();
            regimeJornada.setFuncionario(colaborador);
            regimeJornada.setTipoRegime(TipoRegime.JORNADA);
            regimeJornada.setJornada(jornada);
            regimeJornada.setDataInicioVigencia(LocalDate.now().minusMonths(1));
            regimeTrabalhoRepository.save(regimeJornada);

            RegimeTrabalho regimeEscala = new RegimeTrabalho();
            regimeEscala.setFuncionario(plantonista);
            regimeEscala.setTipoRegime(TipoRegime.ESCALA);
            regimeEscala.setEscala(escala);
            regimeEscala.setDataInicioVigencia(LocalDate.now().minusMonths(1));
            regimeTrabalhoRepository.save(regimeEscala);

            Feriado natal = new Feriado();
            natal.setDescricao("Natal");
            natal.setData(LocalDate.of(2026, 12, 25));
            natal.setAlcance(Alcance.NACIONAL);
            natal.setAtivo(true);
            feriadoRepository.save(natal);

            Feriado saoJoao = new Feriado();
            saoJoao.setDescricao("São João");
            saoJoao.setData(LocalDate.of(2026, 6, 24));
            saoJoao.setAlcance(Alcance.ESTADUAL);
            saoJoao.setUf("PE");
            saoJoao.setAtivo(true);
            feriadoRepository.save(saoJoao);

            Feriado municipal = new Feriado();
            municipal.setDescricao("Aniversário do Município");
            municipal.setData(LocalDate.of(2026, 5, 6));
            municipal.setAlcance(Alcance.MUNICIPAL);
            municipal.setUf("PE");
            municipal.setMunicipio("Serra Talhada");
            municipal.setEmpresa(empresa);
            municipal.setAtivo(true);
            feriadoRepository.save(municipal);

            PontoFacultativo carnaval = new PontoFacultativo();
            carnaval.setDescricao("Carnaval");
            carnaval.setData(LocalDate.of(2026, 2, 17));
            carnaval.setAlcance(Alcance.NACIONAL);
            carnaval.setAtivo(true);
            pontoFacultativoRepository.save(carnaval);

            System.out.println(">>> Seed executado com sucesso! Contas e dados de teste prontos:");
            System.out.println("    - RH:          admin.rh          | Senha: admin123");
            System.out.println("    - GESTOR:      gestor.teste      | Senha: gestor123");
            System.out.println("    - COLABORADOR: colaborador.teste | Senha: user123 (Regime: Jornada 40h)");
            System.out.println("    - COLABORADOR: plantonista.teste | Senha: user123 (Regime: Escala 12x36)");
            System.out.println("    - FERIADOS:    Nacional (Natal), Estadual (São João - PE), Municipal (Serra Talhada - PE)");
            System.out.println("    - FACULTATIVO: Carnaval (Nacional)");
        }
    }
}
