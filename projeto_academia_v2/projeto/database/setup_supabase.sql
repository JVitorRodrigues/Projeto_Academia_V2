-- ==============================================================
-- SETUP COMPLETO PARA O SUPABASE (cole no SQL Editor e clique em Run)
-- Reúne, na ordem certa: tabelas, functions, views, procedures e dados de exemplo.
-- ATENÇÃO: apaga e recria as tabelas do sistema (use só na criação inicial).
-- ==============================================================

-- >>>>>> database/tables/01_criar_tabelas.sql
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


-- >>>>>> database/functions/01_fn_valor_final_plano.sql
-- FUNCTION: calcula o valor final de um plano para um aluno.
-- Regra de negócio: aluno que já teve alguma matrícula (fidelidade) ganha 10% de desconto.
-- Usada em: tela Matrículas (pré-visualização do valor) e dentro da procedure sp_matricular_aluno.
CREATE OR REPLACE FUNCTION fn_valor_final_plano(p_id_plano INT, p_id_aluno INT)
RETURNS DECIMAL(10,2)
LANGUAGE plpgsql STABLE
AS $$
DECLARE
    v_valor    DECIMAL(10,2);
    v_desconto DECIMAL(4,2) := 0;
BEGIN
    SELECT valor INTO v_valor FROM planos WHERE id_plano = p_id_plano;
    IF NOT FOUND THEN
        RAISE EXCEPTION 'Plano % não encontrado.', p_id_plano;
    END IF;

    IF EXISTS (SELECT 1 FROM matriculas WHERE id_aluno = p_id_aluno) THEN
        v_desconto := 0.10;
    END IF;

    RETURN ROUND(v_valor * (1 - v_desconto), 2);
END;
$$;


-- >>>>>> database/functions/02_fn_situacao_aluno.sql
-- FUNCTION: retorna a situação atual de um aluno na academia.
-- Usada em: tela Alunos (coluna "Situação").
CREATE OR REPLACE FUNCTION fn_situacao_aluno(p_id_aluno INT)
RETURNS VARCHAR
LANGUAGE plpgsql STABLE
AS $$
BEGIN
    IF EXISTS (SELECT 1 FROM matriculas
               WHERE id_aluno = p_id_aluno AND status = 'ativa'
                 AND data_fim >= CURRENT_DATE) THEN
        RETURN 'Ativo';
    ELSIF EXISTS (SELECT 1 FROM matriculas
                  WHERE id_aluno = p_id_aluno AND status = 'ativa') THEN
        RETURN 'Vencido';
    ELSIF EXISTS (SELECT 1 FROM matriculas WHERE id_aluno = p_id_aluno) THEN
        RETURN 'Inativo';
    END IF;
    RETURN 'Sem matrícula';
END;
$$;


-- >>>>>> database/views/01_vw_matriculas_detalhadas.sql
-- VIEW: matrículas já "prontas" para exibição (aluno, plano, instrutor, situação e total pago).
-- Tabelas: matriculas, alunos, planos, instrutores, pagamentos.
-- Usada em: tela Matrículas (listagem e filtro).
CREATE OR REPLACE VIEW vw_matriculas_detalhadas AS
SELECT m.id_matricula,
       a.nome             AS aluno,
       p.nome             AS plano,
       m.valor_contratado,
       i.nome             AS instrutor,
       m.status,
       m.data_inicio,
       m.data_fim,
       CASE WHEN m.status = 'cancelada'       THEN 'Cancelada'
            WHEN m.data_fim < CURRENT_DATE    THEN 'Vencida'
            ELSE 'Em dia' END                 AS situacao,
       COALESCE((SELECT SUM(pg.valor) FROM pagamentos pg
                 WHERE pg.id_matricula = m.id_matricula), 0) AS total_pago
FROM matriculas m
INNER JOIN alunos      a ON a.id_aluno  = m.id_aluno
INNER JOIN planos      p ON p.id_plano  = m.id_plano
LEFT  JOIN instrutores i ON i.id_instrutor = m.id_instrutor;


-- >>>>>> database/views/02_vw_resumo_por_plano.sql
-- VIEW: dashboard consolidado por plano (quantidade de matrículas e receita).
-- Tabelas: planos, matriculas, pagamentos.
-- Usada em: tela Relatório Financeiro.
CREATE OR REPLACE VIEW vw_resumo_por_plano AS
SELECT p.id_plano,
       p.nome                                                   AS plano,
       COUNT(DISTINCT m.id_matricula)                           AS total_matriculas,
       COUNT(DISTINCT m.id_matricula) FILTER (WHERE m.status = 'ativa')     AS ativas,
       COUNT(DISTINCT m.id_matricula) FILTER (WHERE m.status = 'cancelada') AS canceladas,
       COALESCE(SUM(pg.valor), 0)                               AS receita_total
FROM planos p
LEFT JOIN matriculas m  ON m.id_plano = p.id_plano
LEFT JOIN pagamentos pg ON pg.id_matricula = m.id_matricula
GROUP BY p.id_plano, p.nome;


-- >>>>>> database/procedures/01_sp_matricular_aluno.sql
-- PROCEDURE: processo completo de matrícula (várias operações em uma transação):
--   1) valida aluno, plano e forma de pagamento
--   2) impede matrícula duplicada (aluno já com matrícula vigente)
--   3) encerra matrículas antigas vencidas
--   4) calcula o valor com a function fn_valor_final_plano
--   5) insere a matrícula e registra o pagamento
-- Usada em: tela Matrículas (botão "Matricular").
CREATE OR REPLACE PROCEDURE sp_matricular_aluno(
    p_id_aluno        INT,
    p_id_plano        INT,
    p_id_instrutor    INT,
    p_forma_pagamento VARCHAR)
