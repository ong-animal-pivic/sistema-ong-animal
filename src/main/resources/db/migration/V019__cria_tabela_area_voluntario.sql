CREATE TABLE IF NOT EXISTS area_voluntario (
    area_id INT NOT NULL,
    voluntario_id INT NOT NULL,
    PRIMARY KEY (area_id, voluntario_id),
    CONSTRAINT fk_area_voluntario_area FOREIGN KEY (area_id) REFERENCES area(id),
    CONSTRAINT fk_area_voluntario_voluntario FOREIGN KEY (voluntario_id) REFERENCES voluntario(id)
);
