TRUNCATE
    animal,
    voluntario,
    adotante,
    especie,
    raca,
    tipo,
    responsavel
RESTART IDENTITY CASCADE;

-- 1. Espécies
INSERT INTO especie (nome) VALUES
                               ('CACHORRO'), -- ID 1
                               ('GATO');     -- ID 2

-- 1.1 Tipos de Responsável
INSERT INTO tipo (nome) VALUES
                            ('ABRIGO'),                 -- ID 1
                            ('LAR_TEMPORARIO'),          -- ID 2
                            ('ONG'),                     -- ID 3
                            ('PROTETOR_INDEPENDENTE');   -- ID 4

-- 2. Raças
INSERT INTO raca (nome, especie_id) VALUES
                                        ('SRD (Vira-lata)', 1),     -- ID 1
                                        ('Labrador', 1),            -- ID 2
                                        ('Poodle', 1),              -- ID 3
                                        ('Bulldog Francês', 1),     -- ID 4
                                        ('SRD (Vira-lata)', 2),     -- ID 5
                                        ('Siamês', 2),              -- ID 6
                                        ('Persa', 2),               -- ID 7
                                        ('Maine Coon', 2);          -- ID 8

-- 3. Adotantes (CPFs com dígito verificador válido, e-mails únicos)
INSERT INTO adotante (
    CPF, RG, orgao_RG, nome, data_nasc, profissao, renda_mensal,
    estado_civil, escolaridade, telefone1, telefone2, email, instagram,
    logradouro, bairro, cidade, estado, CEP, num_endereco, complemento
) VALUES
    ('52998224725', '123456789', 'SSP-SP', 'João da Silva', '1990-05-15',
     'Desenvolvedor', 5000.00, 'SOLTEIRO', 'SUPERIOR_COMPLETO',
     '11999999999', '1133334444', 'joao.silva@email.com', '@joaosilva',
     'Rua das Flores', 'Centro', 'São Paulo', 'SP', '01001000', '100', 'Apto 12'),

    ('11144477735', '234567891', 'SSP-SP', 'Maria Oliveira', '1985-08-22',
     'Advogada', 8500.00, 'CASADO', 'POS_GRADUACAO',
     '11988887777', NULL, 'maria.oliveira@email.com', NULL,
     'Avenida Paulista', 'Bela Vista', 'São Paulo', 'SP', '01310000', '900', NULL),

    ('12345678909', '345678912', 'SSP-RJ', 'Carlos Souza', '1978-01-30',
     'Comerciante', 4200.00, 'DIVORCIADO', 'MEDIO_COMPLETO',
     '21977776666', '2133338888', 'carlos.souza@email.com', '@carlossouza',
     'Rua do Ouvidor', 'Centro', 'Rio de Janeiro', 'RJ', '20040030', '55', NULL),

    ('98765432100', '456789123', 'SSP-MG', 'Fernanda Lima', '1995-11-02',
     'Professora', 3800.00, 'SOLTEIRO', 'MESTRADO',
     '31966665555', NULL, 'fernanda.lima@email.com', NULL,
     'Rua da Bahia', 'Funcionários', 'Belo Horizonte', 'MG', '30130000', '210', 'Casa 2'),

    ('11223344517', NULL, NULL, 'Roberto Alves', '1982-04-18',
     'Motorista', 2500.00, 'CASADO', 'FUNDAMENTAL_COMPLETO',
     '11955554444', '1122223333', 'roberto.alves@email.com', NULL,
     'Rua Vergueiro', 'Liberdade', 'São Paulo', 'SP', '01504000', '1500', NULL),

    ('99887766593', '567891234', 'SSP-SP', 'Patrícia Santos', '1999-07-09',
     'Designer', 4500.00, 'SOLTEIRO', 'SUPERIOR_COMPLETO',
     '11944443333', NULL, 'patricia.santos@email.com', '@patsantos',
     'Rua Augusta', 'Consolação', 'São Paulo', 'SP', '01305000', '780', 'Sala 3'),

    ('13579135759', '678912345', 'SSP-PR', 'Lucas Pereira', '1988-02-25',
     'Engenheiro', 9000.00, 'CASADO', 'SUPERIOR_COMPLETO',
     '41933332222', '4133331111', 'lucas.pereira@email.com', NULL,
     'Rua XV de Novembro', 'Centro', 'Curitiba', 'PR', '80020310', '320', NULL),

    ('24681357928', NULL, NULL, 'Juliana Costa', '2000-12-12',
     'Estudante', 0.00, 'SOLTEIRO', 'SUPERIOR_CURSANDO',
     '11922221111', NULL, 'juliana.costa@email.com', '@julicosta',
     'Rua Oscar Freire', 'Jardins', 'São Paulo', 'SP', '01426000', '640', NULL);

