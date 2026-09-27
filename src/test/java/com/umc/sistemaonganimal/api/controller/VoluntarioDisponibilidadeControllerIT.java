package com.umc.sistemaonganimal.api.controller;

import com.umc.sistemaonganimal.api.dto.request.DisponibilidadeRequestDTO;
import com.umc.sistemaonganimal.domain.model.Responsavel;
import com.umc.sistemaonganimal.domain.model.enums.general.DiaSemana;
import com.umc.sistemaonganimal.domain.model.enums.general.TipoResponsavel;
import com.umc.sistemaonganimal.domain.model.enums.general.Turno;
import com.umc.sistemaonganimal.domain.repository.ResponsavelRepository;
import com.umc.sistemaonganimal.domain.repository.VoluntarioRepository;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.in;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("local")
class VoluntarioDisponibilidadeControllerIT {

    private static final String MENSAGEM_DUPLICADA = "Esta disponibilidade já está cadastrada para o voluntário.";

    @LocalServerPort
    private int port;

    @Autowired
    private VoluntarioRepository voluntarioRepository;

    @Autowired
    private ResponsavelRepository responsavelRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private final List<Long> voluntariosCriados = new ArrayList<>();

    @BeforeEach
    void configurarRestAssured() {
        RestAssured.port = port;
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
    }

    @AfterEach
    void limparVoluntariosCriados() {
        for (Long id : voluntariosCriados) {
            jdbcTemplate.update("DELETE FROM voluntario_disponibilidade WHERE voluntario_id = ?", id);
            jdbcTemplate.update("DELETE FROM voluntario WHERE id = ?", id);
        }
        voluntariosCriados.clear();
    }

    // Cria um voluntário próprio do teste (sem disponibilidades), para não depender
    // dos vínculos semeados no fixture.
    private long criarVoluntario() {
        Long responsavelOngId = responsavelRepository.findAll().stream()
                .filter(r -> r.getTipo() != null && r.getTipo().getNome() == TipoResponsavel.ONG)
                .map(Responsavel::getId)
                .findFirst()
                .orElseThrow();

        Long id = jdbcTemplate.queryForObject("""
                INSERT INTO voluntario (nome, telefone1, frequencia, logradouro, bairro, cidade, estado, cep, num_endereco, responsavel_id)
                VALUES ('Voluntário Disponibilidade IT', '11999990000', 'SEMANAL', 'Rua IT', 'Bairro IT', 'Cidade IT', 'SP', '01001000', '1', ?)
                RETURNING id
                """, Long.class, responsavelOngId);
        voluntariosCriados.add(id);
        return id;
    }

    private long idInexistenteDeVoluntario() {
        return voluntarioRepository.findAll().stream()
                .mapToLong(v -> v.getId())
                .max()
                .orElse(0L) + 1;
    }

    private DisponibilidadeRequestDTO montarDisponibilidade(DiaSemana dia, Turno turno) {
        return DisponibilidadeRequestDTO.builder()
                .diaSemana(dia)
                .turno(turno)
                .observacao("Observação via API")
                .build();
    }

    private long adicionarViaApi(long voluntarioId, DisponibilidadeRequestDTO payload) {
        return given()
                .contentType(ContentType.JSON)
                .body(payload)
                .when()
                .post("/voluntarios/{voluntarioId}/disponibilidades", voluntarioId)
                .then()
                .statusCode(201)
                .extract().jsonPath().getLong("id");
    }

    @Test
    void listar_voluntarioSemDisponibilidades_deveRetornarListaVazia() {
        long voluntarioId = criarVoluntario();

        given()
                .when()
                .get("/voluntarios/{voluntarioId}/disponibilidades", voluntarioId)
                .then()
                .statusCode(200)
                .body("$", empty());
    }

    @Test
    void listar_voluntarioInexistente_deveRetornarNotFound() {
        given()
                .when()
                .get("/voluntarios/{voluntarioId}/disponibilidades", idInexistenteDeVoluntario())
                .then()
                .statusCode(404)
                .body("title", equalTo("Entidade não encontrada"));
    }

