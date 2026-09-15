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
            System.out.println(">>> Seed executado: Usuário RH Master criado com sucesso! Login: admin.rh | Senha: admin123");
        }
    }
}