-- 4. Responsáveis (origem dos animais resgatados) — alternando CPF e CNPJ, e-mails únicos
INSERT INTO responsavel (nome, cpf, telefone1, telefone2, email, instagram, logradouro, bairro, cidade, estado, cep, num_endereco, complemento, tipo_id)
VALUES
    ('Maria Protetora', '19283746546', '11988887777', NULL, 'maria.protetora@email.com', '@mariaprotetora', 'Rua dos Protetores', 'Vila Nova', 'São Paulo', 'SP', '02002000', '50', NULL, 4),
    ('Lar Temporário da Ana', '56473829164', '11977776666', '1133335555', 'ana.lartemporario@email.com', NULL, 'Rua das Acácias', 'Vila Mariana', 'São Paulo', 'SP', '04101000', '85', 'Fundos', 2),
    ('José Protetor', '10293847541', '21966665555', NULL, 'jose.protetor@email.com', NULL, 'Rua das Palmeiras', 'Tijuca', 'Rio de Janeiro', 'RJ', '20520000', '12', NULL, 4),
    ('Lar Temporário do Pedro', '40506070859', '31955554444', NULL, 'pedro.lartemporario@email.com', '@lardopedro', 'Rua dos Ipês', 'Savassi', 'Belo Horizonte', 'MG', '30140000', '410', NULL, 2);

INSERT INTO responsavel (nome, cnpj, telefone1, telefone2, email, instagram, logradouro, bairro, cidade, estado, cep, num_endereco, complemento, tipo_id)
VALUES
    ('ONG Patinhas Carentes', '11444777000161', '1133334444', '1133334445', 'contato@patinhascarentes.org', '@patinhascarentes', 'Avenida dos Animais', 'Jardim Esperança', 'São Paulo', 'SP', '03003000', '200', NULL, 3),
    ('Abrigo Novo Lar', '11222333000181', '1144445555', NULL, 'contato@abrigonovolar.org', NULL, 'Estrada do Abrigo', 'Zona Rural', 'Cotia', 'SP', '06700000', '10', 'Km 5', 1),
    ('ONG Amigos dos Bichos', '99887766000105', '2133332222', NULL, 'contato@amigosdosbichos.org', '@amigosdosbichos', 'Rua dos Amigos', 'Copacabana', 'Rio de Janeiro', 'RJ', '22020000', '75', NULL, 3),
    ('Abrigo Esperança Animal', '55443322000105', '4133221100', '4133221101', 'contato@esperancaanimal.org', NULL, 'Rua da Esperança', 'Água Verde', 'Curitiba', 'PR', '80240000', '300', NULL, 1);

-- 5. Animais — cobrindo os 5 status, os dois sexos, os três portes,
-- castrado/não castrado e vínculos com todos os responsáveis e parte dos adotantes.

-- DISPONIVEL
INSERT INTO animal (nome, idade_meses, sexo, porte, status, castrado, dt_resgate, raca_id, cor_pelagem, observacao, responsavel_id)
VALUES
    ('Paçoca', 24, 'MACHO', 'MEDIO', 'DISPONIVEL', TRUE, '2023-11-10', 1, 'Caramelo', 'Muito dócil e brincalhão', 1),
    ('Bela', 8, 'FEMEA', 'PEQUENO', 'DISPONIVEL', TRUE, '2024-02-10', 3, 'Branco', 'Resgatada em bom estado de saúde', 6),
    ('Rex', 36, 'MACHO', 'MEDIO', 'DISPONIVEL', FALSE, '2024-03-01', 4, 'Preto e branco', 'Ainda não castrado, aguardando cirurgia', 2),
    ('Simba', 18, 'MACHO', 'GRANDE', 'DISPONIVEL', TRUE, '2024-04-05', 8, NULL, 'Bastante sociável com outros gatos', 7),
    ('Bob', 60, 'MACHO', 'GRANDE', 'DISPONIVEL', TRUE, '2024-01-20', 2, 'Dourado', 'Idoso, mas ativo', 8),
    ('Mel', 12, 'FEMEA', 'MEDIO', 'DISPONIVEL', TRUE, '2024-06-18', 7, NULL, 'Muito carinhosa', 6),
    ('Pandora', 6, 'FEMEA', 'PEQUENO', 'DISPONIVEL', FALSE, '2024-07-22', 5, NULL, 'Filhote, ainda em fase de socialização', 5),
    ('Bidu', 4, 'MACHO', 'PEQUENO', 'DISPONIVEL', TRUE, '2024-08-15', 1, 'Marrom', 'Filhote resgatado com a ninhada', 8);

-- EM_TRATAMENTO
INSERT INTO animal (nome, idade_meses, sexo, porte, status, castrado, dt_resgate, raca_id, cor_olhos, observacao, responsavel_id)
VALUES
    ('Luna', 48, 'FEMEA', 'PEQUENO', 'EM_TRATAMENTO', TRUE, '2024-01-05', 6, 'Azul', 'Realizando tratamento dermatológico', 3),
    ('Nina', 30, 'FEMEA', 'PEQUENO', 'EM_TRATAMENTO', FALSE, '2024-05-12', 6, 'Verde', 'Em recuperação de fratura na pata', 4);

