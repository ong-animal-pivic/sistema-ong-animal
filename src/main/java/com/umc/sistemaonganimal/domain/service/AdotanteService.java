package com.umc.sistemaonganimal.domain.service;

import com.umc.sistemaonganimal.domain.exception.AdotanteInUseException;
import com.umc.sistemaonganimal.domain.exception.AdotanteNotFoundException;
import com.umc.sistemaonganimal.domain.exception.DomainException;
import com.umc.sistemaonganimal.domain.model.Adotante;
import com.umc.sistemaonganimal.domain.model.embeddables.Contato;
import com.umc.sistemaonganimal.domain.repository.AdotanteRepository;
import com.umc.sistemaonganimal.domain.repository.AnimalRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class AdotanteService {

    @Autowired
    private AdotanteRepository adotanteRepository;

    @Autowired
    private AnimalRepository animalRepository;

    @Transactional(readOnly = true)
    public List<Adotante> listar() {
        return adotanteRepository.findAll();
    }

    @Transactional(readOnly = true)
    @SuppressWarnings("null")
    public Adotante buscarPorId(Long id) {
        return adotanteRepository.findById(id).orElseThrow(() -> new AdotanteNotFoundException(id));
    }

    public Adotante salvar(@NonNull Adotante adotante) {
        Contato contato = adotante.getContato();
        if (contato != null
                && contato.getTelefonePrincipal() != null
                && contato.getTelefonePrincipal().equals(contato.getTelefoneSecundario())) {
            throw new DomainException("Telefone secundário deve ser diferente do telefone principal");
        }

        return adotanteRepository.save(adotante);
    }

    public void excluir(Long id) {
        Adotante adotante = buscarPorId(id);

        if (animalRepository.existsByAdotanteId(id)) {
            throw new AdotanteInUseException(id);
        }

        adotante.setAtivo(false);
        adotanteRepository.save(adotante);
    }
}
