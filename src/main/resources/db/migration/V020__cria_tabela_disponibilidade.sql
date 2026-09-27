CREATE TABLE IF NOT EXISTS disponibilidade (
    id SERIAL PRIMARY KEY,
    dia_semana VARCHAR(10) NOT NULL CHECK (dia_semana IN ('SEGUNDA', 'TERCA', 'QUARTA', 'QUINTA', 'SEXTA', 'SABADO', 'DOMINGO')),
    turno VARCHAR(10) NOT NULL CHECK (turno IN ('MANHA', 'TARDE', 'NOITE')),
    CONSTRAINT uk_disponibilidade_dia_turno UNIQUE (dia_semana, turno)
);
