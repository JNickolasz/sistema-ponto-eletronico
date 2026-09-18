package com.example.projetoPBD.pontoCerto.service;

import com.example.projetoPBD.pontoCerto.domain.Empresa;
import com.example.projetoPBD.pontoCerto.domain.Funcionario;
import com.example.projetoPBD.pontoCerto.domain.LocalDeTrabalho;
import com.example.projetoPBD.pontoCerto.dto.domaindtos.FuncionarioDTO;
import com.example.projetoPBD.pontoCerto.repository.EmpresaRepository;
import com.example.projetoPBD.pontoCerto.repository.FuncionarioRepository;
import com.example.projetoPBD.pontoCerto.repository.LocalDeTrabalhoRepository;
import com.example.projetoPBD.pontoCerto.service.exceptions.*;
import org.springframework.security.crypto.password.PasswordEncoder;

import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
public class FuncionarioService {

    private final FuncionarioRepository repository;
    private final EmpresaRepository empresaRepository;
    private final LocalDeTrabalhoRepository localDeTrabalhoRepository;
    private final PasswordEncoder passwordEncoder;

    public FuncionarioService(FuncionarioRepository repository, EmpresaRepository empresaRepository, LocalDeTrabalhoRepository localDeTrabalhoRepository, PasswordEncoder passwordEncoder) {
        this.repository = repository;
        this.empresaRepository = empresaRepository;
        this.localDeTrabalhoRepository = localDeTrabalhoRepository;
        this.passwordEncoder = passwordEncoder;

    }

    public FuncionarioDTO.Response criar(FuncionarioDTO.Criar dto) {
        Empresa empresa = empresaRepository.findById(dto.empresaId())
                .orElseThrow(() -> new EmpresaNaoEncontradaException("Empresa não encontrada!"));

        if (repository.existsByUsuarioAndEmpresaId(dto.usuario(), empresa.getId())) {
            throw new UsuarioDuplicadoException("Usuário já cadastrado!");
        }

        if(repository.existsByCPFAndEmpresaId(dto.cpf(), empresa.getId())) {
            throw new CPFJaCadastradoException("CPF já cadastrado nesta empresa!");
        }

        if(repository.existsByMatriculaAndEmpresaId(dto.matricula(), empresa.getId())) {
            throw new MatriculaJaCadastradaException("Matricula já cadastrada nesta empresa!");
        }

        if(repository.existsByPisPasepAndEmpresaId(dto.pis_pasep(), empresa.getId())) {
            throw new PisPasepJaCadastradoException("PIS/PASEP já cadastrado nesta empresa!");
        }

        if(!isPisValido(dto.pis_pasep())) {
            throw new PisPasepInvalidoException("PIS/PASEP com dígito verificador inválido!");
        }

        Funcionario funcionario = new Funcionario();
        funcionario.setEmpresa(empresa);
        funcionario.setNomeCompleto(dto.nomeCompleto());
        funcionario.setUsuario(dto.usuario());
        funcionario.setSenhaHash(passwordEncoder.encode(dto.senha()));
        funcionario.setCpf(dto.cpf());
        funcionario.setPisPasep(dto.pis_pasep());
        funcionario.setMatricula(dto.matricula());
        funcionario.setPerfilAcesso(dto.perfilAcesso());
        // injeta a data atual
        funcionario.setDataAdmissao(dto.dataAdmissao() != null ? dto.dataAdmissao() : LocalDate.now());

        // Busca e vincula o local de trabalho que você acabou de criar na entidade!
        if (dto.localTrabalhoId() != null) {
            LocalDeTrabalho local = localDeTrabalhoRepository.findById(dto.localTrabalhoId())
                    .orElseThrow(() -> new RuntimeException("Local de trabalho não encontrado!"));
            funcionario.setLocalDeTrabalho(local);
        }

        // Vincula o gestor se tiver sido informado
        if (dto.gestorId() != null) {
            Funcionario gestor = repository.findById(dto.gestorId())
                    .orElseThrow(() -> new RuntimeException("Gestor não encontrado!"));
            funcionario.setGestor(gestor);
        }

        Funcionario salvo = repository.save(funcionario);

        return new FuncionarioDTO.Response(salvo);
    }

    private boolean isPisValido(String pis) {
        if(pis == null || pis.isEmpty()) return false;
        pis = pis.replaceAll("\\D", ""); // Tira pontos e traços
        if (pis.length() != 11) return false;

        int[] multiplicadores = {3, 2, 9, 8, 7, 6, 5, 4, 3, 2};
        int soma = 0;

        for (int i = 0; i < 10; i++) {
            soma += Character.getNumericValue(pis.charAt(i)) * multiplicadores[i];
        }

        int resto = soma % 11;
        int digitoEsperado = resto < 2 ? 0 : 11 - resto;

        return digitoEsperado == Character.getNumericValue(pis.charAt(10));
    }

    public FuncionarioDTO.Response desligar(UUID id, FuncionarioDTO.Desligamento dto) {
        Funcionario funcionario = repository.findById(id)
                .orElseThrow(() -> new UsuarioNaoEncontradoNaEmpresa("Colaborador não encontrado!"));

        if (dto.dataDesligamento() == null) {
            throw new IllegalArgumentException("A data de desligamento é obrigatória!");
        }

        if (funcionario.getDataAdmissao() != null && dto.dataDesligamento().isBefore(funcionario.getDataAdmissao())) {
            throw new IllegalArgumentException("A data de desligamento não pode ser anterior à data de admissão!");
        }

        funcionario.setDataDesligamento(dto.dataDesligamento());
        Funcionario salvo = repository.save(funcionario);
        return new FuncionarioDTO.Response(salvo);
    }

    public boolean podeRegistrarPonto(UUID funcionarioId, LocalDate dataBatida) {
        Funcionario funcionario = repository.findById(funcionarioId)
                .orElseThrow(() -> new UsuarioNaoEncontradoNaEmpresa("Colaborador não encontrado!"));

        if (funcionario.getDataDesligamento() != null && !dataBatida.isBefore(funcionario.getDataDesligamento())) {
            return false;
        }

        if (funcionario.getDataAdmissao() != null && dataBatida.isBefore(funcionario.getDataAdmissao())) {
            return false;
        }

        return true;
    }

    public void excluir(UUID id) {
        Funcionario funcionario = repository.findById(id)
                .orElseThrow(() -> new UsuarioNaoEncontradoNaEmpresa("Colaborador não encontrado!"));

        if (possuiMarcacoes(id)) {
            throw new ColaboradorComMarcacaoException("Não é possível excluir o colaborador porque ele já possui marcações de ponto registradas.");
        }

        repository.delete(funcionario);
    }

    public boolean possuiMarcacoes(UUID funcionarioId) {
        return false;
    }

    public java.util.List<FuncionarioDTO.Response> buscar(UUID empresaId, String termo) {
        java.util.List<Funcionario> funcionarios;
        if (termo == null || termo.isBlank()) {
            funcionarios = repository.findByEmpresaId(empresaId);
        } else {
            funcionarios = repository.buscarPorTermo(empresaId, termo.trim());
        }
        return funcionarios.stream().map(FuncionarioDTO.Response::new).toList();
    }
}
