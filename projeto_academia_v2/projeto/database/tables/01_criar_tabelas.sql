-- Tabelas do Sistema de Gestão de Academia (PostgreSQL)
DROP TABLE IF EXISTS pagamentos  CASCADE;
DROP TABLE IF EXISTS matriculas  CASCADE;
DROP TABLE IF EXISTS alunos      CASCADE;
DROP TABLE IF EXISTS instrutores CASCADE;
DROP TABLE IF EXISTS planos      CASCADE;
DROP TABLE IF EXISTS usuarios    CASCADE;

CREATE TABLE alunos (
    id_aluno        SERIAL PRIMARY KEY,
    nome            VARCHAR(50)  NOT NULL,
    cpf             VARCHAR(11)  UNIQUE NOT NULL,
    data_nascimento DATE,
    telefone        VARCHAR(15),
    email           VARCHAR(50)  UNIQUE,
    data_cadastro   TIMESTAMP    DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE instrutores (
    id_instrutor  SERIAL PRIMARY KEY,
    nome          VARCHAR(50)  NOT NULL,
    cpf           VARCHAR(11)  UNIQUE NOT NULL,
    especialidade VARCHAR(100) NOT NULL
);

CREATE TABLE planos (
    id_plano      SERIAL PRIMARY KEY,
    nome          VARCHAR(50)   NOT NULL,
    valor         DECIMAL(10,2) NOT NULL,
    duracao_meses INT
);

CREATE TABLE matriculas (
    id_matricula     SERIAL PRIMARY KEY,
    id_aluno         INT NOT NULL,
    id_plano         INT NOT NULL,
    id_instrutor     INT,
    status           VARCHAR(20) DEFAULT 'ativa'
                     CHECK (status IN ('ativa','cancelada')),
    data_inicio      DATE DEFAULT CURRENT_DATE,
    data_fim         DATE,                    -- data_inicio + duração do plano
    valor_contratado DECIMAL(10,2) NOT NULL,  -- valor já com desconto
    FOREIGN KEY (id_aluno)     REFERENCES alunos(id_aluno)          ON DELETE CASCADE,
    FOREIGN KEY (id_plano)     REFERENCES planos(id_plano),
    FOREIGN KEY (id_instrutor) REFERENCES instrutores(id_instrutor) ON DELETE SET NULL
);

CREATE TABLE pagamentos (
    id_pagamento    SERIAL PRIMARY KEY,
    id_matricula    INT NOT NULL REFERENCES matriculas(id_matricula) ON DELETE CASCADE,
    valor           DECIMAL(10,2) NOT NULL,
    forma_pagamento VARCHAR(20) NOT NULL
                    CHECK (forma_pagamento IN ('pix','cartao','dinheiro')),
    data_pagamento  TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE usuarios (
    id_usuario SERIAL PRIMARY KEY,
    username   VARCHAR(30) UNIQUE NOT NULL,
    senha      VARCHAR(50) NOT NULL
);
