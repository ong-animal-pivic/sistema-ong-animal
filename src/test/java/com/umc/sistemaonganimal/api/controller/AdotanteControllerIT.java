package com.umc.sistemaonganimal.api.controller;

import com.umc.sistemaonganimal.api.dto.embeddables.ContatoDTO;
import com.umc.sistemaonganimal.api.dto.embeddables.DadosDemograficosDTO;
import com.umc.sistemaonganimal.api.dto.embeddables.DocumentoDTO;
import com.umc.sistemaonganimal.api.dto.embeddables.EnderecoDTO;
import com.umc.sistemaonganimal.api.dto.request.AdotanteRequestDTO;
import com.umc.sistemaonganimal.domain.model.enums.general.Escolaridade;
import com.umc.sistemaonganimal.domain.model.enums.general.EstadoCivil;
import com.umc.sistemaonganimal.domain.repository.AdotanteRepository;
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

import java.time.LocalDate;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.notNullValue;

// @SpringBootTest com webEnvironment = RANDOM_PORT sobe o servidor Tomcat embutido numa
// porta livre, permitindo fazer chamadas HTTP de verdade (diferente dos *ServiceIT, que usam
// o modo MOCK padrão e chamam os services diretamente em Java, sem HTTP).
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
// Mesmo profile/banco Postgres local já usado pelos demais testes de integração.
@ActiveProfiles("local")
class AdotanteControllerIT {

    @LocalServerPort
    private int port;

    // Usado só para obter o id "fora da faixa em uso" no teste de 404 (buscarPorId).
    @Autowired
    private AdotanteRepository adotanteRepository;

    // Usado no @AfterEach para apagar fisicamente o que cada teste cria via HTTP. Não dá
    // para usar @Transactional na classe: a requisição HTTP roda numa thread/transação
    // separada (a do servidor embutido), então o rollback automático do teste não alcança
    // o que foi gravado através da API. Também não dá para usar
    // adotanteRepository.deleteById aqui: a entidade tem @SQLRestriction("ativo = true"),
    // então depois que o teste de exclusão lógica desativa o registro (ativo = false),
    // deleteById deixa de "enxergá-lo" e não apaga nada — o email/CPF ficariam presos para
    // sempre. Um DELETE nativo via JdbcTemplate ignora esse filtro do Hibernate.
    @Autowired
    private JdbcTemplate jdbcTemplate;

    private Long adotanteIdCriado;

    @BeforeEach
    void configurarRestAssured() {
        RestAssured.port = port;
        // Loga request e response no terminal somente quando uma asserção do teste falhar,
        // mantendo o terminal limpo numa execução normal.
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();

        // O ObjectMapper padrão do REST Assured não conhece java.time (LocalDate viraria um
        // array [ano, mes, dia] no JSON, que o servidor não sabe desserializar). Registramos
        // o JavaTimeModule e desligamos a escrita de datas como timestamp — a mesma convenção
        // ISO-8601/UTC que o servidor agora declara explicitamente em application.properties
        // (spring.jackson.time-zone=UTC, spring.jackson.serialization.write-dates-as-timestamps=
        // false; ver CLAUDE.md, seção "Data e hora"). A duplicação aqui é intencional: o
        // RestAssured simula um cliente HTTP externo, fora do container Spring, então não herda
        // a auto-configuração do Jackson do servidor.
        // Importante: parte de RestAssured.config() (a config atual, já com o logConfig
        // ligado pela chamada acima) em vez de RestAssuredConfig.config() (que criaria uma
        // config nova em branco e apagaria o logConfig recém-configurado).
        ObjectMapper objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        RestAssured.config = RestAssured.config().objectMapperConfig(
                ObjectMapperConfig.objectMapperConfig()
                        .jackson2ObjectMapperFactory((type, s) -> objectMapper));
    }

    @AfterEach
    void limparAdotanteCriado() {
        if (adotanteIdCriado != null) {
            jdbcTemplate.update("DELETE FROM adotante WHERE id = ?", adotanteIdCriado);
            adotanteIdCriado = null;
        }
    }

