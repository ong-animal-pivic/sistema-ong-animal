package com.umc.sistemaonganimal.domain.service;

import com.umc.sistemaonganimal.domain.exception.AnimalNotFoundException;
import com.umc.sistemaonganimal.domain.exception.DomainException;
import com.umc.sistemaonganimal.domain.model.Adotante;
import com.umc.sistemaonganimal.domain.model.Animal;
import com.umc.sistemaonganimal.domain.model.Raca;
import com.umc.sistemaonganimal.domain.model.Responsavel;
import com.umc.sistemaonganimal.domain.model.enums.animal.AnimalStatus;
import com.umc.sistemaonganimal.domain.repository.AnimalRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@Transactional
public class AnimalService {

    private static final DateTimeFormatter FORMATO_DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    @Autowired
    private AnimalRepository animalRepository;

    @Autowired
    private AdotanteService adotanteService;

    @Autowired
    private RacaService racaService;

    @Autowired
    private ResponsavelService responsavelService;

    @Transactional(readOnly = true)
    public List<Animal> listar() {
        return animalRepository.findAll();
    }

    @Transactional(readOnly = true)
    @SuppressWarnings("null")
    public Animal buscarPorId(Long id) {
        return animalRepository.findById(id)
                .orElseThrow(() -> new AnimalNotFoundException(id) {
                });
    }

    public Animal salvar(Animal animal) {

        boolean statusDeSaida = animal.getStatus() == AnimalStatus.ADOTADO
                || animal.getStatus() == AnimalStatus.OBITO;
        if (statusDeSaida) {
            if (animal.getDataSaida() == null) {
                throw new DomainException(
                        "É obrigatório informar a data de saída quando o status do animal é ADOTADO ou OBITO.");
            }
        } else {
            // Garante consistência: só animais adotados ou em óbito têm data de saída.
            animal.setDataSaida(null);
        }

        if (animal.getDataSaida() != null && animal.getDataResgate() != null
                && animal.getDataSaida().isBefore(animal.getDataResgate())) {
            throw new DomainException(String.format(
                    "A data de saída (%s) não pode ser anterior à data de resgate (%s).",
                    animal.getDataSaida().format(FORMATO_DATA),
                    animal.getDataResgate().format(FORMATO_DATA)));
        }

        Long racaId = animal.getRaca().getId();
        Raca raca = racaService.buscarPorId(racaId);
        animal.setRaca(raca);

        Long responsavelId = animal.getResponsavel().getId();
        Responsavel responsavel = responsavelService.buscarPorId(responsavelId);
        animal.setResponsavel(responsavel);

        if (animal.getStatus().equals(AnimalStatus.ADOTADO)) {
            if (animal.getAdotante() == null || animal.getAdotante().getId() == null) {
                throw new DomainException(
                        "É obrigatório informar o adotante quando o status do animal é ADOTADO.");
            }
            Long adotanteId = animal.getAdotante().getId();
            Adotante adotante = adotanteService.buscarPorId(adotanteId);
            animal.setAdotante(adotante);
        } else {
            // Garante consistência: animal sem status ADOTADO não mantém adotante vinculado.
            animal.setAdotante(null);
        }

        return animalRepository.save(animal);
    }

    public void excluir(Long id) {
        Animal animalExcluir = buscarPorId(id);
        animalExcluir.setAtivo(false);
        animalRepository.save(animalExcluir);
    }
}
