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
