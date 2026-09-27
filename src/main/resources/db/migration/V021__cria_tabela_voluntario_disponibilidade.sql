CREATE TABLE IF NOT EXISTS voluntario_disponibilidade (
    id SERIAL PRIMARY KEY,
    voluntario_id INT NOT NULL,
    disponibilidade_id INT NOT NULL,
    observacao VARCHAR(255),
    CONSTRAINT uk_voluntario_disponibilidade UNIQUE (voluntario_id, disponibilidade_id),
    CONSTRAINT fk_voluntario_disponibilidade_voluntario FOREIGN KEY (voluntario_id) REFERENCES voluntario(id),
    CONSTRAINT fk_voluntario_disponibilidade_disponibilidade FOREIGN KEY (disponibilidade_id) REFERENCES disponibilidade(id)
);
