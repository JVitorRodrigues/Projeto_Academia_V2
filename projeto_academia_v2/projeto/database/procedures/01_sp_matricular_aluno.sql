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
