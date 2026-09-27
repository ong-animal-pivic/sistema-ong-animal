package com.umc.sistemaonganimal.domain.exception;

public class DisponibilidadeNotFoundException extends EntityNotFoundException {
    public DisponibilidadeNotFoundException(String message) {
        super(message);
    }

    public DisponibilidadeNotFoundException(Long voluntarioId, Long disponibilidadeId) {
        this(String.format("Não existe a disponibilidade de id %d cadastrada para o voluntário de id %d",
                disponibilidadeId, voluntarioId));
    }
}
