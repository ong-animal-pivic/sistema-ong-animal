package com.umc.sistemaonganimal.api.dto.request;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// Só a observação do vínculo é editável: dia e turno identificam a disponibilidade.
// Nula ou em branco apaga a observação.
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class DisponibilidadeObservacaoRequestDTO {

    @Size(max = 255)
    private String observacao;
}
