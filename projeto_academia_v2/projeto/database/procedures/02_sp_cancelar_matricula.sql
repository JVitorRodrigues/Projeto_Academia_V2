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
