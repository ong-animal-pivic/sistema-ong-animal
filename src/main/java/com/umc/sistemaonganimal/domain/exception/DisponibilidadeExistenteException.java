package com.umc.sistemaonganimal.domain.exception;

public class DisponibilidadeExistenteException extends EntityExistsException {
    public DisponibilidadeExistenteException(String message) {
        super(message);
    }

    public DisponibilidadeExistenteException() {
        this("Esta disponibilidade já está cadastrada para o voluntário.");
    }
}
