package com.example.projetoPBD.pontoCerto.repository;

import com.example.projetoPBD.pontoCerto.domain.Empresa;
import com.example.projetoPBD.pontoCerto.domain.Funcionario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FuncionarioRepository extends JpaRepository<Funcionario, UUID> {

    Optional<Funcionario> findByUsuarioAndEmpresaId(String usuario, UUID empresaId);
    boolean existsByUsuarioAndEmpresaId(String usuario, UUID empresaId);

    Optional<Funcionario> findByMatriculaAndEmpresaId(String matricula, UUID empresaId);
    boolean existsByMatriculaAndEmpresaId(String matricula, UUID empresaId);

    Optional<Funcionario> findByCpfAndEmpresaId(String cpf, UUID empresaId);
    boolean existsByCpfAndEmpresaId(String cpf, UUID empresaId);

    Optional<Funcionario> findByPisPasepAndEmpresaId(String pisPasep, UUID empresaId); // <-- Corrigido aqui
    boolean existsByPisPasepAndEmpresaId(String pisPasep, UUID empresaId);

    @Query("SELECT f FROM Funcionario f WHERE f.empresa.id = :empresaId AND (" +
            "LOWER(f.nomeCompleto) LIKE LOWER(CONCAT('%', :termo, '%')) OR " +
            "f.cpf = :termo OR f.pisPasep = :termo OR f.matricula = :termo)")
    List<Funcionario> buscarPorTermo(@Param("empresaId") UUID empresaId, @Param("termo") String termo);

    List<Funcionario> findByEmpresaId(UUID empresaId);

    Optional<Funcionario> findByUsuario(String usuario);
    boolean existsByUsuario(String usuario);
    /*
        N vai mais usar esse pq agr como o funcionario tem empresa_id
        da pra fazer a busca completa e correta

        Na vdd vai sim por conta do login, tem q checar o nome e uuid
        mas como n tem como saber o empresaId ainda n tem como usar o
        findByIdAndEmpresa ou algum outro metodo com o empresaId
    */
}
