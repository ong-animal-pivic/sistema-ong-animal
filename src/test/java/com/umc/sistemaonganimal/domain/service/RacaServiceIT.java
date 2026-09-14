package com.umc.sistemaonganimal.domain.service;

import com.umc.sistemaonganimal.domain.exception.RacaExistenteException;
import com.umc.sistemaonganimal.domain.exception.RacaInUseException;
import com.umc.sistemaonganimal.domain.exception.RacaNotFoundException;
import com.umc.sistemaonganimal.domain.model.Animal;
import com.umc.sistemaonganimal.domain.model.Especie;
import com.umc.sistemaonganimal.domain.model.Raca;
import com.umc.sistemaonganimal.domain.model.Responsavel;
import com.umc.sistemaonganimal.domain.model.enums.animal.AnimalPorte;
import com.umc.sistemaonganimal.domain.model.enums.animal.AnimalSexo;
import com.umc.sistemaonganimal.domain.model.enums.animal.AnimalStatus;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// @SpringBootTest sobe o contexto Spring completo (services, repositories, JPA, Flyway),
// exatamente como quando a aplicação roda de verdade — por isso é um teste de INTEGRAÇÃO
// e não um teste unitário (que mockaria as dependências em vez de usá-las de verdade).
@SpringBootTest
// Ativa o profile "local" (application-local.properties), que aponta para o Postgres
// que já roda na máquina do dev (localhost:5433). Não usamos um banco separado de teste.
@ActiveProfiles("local")
// Faz cada método de teste rodar dentro de uma transação que é revertida (rollback)
// automaticamente ao final do método. Isso garante que os testes não deixem sujeira
// no banco e não interfiram uns nos outros, mesmo usando o banco real do dev.
@Transactional
class RacaServiceIT {

    // Spring injeta a instância real de RacaService, já conectada ao banco.
    @Autowired
    private RacaService racaService;

    // Usado no teste que precisa criar um Animal vinculado à Raça, para simular o
    // cenário de "raça em uso".
    @Autowired
    private AnimalService animalService;

    // Usado só para obter o id de uma espécie já existente no banco, sem depender
    // de qual espécie específica é (evita acoplar o teste ao conteúdo do fixture).
    @Autowired
    private EspecieService especieService;

    // Usado só para obter o id de um responsável já existente no banco, sem
    // depender de qual responsável específico é (evita acoplar o teste ao
    // conteúdo do fixture).
    @Autowired
    private ResponsavelService responsavelService;

    // Injeta o EntityManager (JPA) diretamente, para poder limpar o contexto de
    // persistência entre a exclusão e a busca seguinte (ver comentário no primeiro
    // teste abaixo que usa flush()/clear()).
    @Autowired
    private EntityManager entityManager;

    // Método auxiliar que cria e persiste uma Raça nova, isolada dos dados do
    // fixture, para que cada teste tenha seu próprio registro para excluir.
    // O nome inclui um sufixo aleatório para que chamadas repetidas dentro do
    // mesmo teste (mesma espécie) não colidam com a validação de nome duplicado.
    private Raca criarRaca() {
        Especie especieExistente = especieService.listar().get(0);
        return criarRaca("Raça de teste " + sufixoUnico(), especieExistente.getId());
    }

    // Gera um sufixo curto e único (8 caracteres), para caber no limite de 50
    // caracteres da coluna "nome" mesmo quando combinado com um prefixo descritivo.
    private String sufixoUnico() {
        return UUID.randomUUID().toString().substring(0, 8);
    }

    // Variante que permite controlar nome e espécie explicitamente, usada pelos
    // testes de validação de nome duplicado.
    private Raca criarRaca(String nome, Long especieId) {
        Raca raca = Raca.builder()
                .nome(nome)
                .especie(Especie.builder().id(especieId).build())
                .build();

        // Persiste a Raça via service (não direto no repository) para passar pelas
        // mesmas regras que a aplicação real aplicaria ao salvar.
        return racaService.salvar(raca);
    }

