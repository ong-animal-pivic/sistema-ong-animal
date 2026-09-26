package com.umc.sistemaonganimal.domain.exception;

public class AreaNotFoundException extends EntityNotFoundException {
    public AreaNotFoundException(String message) {
        super(message);
    }

    public AreaNotFoundException(Long id) {
        this(String.format("Não existe um registro de uma área com o id: %d", id));
    }
}