    // Monta um payload válido de Adotante, com CPF e email exclusivos deste teste (evita
    // colidir com as constraints UNIQUE do banco e com o fixture de afterMigrate.sql).
    // O CPF usado é um número de teste publicamente conhecido que passa na validação de
    // dígito verificador do @CPF (não é o CPF de uma pessoa real).
    private AdotanteRequestDTO montarAdotanteValido() {
        return AdotanteRequestDTO.builder()
                .nome("Adotante via API")
                .dataNascimento(LocalDate.of(1992, 3, 10))
                .documento(DocumentoDTO.builder().cpf("52998224725").rg("529982247").orgaoRg("SSP").build())
                .dadosDemograficos(DadosDemograficosDTO.builder()
                        .profissao("Testador de API")
                        .rendaMensal(2500.0)
                        .estadoCivil(EstadoCivil.SOLTEIRO)
                        .escolaridade(Escolaridade.SUPERIOR_COMPLETO)
                        .build())
                .contato(ContatoDTO.builder()
                        .telefonePrincipal("11999990001")
                        .telefoneSecundario("11999990002")
                        .email("adotante.api@example.com")
                        .build())
                .endereco(EnderecoDTO.builder()
                        .logradouro("Rua da API")
                        .bairro("Bairro API")
                        .cidade("Cidade API")
                        .estado("SP")
                        .cep("01001000")
                        .numero("100")
                        .build())
                .build();
    }

    // HAPPY PATH: POST /adotantes com payload válido deve criar o registro e devolver
    // 201 Created com o corpo montado por AdotanteResponseDTO.fromEntity, incluindo o id
    // gerado pelo banco.
    @Test
    void adicionar_comPayloadValido_deveRetornarCreatedComAdotantePersistido() {
        adotanteIdCriado = given()
                .contentType(ContentType.JSON)
                .body(montarAdotanteValido())
                .when()
                .post("/adotantes")
                .then()
                .statusCode(201)
                .body("id", notNullValue())
                .body("nome", equalTo("Adotante via API"))
                .body("documento.cpf", equalTo("52998224725"))
                .extract().jsonPath().getLong("id");
    }

    // HAPPY PATH: GET /adotantes/{id} para um adotante recém-criado deve devolver 200 OK
    // com os dados persistidos.
    @Test
    void buscar_comIdExistente_deveRetornarAdotante() {
        adotanteIdCriado = criarAdotanteViaApi();

        given()
                .when()
                .get("/adotantes/{id}", adotanteIdCriado)
                .then()
                .statusCode(200)
                .body("id", equalTo(adotanteIdCriado.intValue()))
                .body("nome", equalTo("Adotante via API"));
    }

    // HAPPY PATH: GET /adotantes deve trazer o adotante recém-criado na listagem. A
    // verificação é por conter o id criado (invariante), não por tamanho fixo da lista,
    // já que o fixture pode ter outros registros.
    @Test
    void listar_deveConterAdotanteRecemCriado() {
        adotanteIdCriado = criarAdotanteViaApi();

        given()
                .when()
                .get("/adotantes")
                .then()
                .statusCode(200)
                .body("id", hasItem(adotanteIdCriado.intValue()));
    }

    // HAPPY PATH: PUT /adotantes/{id} deve atualizar os campos do adotante e devolver o
    // estado atualizado.
    @Test
    void atualizar_comPayloadValido_deveRefletirAlteracao() {
        adotanteIdCriado = criarAdotanteViaApi();

        AdotanteRequestDTO payloadAtualizado = montarAdotanteValido();
        payloadAtualizado.setNome("Adotante via API - atualizado");

        given()
                .contentType(ContentType.JSON)
                .body(payloadAtualizado)
                .when()
                .put("/adotantes/{id}", adotanteIdCriado)
                .then()
                .statusCode(200)
                .body("nome", equalTo("Adotante via API - atualizado"));
    }

