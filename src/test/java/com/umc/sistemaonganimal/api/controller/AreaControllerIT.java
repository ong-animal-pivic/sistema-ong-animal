package com.umc.sistemaonganimal.api.controller;

import com.umc.sistemaonganimal.api.dto.request.AreaRequestDTO;
import com.umc.sistemaonganimal.domain.repository.AreaRepository;
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

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("local")
class AreaControllerIT {

    @LocalServerPort
    private int port;

    @Autowired
    private AreaRepository areaRepository;

    @Autowired
    private VoluntarioRepository voluntarioRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private Long areaIdCriada;

    @BeforeEach
    void configurarRestAssured() {
        RestAssured.port = port;
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
    }

    @AfterEach
    void limparAreaCriada() {
        if (areaIdCriada != null) {
            jdbcTemplate.update("DELETE FROM area_voluntario WHERE area_id = ?", areaIdCriada);
            jdbcTemplate.update("DELETE FROM area WHERE id = ?", areaIdCriada);
            areaIdCriada = null;
        }
    }

    private AreaRequestDTO montarAreaValida() {
        return AreaRequestDTO.builder()
                .nome("Área via API")
                .descricao("Descrição via API")
                .observacao("Observação via API")
                .build();
    }

    private Long criarAreaViaApi() {
        return given()
                .contentType(ContentType.JSON)
                .body(montarAreaValida())
                .when()
                .post("/areas")
                .then()
                .statusCode(201)
                .extract().jsonPath().getLong("id");
    }

    private long buscarIdVoluntarioExistente() {
        return voluntarioRepository.findAll().get(0).getId();
    }

    private long idInexistenteDeArea() {
        return areaRepository.findAll().stream()
                .mapToLong(a -> a.getId())
                .max()
                .orElse(0L) + 1;
    }

    @Test
    void adicionar_comPayloadValido_deveRetornarCreatedComAreaPersistida() {
        areaIdCriada = given()
                .contentType(ContentType.JSON)
                .body(montarAreaValida())
                .when()
                .post("/areas")
                .then()
                .statusCode(201)
                .body("id", notNullValue())
                .body("nome", equalTo("Área via API"))
                .body("descricao", equalTo("Descrição via API"))
                .body("observacao", equalTo("Observação via API"))
                .extract().jsonPath().getLong("id");
    }

    @Test
    void adicionar_somenteComNome_deveRetornarCreated() {
        AreaRequestDTO payload = AreaRequestDTO.builder()
                .nome("Área só com nome")
                .build();

        areaIdCriada = given()
                .contentType(ContentType.JSON)
                .body(payload)
                .when()
                .post("/areas")
                .then()
                .statusCode(201)
                .body("descricao", nullValue())
                .body("observacao", nullValue())
                .extract().jsonPath().getLong("id");
    }

    @Test
    void adicionar_semNome_deveRetornarBadRequestComDetalheDoCampo() {
        AreaRequestDTO payloadInvalido = montarAreaValida();
        payloadInvalido.setNome(null);

        given()
                .contentType(ContentType.JSON)
                .body(payloadInvalido)
                .when()
                .post("/areas")
                .then()
                .statusCode(400)
                .body("detalhes.nome", notNullValue());
    }

    @Test
    void buscar_comIdExistente_deveRetornarArea() {
        areaIdCriada = criarAreaViaApi();

        given()
                .when()
                .get("/areas/{id}", areaIdCriada)
                .then()
                .statusCode(200)
                .body("id", equalTo(areaIdCriada.intValue()))
                .body("nome", equalTo("Área via API"));
    }

    @Test
    void buscar_comIdInexistente_deveRetornarNotFound() {
        given()
                .when()
                .get("/areas/{id}", idInexistenteDeArea())
                .then()
                .statusCode(404)
                .body("title", equalTo("Entidade não encontrada"));
    }

    @Test
    void listar_deveConterAreaRecemCriada() {
        areaIdCriada = criarAreaViaApi();

        given()
                .when()
                .get("/areas")
                .then()
                .statusCode(200)
                .body("id", hasItem(areaIdCriada.intValue()));
    }

    @Test
    void atualizar_comPayloadValido_deveRefletirAlteracao() {
        areaIdCriada = criarAreaViaApi();

        AreaRequestDTO payloadAtualizado = AreaRequestDTO.builder()
                .nome("Área via API - atualizada")
                .descricao("Nova descrição")
                .observacao("Nova observação")
                .build();

        given()
                .contentType(ContentType.JSON)
                .body(payloadAtualizado)
                .when()
                .put("/areas/{id}", areaIdCriada)
                .then()
                .statusCode(200)
                .body("nome", equalTo("Área via API - atualizada"))
                .body("descricao", equalTo("Nova descrição"))
                .body("observacao", equalTo("Nova observação"));
    }

    @Test
    void atualizar_removendoDescricaoEObservacao_deveAceitarNulos() {
        areaIdCriada = criarAreaViaApi();

        AreaRequestDTO payloadAtualizado = AreaRequestDTO.builder()
                .nome("Área sem descrição nem observação")
                .build();

        given()
                .contentType(ContentType.JSON)
                .body(payloadAtualizado)
                .when()
                .put("/areas/{id}", areaIdCriada)
                .then()
                .statusCode(200)
                .body("descricao", nullValue())
                .body("observacao", nullValue());
    }