LANGUAGE plpgsql
AS $$
DECLARE
    v_duracao      INT;
    v_valor        DECIMAL(10,2);
    v_id_matricula INT;
BEGIN
    IF NOT EXISTS (SELECT 1 FROM alunos WHERE id_aluno = p_id_aluno) THEN
        RAISE EXCEPTION 'Aluno não encontrado.';
    END IF;

    SELECT COALESCE(duracao_meses, 1) INTO v_duracao FROM planos WHERE id_plano = p_id_plano;
    IF NOT FOUND THEN
        RAISE EXCEPTION 'Plano não encontrado.';
    END IF;

    IF p_forma_pagamento NOT IN ('pix','cartao','dinheiro') THEN
        RAISE EXCEPTION 'Forma de pagamento inválida: %.', p_forma_pagamento;
    END IF;

    IF EXISTS (SELECT 1 FROM matriculas
               WHERE id_aluno = p_id_aluno AND status = 'ativa' AND data_fim >= CURRENT_DATE) THEN
        RAISE EXCEPTION 'O aluno já possui uma matrícula ativa e em dia.';
    END IF;

    UPDATE matriculas SET status = 'cancelada'
    WHERE id_aluno = p_id_aluno AND status = 'ativa' AND data_fim < CURRENT_DATE;

    v_valor := fn_valor_final_plano(p_id_plano, p_id_aluno);

    INSERT INTO matriculas (id_aluno, id_plano, id_instrutor, status, data_inicio, data_fim, valor_contratado)
    VALUES (p_id_aluno, p_id_plano, p_id_instrutor, 'ativa', CURRENT_DATE,
            (CURRENT_DATE + make_interval(months => v_duracao))::date, v_valor)
    RETURNING id_matricula INTO v_id_matricula;

    INSERT INTO pagamentos (id_matricula, valor, forma_pagamento)
    VALUES (v_id_matricula, v_valor, p_forma_pagamento);
END;
$$;


-- >>>>>> database/procedures/02_sp_cancelar_matricula.sql
-- PROCEDURE: cancela uma matrícula ativa (com validação).
-- Usada em: tela Matrículas (botão "Cancelar matrícula").
CREATE OR REPLACE PROCEDURE sp_cancelar_matricula(p_id_matricula INT)
LANGUAGE plpgsql
AS $$
DECLARE
    v_status VARCHAR(20);
BEGIN
    SELECT status INTO v_status FROM matriculas WHERE id_matricula = p_id_matricula;
    IF NOT FOUND THEN
        RAISE EXCEPTION 'Matrícula % não encontrada.', p_id_matricula;
    ELSIF v_status = 'cancelada' THEN
        RAISE EXCEPTION 'A matrícula já está cancelada.';
    END IF;
    UPDATE matriculas SET status = 'cancelada' WHERE id_matricula = p_id_matricula;
END;
$$;


-- >>>>>> database/inserts/01_dados_exemplo.sql
-- Dados de exemplo (datas relativas a CURRENT_DATE para a demo funcionar em qualquer dia)
INSERT INTO usuarios (username, senha) VALUES ('admin', 'admin123');

INSERT INTO planos (nome, valor, duracao_meses) VALUES
    ('Mensal Básico',  99.90,  1),
    ('Trimestral',    269.90,  3),
    ('Semestral',     479.90,  6),
    ('Anual Premium', 899.90, 12);

INSERT INTO instrutores (nome, cpf, especialidade) VALUES
    ('Carlos Silva',   '11122233344', 'Musculação'),
    ('Ana Oliveira',   '55566677788', 'Yoga e Alongamento'),
    ('Bruno Ferreira', '99900011122', 'Crossfit');

INSERT INTO alunos (nome, cpf, data_nascimento, telefone, email) VALUES
    ('Lucas Mendes',   '12345678901', '1995-03-15', '86991110001', 'lucas@email.com'),
    ('Fernanda Costa', '23456789012', '1999-07-22', '86991110002', 'fernanda@email.com'),
    ('Rafael Souza',   '34567890123', '1988-11-05', '86991110003', 'rafael@email.com'),
    ('Juliana Lima',   '45678901234', '2001-01-30', '86991110004', 'juliana@email.com'),
    ('Pedro Alves',    '56789012345', '1993-09-18', '86991110005', 'pedro@email.com'),
    ('Marina Rocha',   '67890123456', '1997-05-02', '86991110006', 'marina@email.com');

-- Matrículas: Lucas (vigente), Fernanda (vigente), Rafael (vencida), Juliana (vigente), Pedro (cancelada); Marina sem matrícula
INSERT INTO matriculas (id_aluno, id_plano, id_instrutor, status, data_inicio, data_fim, valor_contratado) VALUES
    (1, 1, 1,    'ativa',     CURRENT_DATE - 10,  CURRENT_DATE + 20,  99.90),
    (2, 2, 2,    'ativa',     CURRENT_DATE - 30,  CURRENT_DATE + 60,  269.90),
    (3, 3, 1,    'ativa',     CURRENT_DATE - 240, CURRENT_DATE - 60,  479.90),
    (4, 4, 3,    'ativa',     CURRENT_DATE - 90,  CURRENT_DATE + 275, 899.90),
    (5, 1, NULL, 'cancelada', CURRENT_DATE - 50,  CURRENT_DATE - 20,  99.90);

INSERT INTO pagamentos (id_matricula, valor, forma_pagamento) VALUES
    (1,  99.90, 'pix'),
    (2, 269.90, 'cartao'),
    (3, 479.90, 'cartao'),
    (4, 899.90, 'pix'),
    (5,  99.90, 'dinheiro');

