package com.umc.sistemaonganimal.domain.exception;

public class VoluntarioExistenteException extends EntityExistsException {
    public VoluntarioExistenteException(String message) {
        super(message);
    }

    public static VoluntarioExistenteException porEmail(String email) {
        return new VoluntarioExistenteException(
                String.format("Já existe um voluntário cadastrado com o e-mail '%s'", email));
    }
}
