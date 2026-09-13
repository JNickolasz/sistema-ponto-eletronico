package com.example.projetoPBD.pontoCerto.seed;

import com.example.projetoPBD.pontoCerto.domain.Empresa;
import com.example.projetoPBD.pontoCerto.domain.Funcionario;
import com.example.projetoPBD.pontoCerto.domain.PerfilAcesso;
import com.example.projetoPBD.pontoCerto.repository.FuncionarioRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
public class DataSeeder implements CommandLineRunner {

    private final FuncionarioRepository funcionarioRepository;
    private final PasswordEncoder passwordEncoder;

    public DataSeeder(FuncionarioRepository funcionarioRepository, PasswordEncoder passwordEncoder) {
        this.funcionarioRepository = funcionarioRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) throws Exception {
        // Evita duplicar se a aplicação reiniciar
        if (funcionarioRepository.count() == 0) {
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
            adminRh.setEmpresaId(new Empresa("Bugteco", "12345"));

            // Cifra a senha inicial obrigatória
            adminRh.setSenhaHash(passwordEncoder.encode("admin123"));

            funcionarioRepository.save(adminRh);
            System.out.println(">>> Seed executado: Usuário RH Master criado com sucesso! Login: admin.rh | Senha: admin123");
        }
    }
}
