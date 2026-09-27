package com.umc.sistemaonganimal.api.dto.request;

import com.umc.sistemaonganimal.domain.model.enums.general.DiaSemana;
import com.umc.sistemaonganimal.domain.model.enums.general.Turno;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class DisponibilidadeRequestDTO {

    @NotNull
    private DiaSemana diaSemana;

    @NotNull
    private Turno turno;

    @Size(max = 255)
    private String observacao;
}
