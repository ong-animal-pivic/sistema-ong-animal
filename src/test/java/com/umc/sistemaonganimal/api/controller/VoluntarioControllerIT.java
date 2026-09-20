package com.umc.sistemaonganimal.api.controller;

import com.umc.sistemaonganimal.api.dto.embeddables.ContatoDTO;
import com.umc.sistemaonganimal.api.dto.embeddables.DocumentoDTO;
import com.umc.sistemaonganimal.api.dto.embeddables.EnderecoDTO;
import com.umc.sistemaonganimal.api.dto.request.VoluntarioRequestDTO;
import com.umc.sistemaonganimal.domain.model.enums.general.Frequencia;
import com.umc.sistemaonganimal.domain.repository.ResponsavelRepository;
import com.umc.sistemaonganimal.domain.repository.VoluntarioRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.restassured.RestAssured;
import io.restassured.config.ObjectMapperConfig;
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
import static org.hamcrest.Matchers.notNullValue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("local")
class VoluntarioControllerIT {

    @LocalServerPort
    private int port;

    @Autowired
    private VoluntarioRepository voluntarioRepository;

    @Autowired
    private ResponsavelRepository responsavelRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private Long voluntarioIdCriado;

    @BeforeEach
    void configurarRestAssured() {
        RestAssured.port = port;
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();

        // Mesma convenção ISO-8601/UTC declarada explicitamente pelo servidor em
        // application.properties (spring.jackson.time-zone=UTC, write-dates-as-timestamps=false;
        // ver CLAUDE.md, seção "Data e hora"). Duplicada aqui porque o ObjectMapper do RestAssured
        // simula um cliente HTTP externo e não herda a auto-configuração do Jackson do servidor.
        ObjectMapper objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        RestAssured.config = RestAssured.config().objectMapperConfig(
                ObjectMapperConfig.objectMapperConfig()
                        .jackson2ObjectMapperFactory((type, s) -> objectMapper));
    }

    @AfterEach
    void limparVoluntarioCriado() {
        if (voluntarioIdCriado != null) {
            jdbcTemplate.update("DELETE FROM voluntario WHERE id = ?", voluntarioIdCriado);
            voluntarioIdCriado = null;
        }
    }

    private VoluntarioRequestDTO montarVoluntarioValido() {
        return VoluntarioRequestDTO.builder()
                .nome("Voluntário via API")
                .documento(DocumentoDTO.builder().cpf("52998224725").build())
                .idade(30)
                .profissao("Estudante")
                .contato(ContatoDTO.builder()
                        .telefonePrincipal("11999990004")
                        .email("voluntario.api@example.com")
                        .build())
                .frequencia(Frequencia.SEMANAL)
                .endereco(EnderecoDTO.builder()
                        .logradouro("Rua do Voluntariado")
                        .bairro("Bairro API")
                        .cidade("Cidade API")
                        .estado("SP")
                        .cep("01001000")
                        .numero("100")
                        .build())
                .responsavelId(responsavelRepository.findAll().get(0).getId())
                .build();
    }

    @Test
    void adicionar_comPayloadValido_deveRetornarCreatedComVoluntarioPersistido() {
        voluntarioIdCriado = given()
                .contentType(ContentType.JSON)
                .body(montarVoluntarioValido())
                .when()
                .post("/voluntarios")
                .then()
                .statusCode(201)
                .body("id", notNullValue())
                .body("nome", equalTo("Voluntário via API"))
                .body("documento.cpf", equalTo("52998224725"))
                .body("frequencia", equalTo("SEMANAL"))
                .extract().jsonPath().getLong("id");
    }

    @Test
    void adicionar_semCpfEmailIdadeProfissao_deveRetornarCreated() {
        VoluntarioRequestDTO payload = montarVoluntarioValido();
        payload.setDocumento(null);
        payload.setIdade(null);
        payload.setProfissao(null);
        payload.getContato().setEmail(null);

        voluntarioIdCriado = given()
                .contentType(ContentType.JSON)
                .body(payload)
                .when()
                .post("/voluntarios")
                .then()
                .statusCode(201)
                .body("id", notNullValue())
                .extract().jsonPath().getLong("id");
    }

    @Test
    void adicionar_semNome_deveRetornarBadRequestComDetalheDoCampo() {
        VoluntarioRequestDTO payloadInvalido = montarVoluntarioValido();
        payloadInvalido.setNome(null);

        given()
                .contentType(ContentType.JSON)
                .body(payloadInvalido)
                .when()
                .post("/voluntarios")
                .then()
                .statusCode(400)
                .body("detalhes.nome", notNullValue());
    }

    @Test
    void adicionar_semFrequencia_deveRetornarBadRequestComDetalheDoCampo() {
        VoluntarioRequestDTO payloadInvalido = montarVoluntarioValido();
        payloadInvalido.setFrequencia(null);

        given()
                .contentType(ContentType.JSON)
                .body(payloadInvalido)
                .when()
                .post("/voluntarios")
                .then()
                .statusCode(400)
                .body("detalhes.frequencia", notNullValue());
    }

