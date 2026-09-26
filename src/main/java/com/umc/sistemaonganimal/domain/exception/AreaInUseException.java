package com.umc.sistemaonganimal.domain.exception;

public class AreaInUseException extends EntityInUseException {
    public AreaInUseException(String message) {
        super(message);
    }

    public AreaInUseException(Long id) {
        this(String.format("A entidade Área de código %d não pode ser removida pois está em uso", id));
    }
}