INSERT INTO animal (nome, idade_meses, sexo, porte, status, castrado, dt_resgate, raca_id, observacao, responsavel_id)
VALUES
    ('Fiona', 20, 'FEMEA', 'GRANDE', 'EM_TRATAMENTO', TRUE, '2024-08-01', 8, 'Tratamento de vermifugação em andamento', 7);

INSERT INTO animal (nome, idade_meses, sexo, porte, status, castrado, dt_resgate, raca_id, observacao, responsavel_id)
VALUES
    ('Garfield', 72, 'MACHO', 'MEDIO', 'QUARENTENA', FALSE, '2025-09-19', 7, 'Aguardando exames iniciais', 1),
    ('Toby', 10, 'MACHO', 'PEQUENO', 'QUARENTENA', FALSE, '2025-09-16', 3, 'Recém-chegado, ainda em avaliação veterinária', 2);

-- OBITO
INSERT INTO animal (nome, idade_meses, sexo, porte, status, castrado, dt_resgate, dt_saida, raca_id, observacao, responsavel_id)
VALUES
    ('Velhinho', 180, 'MACHO', 'PEQUENO', 'OBITO', TRUE, '2023-01-01', '2023-01-15', 1, 'Faleceu de causas naturais devido à idade avançada', 1),
    ('Zeca', 144, 'MACHO', 'GRANDE', 'OBITO', TRUE, '2022-12-01', '2023-02-14', 4, 'Faleceu em decorrência de complicações de saúde pré-existentes', 4);

-- ADOTADO (vinculados a adotantes existentes)
INSERT INTO animal (nome, idade_meses, sexo, porte, status, castrado, dt_resgate, dt_saida, raca_id, adotante_id, responsavel_id)
VALUES
    ('Thor', 60, 'MACHO', 'GRANDE', 'ADOTADO', TRUE, '2023-06-20', '2023-12-01', 2, 1, 3),
    ('Mimi', 15, 'FEMEA', 'PEQUENO', 'ADOTADO', TRUE, '2023-08-15', '2023-10-20', 5, 2, 5),
    ('Amora', 22, 'FEMEA', 'MEDIO', 'ADOTADO', TRUE, '2023-05-05', '2023-09-01', 1, 3, 1),
    ('Duque', 40, 'MACHO', 'GRANDE', 'ADOTADO', TRUE, '2023-03-10', '2023-07-19', 2, 4, 6),
    ('Chiquinha', 9, 'FEMEA', 'PEQUENO', 'ADOTADO', TRUE, '2023-09-01', '2023-11-11', 6, 5, 1);

-- 6. Voluntários — alternando CPF preenchido/ausente e e-mail preenchido/ausente,
-- cobrindo os 5 valores de Frequencia e vinculados a responsáveis variados.
INSERT INTO voluntario (nome, cpf, idade, profissao, telefone1, telefone2, email, instagram, frequencia, logradouro, bairro, cidade, estado, cep, num_endereco, complemento, responsavel_id)
VALUES
    ('Beatriz Andrade', '52998224725', 29, 'Veterinária', '11991112222', NULL, 'beatriz.andrade@email.com', '@biaandrade', 'SEMANAL', 'Rua dos Voluntários', 'Vila Nova', 'São Paulo', 'SP', '02002100', '75', NULL, 1),
    ('Rafael Nunes', '11144477735', NULL, NULL, '11992223333', '1133445566', NULL, NULL, 'DIARIA', 'Rua das Acácias', 'Vila Mariana', 'São Paulo', 'SP', '04101100', '90', 'Fundos', 2),
    ('Camila Rocha', NULL, 34, 'Estudante de Medicina Veterinária', '21993334444', NULL, 'camila.rocha@email.com', '@camilarocha', 'QUINZENAL', 'Rua das Palmeiras', 'Tijuca', 'Rio de Janeiro', 'RJ', '20520100', '18', NULL, 3),
    ('Diego Martins', NULL, NULL, 'Autônomo', '31994445555', NULL, NULL, NULL, 'MENSAL', 'Rua dos Ipês', 'Savassi', 'Belo Horizonte', 'MG', '30140100', '415', NULL, 4),
    ('Larissa Fontes', '98765432100', 22, 'Estudante', '11995556666', NULL, 'larissa.fontes@email.com', NULL, 'EVENTUAL', 'Avenida dos Animais', 'Jardim Esperança', 'São Paulo', 'SP', '03003100', '210', NULL, 5),
    ('Thiago Barros', NULL, 41, 'Professor', '11996667777', '1133339999', 'thiago.barros@email.com', '@thiagobarros', 'SEMANAL', 'Estrada do Abrigo', 'Zona Rural', 'Cotia', 'SP', '06700100', '15', 'Km 5', 6);
