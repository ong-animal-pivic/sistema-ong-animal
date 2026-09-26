package com.umc.sistemaonganimal.api.dto.response;

import com.umc.sistemaonganimal.domain.model.Voluntario;
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
public class VoluntarioResumoResponseDTO {

    private Long id;

    private String nome;

    public static VoluntarioResumoResponseDTO fromEntity(Voluntario voluntario) {
        if (voluntario == null) {
            return null;
        }
        return VoluntarioResumoResponseDTO.builder()
                .id(voluntario.getId())
                .nome(voluntario.getNome())
                .build();
    }
}
