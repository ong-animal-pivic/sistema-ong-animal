package com.umc.sistemaonganimal.domain.service;

import com.umc.sistemaonganimal.domain.exception.AreaInUseException;
import com.umc.sistemaonganimal.domain.exception.AreaNotFoundException;
import com.umc.sistemaonganimal.domain.model.Area;
import com.umc.sistemaonganimal.domain.model.Voluntario;
import com.umc.sistemaonganimal.domain.repository.AreaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class AreaService {

    @Autowired
    private AreaRepository areaRepository;

    @Autowired
    private VoluntarioService voluntarioService;

    @Transactional(readOnly = true)
    public List<Area> listar() {
        return areaRepository.findAll();
    }

    @Transactional(readOnly = true)
    @SuppressWarnings("null")
    public Area buscarPorId(Long id) {
        return areaRepository.findById(id).orElseThrow(() -> new AreaNotFoundException(id));
    }

    public Area salvar(Area area) {
        return areaRepository.save(area);
    }

    public void excluir(Long id) {
        Area area = buscarPorId(id);

        if (!area.getVoluntarios().isEmpty()) {
            throw new AreaInUseException(id);
        }

        area.setAtivo(false);
        areaRepository.save(area);
    }

    public void associarVoluntario(Long areaId, Long voluntarioId) {
        Area area = buscarPorId(areaId);
        Voluntario voluntario = voluntarioService.buscarPorId(voluntarioId);

        area.getVoluntarios().add(voluntario);
        areaRepository.save(area);
    }

    public void desassociarVoluntario(Long areaId, Long voluntarioId) {
        Area area = buscarPorId(areaId);
        Voluntario voluntario = voluntarioService.buscarPorId(voluntarioId);

        area.getVoluntarios().remove(voluntario);
        areaRepository.save(area);
    }

}
