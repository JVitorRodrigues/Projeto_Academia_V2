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
