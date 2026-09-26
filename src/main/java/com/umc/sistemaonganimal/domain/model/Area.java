package com.umc.sistemaonganimal.domain.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLRestriction;

import java.util.HashSet;
import java.util.Set;

@Getter @Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString
@AllArgsConstructor
@NoArgsConstructor
@Builder

@Entity
@Table(name = "area")
@SQLRestriction("ativo = true")
public class Area {

    @Id
    @EqualsAndHashCode.Include
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String nome;

    @Column(length = 255)
    private String descricao;

    @Column(length = 255)
    private String observacao;

    @ManyToMany
    @JoinTable(
            name = "area_voluntario",
            joinColumns = @JoinColumn(name = "area_id"),
            inverseJoinColumns = @JoinColumn(name = "voluntario_id")
    )
    @Builder.Default
    @ToString.Exclude
    private Set<Voluntario> voluntarios = new HashSet<>();

    @Builder.Default
    @Column(nullable = false)
    private boolean ativo = true;

}
