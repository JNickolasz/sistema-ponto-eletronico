package com.example.projetoPBD.pontoCerto.service;


import com.example.projetoPBD.pontoCerto.dto.PainelDTO;
import com.example.projetoPBD.pontoCerto.repository.FuncionarioRepository;
import com.example.projetoPBD.pontoCerto.service.exceptions.AcessoNegadoException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class PainelService {

    private final FuncionarioRepository funcionarioRepository;


    public PainelService(FuncionarioRepository funcionarioRepository) {
        this.funcionarioRepository = funcionarioRepository;
    }

    public PainelDTO.AcessoEspelho verEspelhoColaborador(UUID id, String usuarioLogado) {
        var funcionarioLogado = funcionarioRepository.findByUsuario(usuarioLogado).orElseThrow(
                () -> new UsernameNotFoundException("Usuário não encontrado."));


        if (!(funcionarioLogado.getId().equals(id))){
            throw new AcessoNegadoException("Você só pode acessar o seu próprio espelho de ponto");}


        return new PainelDTO.AcessoEspelho(
                "Id_Espelho" + id,
                "Dono_Espelho" + funcionarioLogado.getUsuario()
        );

    }


    public PainelDTO.AcessoLiberado liberarAcesso(String usuarioLogado, String permissoes) {
        return new PainelDTO.AcessoLiberado(usuarioLogado, permissoes);
    }
}
