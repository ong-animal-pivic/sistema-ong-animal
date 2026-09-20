package com.umc.sistemaonganimal.api.dto.response;

import com.umc.sistemaonganimal.api.dto.embeddables.ContatoDTO;
import com.umc.sistemaonganimal.api.dto.embeddables.DocumentoDTO;
import com.umc.sistemaonganimal.api.dto.embeddables.EnderecoDTO;
import com.umc.sistemaonganimal.domain.model.Voluntario;
import com.umc.sistemaonganimal.domain.model.enums.general.Frequencia;
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
public class VoluntarioResponseDTO {

    private Long id;

    private String nome;

    private DocumentoDTO documento;

    private Integer idade;

    private String profissao;

    private ContatoDTO contato;

    private Frequencia frequencia;

    private EnderecoDTO endereco;

    private ResponsavelResponseDTO responsavel;

    public static VoluntarioResponseDTO fromEntity(Voluntario voluntario) {
        if (voluntario == null) {
            return null;
        }
        return VoluntarioResponseDTO.builder()
                .id(voluntario.getId())
                .nome(voluntario.getNome())
                .documento(DocumentoDTO.fromEntity(voluntario.getDocumento()))
                .idade(voluntario.getIdade())
                .profissao(voluntario.getProfissao())
                .contato(ContatoDTO.fromEntity(voluntario.getContato()))
                .frequencia(voluntario.getFrequencia())
                .endereco(EnderecoDTO.fromEntity(voluntario.getEndereco()))
                .responsavel(ResponsavelResponseDTO.fromEntity(voluntario.getResponsavel()))
                .build();
    }
}
