package com.umc.sistemaonganimal.domain.service;

import com.umc.sistemaonganimal.domain.exception.TipoNotFoundException;
import com.umc.sistemaonganimal.domain.model.Tipo;
import com.umc.sistemaonganimal.domain.repository.TipoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class TipoService {

    @Autowired
    private TipoRepository tipoRepository;

    public List<Tipo> listar() {
        return tipoRepository.findAll();
    }

    @SuppressWarnings("null")
    public Tipo buscarPorId(Long id) {
        return tipoRepository.findById(id).orElseThrow(() -> new TipoNotFoundException(id));
    }
}
