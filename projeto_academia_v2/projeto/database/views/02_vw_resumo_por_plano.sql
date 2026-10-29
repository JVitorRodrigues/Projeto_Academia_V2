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
