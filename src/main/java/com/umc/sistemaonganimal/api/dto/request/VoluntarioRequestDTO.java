package com.umc.sistemaonganimal.api.dto.request;

import com.umc.sistemaonganimal.api.dto.embeddables.ContatoDTO;
import com.umc.sistemaonganimal.api.dto.embeddables.DocumentoDTO;
import com.umc.sistemaonganimal.api.dto.embeddables.EnderecoDTO;
import com.umc.sistemaonganimal.domain.model.Responsavel;
import com.umc.sistemaonganimal.domain.model.Voluntario;
import com.umc.sistemaonganimal.domain.model.enums.general.Frequencia;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
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
public class VoluntarioRequestDTO {

    @NotBlank
    private String nome;

    // @Valid roda no grupo Default: valida o formato do CPF (@CPF) quando presente,
    // mas não exige (o @NotBlank do cpf só é checado no grupo Groups.CpfObrigatorio,
    // usado por AdotanteController) — aqui CPF é opcional, igual a Responsavel.
    @Valid
    private DocumentoDTO documento;

    @PositiveOrZero
    private Integer idade;

    private String profissao;

    @NotNull
    @Valid
    private ContatoDTO contato;

    @NotNull
    private Frequencia frequencia;

    @NotNull
    @Valid
    private EnderecoDTO endereco;

    @NotNull
    private Long responsavelId;

    public Voluntario toEntity() {
        return Voluntario.builder()
                .nome(nome)
                .documento(documento != null ? documento.toEntity() : null)
                .idade(idade)
                .profissao(profissao)
                .contato(contato != null ? contato.toEntity() : null)
                .frequencia(frequencia)
                .endereco(endereco != null ? endereco.toEntity() : null)
                .responsavel(Responsavel.builder().id(responsavelId).build())
                .build();
    }
}
