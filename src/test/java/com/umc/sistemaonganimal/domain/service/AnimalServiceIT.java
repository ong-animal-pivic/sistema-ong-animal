package com.umc.sistemaonganimal.domain.service;

import com.umc.sistemaonganimal.domain.exception.AnimalNotFoundException;
import com.umc.sistemaonganimal.domain.exception.DomainException;
import com.umc.sistemaonganimal.domain.model.Animal;
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
class AnimalServiceIT {

    // Spring injeta a instância real de AnimalService, já conectada ao banco.
    @Autowired
    private AnimalService animalService;

    // Usado só para obter o id de uma raça já existente no banco, sem depender de
    // qual raça específica é (evita acoplar o teste ao conteúdo do fixture).
    @Autowired
    private RacaService racaService;

    // Usado só para obter o id de um responsável já existente no banco, sem depender
    // de qual responsável específico é (evita acoplar o teste ao conteúdo do fixture).
    @Autowired
    private ResponsavelService responsavelService;

    // Injeta o EntityManager (JPA) diretamente, para poder limpar o contexto de
    // persistência entre a exclusão e a busca seguinte (ver comentário no teste
    // abaixo que usa flush()/clear()).
    @Autowired
    private EntityManager entityManager;

    // Método auxiliar que cria e persiste um Animal novo, isolado dos dados do
    // fixture, para que cada teste tenha seu próprio registro para excluir.
    private Animal criarAnimal() {
        // Pega uma raça qualquer já cadastrada no banco (não importa qual), só para
        // satisfazer o vínculo obrigatório Animal -> Raca.
        Raca racaExistente = racaService.listar().get(0);

        // Pega um responsável qualquer já cadastrado no banco (não importa qual), só
        // para satisfazer o vínculo obrigatório Animal -> Responsavel.
        Responsavel responsavelExistente = responsavelService.listar().get(0);

        // Monta um Animal válido em memória, com todos os campos obrigatórios
        // preenchidos, vinculado à raça e ao responsável obtidos acima.
        Animal animal = Animal.builder()
                .nome("Animal de teste")
                .idadeMeses(1)
                .porte(AnimalPorte.PEQUENO)
                .sexo(AnimalSexo.MACHO)
                .status(AnimalStatus.DISPONIVEL)
                .castrado(true)
                .dataResgate(LocalDate.now())
                .raca(Raca.builder().id(racaExistente.getId()).build())
                .responsavel(Responsavel.builder().id(responsavelExistente.getId()).build())
                .build();

        // Persiste o Animal via service (não direto no repository) para passar
        // pelas mesmas regras que a aplicação real aplicaria ao salvar.
        return animalService.salvar(animal);
    }

