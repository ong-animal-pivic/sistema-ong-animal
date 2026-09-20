package com.umc.sistemaonganimal.domain.repository;

import com.umc.sistemaonganimal.domain.model.Voluntario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface VoluntarioRepository extends JpaRepository<Voluntario, Long> {
    boolean existsByContatoEmailIgnoreCase(String email);

    boolean existsByContatoEmailIgnoreCaseAndIdNot(String email, Long id);

    boolean existsByResponsavelId(Long responsavelId);
}
