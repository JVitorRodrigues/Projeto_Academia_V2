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
