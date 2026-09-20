package com.umc.sistemaonganimal.api.controller;

import com.umc.sistemaonganimal.api.dto.request.VoluntarioRequestDTO;
import com.umc.sistemaonganimal.api.dto.response.VoluntarioResponseDTO;
import com.umc.sistemaonganimal.domain.model.Responsavel;
import com.umc.sistemaonganimal.domain.model.Voluntario;
import com.umc.sistemaonganimal.domain.service.VoluntarioService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/voluntarios")
public class VoluntarioController {

    @Autowired
    private VoluntarioService voluntarioService;

    @GetMapping
    public List<VoluntarioResponseDTO> listar() {
        return voluntarioService.listar().stream()
                .map(VoluntarioResponseDTO::fromEntity)
                .toList();
    }

    @GetMapping("/{voluntarioId}")
    public VoluntarioResponseDTO buscar(@PathVariable Long voluntarioId) {
        Voluntario voluntario = voluntarioService.buscarPorId(voluntarioId);
        return VoluntarioResponseDTO.fromEntity(voluntario);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public VoluntarioResponseDTO adicionar(@RequestBody @Valid VoluntarioRequestDTO voluntarioDTO) {
        Voluntario voluntario = voluntarioService.salvar(voluntarioDTO.toEntity());
        return VoluntarioResponseDTO.fromEntity(voluntario);
    }

    @PutMapping("/{voluntarioId}")
    public VoluntarioResponseDTO atualizar(@PathVariable Long voluntarioId, @RequestBody @Valid VoluntarioRequestDTO voluntarioDTO) {
        Voluntario voluntarioAtualizar = voluntarioService.buscarPorId(voluntarioId);

        voluntarioAtualizar.setNome(voluntarioDTO.getNome());
        voluntarioAtualizar.setDocumento(voluntarioDTO.getDocumento() != null ? voluntarioDTO.getDocumento().toEntity() : null);
        voluntarioAtualizar.setIdade(voluntarioDTO.getIdade());
        voluntarioAtualizar.setProfissao(voluntarioDTO.getProfissao());
        voluntarioAtualizar.setContato(voluntarioDTO.getContato() != null ? voluntarioDTO.getContato().toEntity() : null);
        voluntarioAtualizar.setFrequencia(voluntarioDTO.getFrequencia());
        voluntarioAtualizar.setEndereco(voluntarioDTO.getEndereco() != null ? voluntarioDTO.getEndereco().toEntity() : null);
        voluntarioAtualizar.setResponsavel(Responsavel.builder().id(voluntarioDTO.getResponsavelId()).build());

        Voluntario voluntario = voluntarioService.salvar(voluntarioAtualizar);
        return VoluntarioResponseDTO.fromEntity(voluntario);
    }

    @DeleteMapping("/{voluntarioId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void excluir(@PathVariable Long voluntarioId) {
        voluntarioService.excluir(voluntarioId);
    }
}
