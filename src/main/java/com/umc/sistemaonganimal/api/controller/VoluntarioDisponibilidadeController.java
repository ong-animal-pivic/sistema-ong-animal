package com.umc.sistemaonganimal.api.controller;

import com.umc.sistemaonganimal.api.dto.request.DisponibilidadeRequestDTO;
import com.umc.sistemaonganimal.api.dto.response.DisponibilidadeResponseDTO;
import com.umc.sistemaonganimal.domain.model.VoluntarioDisponibilidade;
import com.umc.sistemaonganimal.domain.service.DisponibilidadeService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// Disponibilidade não tem gerenciamento independente: só é acessada a partir do voluntário.
@RestController
@RequestMapping("/voluntarios/{voluntarioId}/disponibilidades")
public class VoluntarioDisponibilidadeController {

    @Autowired
    private DisponibilidadeService disponibilidadeService;

    @GetMapping
    public List<DisponibilidadeResponseDTO> listar(@PathVariable Long voluntarioId) {
        return disponibilidadeService.listarPorVoluntario(voluntarioId).stream()
                .map(DisponibilidadeResponseDTO::fromEntity)
                .toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public DisponibilidadeResponseDTO adicionar(@PathVariable Long voluntarioId,
                                                @RequestBody @Valid DisponibilidadeRequestDTO disponibilidadeDTO) {
        VoluntarioDisponibilidade vinculo = disponibilidadeService.adicionar(
                voluntarioId,
                disponibilidadeDTO.getDiaSemana(),
                disponibilidadeDTO.getTurno(),
                disponibilidadeDTO.getObservacao());
        return DisponibilidadeResponseDTO.fromEntity(vinculo);
    }

    @DeleteMapping("/{disponibilidadeId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void remover(@PathVariable Long voluntarioId, @PathVariable Long disponibilidadeId) {
        disponibilidadeService.remover(voluntarioId, disponibilidadeId);
    }

}
