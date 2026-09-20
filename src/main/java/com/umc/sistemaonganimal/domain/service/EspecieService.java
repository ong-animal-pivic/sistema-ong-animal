package com.umc.sistemaonganimal.domain.service;

import com.umc.sistemaonganimal.domain.exception.EspecieNotFoundException;
import com.umc.sistemaonganimal.domain.model.Especie;
import com.umc.sistemaonganimal.domain.repository.EspecieRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class EspecieService {

    @Autowired
    private EspecieRepository especieRepository;

    public List<Especie> listar() {
        return especieRepository.findAll();
    }

    @SuppressWarnings("null")
    public Especie buscarPorId(Long id) {
        return especieRepository.findById(id).orElseThrow(() -> new EspecieNotFoundException(id));
    }
}