    @Test
    void listar_deveOrdenarPorDiaDaSemanaETurno() {
        long voluntarioId = criarVoluntario();
        adicionarViaApi(voluntarioId, montarDisponibilidade(DiaSemana.SABADO, Turno.MANHA));
        adicionarViaApi(voluntarioId, montarDisponibilidade(DiaSemana.SEGUNDA, Turno.NOITE));
        adicionarViaApi(voluntarioId, montarDisponibilidade(DiaSemana.SEGUNDA, Turno.MANHA));

        given()
                .when()
                .get("/voluntarios/{voluntarioId}/disponibilidades", voluntarioId)
                .then()
                .statusCode(200)
                .body("diaSemana", contains("SEGUNDA", "SEGUNDA", "SABADO"))
                .body("turno", contains("MANHA", "NOITE", "MANHA"));
    }

    @Test
    void listar_todosOsVoluntarios_devemTerDiaETurnoValidos() {
        List<String> dias = Arrays.stream(DiaSemana.values()).map(Enum::name).toList();
        List<String> turnos = Arrays.stream(Turno.values()).map(Enum::name).toList();

        voluntarioRepository.findAll().forEach(v ->
                given()
                        .when()
                        .get("/voluntarios/{voluntarioId}/disponibilidades", v.getId())
                        .then()
                        .statusCode(200)
                        .body("diaSemana", everyItem(in(dias)))
                        .body("turno", everyItem(in(turnos))));
    }

    @Test
    void adicionar_comPayloadValido_deveRetornarCreatedEAparecerNaListagem() {
        long voluntarioId = criarVoluntario();

        long disponibilidadeId = given()
                .contentType(ContentType.JSON)
                .body(montarDisponibilidade(DiaSemana.TERCA, Turno.TARDE))
                .when()
                .post("/voluntarios/{voluntarioId}/disponibilidades", voluntarioId)
                .then()
                .statusCode(201)
                .body("id", notNullValue())
                .body("diaSemana", equalTo("TERCA"))
                .body("turno", equalTo("TARDE"))
                .body("observacao", equalTo("Observação via API"))
                .extract().jsonPath().getLong("id");

        given()
                .when()
                .get("/voluntarios/{voluntarioId}/disponibilidades", voluntarioId)
                .then()
                .statusCode(200)
                .body("id", hasItem((int) disponibilidadeId));
    }

    @Test
    void adicionar_semObservacao_deveRetornarCreated() {
        long voluntarioId = criarVoluntario();
        DisponibilidadeRequestDTO payload = montarDisponibilidade(DiaSemana.QUARTA, Turno.NOITE);
        payload.setObservacao(null);

        given()
                .contentType(ContentType.JSON)
                .body(payload)
                .when()
                .post("/voluntarios/{voluntarioId}/disponibilidades", voluntarioId)
                .then()
                .statusCode(201)
                .body("observacao", nullValue());
    }

    @Test
    void adicionar_variasParaOMesmoVoluntario_deveAceitarTodas() {
        long voluntarioId = criarVoluntario();
        adicionarViaApi(voluntarioId, montarDisponibilidade(DiaSemana.QUINTA, Turno.MANHA));
        adicionarViaApi(voluntarioId, montarDisponibilidade(DiaSemana.QUINTA, Turno.TARDE));

        given()
                .when()
                .get("/voluntarios/{voluntarioId}/disponibilidades", voluntarioId)
                .then()
                .statusCode(200)
                .body("$", hasSize(2));
    }

    @Test
    void adicionar_duplicadaParaOMesmoVoluntario_deveRetornarConflictComMensagem() {
        long voluntarioId = criarVoluntario();
        adicionarViaApi(voluntarioId, montarDisponibilidade(DiaSemana.SEXTA, Turno.MANHA));

        given()
                .contentType(ContentType.JSON)
                .body(montarDisponibilidade(DiaSemana.SEXTA, Turno.MANHA))
                .when()
                .post("/voluntarios/{voluntarioId}/disponibilidades", voluntarioId)
                .then()
                .statusCode(409)
                .body("detail", equalTo(MENSAGEM_DUPLICADA));
    }