    @Test
    void adicionar_comCpfInvalido_deveRetornarBadRequest() {
        VoluntarioRequestDTO payloadInvalido = montarVoluntarioValido();
        payloadInvalido.setDocumento(DocumentoDTO.builder().cpf("11111111111").build());

        given()
                .contentType(ContentType.JSON)
                .body(payloadInvalido)
                .when()
                .post("/voluntarios")
                .then()
                .statusCode(400)
                .body("detalhes['documento.cpf']", notNullValue());
    }

    @Test
    void adicionar_comEmailDuplicado_deveRetornarConflict() {
        voluntarioIdCriado = criarVoluntarioViaApi();

        VoluntarioRequestDTO payloadDuplicado = montarVoluntarioValido();
        payloadDuplicado.setNome("Outro Voluntário");

        given()
                .contentType(ContentType.JSON)
                .body(payloadDuplicado)
                .when()
                .post("/voluntarios")
                .then()
                .statusCode(409)
                .body("title", equalTo("Entidade já existente"));
    }

    @Test
    void adicionar_comResponsavelInexistente_deveRetornarNotFound() {
        long responsavelIdInexistente = responsavelRepository.findAll().stream()
                .mapToLong(r -> r.getId())
                .max()
                .orElse(0L) + 1;

        VoluntarioRequestDTO payload = montarVoluntarioValido();
        payload.setResponsavelId(responsavelIdInexistente);

        given()
                .contentType(ContentType.JSON)
                .body(payload)
                .when()
                .post("/voluntarios")
                .then()
                .statusCode(404)
                .body("title", equalTo("Entidade não encontrada"));
    }

    @Test
    void buscar_comIdExistente_deveRetornarVoluntario() {
        voluntarioIdCriado = criarVoluntarioViaApi();

        given()
                .when()
                .get("/voluntarios/{id}", voluntarioIdCriado)
                .then()
                .statusCode(200)
                .body("id", equalTo(voluntarioIdCriado.intValue()))
                .body("nome", equalTo("Voluntário via API"));
    }

    @Test
    void buscar_comIdInexistente_deveRetornarNotFound() {
        long idInexistente = voluntarioRepository.findAll().stream()
                .mapToLong(v -> v.getId())
                .max()
                .orElse(0L) + 1;

        given()
                .when()
                .get("/voluntarios/{id}", idInexistente)
                .then()
                .statusCode(404)
                .body("title", equalTo("Entidade não encontrada"));
    }

    @Test
    void listar_deveConterVoluntarioRecemCriado() {
        voluntarioIdCriado = criarVoluntarioViaApi();

        given()
                .when()
                .get("/voluntarios")
                .then()
                .statusCode(200)
                .body("id", hasItem(voluntarioIdCriado.intValue()));
    }

    @Test
    void atualizar_comPayloadValido_deveRefletirAlteracao() {
        voluntarioIdCriado = criarVoluntarioViaApi();

        VoluntarioRequestDTO payloadAtualizado = montarVoluntarioValido();
        payloadAtualizado.setNome("Voluntário via API - atualizado");

        given()
                .contentType(ContentType.JSON)
                .body(payloadAtualizado)
                .when()
                .put("/voluntarios/{id}", voluntarioIdCriado)
                .then()
                .statusCode(200)
                .body("nome", equalTo("Voluntário via API - atualizado"));
    }

    @Test
    void atualizar_comEmailJaUsadoPorOutroVoluntario_deveRetornarConflict() {
        voluntarioIdCriado = criarVoluntarioViaApi();

        VoluntarioRequestDTO segundoVoluntario = montarVoluntarioValido();
        segundoVoluntario.setNome("Segundo Voluntário");
        segundoVoluntario.getContato().setEmail("segundo.voluntario@example.com");

        Long segundoVoluntarioId = given()
                .contentType(ContentType.JSON)
                .body(segundoVoluntario)
                .when()
                .post("/voluntarios")
                .then()
                .statusCode(201)
                .extract().jsonPath().getLong("id");

        try {
            VoluntarioRequestDTO payloadComEmailDuplicado = montarVoluntarioValido();
            payloadComEmailDuplicado.getContato().setEmail("voluntario.api@example.com");

            given()
                    .contentType(ContentType.JSON)
                    .body(payloadComEmailDuplicado)
                    .when()
                    .put("/voluntarios/{id}", segundoVoluntarioId)
                    .then()
                    .statusCode(409)
                    .body("title", equalTo("Entidade já existente"));
        } finally {
            jdbcTemplate.update("DELETE FROM voluntario WHERE id = ?", segundoVoluntarioId);
        }
    }

    @Test
    void excluir_comIdExistente_deveRetornarNoContentEDeixarDeAparecerNaBusca() {
        voluntarioIdCriado = criarVoluntarioViaApi();

        given()
                .when()
                .delete("/voluntarios/{id}", voluntarioIdCriado)
                .then()
                .statusCode(204);

        given()
                .when()
                .get("/voluntarios/{id}", voluntarioIdCriado)
                .then()
                .statusCode(404);
    }

    private Long criarVoluntarioViaApi() {
        return given()
                .contentType(ContentType.JSON)
                .body(montarVoluntarioValido())
                .when()
                .post("/voluntarios")
                .then()
                .statusCode(201)
                .extract().jsonPath().getLong("id");
    }
}