    @Test
    void atualizar_semNome_deveRetornarBadRequest() {
        areaIdCriada = criarAreaViaApi();

        AreaRequestDTO payloadInvalido = montarAreaValida();
        payloadInvalido.setNome(null);

        given()
                .contentType(ContentType.JSON)
                .body(payloadInvalido)
                .when()
                .put("/areas/{id}", areaIdCriada)
                .then()
                .statusCode(400)
                .body("detalhes.nome", notNullValue());
    }

    @Test
    void atualizar_comIdInexistente_deveRetornarNotFound() {
        given()
                .contentType(ContentType.JSON)
                .body(montarAreaValida())
                .when()
                .put("/areas/{id}", idInexistenteDeArea())
                .then()
                .statusCode(404)
                .body("title", equalTo("Entidade não encontrada"));
    }

    @Test
    void excluir_semVoluntarioVinculado_deveRetornarNoContentEDeixarDeAparecerNaBusca() {
        areaIdCriada = criarAreaViaApi();

        given()
                .when()
                .delete("/areas/{id}", areaIdCriada)
                .then()
                .statusCode(204);

        given()
                .when()
                .get("/areas/{id}", areaIdCriada)
                .then()
                .statusCode(404);
    }

    @Test
    void excluir_comVoluntarioVinculado_deveRetornarConflict() {
        areaIdCriada = criarAreaViaApi();
        long voluntarioId = buscarIdVoluntarioExistente();

        given()
                .when()
                .post("/areas/{areaId}/voluntarios/{voluntarioId}", areaIdCriada, voluntarioId)
                .then()
                .statusCode(204);

        given()
                .when()
                .delete("/areas/{id}", areaIdCriada)
                .then()
                .statusCode(409)
                .body("title", equalTo("Entidade em uso"));
    }

    @Test
    void associarVoluntario_comAreaEVoluntarioExistentes_deveRetornarNoContent() {
        areaIdCriada = criarAreaViaApi();
        long voluntarioId = buscarIdVoluntarioExistente();

        given()
                .when()
                .post("/areas/{areaId}/voluntarios/{voluntarioId}", areaIdCriada, voluntarioId)
                .then()
                .statusCode(204);

        given()
                .when()
                .get("/areas/{id}", areaIdCriada)
                .then()
                .statusCode(200)
                .body("voluntarios.id", hasItem((int) voluntarioId));
    }

    @Test
    void associarVoluntario_comAreaInexistente_deveRetornarNotFound() {
        long voluntarioId = buscarIdVoluntarioExistente();

        given()
                .when()
                .post("/areas/{areaId}/voluntarios/{voluntarioId}", idInexistenteDeArea(), voluntarioId)
                .then()
                .statusCode(404)
                .body("title", equalTo("Entidade não encontrada"));
    }

    @Test
    void associarVoluntario_comVoluntarioInexistente_deveRetornarNotFound() {
        areaIdCriada = criarAreaViaApi();

        long voluntarioIdInexistente = voluntarioRepository.findAll().stream()
                .mapToLong(v -> v.getId())
                .max()
                .orElse(0L) + 1;

        given()
                .when()
                .post("/areas/{areaId}/voluntarios/{voluntarioId}", areaIdCriada, voluntarioIdInexistente)
                .then()
                .statusCode(404)
                .body("title", equalTo("Entidade não encontrada"));
    }

    @Test
    void associarVoluntario_chamadoDuasVezes_deveSerIdempotenteERetornarNoContentNasDuasVezes() {
        areaIdCriada = criarAreaViaApi();
        long voluntarioId = buscarIdVoluntarioExistente();

        given()
                .when()
                .post("/areas/{areaId}/voluntarios/{voluntarioId}", areaIdCriada, voluntarioId)
                .then()
                .statusCode(204);

        given()
                .when()
                .post("/areas/{areaId}/voluntarios/{voluntarioId}", areaIdCriada, voluntarioId)
                .then()
                .statusCode(204);
    }

    @Test
    void desassociarVoluntario_comVinculoExistente_deveRetornarNoContent() {
        areaIdCriada = criarAreaViaApi();
        long voluntarioId = buscarIdVoluntarioExistente();

        given()
                .when()
                .post("/areas/{areaId}/voluntarios/{voluntarioId}", areaIdCriada, voluntarioId)
                .then()
                .statusCode(204);

        given()
                .when()
                .delete("/areas/{areaId}/voluntarios/{voluntarioId}", areaIdCriada, voluntarioId)
                .then()
                .statusCode(204);
    }

    @Test
    void desassociarVoluntario_comVinculoInexistente_deveSerIdempotenteERetornarNoContent() {
        areaIdCriada = criarAreaViaApi();
        long voluntarioId = buscarIdVoluntarioExistente();

        given()
                .when()
                .delete("/areas/{areaId}/voluntarios/{voluntarioId}", areaIdCriada, voluntarioId)
                .then()
                .statusCode(204);
    }

    @Test
    void desassociarVoluntario_seguidoDeBusca_naoDeveMaisAparecerNaListaDeVoluntariosDaArea() {
        areaIdCriada = criarAreaViaApi();
        long voluntarioId = buscarIdVoluntarioExistente();

        given()
                .when()
                .post("/areas/{areaId}/voluntarios/{voluntarioId}", areaIdCriada, voluntarioId)
                .then()
                .statusCode(204);

        given()
                .when()
                .delete("/areas/{areaId}/voluntarios/{voluntarioId}", areaIdCriada, voluntarioId)
                .then()
                .statusCode(204);

        given()
                .when()
                .get("/areas/{id}", areaIdCriada)
                .then()
                .statusCode(200)
                .body("voluntarios.id", not(hasItem((int) voluntarioId)));
    }
}
