import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import java.sql.*;


public class Matriculas extends JFrame {

    private JComboBox<ComboItem> comboAluno, comboPlano, comboInstrutor;
    private JComboBox<String> comboPagamento, comboFiltroStatus;
    private JLabel lblValorFinal;
    private JTable tabela;
    private DefaultTableModel modelo;
    private int idSelecionado = -1;

    public Matriculas() {
        setTitle("Gerenciar Matrículas");
        setSize(1050, 600);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        construirTela();
        carregarCombos();
        atualizarValorFinal();
        carregarTabela("todas");
    }

   
    static class ComboItem {
        int id;
        String descricao;
        ComboItem(int id, String descricao) { this.id = id; this.descricao = descricao; }
        public String toString() { return descricao; }
    }

    
    private void construirTela() {


        JPanel form = new JPanel(new GridLayout(5, 2, 8, 8));
        form.setBorder(BorderFactory.createTitledBorder("Dados da Matrícula"));
        form.setPreferredSize(new Dimension(0, 195));

        form.add(new JLabel("Aluno:"));
        comboAluno = new JComboBox<>();
        form.add(comboAluno);

        form.add(new JLabel("Plano:"));
        comboPlano = new JComboBox<>();
        form.add(comboPlano);

        form.add(new JLabel("Instrutor (opcional):"));
        comboInstrutor = new JComboBox<>();
        form.add(comboInstrutor);

        form.add(new JLabel("Forma de pagamento:"));
        comboPagamento = new JComboBox<>(new String[]{"pix", "cartao", "dinheiro"});
        form.add(comboPagamento);

        form.add(new JLabel("Valor final (função do banco):"));
        lblValorFinal = new JLabel("—");
        lblValorFinal.setFont(new Font("Arial", Font.BOLD, 14));
        form.add(lblValorFinal);

       
        JPanel botoes = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 6));
        JButton btnInserir   = new JButton("➕ Matricular");
        JButton btnCancelar  = new JButton("⛔ Cancelar matrícula");
        JButton btnExcluir   = new JButton("🗑 Excluir");
        JButton btnLimpar    = new JButton("🔄 Limpar");

        btnInserir.setBackground(new Color(76, 175, 80));    btnInserir.setForeground(Color.WHITE);
        btnCancelar.setBackground(new Color(255, 152, 0));   btnCancelar.setForeground(Color.WHITE);
        btnExcluir.setBackground(new Color(244, 67, 54));    btnExcluir.setForeground(Color.WHITE);

        for (JButton b : new JButton[]{btnInserir, btnCancelar, btnExcluir, btnLimpar}) {
            b.setFocusPainted(false);
            b.setCursor(new Cursor(Cursor.HAND_CURSOR));
            botoes.add(b);
        }

        
        JPanel filtroPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        filtroPanel.add(new JLabel("Filtrar por status:"));
        comboFiltroStatus = new JComboBox<>(new String[]{"todas", "ativa", "cancelada"});
        JButton btnFiltrar = new JButton("🔍 Filtrar");
        filtroPanel.add(comboFiltroStatus);
        filtroPanel.add(btnFiltrar);

        
        modelo = new DefaultTableModel(
            new String[]{"ID", "Aluno", "Plano", "Valor (R$)", "Instrutor", "Status", "Início", "Vencimento", "Situação", "Total pago"}, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };
        tabela = new JTable(modelo);
        tabela.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tabela.getColumnModel().getColumn(0).setMaxWidth(50);
        tabela.getColumnModel().getColumn(3).setMaxWidth(90);
        tabela.getColumnModel().getColumn(5).setMaxWidth(80);
        tabela.setRowHeight(24);

    
        JPanel topo = new JPanel(new BorderLayout());
        topo.add(form,   BorderLayout.CENTER);
        topo.add(botoes, BorderLayout.SOUTH);

        JPanel centro = new JPanel(new BorderLayout(0, 4));
        centro.add(filtroPanel,             BorderLayout.NORTH);
        centro.add(new JScrollPane(tabela), BorderLayout.CENTER);

        setLayout(new BorderLayout(0, 8));
        add(topo,   BorderLayout.NORTH);
        add(centro, BorderLayout.CENTER);

        
        btnInserir.addActionListener(e   -> inserir());
        btnCancelar.addActionListener(e  -> cancelar());
        btnExcluir.addActionListener(e   -> excluir());
        btnLimpar.addActionListener(e    -> limparCampos());
        btnFiltrar.addActionListener(e   -> carregarTabela((String) comboFiltroStatus.getSelectedItem()));
        comboAluno.addActionListener(e -> atualizarValorFinal());
        comboPlano.addActionListener(e -> atualizarValorFinal());

        tabela.getSelectionModel().addListSelectionListener(e -> {
            int linha = tabela.getSelectedRow();
            if (linha >= 0) {
                idSelecionado = (int) modelo.getValueAt(linha, 0);
            }
        });
    }

  
    private void carregarCombos() {
  
        try (Connection conn = ConexaoBD.conectar();
             PreparedStatement ps = conn.prepareStatement(
                "SELECT id_aluno, nome FROM alunos ORDER BY nome");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next())
                comboAluno.addItem(new ComboItem(rs.getInt(1), rs.getString(2)));
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Erro ao carregar alunos: " + ex.getMessage());
        }

     
        try (Connection conn = ConexaoBD.conectar();
             PreparedStatement ps = conn.prepareStatement(
                "SELECT id_plano, nome FROM planos ORDER BY nome");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next())
                comboPlano.addItem(new ComboItem(rs.getInt(1), rs.getString(2)));
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Erro ao carregar planos: " + ex.getMessage());
        }

       
        comboInstrutor.addItem(new ComboItem(-1, "— Nenhum —"));
        try (Connection conn = ConexaoBD.conectar();
             PreparedStatement ps = conn.prepareStatement(
                "SELECT id_instrutor, nome FROM instrutores ORDER BY nome");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next())
                comboInstrutor.addItem(new ComboItem(rs.getInt(1), rs.getString(2)));
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Erro ao carregar instrutores: " + ex.getMessage());
        }
    }

    private void carregarTabela(String filtroStatus) {
        modelo.setRowCount(0);

        // Consulta a VIEW vw_matriculas_detalhadas (os JOINs ficam no banco)
        String sql = "SELECT id_matricula, aluno, plano, valor_contratado, instrutor, status, "
                   + "       data_inicio, data_fim, situacao, total_pago "
                   + "FROM vw_matriculas_detalhadas "
                   + (filtroStatus.equals("todas") ? "" : "WHERE status = ? ")
                   + "ORDER BY id_matricula DESC";

        try (Connection conn = ConexaoBD.conectar();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            if (!filtroStatus.equals("todas")) ps.setString(1, filtroStatus);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                modelo.addRow(new Object[]{
                    rs.getInt("id_matricula"),
                    rs.getString("aluno"),
                    rs.getString("plano"),
                    String.format("%.2f", rs.getDouble("valor_contratado")),
                    rs.getString("instrutor") != null ? rs.getString("instrutor") : "—",
                    rs.getString("status"),
                    rs.getDate("data_inicio"),
                    rs.getDate("data_fim"),
                    rs.getString("situacao"),
                    String.format("%.2f", rs.getDouble("total_pago"))
                });
            }
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Erro ao carregar matrículas: " + ex.getMessage());
        }
    }

    // FUNCTION fn_valor_final_plano: mostra o valor (com desconto de fidelidade) antes de matricular
    private void atualizarValorFinal() {
        ComboItem aluno = (ComboItem) comboAluno.getSelectedItem();
        ComboItem plano = (ComboItem) comboPlano.getSelectedItem();
        if (aluno == null || plano == null) return;
        try (Connection conn = ConexaoBD.conectar();
             PreparedStatement ps = conn.prepareStatement("SELECT fn_valor_final_plano(?, ?)")) {
            ps.setInt(1, plano.id);
            ps.setInt(2, aluno.id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) lblValorFinal.setText(String.format("R$ %.2f", rs.getDouble(1)));
        } catch (SQLException ex) {
            lblValorFinal.setText("erro");
        }
    }

    // ── INSERT ───────────────────────────────────────────────────────────────
    private void inserir() {
        ComboItem aluno     = (ComboItem) comboAluno.getSelectedItem();
        ComboItem plano     = (ComboItem) comboPlano.getSelectedItem();
        ComboItem instrutor = (ComboItem) comboInstrutor.getSelectedItem();

        if (aluno == null || plano == null) {
            JOptionPane.showMessageDialog(this, "Selecione aluno e plano.");
            return;
        }

        // PROCEDURE sp_matricular_aluno: valida, calcula valor, cria matrícula e registra pagamento
        try (Connection conn = ConexaoBD.conectar();
             PreparedStatement ps = conn.prepareStatement("CALL sp_matricular_aluno(?, ?, ?, ?)")) {

            ps.setInt(1, aluno.id);
            ps.setInt(2, plano.id);
            if (instrutor != null && instrutor.id != -1)
                ps.setInt(3, instrutor.id);
            else
                ps.setNull(3, Types.INTEGER);
            ps.setString(4, (String) comboPagamento.getSelectedItem());
            ps.execute();

            JOptionPane.showMessageDialog(this, "Matrícula realizada e pagamento registrado!");
            limparCampos();
            carregarTabela("todas");
            RelatorioService.invalidar();   // REDIS: dados mudaram, descarta o cache do relatório
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Não foi possível matricular:\n" + ex.getMessage());
        }
    }

    // PROCEDURE sp_cancelar_matricula
    private void cancelar() {
        if (idSelecionado == -1) {
            JOptionPane.showMessageDialog(this, "Clique em uma matrícula na tabela para cancelar.");
            return;
        }
        try (Connection conn = ConexaoBD.conectar();
             PreparedStatement ps = conn.prepareStatement("CALL sp_cancelar_matricula(?)")) {

            ps.setInt(1, idSelecionado);
            ps.execute();

            JOptionPane.showMessageDialog(this, "Matrícula cancelada!");
            limparCampos();
            carregarTabela("todas");
            RelatorioService.invalidar();   // REDIS: dados mudaram, descarta o cache do relatório
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Não foi possível cancelar:\n" + ex.getMessage());
        }
    }

    private void excluir() {
        if (idSelecionado == -1) {
            JOptionPane.showMessageDialog(this, "Clique em uma matrícula para excluir.");
            return;
        }
        int confirm = JOptionPane.showConfirmDialog(this,
            "Excluir matrícula selecionada?", "Confirmar", JOptionPane.YES_NO_OPTION);
        if (confirm != JOptionPane.YES_OPTION) return;

        try (Connection conn = ConexaoBD.conectar();
             PreparedStatement ps = conn.prepareStatement(
                "DELETE FROM matriculas WHERE id_matricula=?")) {

            ps.setInt(1, idSelecionado);
            ps.executeUpdate();

            JOptionPane.showMessageDialog(this, "Matrícula excluída!");
            limparCampos();
            carregarTabela("todas");
            RelatorioService.invalidar();   // REDIS: dados mudaram, descarta o cache do relatório
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Erro ao excluir: " + ex.getMessage());
        }
    }

   
    private void limparCampos() {
        if (comboAluno.getItemCount()     > 0) comboAluno.setSelectedIndex(0);
        if (comboPlano.getItemCount()     > 0) comboPlano.setSelectedIndex(0);
        if (comboInstrutor.getItemCount() > 0) comboInstrutor.setSelectedIndex(0);
        comboPagamento.setSelectedIndex(0);
        idSelecionado = -1;
        tabela.clearSelection();
    }
}
