package com.umc.sistemaonganimal.api.controller;

import com.umc.sistemaonganimal.api.dto.embeddables.ContatoDTO;
import com.umc.sistemaonganimal.api.dto.embeddables.DocumentoDTO;
import com.umc.sistemaonganimal.api.dto.embeddables.EnderecoDTO;
import com.umc.sistemaonganimal.api.dto.request.ResponsavelRequestDTO;
import com.umc.sistemaonganimal.api.dto.request.VoluntarioRequestDTO;
import com.umc.sistemaonganimal.domain.model.Tipo;
import com.umc.sistemaonganimal.domain.model.enums.general.Frequencia;
import com.umc.sistemaonganimal.domain.model.enums.general.TipoResponsavel;
import com.umc.sistemaonganimal.domain.repository.ResponsavelRepository;
import com.umc.sistemaonganimal.domain.repository.TipoRepository;
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
class ResponsavelControllerIT {

    @LocalServerPort
    private int port;

    @Autowired
    private ResponsavelRepository responsavelRepository;

    @Autowired
    private TipoRepository tipoRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private Long responsavelIdCriado;

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
    void limparResponsavelCriado() {
        if (responsavelIdCriado != null) {
            jdbcTemplate.update("DELETE FROM responsavel WHERE id = ?", responsavelIdCriado);
            responsavelIdCriado = null;
        }
    }

    private ResponsavelRequestDTO montarResponsavelValido() {
        return ResponsavelRequestDTO.builder()
                .nome("Responsável via API")
                .documento(DocumentoDTO.builder().cpf("52998224725").build())
                .contato(ContatoDTO.builder()
                        .telefonePrincipal("11999990003")
                        .email("responsavel.api@example.com")
                        .build())
                .endereco(EnderecoDTO.builder()
                        .logradouro("Rua do Resgate")
                        .bairro("Bairro API")
                        .cidade("Cidade API")
                        .estado("SP")
                        .cep("01001000")
                        .numero("100")
                        .build())
                .tipoId(buscarIdTipo(TipoResponsavel.ONG))
                .build();
    }

    private Long buscarIdTipo(TipoResponsavel nome) {
        return tipoRepository.findAll().stream()
                .filter(t -> t.getNome() == nome)
                .map(Tipo::getId)
                .findFirst()
                .orElseThrow();
    }

    @Test
    void adicionar_comPayloadValido_deveRetornarCreatedComResponsavelPersistido() {
        responsavelIdCriado = given()
                .contentType(ContentType.JSON)
                .body(montarResponsavelValido())
                .when()
                .post("/responsaveis")
                .then()
                .statusCode(201)
                .body("id", notNullValue())
                .body("nome", equalTo("Responsável via API"))
                .body("documento.cpf", equalTo("52998224725"))
                .extract().jsonPath().getLong("id");
    }

    @Test
    void buscar_comIdExistente_deveRetornarResponsavel() {
        responsavelIdCriado = criarResponsavelViaApi();

        given()
                .when()
                .get("/responsaveis/{id}", responsavelIdCriado)
                .then()
                .statusCode(200)
                .body("id", equalTo(responsavelIdCriado.intValue()))
                .body("nome", equalTo("Responsável via API"));
    }

    @Test
    void listar_deveConterResponsavelRecemCriado() {
        responsavelIdCriado = criarResponsavelViaApi();

        given()
                .when()
                .get("/responsaveis")
                .then()
                .statusCode(200)
                .body("id", hasItem(responsavelIdCriado.intValue()));
    }

    @Test
    void atualizar_comPayloadValido_deveRefletirAlteracao() {
        responsavelIdCriado = criarResponsavelViaApi();

        ResponsavelRequestDTO payloadAtualizado = montarResponsavelValido();
        payloadAtualizado.setNome("Responsável via API - atualizado");

        given()
                .contentType(ContentType.JSON)
                .body(payloadAtualizado)
                .when()
                .put("/responsaveis/{id}", responsavelIdCriado)
                .then()
                .statusCode(200)
                .body("nome", equalTo("Responsável via API - atualizado"));
    }

