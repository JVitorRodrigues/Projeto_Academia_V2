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
