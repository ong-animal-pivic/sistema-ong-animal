package com.umc.sistemaonganimal.domain.model;

import jakarta.persistence.*;
import lombok.*;

// Vínculo N:N entre Voluntario e Disponibilidade. É uma entidade (e não um
// @ManyToMany simples, como em Area) porque a observação pertence ao par
// voluntário/disponibilidade, não à disponibilidade em si.
@Getter @Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString
@AllArgsConstructor
@NoArgsConstructor
@Builder

@Entity
@Table(name = "voluntario_disponibilidade")
public class VoluntarioDisponibilidade {

    @Id
    @EqualsAndHashCode.Include
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "voluntario_id", nullable = false)
    @ToString.Exclude
    private Voluntario voluntario;

    @ManyToOne
    @JoinColumn(name = "disponibilidade_id", nullable = false)
    private Disponibilidade disponibilidade;

    @Column(length = 255)
    private String observacao;

}