    // HAPPY PATH: excluir() deve fazer exclusão LÓGICA — o registro some das buscas,
    // mas continua existindo fisicamente na tabela (não é um DELETE físico).
    // Passo a passo:
    // 1. Cria um Animal novo (isolado do fixture) para excluir neste teste.
    // 2. Chama animalService.excluir(id), que internamente marca "ativo = false" e
    //    salva a entidade (em vez de apagar a linha do banco).
    // 3. Chama entityManager.flush() (envia o UPDATE pendente para o banco) e depois
    //    entityManager.clear() (esvazia o cache de 1º nível da sessão JPA). Isso é
    //    necessário porque @SQLRestriction("ativo = true") só filtra quando o
    //    Hibernate precisa ir ao banco — se a entidade já estiver em memória no
    //    contexto de persistência (como ficaria logo após o passo 2, na mesma
    //    transação), findById devolveria ela direto do cache, sem passar pelo
    //    filtro. Fazendo flush+clear, simulamos o que aconteceria de verdade em uma
    //    request HTTP nova (com um EntityManager novo, sem cache prévio).
    // 4. Verificamos que buscarPorId(id) agora lança AnimalNotFoundException, ou
    //    seja, o Animal excluído não é mais "encontrável" pela aplicação.
    // 5. Verificamos também que listar() não traz mais o id excluído na lista.
    @Test
    void excluir_comIdExistente_devePararDeAparecerEmBuscaEListagem() {
        Animal animalCriado = criarAnimal();

        animalService.excluir(animalCriado.getId());
        entityManager.flush();
        entityManager.clear();

        assertThatThrownBy(() -> animalService.buscarPorId(animalCriado.getId()))
                .isInstanceOf(AnimalNotFoundException.class);
        assertThat(animalService.listar())
                .extracting(Animal::getId)
                .doesNotContain(animalCriado.getId());

        // Prova de que é exclusão LÓGICA, não física: consultamos a tabela via SQL
        // nativo, contornando o @SQLRestriction (que só existe no mapeamento JPA da
        // entidade Animal, não no banco em si). Se getSingleResult() não lançar
        // NoResultException, a linha ainda existe fisicamente; e o valor de "ativo"
        // deve ter sido marcado como false pelo excluir().
        Boolean ativo = (Boolean) entityManager
                .createNativeQuery("SELECT ativo FROM animal WHERE id = :id")
                .setParameter("id", animalCriado.getId())
                .getSingleResult();
        assertThat(ativo).isFalse();
    }

    // UNHAPPY PATH: excluir() com um id que não existe (nem nunca vai existir, por
    // ser Long.MAX_VALUE — ver explicação equivalente em EspecieServiceIT) deve
    // lançar AnimalNotFoundException, em vez de falhar silenciosamente ou de forma
    // inesperada. Isso é garantido porque excluir() chama buscarPorId(id) por
    // dentro antes de tentar desativar o registro.
    @Test
    void excluir_comIdInexistente_deveLancarAnimalNotFoundException() {
        assertThatThrownBy(() -> animalService.excluir(Long.MAX_VALUE))
                .isInstanceOf(AnimalNotFoundException.class);
    }

    // UNHAPPY PATH: excluir() sobre um id que já foi excluído antes deve se
    // comportar como um id inexistente (AnimalNotFoundException), e não permitir
    // "excluir de novo" silenciosamente. Isso acontece porque, depois do
    // flush()+clear(), o @SQLRestriction faz esse Animal desaparecer de qualquer
    // busca subsequente — inclusive do buscarPorId() interno da segunda chamada a
    // excluir().
    @Test
    void excluir_comIdJaExcluido_deveLancarAnimalNotFoundException() {
        Animal animalCriado = criarAnimal();

        animalService.excluir(animalCriado.getId());
        entityManager.flush();
        entityManager.clear();

        assertThatThrownBy(() -> animalService.excluir(animalCriado.getId()))
                .isInstanceOf(AnimalNotFoundException.class);
    }

    // HAPPY PATH: listar() deve refletir corretamente um cenário misto — com um
    // Animal ativo e outro excluído lado a lado — trazendo só o ativo.
    // Passo a passo:
    // 1. Cria dois Animals novos e isolados: um que vai continuar ativo
    //    (animalMantido) e outro que será excluído (animalExcluido).
    // 2. Exclui apenas o segundo, e faz flush()+clear() para forçar a próxima
    //    chamada a listar() a ir ao banco (mesmo motivo do teste de exclusão acima).
    // 3. Verifica que a lista resultante contém o id do Animal mantido e não
    //    contém o id do Animal excluído — provando que o filtro do
    //    @SQLRestriction distingue corretamente entre os dois, e não é apenas
    //    "sorte" de o excluído não estar lá por outro motivo.
    @Test
    void listar_comRegistroExcluido_deveConterApenasOsAtivos() {
        Animal animalMantido = criarAnimal();
        Animal animalExcluido = criarAnimal();

        animalService.excluir(animalExcluido.getId());
        entityManager.flush();
        entityManager.clear();

        assertThat(animalService.listar())
                .extracting(Animal::getId)
                .contains(animalMantido.getId())
                .doesNotContain(animalExcluido.getId());
    }

