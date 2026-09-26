package com.umc.sistemaonganimal.api.dto.response;

import com.umc.sistemaonganimal.domain.model.Area;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class AreaResponseDTO {

    private Long id;
    private String nome;
    private String descricao;
    private String observacao;
    private List<VoluntarioResumoResponseDTO> voluntarios;

    public static AreaResponseDTO fromEntity(Area area) {
        if (area == null) {
            return null;
        }
        return AreaResponseDTO.builder()
                .id(area.getId())
                .nome(area.getNome())
                .descricao(area.getDescricao())
                .observacao(area.getObservacao())
                .voluntarios(area.getVoluntarios().stream()
                        .map(VoluntarioResumoResponseDTO::fromEntity)
                        .toList())
                .build();
    }
}
