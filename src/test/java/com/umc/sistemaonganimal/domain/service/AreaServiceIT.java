package com.umc.sistemaonganimal.domain.service;

import com.umc.sistemaonganimal.domain.exception.AreaInUseException;
import com.umc.sistemaonganimal.domain.exception.AreaNotFoundException;
import com.umc.sistemaonganimal.domain.exception.VoluntarioNotFoundException;
import com.umc.sistemaonganimal.domain.model.Area;
import com.umc.sistemaonganimal.domain.model.Voluntario;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
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
class AreaServiceIT {

    @Autowired
    private AreaService areaService;

    // Usado só para obter voluntários já existentes no banco (via fixture), sem
    // depender de qual voluntário específico é, e para desativar um voluntário
    // no teste que prova o comportamento de @SQLRestriction na checagem "em uso".
    @Autowired
    private VoluntarioService voluntarioService;

    @Autowired
    private EntityManager entityManager;

    // Método auxiliar que cria e persiste uma Area nova, isolada dos dados do
    // fixture, para que cada teste tenha seu próprio registro.
    // O nome inclui um sufixo aleatório para evitar qualquer colisão entre testes.
    private Area criarArea() {
        return criarArea("Área de teste " + sufixoUnico());
    }

    private Area criarArea(String nome) {
        Area area = Area.builder()
                .nome(nome)
                .build();

        return areaService.salvar(area);
    }

    private String sufixoUnico() {
        return UUID.randomUUID().toString().substring(0, 8);
    }

    @Test
    void salvar_comPayloadValido_devePersistirArea() {
        Area area = Area.builder()
                .nome("Área " + sufixoUnico())
                .descricao("Descrição de teste")
                .observacao("Observação de teste")
                .build();

        Area resultado = areaService.salvar(area);

        assertThat(resultado.getId()).isNotNull();
        assertThat(resultado.getDescricao()).isEqualTo("Descrição de teste");
        assertThat(resultado.getObservacao()).isEqualTo("Observação de teste");
    }

    // HAPPY PATH: excluir() deve fazer exclusão LÓGICA — o registro some das buscas,
    // mas continua existindo fisicamente na tabela (não é um DELETE físico). Mesma
    // lógica de flush()+clear() usada em RacaServiceIT (ver comentários lá).
    @Test
    void excluir_semVoluntarioVinculado_deveDesativarArea() {
        Area areaCriada = criarArea();

        areaService.excluir(areaCriada.getId());
        entityManager.flush();
        entityManager.clear();

        assertThatThrownBy(() -> areaService.buscarPorId(areaCriada.getId()))
                .isInstanceOf(AreaNotFoundException.class);
        assertThat(areaService.listar())
                .extracting(Area::getId)
                .doesNotContain(areaCriada.getId());

        Boolean ativo = (Boolean) entityManager
                .createNativeQuery("SELECT ativo FROM area WHERE id = :id")
                .setParameter("id", areaCriada.getId())
                .getSingleResult();
        assertThat(ativo).isFalse();
    }

    // UNHAPPY PATH: uma Area com voluntário ativo vinculado não pode ser excluída.
    @Test
    void excluir_comVoluntarioVinculado_deveLancarAreaInUseException() {
        Area areaCriada = criarArea();
        Voluntario voluntarioExistente = voluntarioService.listar().get(0);

        areaService.associarVoluntario(areaCriada.getId(), voluntarioExistente.getId());

        assertThatThrownBy(() -> areaService.excluir(areaCriada.getId()))
                .isInstanceOf(AreaInUseException.class);
    }

    @Test
    void excluir_comIdInexistente_deveLancarAreaNotFoundException() {
        assertThatThrownBy(() -> areaService.excluir(Long.MAX_VALUE))
                .isInstanceOf(AreaNotFoundException.class);
    }

    @Test
    void excluir_comIdJaExcluido_deveLancarAreaNotFoundException() {
        Area areaCriada = criarArea();

        areaService.excluir(areaCriada.getId());
        entityManager.flush();
        entityManager.clear();

        assertThatThrownBy(() -> areaService.excluir(areaCriada.getId()))
                .isInstanceOf(AreaNotFoundException.class);
    }

    @Test
    void listar_comRegistroExcluido_deveConterApenasOsAtivos() {
        Area areaMantida = criarArea();
        Area areaExcluida = criarArea();

        areaService.excluir(areaExcluida.getId());
        entityManager.flush();
        entityManager.clear();

        assertThat(areaService.listar())
                .extracting(Area::getId)
                .contains(areaMantida.getId())
                .doesNotContain(areaExcluida.getId());
    }

    @Test
    void associarVoluntario_comAreaEVoluntarioExistentes_deveAdicionarVinculo() {
        Area areaCriada = criarArea();
        Voluntario voluntarioExistente = voluntarioService.listar().get(0);

        areaService.associarVoluntario(areaCriada.getId(), voluntarioExistente.getId());
        entityManager.flush();
        entityManager.clear();

        Area areaAtualizada = areaService.buscarPorId(areaCriada.getId());
        assertThat(areaAtualizada.getVoluntarios())
                .extracting(Voluntario::getId)
                .contains(voluntarioExistente.getId());
    }

