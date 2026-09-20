package com.umc.sistemaonganimal.api.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.JsonTest;

/**
 * Trava a convenção de data/hora documentada no CLAUDE.md (seção "Data e hora"): o
 * {@link ObjectMapper} de produção (o mesmo autoconfigurado pelo Spring Boot para os
 * controllers, agora com {@code spring.jackson.time-zone=UTC} e
 * {@code spring.jackson.serialization.write-dates-as-timestamps=false} explícitos em
 * application.properties) serializa {@link LocalDate} como string ISO-8601
 * ({@code yyyy-MM-dd}), nunca como array [ano, mes, dia] nem como timestamp numérico.
 *
 * <p>Serve também de modelo para o teste equivalente do primeiro campo
 * {@code OffsetDateTime}/{@code Instant} que o projeto vier a ter.
 */
@JsonTest
class JacksonDataHoraConfigTest {

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void deveSerializarLocalDateComoStringIso() throws Exception {
        String json = objectMapper.writeValueAsString(LocalDate.of(2026, 9, 19));

        assertThat(json).isEqualTo("\"2026-09-19\"");
    }

    @Test
    void deveDesserializarStringIsoComoLocalDate() throws Exception {
        LocalDate data = objectMapper.readValue("\"2026-09-19\"", LocalDate.class);

        assertThat(data).isEqualTo(LocalDate.of(2026, 9, 19));
    }
}
