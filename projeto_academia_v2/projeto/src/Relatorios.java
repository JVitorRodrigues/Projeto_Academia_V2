import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import java.sql.SQLException;

/**
 * Relatório Financeiro: dados da VIEW vw_resumo_por_plano (Supabase) servidos com cache no Redis.
 * A tela mostra de onde vieram os dados e quanto tempo levou, para evidenciar o ganho do cache.
 */
public class Relatorios extends JFrame {

    private DefaultTableModel modelo;
    private JLabel lblTotais, lblFonte;

    public Relatorios() {
        setTitle("Relatório Financeiro por Plano");
        setSize(780, 420);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        modelo = new DefaultTableModel(
            new String[]{"Plano", "Matrículas", "Ativas", "Canceladas", "Receita (R$)"}, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable tabela = new JTable(modelo);
        tabela.setRowHeight(24);

        JButton btnAtualizar = new JButton("🔄 Atualizar");
        JButton btnLimpar    = new JButton("🗑 Limpar cache Redis");
        btnAtualizar.addActionListener(e -> carregar());
        btnLimpar.addActionListener(e -> {
            boolean apagou = RelatorioService.invalidar();
            lblFonte.setForeground(Color.DARK_GRAY);
            lblFonte.setText(apagou ? "Cache apagado. A próxima consulta vai ao Supabase."
                                    : "Nada para apagar (cache vazio ou Redis indisponível).");
        });

        lblFonte = new JLabel(" ");
        lblFonte.setFont(new Font("Arial", Font.BOLD, 12));
        lblTotais = new JLabel(" ");
        lblTotais.setFont(new Font("Arial", Font.BOLD, 13));

        JPanel botoes = new JPanel(new FlowLayout(FlowLayout.LEFT));
        botoes.add(btnAtualizar);
        botoes.add(btnLimpar);
        JPanel topo = new JPanel(new BorderLayout());
        topo.add(botoes, BorderLayout.NORTH);
        JPanel faixa = new JPanel(new FlowLayout(FlowLayout.LEFT));
        faixa.add(lblFonte);
        topo.add(faixa, BorderLayout.SOUTH);

        JPanel rodape = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        rodape.add(lblTotais);

        setLayout(new BorderLayout(0, 6));
        add(topo, BorderLayout.NORTH);
        add(new JScrollPane(tabela), BorderLayout.CENTER);
        add(rodape, BorderLayout.SOUTH);
        carregar();
    }

    private void carregar() {
        modelo.setRowCount(0);
        try {
            RelatorioService.Resultado r = RelatorioService.buscar();
            double receita = 0; int total = 0;
            for (String[] l : r.linhas) {
                modelo.addRow(new Object[]{l[0], l[1], l[2], l[3], String.format("%.2f", Double.parseDouble(l[4]))});
                total += Integer.parseInt(l[1]);
                receita += Double.parseDouble(l[4]);
            }
            lblTotais.setText(String.format("Total: %d matrículas  |  Receita: R$ %.2f", total, receita));

            if (r.doCache) {
                lblFonte.setForeground(new Color(0, 128, 0));
                lblFonte.setText("⚡ REDIS (cache HIT) — " + r.milissegundos + " ms — expira em " + r.ttlRestante + " s");
            } else if (r.redisOk) {
                lblFonte.setForeground(new Color(200, 110, 0));
                lblFonte.setText("🗄 SUPABASE (cache MISS) — " + r.milissegundos + " ms — resultado salvo no Redis por "
                                 + RelatorioService.TTL_SEGUNDOS + " s");
            } else {
                lblFonte.setForeground(Color.RED);
                lblFonte.setText("🗄 SUPABASE — " + r.milissegundos + " ms — Redis indisponível (sem cache)");
            }
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Erro ao carregar relatório: " + ex.getMessage());
        }
    }
}
