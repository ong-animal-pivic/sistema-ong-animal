package com.umc.sistemaonganimal.domain.service;

import com.umc.sistemaonganimal.domain.exception.DomainException;
import com.umc.sistemaonganimal.domain.exception.ResponsavelExistenteException;
import com.umc.sistemaonganimal.domain.exception.ResponsavelInUseException;
import com.umc.sistemaonganimal.domain.exception.ResponsavelNotFoundException;
import com.umc.sistemaonganimal.domain.model.Animal;
import com.umc.sistemaonganimal.domain.model.Responsavel;
import com.umc.sistemaonganimal.domain.model.Tipo;
import com.umc.sistemaonganimal.domain.model.embeddables.Contato;
import com.umc.sistemaonganimal.domain.model.embeddables.Documento;
import com.umc.sistemaonganimal.domain.repository.AnimalRepository;
import com.umc.sistemaonganimal.domain.repository.ResponsavelRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class ResponsavelService {

    @Autowired
    private ResponsavelRepository responsavelRepository;

    @Autowired
    private AnimalRepository animalRepository;

    @Autowired
    private TipoService tipoService;

    @Transactional(readOnly = true)
    public List<Responsavel> listar() {
        return responsavelRepository.findAll();
    }

    @Transactional(readOnly = true)
    @SuppressWarnings("null")
    public Responsavel buscarPorId(Long id) {
        return responsavelRepository.findById(id).orElseThrow(() -> new ResponsavelNotFoundException(id));
    }

    @Transactional(readOnly = true)
    public List<Animal> listarAnimaisVinculados(Long responsavelId) {
        return animalRepository.findByResponsavelId(responsavelId);
    }

    @Transactional(readOnly = true)
    public long contarAnimaisVinculados(Long responsavelId) {
        return animalRepository.countByResponsavelId(responsavelId);
    }

    public Responsavel salvar(Responsavel responsavel) {
        Tipo tipo = tipoService.buscarPorId(responsavel.getTipo().getId());
        responsavel.setTipo(tipo);

        Documento documento = responsavel.getDocumento();
        String cpf = documento != null ? documento.getCpf() : null;
        String cnpj = responsavel.getCnpj();

        if (cpf != null && !cpf.isBlank() && cnpj != null && !cnpj.isBlank()) {
            throw new DomainException("CPF e CNPJ não podem ser preenchidos ao mesmo tempo.");
        }

        if ((cpf == null || cpf.isBlank()) && (cnpj == null || cnpj.isBlank())) {
            throw new DomainException("É obrigatório informar CPF ou CNPJ.");
        }

        Contato contato = responsavel.getContato();
        String email = contato != null ? contato.getEmail() : null;

        if (email != null && !email.isBlank()) {
            boolean emailDuplicado = responsavel.getId() == null
                    ? responsavelRepository.existsByContatoEmailIgnoreCase(email)
                    : responsavelRepository.existsByContatoEmailIgnoreCaseAndIdNot(email, responsavel.getId());

            if (emailDuplicado) {
                throw ResponsavelExistenteException.porEmail(email);
            }
        }

        return responsavelRepository.save(responsavel);
    }

    public void excluir(Long id) {
        Responsavel responsavel = buscarPorId(id);

        if (animalRepository.existsByResponsavelId(id)) {
            throw new ResponsavelInUseException(id);
        }

        // TODO: bloquear exclusão também se o responsável estiver vinculado a um Voluntário,
        // quando essa entidade existir no domínio (não existe hoje no projeto).

        responsavel.setAtivo(false);
        responsavelRepository.save(responsavel);
    }
}
