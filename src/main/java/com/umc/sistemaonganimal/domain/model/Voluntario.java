package com.umc.sistemaonganimal.domain.model;

import com.umc.sistemaonganimal.domain.model.embeddables.Contato;
import com.umc.sistemaonganimal.domain.model.embeddables.Documento;
import com.umc.sistemaonganimal.domain.model.embeddables.Endereco;
import com.umc.sistemaonganimal.domain.model.enums.general.Frequencia;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLRestriction;

@Getter @Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString
@AllArgsConstructor
@NoArgsConstructor
@Builder

@Entity
@Table(name = "voluntario")
@SQLRestriction("ativo = true")
public class Voluntario {

    @Id
    @EqualsAndHashCode.Include
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String nome;

    @Embedded
    private Documento documento;

    private Integer idade;

    @Column(length = 50)
    private String profissao;

    @Embedded
    private Contato contato;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private Frequencia frequencia;

    @Embedded
    private Endereco endereco;

    @ManyToOne
    @JoinColumn(name = "responsavel_id", nullable = false)
    private Responsavel responsavel;

    @Builder.Default
    @Column(nullable = false)
    private boolean ativo = true;

}