    // Monta (sem salvar) um Animal válido com o status e as datas informados, para os
    // testes das regras de data de saída. OBITO é usado quando há data de saída, pois
    // ADOTADO exigiria também um adotante.
    private Animal montarAnimalComDatas(AnimalStatus status, LocalDate dataResgate, LocalDate dataSaida) {
        Raca racaExistente = racaService.listar().get(0);
        Responsavel responsavelExistente = responsavelService.listar().get(0);

        return Animal.builder()
                .nome("Animal de teste")
                .idadeMeses(1)
                .porte(AnimalPorte.PEQUENO)
                .sexo(AnimalSexo.MACHO)
                .status(status)
                .castrado(true)
                .dataResgate(dataResgate)
                .dataSaida(dataSaida)
                .raca(Raca.builder().id(racaExistente.getId()).build())
                .responsavel(Responsavel.builder().id(responsavelExistente.getId()).build())
                .build();
    }

    // UNHAPPY PATH: data de saída anterior à data de resgate deve ser barrada no
    // service com DomainException e mensagem específica, em vez de estourar a
    // constraint CHECK do banco (que resultaria em erro 500 genérico).
    @Test
    void salvar_comDataSaidaAnteriorAoResgate_deveLancarDomainException() {
        LocalDate resgate = LocalDate.now().minusDays(1);
        Animal animal = montarAnimalComDatas(AnimalStatus.OBITO, resgate, resgate.minusDays(1));

        assertThatThrownBy(() -> animalService.salvar(animal))
                .isInstanceOf(DomainException.class)
                .hasMessageContaining("não pode ser anterior à data de resgate");
    }

    // HAPPY PATH: saída no mesmo dia do resgate é permitida (regra é ">=").
    @Test
    void salvar_comDataSaidaIgualAoResgate_deveSalvar() {
        LocalDate resgate = LocalDate.now();
        Animal salvo = animalService.salvar(montarAnimalComDatas(AnimalStatus.OBITO, resgate, resgate));

        assertThat(salvo.getId()).isNotNull();
    }

    // HAPPY PATH: data de saída é opcional para status que não são de saída.
    @Test
    void salvar_disponivelSemDataSaida_deveSalvar() {
        Animal salvo = animalService.salvar(
                montarAnimalComDatas(AnimalStatus.DISPONIVEL, LocalDate.now(), null));

        assertThat(salvo.getId()).isNotNull();
    }

    // UNHAPPY PATH: status OBITO exige data de saída.
    @Test
    void salvar_obitoSemDataSaida_deveLancarDomainException() {
        Animal animal = montarAnimalComDatas(AnimalStatus.OBITO, LocalDate.now(), null);

        assertThatThrownBy(() -> animalService.salvar(animal))
                .isInstanceOf(DomainException.class)
                .hasMessageContaining("data de saída");
    }

    // UNHAPPY PATH: status ADOTADO exige data de saída (a regra é checada antes do adotante).
    @Test
    void salvar_adotadoSemDataSaida_deveLancarDomainException() {
        Animal animal = montarAnimalComDatas(AnimalStatus.ADOTADO, LocalDate.now(), null);

        assertThatThrownBy(() -> animalService.salvar(animal))
                .isInstanceOf(DomainException.class)
                .hasMessageContaining("data de saída");
    }

    // Consistência: status que não é de saída descarta a data de saída enviada.
    @Test
    void salvar_disponivelComDataSaida_deveDescartarDataSaida() {
        LocalDate hoje = LocalDate.now();
        Animal salvo = animalService.salvar(
                montarAnimalComDatas(AnimalStatus.DISPONIVEL, hoje, hoje));

        assertThat(salvo.getDataSaida()).isNull();
    }
}