    @Test
    void adicionar_mesmaCombinacaoParaVoluntariosDiferentes_devePermitir() {
        long voluntarioA = criarVoluntario();
        long voluntarioB = criarVoluntario();

        long idA = adicionarViaApi(voluntarioA, montarDisponibilidade(DiaSemana.DOMINGO, Turno.TARDE));
        long idB = adicionarViaApi(voluntarioB, montarDisponibilidade(DiaSemana.DOMINGO, Turno.TARDE));

        // Mesma Disponibilidade do catálogo, compartilhada pelos dois voluntários.
        assertEquals(idA, idB);
    }

    @Test
    void adicionar_semDiaETurno_deveRetornarBadRequestComDetalhes() {
        long voluntarioId = criarVoluntario();

        given()
                .contentType(ContentType.JSON)
                .body(DisponibilidadeRequestDTO.builder().observacao("Sem dia e turno").build())
                .when()
                .post("/voluntarios/{voluntarioId}/disponibilidades", voluntarioId)
                .then()
                .statusCode(400)
                .body("detalhes.diaSemana", notNullValue())
                .body("detalhes.turno", notNullValue());
    }

    @Test
    void adicionar_comValorDeEnumInvalido_deveRetornarBadRequest() {
        long voluntarioId = criarVoluntario();

        given()
                .contentType(ContentType.JSON)
                .body(Map.of("diaSemana", "FERIADO", "turno", "MANHA"))
                .when()
                .post("/voluntarios/{voluntarioId}/disponibilidades", voluntarioId)
                .then()
                .statusCode(400);
    }

    @Test
    void adicionar_voluntarioInexistente_deveRetornarNotFound() {
        given()
                .contentType(ContentType.JSON)
                .body(montarDisponibilidade(DiaSemana.SEGUNDA, Turno.MANHA))
                .when()
                .post("/voluntarios/{voluntarioId}/disponibilidades", idInexistenteDeVoluntario())
                .then()
                .statusCode(404)
                .body("title", equalTo("Entidade não encontrada"));
    }

    @Test
    void remover_vinculoExistente_deveRetornarNoContentESumirDaListagem() {
        long voluntarioId = criarVoluntario();
        long disponibilidadeId = adicionarViaApi(voluntarioId, montarDisponibilidade(DiaSemana.TERCA, Turno.NOITE));

        given()
                .when()
                .delete("/voluntarios/{voluntarioId}/disponibilidades/{disponibilidadeId}", voluntarioId, disponibilidadeId)
                .then()
                .statusCode(204);

        given()
                .when()
                .get("/voluntarios/{voluntarioId}/disponibilidades", voluntarioId)
                .then()
                .statusCode(200)
                .body("$", empty());
    }

    @Test
    void remover_naoAfetaOutroVoluntarioComAMesmaDisponibilidade() {
        long voluntarioA = criarVoluntario();
        long voluntarioB = criarVoluntario();
        long disponibilidadeId = adicionarViaApi(voluntarioA, montarDisponibilidade(DiaSemana.QUARTA, Turno.MANHA));
        adicionarViaApi(voluntarioB, montarDisponibilidade(DiaSemana.QUARTA, Turno.MANHA));

        given()
                .when()
                .delete("/voluntarios/{voluntarioId}/disponibilidades/{disponibilidadeId}", voluntarioA, disponibilidadeId)
                .then()
                .statusCode(204);

        given()
                .when()
                .get("/voluntarios/{voluntarioId}/disponibilidades", voluntarioB)
                .then()
                .statusCode(200)
                .body("id", hasItem((int) disponibilidadeId));
    }

    @Test
    void remover_vinculoInexistente_deveRetornarNotFound() {
        long voluntarioId = criarVoluntario();
        long outroVoluntario = criarVoluntario();
        long disponibilidadeId = adicionarViaApi(outroVoluntario, montarDisponibilidade(DiaSemana.SABADO, Turno.NOITE));

        given()
                .when()
                .delete("/voluntarios/{voluntarioId}/disponibilidades/{disponibilidadeId}", voluntarioId, disponibilidadeId)
                .then()
                .statusCode(404)
                .body("title", equalTo("Entidade não encontrada"));
    }
}
