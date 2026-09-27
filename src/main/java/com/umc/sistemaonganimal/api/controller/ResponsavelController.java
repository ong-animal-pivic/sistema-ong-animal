package com.umc.sistemaonganimal.api.controller;

import com.umc.sistemaonganimal.api.dto.request.ResponsavelRequestDTO;
import com.umc.sistemaonganimal.api.dto.response.ResponsavelResponseDTO;
import com.umc.sistemaonganimal.domain.model.Animal;
import com.umc.sistemaonganimal.domain.model.Responsavel;
import com.umc.sistemaonganimal.domain.model.Tipo;
import com.umc.sistemaonganimal.domain.model.Voluntario;
import com.umc.sistemaonganimal.domain.service.ResponsavelService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/responsaveis")
public class ResponsavelController {

    @Autowired
    private ResponsavelService responsavelService;

    @GetMapping
    public List<ResponsavelResponseDTO> listar() {
        return responsavelService.listar().stream()
                .map(responsavel -> ResponsavelResponseDTO.fromEntity(
                        responsavel, responsavelService.contarAnimaisVinculados(responsavel.getId())))
                .toList();
    }

    @GetMapping("/{responsavelId}")
    public ResponsavelResponseDTO buscar(@PathVariable Long responsavelId) {
        Responsavel responsavel = responsavelService.buscarPorId(responsavelId);
        List<Animal> animaisVinculados = responsavelService.listarAnimaisVinculados(responsavelId);
        List<Voluntario> voluntariosVinculados = responsavelService.listarVoluntariosVinculados(responsavelId);
        return ResponsavelResponseDTO.fromEntity(
                responsavel, (long) animaisVinculados.size(), animaisVinculados, voluntariosVinculados);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ResponsavelResponseDTO adicionar(@RequestBody @Valid ResponsavelRequestDTO responsavelDTO) {
        Responsavel responsavel = responsavelService.salvar(responsavelDTO.toEntity());
        return ResponsavelResponseDTO.fromEntity(responsavel, responsavelService.contarAnimaisVinculados(responsavel.getId()));
    }

    @PutMapping("/{responsavelId}")
    public ResponsavelResponseDTO atualizar(@PathVariable Long responsavelId, @RequestBody @Valid ResponsavelRequestDTO responsavelDTO) {
        Responsavel responsavelAtualizar = responsavelService.buscarPorId(responsavelId);

        responsavelAtualizar.setNome(responsavelDTO.getNome());
        responsavelAtualizar.setDocumento(responsavelDTO.getDocumento() != null ? responsavelDTO.getDocumento().toEntity() : null);
        responsavelAtualizar.setCnpj(responsavelDTO.getCnpj());
        responsavelAtualizar.setContato(responsavelDTO.getContato() != null ? responsavelDTO.getContato().toEntity() : null);
        responsavelAtualizar.setEndereco(responsavelDTO.getEndereco() != null ? responsavelDTO.getEndereco().toEntity() : null);
        responsavelAtualizar.setTipo(Tipo.builder().id(responsavelDTO.getTipoId()).build());

        Responsavel responsavel = responsavelService.salvar(responsavelAtualizar);
        return ResponsavelResponseDTO.fromEntity(responsavel, responsavelService.contarAnimaisVinculados(responsavel.getId()));
    }

    @DeleteMapping("/{responsavelId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void excluir(@PathVariable Long responsavelId) {
        responsavelService.excluir(responsavelId);
    }
}