    // HAPPY PATH: DELETE /adotantes/{id} deve responder 204 No Content e, a partir daí,
    // o GET para o mesmo id deve passar a responder 404 (exclusão lógica).
    @Test
    void excluir_comIdExistente_deveRetornarNoContentEDeixarDeAparecerNaBusca() {
        // Atribuído antes das asserções (e não depois) para que o @AfterEach sempre limpe
        // este registro, mesmo que uma asserção abaixo falhe — a exclusão é lógica, então o
        // registro continua existindo na tabela e prenderia o email/CPF únicos para os
        // testes seguintes se não fosse removido.
        adotanteIdCriado = criarAdotanteViaApi();

        given()
                .when()
                .delete("/adotantes/{id}", adotanteIdCriado)
                .then()
                .statusCode(204);

        given()
                .when()
                .get("/adotantes/{id}", adotanteIdCriado)
                .then()
                .statusCode(404);
    }

    // UNHAPPY PATH: GET /adotantes/{id} com um id fora da faixa em uso (maior id atual + 1,
    // obtido dinamicamente em vez de um número mágico) deve responder 404 Not Found com
    // um ProblemDetail (RFC 7807).
    @Test
    void buscar_comIdInexistente_deveRetornarNotFound() {
        long idInexistente = adotanteRepository.findAll().stream()
                .mapToLong(a -> a.getId())
                .max()
                .orElse(0L) + 1;

        given()
                .when()
                .get("/adotantes/{id}", idInexistente)
                .then()
                .statusCode(404)
                .body("title", equalTo("Entidade não encontrada"));
    }

    // UNHAPPY PATH: POST /adotantes com telefone principal igual ao secundário viola a
    // regra de negócio de AdotanteService.salvar (DomainException) e deve resultar em 400
    // Bad Request, sem chegar a persistir nada.
    @Test
    void adicionar_comTelefonesIguais_deveRetornarBadRequest() {
        AdotanteRequestDTO payloadInvalido = montarAdotanteValido();
        payloadInvalido.getContato().setTelefoneSecundario(payloadInvalido.getContato().getTelefonePrincipal());

        given()
                .contentType(ContentType.JSON)
                .body(payloadInvalido)
                .when()
                .post("/adotantes")
                .then()
                .statusCode(400)
                .body("title", equalTo("Violação de regra de negócio"));
    }

    // UNHAPPY PATH: POST /adotantes sem o campo obrigatório "nome" deve ser barrado pela
    // validação de bean (@NotBlank) antes de chegar ao service, resultando em 400 Bad
    // Request com o mapa "detalhes" indicando o campo inválido.
    @Test
    void adicionar_semNome_deveRetornarBadRequestComDetalheDoCampo() {
        AdotanteRequestDTO payloadInvalido = montarAdotanteValido();
        payloadInvalido.setNome(null);

        given()
                .contentType(ContentType.JSON)
                .body(payloadInvalido)
                .when()
                .post("/adotantes")
                .then()
                .statusCode(400)
                .body("detalhes.nome", notNullValue());
    }

    // UNHAPPY PATH: POST /adotantes sem CPF deve ser barrado pela validação de bean
    // (Groups.CpfObrigatorio, exigido pelo controller via @Validated) antes de chegar
    // ao service, resultando em 400 Bad Request com o campo inválido em "detalhes".
    // Guarda contra regressão: CPF precisa continuar obrigatório para Adotante mesmo
    // com DocumentoDTO.cpf agora sendo opcional por padrão (usado também por
    // Responsavel).
    @Test
    void adicionar_semCpf_deveRetornarBadRequestComDetalheDoCampo() {
        AdotanteRequestDTO payloadInvalido = montarAdotanteValido();
        payloadInvalido.getDocumento().setCpf(null);

        given()
                .contentType(ContentType.JSON)
                .body(payloadInvalido)
                .when()
                .post("/adotantes")
                .then()
                .statusCode(400)
                .body("detalhes['documento.cpf']", notNullValue());
    }

    private Long criarAdotanteViaApi() {
        return given()
                .contentType(ContentType.JSON)
                .body(montarAdotanteValido())
                .when()
                .post("/adotantes")
                .then()
                .statusCode(201)
                .extract().jsonPath().getLong("id");
    }
}
