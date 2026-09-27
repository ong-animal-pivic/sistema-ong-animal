package com.umc.sistemaonganimal.domain.repository;

import com.umc.sistemaonganimal.domain.model.Disponibilidade;
import com.umc.sistemaonganimal.domain.model.enums.general.DiaSemana;
import com.umc.sistemaonganimal.domain.model.enums.general.Turno;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DisponibilidadeRepository extends JpaRepository<Disponibilidade, Long> {
    Optional<Disponibilidade> findByDiaSemanaAndTurno(DiaSemana diaSemana, Turno turno);
}
