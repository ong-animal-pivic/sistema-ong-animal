package com.umc.sistemaonganimal.api.dto.response;

import com.umc.sistemaonganimal.domain.model.VoluntarioDisponibilidade;
import com.umc.sistemaonganimal.domain.model.enums.general.DiaSemana;
import com.umc.sistemaonganimal.domain.model.enums.general.Turno;
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
public class DisponibilidadeResponseDTO {

    // id da Disponibilidade (não do vínculo): é o usado no
    // DELETE /voluntarios/{voluntarioId}/disponibilidades/{id}.
    private Long id;
    private DiaSemana diaSemana;
    private Turno turno;
    private String observacao;

    public static DisponibilidadeResponseDTO fromEntity(VoluntarioDisponibilidade vinculo) {
        if (vinculo == null) {
            return null;
        }
        return DisponibilidadeResponseDTO.builder()
                .id(vinculo.getDisponibilidade().getId())
                .diaSemana(vinculo.getDisponibilidade().getDiaSemana())
                .turno(vinculo.getDisponibilidade().getTurno())
                .observacao(vinculo.getObservacao())
                .build();
    }
}