    // HAPPY PATH: excluir() deve fazer exclusão LÓGICA — o registro some das buscas,
    // mas continua existindo fisicamente na tabela (não é um DELETE físico).
    // Passo a passo:
    // 1. Cria uma Raça nova (isolada do fixture), sem nenhum Animal vinculado a
    //    ela, para excluir neste teste.
    // 2. Chama racaService.excluir(id), que internamente verifica que não há Animal
    //    vinculado e então marca "ativo = false" e salva a entidade (em vez de
    //    apagar a linha do banco).
    // 3. Chama entityManager.flush() (envia o UPDATE pendente para o banco) e depois
    //    entityManager.clear() (esvazia o cache de 1º nível da sessão JPA). Isso é
    //    necessário porque @SQLRestriction("ativo = true") só filtra quando o
    //    Hibernate precisa ir ao banco — se a entidade já estiver em memória no
    //    contexto de persistência (como ficaria logo após o passo 2, na mesma
    //    transação), findById devolveria ela direto do cache, sem passar pelo
    //    filtro. Fazendo flush+clear, simulamos o que aconteceria de verdade em uma
    //    request HTTP nova (com um EntityManager novo, sem cache prévio).
    // 4. Verificamos que buscarPorId(id) agora lança RacaNotFoundException, ou
    //    seja, a Raça excluída não é mais "encontrável" pela aplicação.
    // 5. Verificamos também que listar() não traz mais o id excluído na lista.
    @Test
    void excluir_semAnimalVinculado_devePararDeAparecerEmBuscaEListagem() {
        Raca racaCriada = criarRaca();

        racaService.excluir(racaCriada.getId());
        entityManager.flush();
        entityManager.clear();

        assertThatThrownBy(() -> racaService.buscarPorId(racaCriada.getId()))
                .isInstanceOf(RacaNotFoundException.class);
        assertThat(racaService.listar())
                .extracting(Raca::getId)
                .doesNotContain(racaCriada.getId());

        // Prova de que é exclusão LÓGICA, não física: consultamos a tabela via SQL
        // nativo, contornando o @SQLRestriction (que só existe no mapeamento JPA da
        // entidade Raca, não no banco em si). Se getSingleResult() não lançar
        // NoResultException, a linha ainda existe fisicamente; e o valor de "ativo"
        // deve ter sido marcado como false pelo excluir().
        Boolean ativo = (Boolean) entityManager
                .createNativeQuery("SELECT ativo FROM raca WHERE id = :id")
                .setParameter("id", racaCriada.getId())
                .getSingleResult();
        assertThat(ativo).isFalse();
    }

    // UNHAPPY PATH: uma Raça referenciada por um Animal ativo não pode ser
    // excluída. Antes, essa checagem vinha "de graça" da FK do banco (o DELETE
    // físico falhava com DataIntegrityViolationException); agora que a exclusão é
    // lógica (um UPDATE, não um DELETE), a FK não impede mais nada sozinha — por
    // isso o service passou a checar isso explicitamente, via
    // AnimalRepository.existsByRacaId, antes de desativar a Raça.
    // Passo a passo:
    // 1. Cria uma Raça nova, isolada do fixture.
    // 2. Cria e salva um Animal vinculado a essa Raça — reproduzindo, dentro do
    //    próprio teste, o estado necessário para o cenário (em vez de depender de
    //    um vínculo já existente no fixture).
    // 3. Chama racaService.excluir() para essa Raça e verifica que ele lança
    //    RacaInUseException, em vez de excluir o registro.
    @Test
    void excluir_comAnimalVinculado_deveLancarRacaInUseException() {
        Raca racaCriada = criarRaca();
        Responsavel responsavelExistente = responsavelService.listar().get(0);

        Animal animal = Animal.builder()
                .nome("Animal de teste")
                .idadeMeses(1)
                .porte(AnimalPorte.PEQUENO)
                .sexo(AnimalSexo.MACHO)
                .status(AnimalStatus.DISPONIVEL)
                .castrado(true)
                .dataResgate(LocalDate.now())
                .raca(Raca.builder().id(racaCriada.getId()).build())
                .responsavel(Responsavel.builder().id(responsavelExistente.getId()).build())
                .build();
        animalService.salvar(animal);

        assertThatThrownBy(() -> racaService.excluir(racaCriada.getId()))
                .isInstanceOf(RacaInUseException.class);
    }

