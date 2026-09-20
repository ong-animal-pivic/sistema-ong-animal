package com.umc.sistemaonganimal.domain.service;

import com.umc.sistemaonganimal.domain.exception.VoluntarioExistenteException;
import com.umc.sistemaonganimal.domain.exception.VoluntarioNotFoundException;
import com.umc.sistemaonganimal.domain.model.Responsavel;
import com.umc.sistemaonganimal.domain.model.Voluntario;
import com.umc.sistemaonganimal.domain.model.embeddables.Contato;
import com.umc.sistemaonganimal.domain.repository.VoluntarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class VoluntarioService {

    @Autowired
    private VoluntarioRepository voluntarioRepository;

    @Autowired
    private ResponsavelService responsavelService;

    @Transactional(readOnly = true)
    public List<Voluntario> listar() {
        return voluntarioRepository.findAll();
    }

    @Transactional(readOnly = true)
    @SuppressWarnings("null")
    public Voluntario buscarPorId(Long id) {
        return voluntarioRepository.findById(id).orElseThrow(() -> new VoluntarioNotFoundException(id));
    }

    public Voluntario salvar(Voluntario voluntario) {
        Responsavel responsavel = responsavelService.buscarPorId(voluntario.getResponsavel().getId());
        voluntario.setResponsavel(responsavel);

        Contato contato = voluntario.getContato();
        String email = contato != null ? contato.getEmail() : null;

        if (email != null && !email.isBlank()) {
            boolean emailDuplicado = voluntario.getId() == null
                    ? voluntarioRepository.existsByContatoEmailIgnoreCase(email)
                    : voluntarioRepository.existsByContatoEmailIgnoreCaseAndIdNot(email, voluntario.getId());

            if (emailDuplicado) {
                throw VoluntarioExistenteException.porEmail(email);
            }
        }

        return voluntarioRepository.save(voluntario);
    }

    public void excluir(Long id) {
        Voluntario voluntario = buscarPorId(id);
        voluntario.setAtivo(false);
        voluntarioRepository.save(voluntario);
    }
}
