package com.umc.sistemaonganimal.domain.exception;

public class VoluntarioInUseException extends EntityInUseException {
    public VoluntarioInUseException(String message) {
        super(message);
    }

    public VoluntarioInUseException(Long id) {
        this(String.format("A entidade Voluntário de código %d não pode ser removida pois está em uso", id));
    }
}