    // HAPPY PATH: associar o mesmo par (área, voluntário) duas vezes deve ser
    // idempotente — a coleção é um Set, então a segunda chamada não deve gerar
    // erro nem duplicar a linha da tabela associativa.
    @Test
    void associarVoluntario_chamadoDuasVezesComMesmoPar_deveSerIdempotente() {
        Area areaCriada = criarArea();
        Voluntario voluntarioExistente = voluntarioService.listar().get(0);

        areaService.associarVoluntario(areaCriada.getId(), voluntarioExistente.getId());
        areaService.associarVoluntario(areaCriada.getId(), voluntarioExistente.getId());
        entityManager.flush();
        entityManager.clear();

        Area areaAtualizada = areaService.buscarPorId(areaCriada.getId());
        assertThat(areaAtualizada.getVoluntarios()).hasSize(1);
    }

    @Test
    void associarVoluntario_comAreaInexistente_deveLancarAreaNotFoundException() {
        Voluntario voluntarioExistente = voluntarioService.listar().get(0);

        assertThatThrownBy(() -> areaService.associarVoluntario(Long.MAX_VALUE, voluntarioExistente.getId()))
                .isInstanceOf(AreaNotFoundException.class);
    }

    @Test
    void associarVoluntario_comVoluntarioInexistente_deveLancarVoluntarioNotFoundException() {
        Area areaCriada = criarArea();

        assertThatThrownBy(() -> areaService.associarVoluntario(areaCriada.getId(), Long.MAX_VALUE))
                .isInstanceOf(VoluntarioNotFoundException.class);
    }

    @Test
    void desassociarVoluntario_comVinculoExistente_deveRemoverVinculo() {
        Area areaCriada = criarArea();
        Voluntario voluntarioExistente = voluntarioService.listar().get(0);
        areaService.associarVoluntario(areaCriada.getId(), voluntarioExistente.getId());

        areaService.desassociarVoluntario(areaCriada.getId(), voluntarioExistente.getId());
        entityManager.flush();
        entityManager.clear();

        Area areaAtualizada = areaService.buscarPorId(areaCriada.getId());
        assertThat(areaAtualizada.getVoluntarios())
                .extracting(Voluntario::getId)
                .doesNotContain(voluntarioExistente.getId());
    }

    // desassociar é idempotente (mesma semântica de associar): remover um vínculo
    // que não existe não deve lançar exceção.
    @Test
    void desassociarVoluntario_comVinculoInexistente_deveSerIdempotente() {
        Area areaCriada = criarArea();
        Voluntario voluntarioExistente = voluntarioService.listar().get(0);

        assertThatCode(() -> areaService.desassociarVoluntario(areaCriada.getId(), voluntarioExistente.getId()))
                .doesNotThrowAnyException();
    }

    // Prova de que desassociar de uma área não afeta o vínculo do mesmo
    // voluntário com outra área.
    @Test
    void desassociarVoluntario_naoDeveAfetarVoluntarioEmOutrasAreas() {
        Area primeiraArea = criarArea();
        Area segundaArea = criarArea();
        Voluntario voluntarioExistente = voluntarioService.listar().get(0);

        areaService.associarVoluntario(primeiraArea.getId(), voluntarioExistente.getId());
        areaService.associarVoluntario(segundaArea.getId(), voluntarioExistente.getId());

        areaService.desassociarVoluntario(primeiraArea.getId(), voluntarioExistente.getId());
        entityManager.flush();
        entityManager.clear();

        Area segundaAreaAtualizada = areaService.buscarPorId(segundaArea.getId());
        assertThat(segundaAreaAtualizada.getVoluntarios())
                .extracting(Voluntario::getId)
                .contains(voluntarioExistente.getId());
    }

    // Prova de que a checagem "em uso" de excluir() respeita o @SQLRestriction de
    // Voluntario: um voluntário vinculado mas já desativado (soft-deleted) não
    // deve contar como vínculo ativo, permitindo a exclusão da área.
    @Test
    void excluir_comVoluntarioVinculadoMasDesativado_devePermitirExclusao() {
        Area areaCriada = criarArea();
        List<Voluntario> voluntarios = voluntarioService.listar();
        Voluntario voluntarioParaDesativar = voluntarios.get(voluntarios.size() - 1);

        areaService.associarVoluntario(areaCriada.getId(), voluntarioParaDesativar.getId());
        voluntarioService.excluir(voluntarioParaDesativar.getId());
        entityManager.flush();
        entityManager.clear();

        areaService.excluir(areaCriada.getId());
        entityManager.flush();
        entityManager.clear();

        assertThatThrownBy(() -> areaService.buscarPorId(areaCriada.getId()))
                .isInstanceOf(AreaNotFoundException.class);
    }
}
