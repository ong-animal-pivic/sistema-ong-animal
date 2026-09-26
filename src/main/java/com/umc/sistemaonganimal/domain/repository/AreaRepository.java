package com.umc.sistemaonganimal.domain.repository;

import com.umc.sistemaonganimal.domain.model.Area;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AreaRepository extends JpaRepository<Area, Long> {

    // "voluntarios" é lazy e open-in-view=false: sem o @EntityGraph, serializar a
    // coleção no controller lançaria LazyInitializationException.
    @EntityGraph(attributePaths = "voluntarios")
    @Override
    Optional<Area> findById(Long id);

    @EntityGraph(attributePaths = "voluntarios")
    @Override
    List<Area> findAll();
}
