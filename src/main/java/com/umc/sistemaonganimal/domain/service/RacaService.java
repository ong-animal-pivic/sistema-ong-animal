package com.umc.sistemaonganimal.domain.service;

import com.umc.sistemaonganimal.domain.exception.RacaExistenteException;
import com.umc.sistemaonganimal.domain.exception.RacaInUseException;
import com.umc.sistemaonganimal.domain.exception.RacaNotFoundException;
import com.umc.sistemaonganimal.domain.model.Especie;
import com.umc.sistemaonganimal.domain.model.Raca;
import com.umc.sistemaonganimal.domain.repository.AnimalRepository;
import com.umc.sistemaonganimal.domain.repository.RacaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class RacaService {

    @Autowired
    private RacaRepository racaRepository;

    @Autowired
    private EspecieService especieService;

    @Autowired
    private AnimalRepository animalRepository;

    public List<Raca> listar() {
        return racaRepository.findAll();
    }

    @SuppressWarnings("null")
    public Raca buscarPorId(Long id) {
        return racaRepository.findById(id).orElseThrow(() -> new RacaNotFoundException(id));
    }

    public Raca salvar(Raca raca) {

        if (raca.getNome() != null) {
            raca.setNome(raca.getNome().strip());
        }

        Long especieId = raca.getEspecie().getId();
        Especie especie = especieService.buscarPorId(especieId);

        raca.setEspecie(especie);

        boolean nomeDuplicado = raca.getId() == null
                ? racaRepository.existsByNomeIgnoreCaseAndEspecieId(raca.getNome(), especieId)
                : racaRepository.existsByNomeIgnoreCaseAndEspecieIdAndIdNot(raca.getNome(), especieId, raca.getId());

        if (nomeDuplicado) {
            throw new RacaExistenteException(raca.getNome(), especie.getNome().name());
        }

        return racaRepository.save(raca);
    }

    public void excluir(Long id) {
        Raca raca = buscarPorId(id);

        if (animalRepository.existsByRacaId(id)) {
            throw new RacaInUseException(id);
        }

        raca.setAtivo(false);
        racaRepository.save(raca);
    }

}
