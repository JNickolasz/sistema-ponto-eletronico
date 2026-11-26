package com.example.projetoPBD.pontoCerto.config;

import com.example.projetoPBD.pontoCerto.domain.*;
import com.example.projetoPBD.pontoCerto.domain.enums.*;
import com.example.projetoPBD.pontoCerto.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

/**
 * Seeder idempotente da empresa fictícia para apresentação de Pitch (Vértice Engenharia & Sistemas).
 * Ativado exclusivamente quando APP_SEED_PITCH=true (nunca roda em produção por padrão).
 */
@Component
@ConditionalOnProperty(name = "app.seed.pitch", havingValue = "true", matchIfMissing = false)
public class PitchDataSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(PitchDataSeeder.class);

    @Value("${app.seed.pitch.password:Vertice@2026}")
    private String pitchPassword;

    private final EmpresaRepository empresaRepository;
    private final LocalDeTrabalhoRepository localRepository;
    private final EstacaoRepository estacaoRepository;
    private final FuncionarioRepository funcionarioRepository;
    private final TipoOcorrenciaRepository tipoOcorrenciaRepository;
    private final FeriadoRepository feriadoRepository;
    private final BatidaRepository batidaRepository;
    private final PasswordEncoder passwordEncoder;

    public PitchDataSeeder(
            EmpresaRepository empresaRepository,
            LocalDeTrabalhoRepository localRepository,
            EstacaoRepository estacaoRepository,
            FuncionarioRepository funcionarioRepository,
            TipoOcorrenciaRepository tipoOcorrenciaRepository,
            FeriadoRepository feriadoRepository,
            BatidaRepository batidaRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.empresaRepository = empresaRepository;
        this.localRepository = localRepository;
        this.estacaoRepository = estacaoRepository;
        this.funcionarioRepository = funcionarioRepository;
        this.tipoOcorrenciaRepository = tipoOcorrenciaRepository;
        this.feriadoRepository = feriadoRepository;
        this.batidaRepository = batidaRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        var existenteOpt = empresaRepository.findBySubDominioIgnoreCase("vertice");
        if (existenteOpt.isPresent()) {
            Empresa existente = existenteOpt.get();
            if (existente.getLogoUrl() == null || existente.getLogoUrl().isBlank() || existente.getConfigWhiteLabel() == null) {
                existente.setLogoUrl("/logos/vertice.svg");
                existente.setConfigWhiteLabel(new WhiteLabel(
                        "vertice",
                        "/logos/vertice.svg",
                        "#0f766e",
                        "#115e59"
                ));
                empresaRepository.save(existente);
                log.info("[PitchDataSeeder] Branding White-Label da empresa 'vertice' atualizado (logo + cor #0f766e).");
            }
            preencherCargoEmailExistentes(existente.getId());
            return;
        }

        log.info("[PitchDataSeeder] Populando empresa modelo 'Vértice Engenharia & Sistemas' (subdominio: vertice)...");

        Empresa empresa = new Empresa();
        empresa.setRazaoSocial("Vértice Engenharia & Sistemas Ltda");
        empresa.setCnpj("42819304000175");
        empresa.setSubDominio("vertice");
        empresa.setEndereco("Av. Boa Viagem, 1420 — Recife/PE");
        empresa.setLogoUrl("/logos/vertice.svg");
        empresa.setConfigWhiteLabel(new WhiteLabel(
                "vertice",
                "/logos/vertice.svg",
                "#0f766e",
                "#115e59"
        ));
        empresa = empresaRepository.save(empresa);

        LocalDeTrabalho matriz = new LocalDeTrabalho();
        matriz.setEmpresa(empresa);
        matriz.setNome("Matriz — Recife");
        matriz.setEndereco("Av. Boa Viagem, 1420");
        matriz.setMunicipio("Recife");
        matriz.setUf("PE");
        matriz.setRaioMetros(150);
        matriz = localRepository.save(matriz);

        LocalDeTrabalho filial = new LocalDeTrabalho();
        filial.setEmpresa(empresa);
        filial.setNome("Filial — São Paulo");
        filial.setEndereco("Av. Paulista, 900");
        filial.setMunicipio("São Paulo");
        filial.setUf("SP");
        filial.setRaioMetros(150);
        filial = localRepository.save(filial);

        Estacao estacao = new Estacao();
        estacao.setEmpresa(empresa);
        estacao.setLocalTrabalho(matriz);
        estacao.setIdentificacao("Estação Web — Recepção Matriz");
        estacao.setCodigo("EST-VERT-01");
        estacao.setStatus(StatusEquipamento.ATIVO);
        estacao.setStatusEstacao(StatusEstacao.ATIVO);
        estacao = estacaoRepository.save(estacao);

        String hash = passwordEncoder.encode(pitchPassword);

        Funcionario rh = salvarFunc(empresa, matriz, null, "Helena Vasconcelos", "rh.vertice", hash,
                "11144477735", "12076152267", "VRT-001", PerfilAcesso.RH);
        Funcionario gestorEng = salvarFunc(empresa, matriz, rh, "Bruno Albuquerque", "gestor.eng", hash,
                "52998224725", "17033259504", "VRT-002", PerfilAcesso.GESTOR);
        Funcionario gestorOps = salvarFunc(empresa, filial, rh, "Patrícia Mendes", "gestor.ops", hash,
                "15314992004", "20496891439", "VRT-003", PerfilAcesso.GESTOR);

        Funcionario camila = salvarFunc(empresa, matriz, gestorEng, "Camila Ferreira", "camila.dev", hash,
                "71428793860", "12678069386", "VRT-101", PerfilAcesso.COLABORADOR);
        Funcionario rafael = salvarFunc(empresa, matriz, gestorEng, "Rafael Nogueira", "rafael.eng", hash,
                "86288366757", "16329004982", "VRT-102", PerfilAcesso.COLABORADOR);
        Funcionario mariana = salvarFunc(empresa, filial, gestorOps, "Mariana Souza", "mariana.ops", hash,
                "39053344705", "12008914521", "VRT-103", PerfilAcesso.COLABORADOR);

        // Tipos de ocorrência da empresa
        criarTipoOcorrencia(empresa, "Atestado Médico", "Abono integral mediante apresentação de atestado",
                TipoAbonoOcorrencia.INTEGRAL, null, true);
        criarTipoOcorrencia(empresa, "Declaração de Comparecimento", "Abono parcial de 2 horas para consulta",
                TipoAbonoOcorrencia.PARCIAL, 120, true);
        criarTipoOcorrencia(empresa, "Férias", "Período de férias regulamentares",
                TipoAbonoOcorrencia.INTEGRAL, null, false);

        // Feriado municipal da Matriz
        Feriado feriadoRecife = new Feriado();
        feriadoRecife.setEmpresa(empresa);
        feriadoRecife.setDescricao("Nossa Senhora do Carmo (Recife)");
        feriadoRecife.setData(LocalDate.of(LocalDate.now().getYear(), 7, 16));
        feriadoRecife.setAlcance(Alcance.MUNICIPAL);
        feriadoRecife.setUf("PE");
        feriadoRecife.setMunicipio("Recife");
        feriadoRecife.setAtivo(true);
        feriadoRepository.save(feriadoRecife);

        // Batidas realistas dos últimos 5 dias úteis
        List<Funcionario> equipe = List.of(camila, rafael, mariana);
        LocalDate cursor = LocalDate.now().minusDays(1);
        int diasPopulados = 0;
        while (diasPopulados < 5) {
            if (cursor.getDayOfWeek() != DayOfWeek.SATURDAY && cursor.getDayOfWeek() != DayOfWeek.SUNDAY) {
                for (Funcionario f : equipe) {
                    salvarBatida(f, estacao, LocalDateTime.of(cursor, LocalTime.of(8, 0)), TipoRegistro.ENTRADA);
                    salvarBatida(f, estacao, LocalDateTime.of(cursor, LocalTime.of(12, 0)), TipoRegistro.SAIDA);
                    salvarBatida(f, estacao, LocalDateTime.of(cursor, LocalTime.of(13, 0)), TipoRegistro.ENTRADA);
                    // Deixa Mariana com 1 marcação faltando no último dia para gerar inconsistência real
                    if (!(diasPopulados == 0 && f.getId().equals(mariana.getId()))) {
                        salvarBatida(f, estacao, LocalDateTime.of(cursor, LocalTime.of(17, 5)), TipoRegistro.SAIDA);
                    }
                }
                diasPopulados++;
            }
            cursor = cursor.minusDays(1);
        }

        log.info("[PitchDataSeeder] Empresa modelo 'Vértice Engenharia' populada! Estação ID: {}", estacao.getId());
    }

    private Funcionario salvarFunc(
            Empresa empresa, LocalDeTrabalho local, Funcionario gestor,
            String nome, String usuario, String hash, String cpf, String pis, String matricula, PerfilAcesso perfil
    ) {
        Funcionario f = new Funcionario();
        f.setEmpresa(empresa);
        f.setLocalDeTrabalho(local);
        f.setGestor(gestor);
        f.setNomeCompleto(nome);
        f.setUsuario(usuario);
        f.setSenhaHash(hash);
        f.setCpf(cpf);
        f.setPisPasep(pis);
        f.setMatricula(matricula);
        f.setPerfilAcesso(perfil);
        f.setCargo(cargoPadraoParaUsuario(usuario, perfil));
        f.setEmail(usuario + "@vertice.com.br");
        f.setDataAdmissao(LocalDate.now().minusMonths(8));
        return funcionarioRepository.save(f);
    }

    private void preencherCargoEmailExistentes(java.util.UUID empresaId) {
        List<Funcionario> lista = funcionarioRepository.findByEmpresaId(empresaId);
        for (Funcionario f : lista) {
            boolean alterou = false;
            if (f.getCargo() == null || f.getCargo().isBlank()) {
                f.setCargo(cargoPadraoParaUsuario(f.getUsuario(), f.getPerfilAcesso()));
                alterou = true;
            }
            if (f.getEmail() == null || f.getEmail().isBlank()) {
                f.setEmail((f.getUsuario() != null ? f.getUsuario() : "colaborador") + "@vertice.com.br");
                alterou = true;
            }
            if (alterou) {
                funcionarioRepository.save(f);
            }
        }
    }

    private String cargoPadraoParaUsuario(String usuario, PerfilAcesso perfil) {
        if ("rh.vertice".equalsIgnoreCase(usuario)) return "Coordenadora de RH";
        if ("gestor.eng".equalsIgnoreCase(usuario)) return "Gerente de Engenharia";
        if ("gestor.ops".equalsIgnoreCase(usuario)) return "Gerente de Operações";
        if ("camila.dev".equalsIgnoreCase(usuario)) return "Engenheira de Software";
        if ("rafael.eng".equalsIgnoreCase(usuario)) return "Engenheiro Civil";
        if ("mariana.ops".equalsIgnoreCase(usuario)) return "Analista de Operações";
        if (perfil == PerfilAcesso.RH) return "Analista de RH";
        if (perfil == PerfilAcesso.GESTOR) return "Gestor de Equipe";
        return "Analista / Colaborador";
    }

    private void criarTipoOcorrencia(
            Empresa empresa, String nome, String desc, TipoAbonoOcorrencia tipoAbono, Integer minutos, boolean exigeAnexo
    ) {
        TipoOcorrencia t = new TipoOcorrencia();
        t.setEmpresa(empresa);
        t.setNome(nome);
        t.setDescricao(desc);
        t.setTipoAbono(tipoAbono);
        t.setMinutosAbonoPadrao(minutos);
        t.setExigeAnexo(exigeAnexo);
        t.setAtivo(true);
        tipoOcorrenciaRepository.save(t);
    }

    private void salvarBatida(Funcionario f, Equipamento eq, LocalDateTime dataHora, TipoRegistro tipo) {
        Batida b = new Batida();
        b.setFuncionario(f);
        b.setEquipamento(eq);
        b.setDataHora(dataHora);
        b.setTipoRegistro(tipo);
        b.setStatusBatida(StatusBatida.NORMAL);
        b.setTipoOrigem(TipoOrigem.ESTACAO);
        b.setNsr(batidaRepository.proximoNsrSequence());
        batidaRepository.save(b);
    }
}