    @Test
    void excluir_comIdExistente_deveRetornarNoContentEDeixarDeAparecerNaBusca() {
        responsavelIdCriado = criarResponsavelViaApi();

        given()
                .when()
                .delete("/responsaveis/{id}", responsavelIdCriado)
                .then()
                .statusCode(204);

        given()
                .when()
                .get("/responsaveis/{id}", responsavelIdCriado)
                .then()
                .statusCode(404);
    }

    @Test
    void buscar_comIdInexistente_deveRetornarNotFound() {
        long idInexistente = responsavelRepository.findAll().stream()
                .mapToLong(r -> r.getId())
                .max()
                .orElse(0L) + 1;

        given()
                .when()
                .get("/responsaveis/{id}", idInexistente)
                .then()
                .statusCode(404)
                .body("title", equalTo("Entidade não encontrada"));
    }

    @Test
    void adicionar_comCpfECnpjPreenchidos_deveRetornarBadRequest() {
        ResponsavelRequestDTO payloadInvalido = montarResponsavelValido();
        payloadInvalido.setCnpj("11444777000161");

        given()
                .contentType(ContentType.JSON)
                .body(payloadInvalido)
                .when()
                .post("/responsaveis")
                .then()
                .statusCode(400)
                .body("title", equalTo("Violação de regra de negócio"));
    }

    @Test
    void adicionar_comCpfInvalido_deveRetornarBadRequest() {
        ResponsavelRequestDTO payloadInvalido = montarResponsavelValido();
        payloadInvalido.setDocumento(DocumentoDTO.builder().cpf("11111111111").build());

        given()
                .contentType(ContentType.JSON)
                .body(payloadInvalido)
                .when()
                .post("/responsaveis")
                .then()
                .statusCode(400)
                .body("detalhes['documento.cpf']", notNullValue());
    }

    @Test
    void adicionar_semNome_deveRetornarBadRequestComDetalheDoCampo() {
        ResponsavelRequestDTO payloadInvalido = montarResponsavelValido();
        payloadInvalido.setNome(null);

        given()
                .contentType(ContentType.JSON)
                .body(payloadInvalido)
                .when()
                .post("/responsaveis")
                .then()
                .statusCode(400)
                .body("detalhes.nome", notNullValue());
    }

    @Test
    void excluir_comResponsavelVinculadoAVoluntario_deveRetornarConflict() {
        Long responsavelId = criarResponsavelViaApi();
        responsavelIdCriado = responsavelId;

        VoluntarioRequestDTO voluntarioDTO = VoluntarioRequestDTO.builder()
                .nome("Voluntário vinculado")
                .contato(ContatoDTO.builder()
                        .telefonePrincipal("11999990005")
                        .build())
                .frequencia(Frequencia.MENSAL)
                .endereco(EnderecoDTO.builder()
                        .logradouro("Rua do Voluntariado")
                        .bairro("Bairro API")
                        .cidade("Cidade API")
                        .estado("SP")
                        .cep("01001000")
                        .numero("100")
                        .build())
                .responsavelId(responsavelId)
                .build();

        Long voluntarioId = given()
                .contentType(ContentType.JSON)
                .body(voluntarioDTO)
                .when()
                .post("/voluntarios")
                .then()
                .statusCode(201)
                .extract().jsonPath().getLong("id");

        try {
            given()
                    .when()
                    .delete("/responsaveis/{id}", responsavelId)
                    .then()
                    .statusCode(409)
                    .body("title", equalTo("Entidade em uso"));
        } finally {
            jdbcTemplate.update("DELETE FROM voluntario WHERE id = ?", voluntarioId);
        }
    }

    private Long criarResponsavelViaApi() {
        return given()
                .contentType(ContentType.JSON)
                .body(montarResponsavelValido())
                .when()
                .post("/responsaveis")
                .then()
                .statusCode(201)
                .extract().jsonPath().getLong("id");
    }
}