    // UNHAPPY PATH: excluir() com um id que não existe (nem nunca vai existir, por
    // ser Long.MAX_VALUE — ver explicação equivalente em EspecieServiceIT) deve
    // lançar RacaNotFoundException, em vez de falhar silenciosamente ou de forma
    // inesperada. Isso é garantido porque excluir() chama buscarPorId(id) por
    // dentro antes de tentar desativar o registro.
    @Test
    void excluir_comIdInexistente_deveLancarRacaNotFoundException() {
        assertThatThrownBy(() -> racaService.excluir(Long.MAX_VALUE))
                .isInstanceOf(RacaNotFoundException.class);
    }

    // UNHAPPY PATH: excluir() sobre um id que já foi excluído antes deve se
    // comportar como um id inexistente (RacaNotFoundException), e não permitir
    // "excluir de novo" silenciosamente. Isso acontece porque, depois do
    // flush()+clear(), o @SQLRestriction faz essa Raça desaparecer de qualquer
    // busca subsequente — inclusive do buscarPorId() interno da segunda chamada a
    // excluir().
    @Test
    void excluir_comIdJaExcluido_deveLancarRacaNotFoundException() {
        Raca racaCriada = criarRaca();

        racaService.excluir(racaCriada.getId());
        entityManager.flush();
        entityManager.clear();

        assertThatThrownBy(() -> racaService.excluir(racaCriada.getId()))
                .isInstanceOf(RacaNotFoundException.class);
    }

    // HAPPY PATH: listar() deve refletir corretamente um cenário misto — com uma
    // Raça ativa e outra excluída lado a lado — trazendo só a ativa.
    // Passo a passo:
    // 1. Cria duas Raças novas e isoladas: uma que vai continuar ativa
    //    (racaMantida) e outra que será excluída (racaExcluida).
    // 2. Exclui apenas a segunda, e faz flush()+clear() para forçar a próxima
    //    chamada a listar() a ir ao banco (mesmo motivo do teste de exclusão acima).
    // 3. Verifica que a lista resultante contém o id da Raça mantida e não
    //    contém o id da Raça excluída — provando que o filtro do @SQLRestriction
    //    distingue corretamente entre as duas, e não é apenas "sorte" de a
    //    excluída não estar lá por outro motivo.
    @Test
    void listar_comRegistroExcluido_deveConterApenasOsAtivos() {
        Raca racaMantida = criarRaca();
        Raca racaExcluida = criarRaca();

        racaService.excluir(racaExcluida.getId());
        entityManager.flush();
        entityManager.clear();

        assertThat(racaService.listar())
                .extracting(Raca::getId)
                .contains(racaMantida.getId())
                .doesNotContain(racaExcluida.getId());
    }

    // UNHAPPY PATH: não deve ser possível cadastrar duas Raças com o mesmo nome
    // para a mesma Espécie.
    @Test
    void salvar_comNomeJaExistenteNaMesmaEspecie_deveLancarRacaExistenteException() {
        Especie especie = especieService.listar().get(0);
        Raca racaExistente = criarRaca("Raça duplicada " + sufixoUnico(), especie.getId());

        Raca racaNova = Raca.builder()
                .nome(racaExistente.getNome())
                .especie(Especie.builder().id(especie.getId()).build())
                .build();

        assertThatThrownBy(() -> racaService.salvar(racaNova))
                .isInstanceOf(RacaExistenteException.class);
    }

    // HAPPY PATH: espaços no início/fim do nome devem ser removidos antes de salvar.
    @Test
    void salvar_comEspacosNasBordasDoNome_deveSalvarNomeSemEspacos() {
        Especie especie = especieService.listar().get(0);
        String nome = "Raça com espaços " + sufixoUnico();

        Raca resultado = criarRaca("  " + nome + "  ", especie.getId());

        assertThat(resultado.getNome()).isEqualTo(nome);
    }

