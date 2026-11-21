package com.example.projetoPBD.pontoCerto.service;

import com.example.projetoPBD.pontoCerto.domain.Empresa;
import com.example.projetoPBD.pontoCerto.domain.Equipamento;
import com.example.projetoPBD.pontoCerto.domain.Funcionario;
import com.example.projetoPBD.pontoCerto.domain.LocalDeTrabalho;
import com.example.projetoPBD.pontoCerto.domain.WhiteLabel;
import com.example.projetoPBD.pontoCerto.domain.enums.PerfilAcesso;
import com.example.projetoPBD.pontoCerto.domain.enums.StatusInconsistencia;
import com.example.projetoPBD.pontoCerto.repository.BatidaRepository;
import com.example.projetoPBD.pontoCerto.repository.EmpresaRepository;
import com.example.projetoPBD.pontoCerto.repository.EquipamentoRepository;
import com.example.projetoPBD.pontoCerto.repository.FuncionarioRepository;
import com.example.projetoPBD.pontoCerto.repository.InconsistenciaRepository;
import com.example.projetoPBD.pontoCerto.repository.LocalDeTrabalhoRepository;
import com.example.projetoPBD.pontoCerto.service.exceptions.EmpresaNaoEncontradaException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class AdminDashboardService {

    private final EmpresaRepository empresaRepository;
    private final FuncionarioRepository funcionarioRepository;
    private final LocalDeTrabalhoRepository localRepository;
    private final EquipamentoRepository equipamentoRepository;
    private final BatidaRepository batidaRepository;
    private final InconsistenciaRepository inconsistenciaRepository;

    public AdminDashboardService(
            EmpresaRepository empresaRepository,
            FuncionarioRepository funcionarioRepository,
            LocalDeTrabalhoRepository localRepository,
            EquipamentoRepository equipamentoRepository,
            BatidaRepository batidaRepository,
            InconsistenciaRepository inconsistenciaRepository
    ) {
        this.empresaRepository = empresaRepository;
        this.funcionarioRepository = funcionarioRepository;
        this.localRepository = localRepository;
        this.equipamentoRepository = equipamentoRepository;
        this.batidaRepository = batidaRepository;
        this.inconsistenciaRepository = inconsistenciaRepository;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> obterEstatisticasGlobais() {
        List<Empresa> empresas = empresaRepository.findAll();
        LocalDate hoje = LocalDate.now();
        LocalDateTime inicioHoje = hoje.atStartOfDay();
        LocalDateTime fimHoje = hoje.atTime(LocalTime.MAX);
        YearMonth mesAtual = YearMonth.from(hoje);
        LocalDateTime inicioMes = mesAtual.atDay(1).atStartOfDay();
        LocalDateTime fimMes = mesAtual.atEndOfMonth().atTime(LocalTime.MAX);

        long totalColab = 0;
        long totalAtivos = 0;
        long totalDesligados = 0;
        long totalLocais = 0;
        long totalEquip = 0;
        long totalBatidasHoje = 0;
        long totalBatidasMes = 0;
        long totalInconsistenciasAbertas = 0;

        List<Map<String, Object>> resumoEmpresas = new ArrayList<>();

        for (Empresa emp : empresas) {
            List<Funcionario> funcs = funcionarioRepository.findByEmpresaId(emp.getId());
            long ativos = funcs.stream().filter(f -> f.getDataDesligamento() == null || f.getDataDesligamento().isAfter(hoje)).count();
            long desligados = funcs.size() - ativos;
            long locais = localRepository.findByEmpresaId(emp.getId()).size();
            long equips = equipamentoRepository.findByEmpresaId(emp.getId()).size();
            long bHoje = batidaRepository.countByEmpresaIdAndDataHoraBetween(emp.getId(), inicioHoje, fimHoje);
            long bMes = batidaRepository.countByEmpresaIdAndDataHoraBetween(emp.getId(), inicioMes, fimMes);
            long incAbertas = inconsistenciaRepository.countByEmpresaIdAndSituacao(emp.getId(), StatusInconsistencia.ABERTA);

            totalColab += funcs.size();
            totalAtivos += ativos;
            totalDesligados += desligados;
            totalLocais += locais;
            totalEquip += equips;
            totalBatidasHoje += bHoje;
            totalBatidasMes += bMes;
            totalInconsistenciasAbertas += incAbertas;

            WhiteLabel wl = emp.getConfigWhiteLabel();
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", emp.getId());
            item.put("razaoSocial", emp.getRazaoSocial());
            item.put("cnpj", emp.getCnpj());
            item.put("subdominio", emp.getSubDominio());
            item.put("endereco", emp.getEndereco());
            item.put("logoUrl", resolverLogo(emp));
            item.put("corPrimaria", wl != null && wl.getPrimaryColor() != null ? wl.getPrimaryColor() : "#2563eb");
            item.put("totalColaboradores", funcs.size());
            item.put("colaboradoresAtivos", ativos);
            item.put("colaboradoresDesligados", desligados);
            item.put("totalLocais", locais);
            item.put("totalEquipamentos", equips);
            item.put("batidasHoje", bHoje);
            item.put("batidasMes", bMes);
            item.put("inconsistenciasAbertas", incAbertas);
            resumoEmpresas.add(item);
        }

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("totalEmpresas", empresas.size());
        payload.put("totalColaboradores", totalColab);
        payload.put("colaboradoresAtivos", totalAtivos);
        payload.put("colaboradoresDesligados", totalDesligados);
        payload.put("totalLocaisTrabalho", totalLocais);
        payload.put("totalEquipamentos", totalEquip);
        payload.put("batidasHoje", totalBatidasHoje);
        payload.put("batidasMes", totalBatidasMes);
        payload.put("inconsistenciasAbertas", totalInconsistenciasAbertas);
        payload.put("competenciaAtual", mesAtual.toString());
        payload.put("empresas", resumoEmpresas);
        return payload;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> obterDetalhesEmpresa(UUID empresaId) {
        Empresa emp = empresaRepository.findById(empresaId)
                .orElseThrow(() -> new EmpresaNaoEncontradaException("Empresa não encontrada."));

        LocalDate hoje = LocalDate.now();
        YearMonth mesAtual = YearMonth.from(hoje);
        List<Funcionario> funcs = funcionarioRepository.findByEmpresaId(emp.getId());
        List<LocalDeTrabalho> locais = localRepository.findByEmpresaId(emp.getId());
        List<Equipamento> equips = equipamentoRepository.findByEmpresaId(emp.getId());

        long ativos = funcs.stream().filter(f -> f.getDataDesligamento() == null || f.getDataDesligamento().isAfter(hoje)).count();
        long rhs = funcs.stream().filter(f -> f.getPerfilAcesso() == PerfilAcesso.RH).count();
        long gestores = funcs.stream().filter(f -> f.getPerfilAcesso() == PerfilAcesso.GESTOR).count();
        long colaboradores = funcs.stream().filter(f -> f.getPerfilAcesso() == PerfilAcesso.COLABORADOR).count();

        long bHoje = batidaRepository.countByEmpresaIdAndDataHoraBetween(
                emp.getId(), hoje.atStartOfDay(), hoje.atTime(LocalTime.MAX));
        long bMes = batidaRepository.countByEmpresaIdAndDataHoraBetween(
                emp.getId(), mesAtual.atDay(1).atStartOfDay(), mesAtual.atEndOfMonth().atTime(LocalTime.MAX));
        long incAbertas = inconsistenciaRepository.countByEmpresaIdAndSituacao(emp.getId(), StatusInconsistencia.ABERTA);

        WhiteLabel wl = emp.getConfigWhiteLabel();
        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("id", emp.getId());
        detail.put("razaoSocial", emp.getRazaoSocial());
        detail.put("cnpj", emp.getCnpj());
        detail.put("subdominio", emp.getSubDominio());
        detail.put("endereco", emp.getEndereco());
        detail.put("logoUrl", resolverLogo(emp));
        detail.put("corPrimaria", wl != null && wl.getPrimaryColor() != null ? wl.getPrimaryColor() : "#2563eb");
        detail.put("corSecundaria", wl != null && wl.getSecundaryColor() != null ? wl.getSecundaryColor() : "#1d4ed8");
        detail.put("totalColaboradores", funcs.size());
        detail.put("colaboradoresAtivos", ativos);
        detail.put("colaboradoresDesligados", funcs.size() - ativos);
        detail.put("perfis", Map.of("RH", rhs, "GESTOR", gestores, "COLABORADOR", colaboradores));
        detail.put("batidasHoje", bHoje);
        detail.put("batidasMes", bMes);
        detail.put("inconsistenciasAbertas", incAbertas);
        detail.put("locais", locais.stream().map(l -> Map.of(
                "id", l.getId(),
                "nome", l.getNome(),
                "municipio", l.getMunicipio() != null ? l.getMunicipio() : "",
                "uf", l.getUf() != null ? l.getUf() : ""
        )).toList());
        detail.put("equipamentos", equips.stream().map(e -> Map.of(
                "id", e.getId(),
                "identificacao", e.getIdentificacao(),
                "codigo", e.getCodigo(),
                "tipo", e.getTipo().name(),
                "status", e.getStatus().name()
        )).toList());
        return detail;
    }

    @Transactional
    public Map<String, Object> atualizarBranding(UUID empresaId, String logoUrl, String corPrimaria, String corSecundaria) {
        Empresa emp = empresaRepository.findById(empresaId)
                .orElseThrow(() -> new EmpresaNaoEncontradaException("Empresa não encontrada."));

        if (logoUrl != null) {
            emp.setLogoUrl(logoUrl.trim());
        }
        WhiteLabel wl = emp.getConfigWhiteLabel() != null ? emp.getConfigWhiteLabel() : new WhiteLabel();
        if (logoUrl != null) {
            wl.setUrlLogo(logoUrl.trim());
        }
        if (corPrimaria != null && !corPrimaria.isBlank()) {
            wl.setPrimaryColor(corPrimaria.trim());
        }
        if (corSecundaria != null && !corSecundaria.isBlank()) {
            wl.setSecundaryColor(corSecundaria.trim());
        }
        emp.setConfigWhiteLabel(wl);
        empresaRepository.save(emp);

        return obterDetalhesEmpresa(empresaId);
    }

    private String resolverLogo(Empresa emp) {
        WhiteLabel wl = emp.getConfigWhiteLabel();
        if (wl != null && wl.getUrlLogo() != null && !wl.getUrlLogo().isBlank()) {
            return wl.getUrlLogo();
        }
        return emp.getLogoUrl() != null ? emp.getLogoUrl() : "";
    }
}
