package com.umc.sistemaonganimal.domain.repository;

import com.umc.sistemaonganimal.domain.model.VoluntarioDisponibilidade;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface VoluntarioDisponibilidadeRepository extends JpaRepository<VoluntarioDisponibilidade, Long> {
    List<VoluntarioDisponibilidade> findByVoluntarioId(Long voluntarioId);

    boolean existsByVoluntarioIdAndDisponibilidadeId(Long voluntarioId, Long disponibilidadeId);

    Optional<VoluntarioDisponibilidade> findByVoluntarioIdAndDisponibilidadeId(Long voluntarioId, Long disponibilidadeId);
}