    // UNHAPPY PATH: um nome que só difere por espaços nas bordas deve ser tratado
    // como duplicado (ex.: "Yorkshire " e "Yorkshire").
    @Test
    void salvar_comNomeExistenteAcrescidoDeEspacos_deveLancarRacaExistenteException() {
        Especie especie = especieService.listar().get(0);
        Raca racaExistente = criarRaca("Raça duplicada " + sufixoUnico(), especie.getId());

        Raca racaNova = Raca.builder()
                .nome(" " + racaExistente.getNome() + "  ")
                .especie(Especie.builder().id(especie.getId()).build())
                .build();

        assertThatThrownBy(() -> racaService.salvar(racaNova))
                .isInstanceOf(RacaExistenteException.class);
    }

    // HAPPY PATH: o mesmo nome de Raça pode ser cadastrado em Espécies diferentes,
    // já que a regra de duplicidade é escopada por (nome, espécie).
    @Test
    void salvar_comMesmoNomeEmEspeciesDiferentes_devePermitir() {
        List<Especie> especies = especieService.listar();
        assertThat(especies).hasSizeGreaterThanOrEqualTo(2);

        String nome = "Raça comum " + sufixoUnico();
        Raca racaPrimeiraEspecie = criarRaca(nome, especies.get(0).getId());

        Raca racaSegundaEspecie = Raca.builder()
                .nome(nome)
                .especie(Especie.builder().id(especies.get(1).getId()).build())
                .build();

        Raca resultado = racaService.salvar(racaSegundaEspecie);

        assertThat(resultado.getId()).isNotEqualTo(racaPrimeiraEspecie.getId());
        assertThat(resultado.getNome()).isEqualTo(nome);
    }

    // HAPPY PATH: atualizar uma Raça mantendo o próprio nome não deve disparar a
    // validação de duplicidade (a Raça não pode "colidir consigo mesma").
    // Monta a entidade de atualização "solta" (só com os campos necessários, sem
    // reaproveitar a instância retornada por criarRaca()) para reproduzir fielmente
    // o que o controller faz em RacaController.atualizar: busca em uma chamada,
    // atualiza em outra. Reutilizar a instância gerenciada faria o Hibernate
    // auto-flush a mudança antes da checagem de duplicidade rodar, mascarando o
    // comportamento real da regra de negócio.
    @Test
    void salvar_atualizandoComMesmoNomeDaPropriaRaca_naoDeveLancarExcecao() {
        Raca racaCriada = criarRaca();

        Raca racaParaAtualizar = Raca.builder()
                .id(racaCriada.getId())
                .nome(racaCriada.getNome())
                .especie(Especie.builder().id(racaCriada.getEspecie().getId()).build())
                .build();

        Raca resultado = racaService.salvar(racaParaAtualizar);

        assertThat(resultado.getId()).isEqualTo(racaCriada.getId());
    }

    // UNHAPPY PATH: atualizar uma Raça para um nome já usado por outra Raça da
    // mesma Espécie deve lançar RacaExistenteException. Mesma observação do teste
    // acima sobre usar uma entidade "solta" em vez da instância gerenciada.
    @Test
    void salvar_atualizandoParaNomeUsadoPorOutraRacaNaMesmaEspecie_deveLancarRacaExistenteException() {
        Especie especie = especieService.listar().get(0);
        Raca racaExistente = criarRaca("Raça alvo " + sufixoUnico(), especie.getId());
        Raca racaParaAtualizar = criarRaca("Raça a atualizar " + sufixoUnico(), especie.getId());

        Raca racaComNomeConflitante = Raca.builder()
                .id(racaParaAtualizar.getId())
                .nome(racaExistente.getNome())
                .especie(Especie.builder().id(especie.getId()).build())
                .build();

        assertThatThrownBy(() -> racaService.salvar(racaComNomeConflitante))
                .isInstanceOf(RacaExistenteException.class);
    }
}
