package com.umc.sistemaonganimal.domain.service;

import com.umc.sistemaonganimal.domain.exception.DisponibilidadeExistenteException;
import com.umc.sistemaonganimal.domain.exception.DisponibilidadeNotFoundException;
import com.umc.sistemaonganimal.domain.model.Disponibilidade;
import com.umc.sistemaonganimal.domain.model.Voluntario;
import com.umc.sistemaonganimal.domain.model.VoluntarioDisponibilidade;
import com.umc.sistemaonganimal.domain.model.enums.general.DiaSemana;
import com.umc.sistemaonganimal.domain.model.enums.general.Turno;
import com.umc.sistemaonganimal.domain.repository.DisponibilidadeRepository;
import com.umc.sistemaonganimal.domain.repository.VoluntarioDisponibilidadeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

@Service
@Transactional
public class DisponibilidadeService {

    // Ordem declarada nos enums (SEGUNDA..DOMINGO, MANHA..NOITE); no banco as colunas
    // são VARCHAR e um ORDER BY resultaria em ordem alfabética.
    private static final Comparator<VoluntarioDisponibilidade> ORDEM_DIA_TURNO = Comparator
            .comparing((VoluntarioDisponibilidade vd) -> vd.getDisponibilidade().getDiaSemana())
            .thenComparing(vd -> vd.getDisponibilidade().getTurno());

    @Autowired
    private DisponibilidadeRepository disponibilidadeRepository;

    @Autowired
    private VoluntarioDisponibilidadeRepository voluntarioDisponibilidadeRepository;

    @Autowired
    private VoluntarioService voluntarioService;

    @Transactional(readOnly = true)
    public List<VoluntarioDisponibilidade> listarPorVoluntario(Long voluntarioId) {
        voluntarioService.buscarPorId(voluntarioId);

        return voluntarioDisponibilidadeRepository.findByVoluntarioId(voluntarioId).stream()
                .sorted(ORDEM_DIA_TURNO)
                .toList();
    }

    public VoluntarioDisponibilidade adicionar(Long voluntarioId, DiaSemana diaSemana, Turno turno, String observacao) {
        Voluntario voluntario = voluntarioService.buscarPorId(voluntarioId);

        // O catálogo (dia, turno) não é semeado por migration, então é criado sob demanda.
        Disponibilidade disponibilidade = disponibilidadeRepository.findByDiaSemanaAndTurno(diaSemana, turno)
                .orElseGet(() -> disponibilidadeRepository.save(
                        Disponibilidade.builder().diaSemana(diaSemana).turno(turno).build()));

        if (voluntarioDisponibilidadeRepository.existsByVoluntarioIdAndDisponibilidadeId(voluntarioId, disponibilidade.getId())) {
            throw new DisponibilidadeExistenteException();
        }

        return voluntarioDisponibilidadeRepository.save(VoluntarioDisponibilidade.builder()
                .voluntario(voluntario)
                .disponibilidade(disponibilidade)
                .observacao(observacao)
                .build());
    }

    public void remover(Long voluntarioId, Long disponibilidadeId) {
        VoluntarioDisponibilidade vinculo = voluntarioDisponibilidadeRepository
                .findByVoluntarioIdAndDisponibilidadeId(voluntarioId, disponibilidadeId)
                .orElseThrow(() -> new DisponibilidadeNotFoundException(voluntarioId, disponibilidadeId));

        voluntarioDisponibilidadeRepository.delete(vinculo);
    }

}
