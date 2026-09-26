package com.umc.sistemaonganimal.api.controller;

import com.umc.sistemaonganimal.api.dto.request.AreaRequestDTO;
import com.umc.sistemaonganimal.api.dto.response.AreaResponseDTO;
import com.umc.sistemaonganimal.domain.model.Area;
import com.umc.sistemaonganimal.domain.service.AreaService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/areas")
public class AreaController {

    @Autowired
    private AreaService areaService;

    @GetMapping
    public List<AreaResponseDTO> listar() {
        return areaService.listar().stream()
                .map(AreaResponseDTO::fromEntity)
                .toList();
    }

    @GetMapping("/{areaId}")
    public AreaResponseDTO buscar(@PathVariable Long areaId) {
        Area area = areaService.buscarPorId(areaId);
        return AreaResponseDTO.fromEntity(area);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AreaResponseDTO adicionar(@RequestBody @Valid AreaRequestDTO areaDTO) {
        Area area = areaService.salvar(areaDTO.toEntity());
        return AreaResponseDTO.fromEntity(area);
    }

    @PutMapping("/{areaId}")
    public AreaResponseDTO atualizar(@PathVariable Long areaId, @RequestBody @Valid AreaRequestDTO areaDTO) {
        Area areaAtualizar = areaService.buscarPorId(areaId);

        areaAtualizar.setNome(areaDTO.getNome());
        areaAtualizar.setDescricao(areaDTO.getDescricao());
        areaAtualizar.setObservacao(areaDTO.getObservacao());

        Area area = areaService.salvar(areaAtualizar);
        return AreaResponseDTO.fromEntity(area);
    }

    @DeleteMapping("/{areaId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void excluir(@PathVariable Long areaId) {
        areaService.excluir(areaId);
    }

    @PostMapping("/{areaId}/voluntarios/{voluntarioId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void associarVoluntario(@PathVariable Long areaId, @PathVariable Long voluntarioId) {
        areaService.associarVoluntario(areaId, voluntarioId);
    }

    @DeleteMapping("/{areaId}/voluntarios/{voluntarioId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void desassociarVoluntario(@PathVariable Long areaId, @PathVariable Long voluntarioId) {
        areaService.desassociarVoluntario(areaId, voluntarioId);
    }

}
