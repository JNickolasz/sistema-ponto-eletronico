package com.example.projetoPBD.pontoCerto.seed;

import com.example.projetoPBD.pontoCerto.domain.Empresa;
import com.example.projetoPBD.pontoCerto.domain.Funcionario;
import com.example.projetoPBD.pontoCerto.domain.PerfilAcesso;
import com.example.projetoPBD.pontoCerto.repository.EmpresaRepository;
import com.example.projetoPBD.pontoCerto.repository.FuncionarioRepository;
import jakarta.validation.Valid;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
public class DataSeeder implements CommandLineRunner {

    private final FuncionarioRepository funcionarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmpresaRepository empresaRepository;

    public DataSeeder(FuncionarioRepository funcionarioRepository, PasswordEncoder passwordEncoder, EmpresaRepository empresaRepository) {
        this.funcionarioRepository = funcionarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.empresaRepository = empresaRepository;
    }

    @Override
    public void run(String... args) throws Exception {

        // Evita duplicar se a aplicação reiniciar
        if (funcionarioRepository.count() == 0) {

            Empresa empresa = new Empresa();
            empresa.setCnpj("00.000.000/0000-00");
            empresa.setRazaoSocial("Empresa Ponto Certo");
            empresa.setEndereco("Rua ABC, Numero 123");
            empresa.setSubDominio("pontocertoWeb");
            empresa.setLogoUrl("www.logoPontoCerto.com");
            empresaRepository.save(empresa);


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

            // Cifra a senha inicial obrigatória
            adminRh.setSenhaHash(passwordEncoder.encode("admin123"));

            funcionarioRepository.save(adminRh);

            // Gestor Padrão para Testes
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

            // Colaborador Padrão para Testes
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

            System.out.println(">>> Seed executado com sucesso! Contas de teste prontas:");
            System.out.println("    - RH:          admin.rh          | Senha: admin123");
            System.out.println("    - GESTOR:      gestor.teste      | Senha: gestor123");
            System.out.println("    - COLABORADOR: colaborador.teste | Senha: user123");
        }
    }
}
