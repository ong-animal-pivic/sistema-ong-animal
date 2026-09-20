package com.umc.sistemaonganimal.domain.exception;

public class VoluntarioNotFoundException extends EntityNotFoundException {
    public VoluntarioNotFoundException(String message) {
        super(message);
    }

    public VoluntarioNotFoundException(Long id) {
        this(String.format("Não existe um registro de Voluntário com o id: %d", id));
    }
}
