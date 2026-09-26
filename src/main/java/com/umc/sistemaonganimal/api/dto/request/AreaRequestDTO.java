package com.umc.sistemaonganimal.api.dto.request;

import com.umc.sistemaonganimal.domain.model.Area;
import jakarta.validation.constraints.NotBlank;
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
public class AreaRequestDTO {

    @NotBlank
    private String nome;

    private String descricao;

    private String observacao;

    public Area toEntity() {
        return Area.builder()
                .nome(nome)
                .descricao(descricao)
                .observacao(observacao)
                .build();
    }
}
