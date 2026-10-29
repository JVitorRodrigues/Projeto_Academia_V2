import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import java.sql.*;

public class Planos extends JFrame {

    private JTextField campoNome, campoValor, campoDuracao;
    private JTable tabela;
    private DefaultTableModel modelo;
    private int idSelecionado = -1;

    public Planos() {
        setTitle("Gerenciar Planos");
        setSize(550, 420);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        construirTela();
        carregarTabela();
    }

    private void construirTela() {

        JPanel form = new JPanel(new GridLayout(3, 2, 8, 8));
        form.setBorder(BorderFactory.createTitledBorder("Dados do Plano"));
        form.setPreferredSize(new Dimension(0, 130));

        form.add(new JLabel("Nome:"));
        campoNome = new JTextField();
        form.add(campoNome);

        form.add(new JLabel("Valor (R$):"));
        campoValor = new JTextField();
        form.add(campoValor);

        form.add(new JLabel("Duração (meses):"));
        campoDuracao = new JTextField();
        form.add(campoDuracao);

        JPanel botoes = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 6));
        JButton btnInserir   = new JButton("➕ Inserir");
        JButton btnAtualizar = new JButton("✏️ Atualizar");
        JButton btnExcluir   = new JButton("🗑 Excluir");
        JButton btnLimpar    = new JButton("🔄 Limpar");

        btnInserir.setBackground(new Color(76, 175, 80));    btnInserir.setForeground(Color.WHITE);
        btnAtualizar.setBackground(new Color(33, 150, 243)); btnAtualizar.setForeground(Color.WHITE);
        btnExcluir.setBackground(new Color(244, 67, 54));    btnExcluir.setForeground(Color.WHITE);

        for (JButton b : new JButton[]{btnInserir, btnAtualizar, btnExcluir, btnLimpar}) {
            b.setFocusPainted(false);
            b.setCursor(new Cursor(Cursor.HAND_CURSOR));
            botoes.add(b);
        }

        modelo = new DefaultTableModel(
            new String[]{"ID", "Nome", "Valor", "Duração (meses)"}, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };
        tabela = new JTable(modelo);
        tabela.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tabela.getColumnModel().getColumn(0).setMaxWidth(50);
        tabela.setRowHeight(24);

        JPanel topo = new JPanel(new BorderLayout());
        topo.add(form,   BorderLayout.CENTER);
        topo.add(botoes, BorderLayout.SOUTH);

        setLayout(new BorderLayout(0, 8));
        add(topo, BorderLayout.NORTH);
        add(new JScrollPane(tabela), BorderLayout.CENTER);

        btnInserir.addActionListener(e   -> inserir());
        btnAtualizar.addActionListener(e -> atualizar());
        btnExcluir.addActionListener(e   -> excluir());
        btnLimpar.addActionListener(e    -> limparCampos());

        tabela.getSelectionModel().addListSelectionListener(e -> {
            int linha = tabela.getSelectedRow();
            if (linha >= 0) {
                idSelecionado = (int) modelo.getValueAt(linha, 0);
                campoNome.setText((String) modelo.getValueAt(linha, 1));
                campoValor.setText(modelo.getValueAt(linha, 2).toString());
                campoDuracao.setText(modelo.getValueAt(linha, 3) != null
                    ? modelo.getValueAt(linha, 3).toString() : "");
            }
        });
    }

    private void carregarTabela() {
        modelo.setRowCount(0);
        try (Connection conn = ConexaoBD.conectar();
             PreparedStatement ps = conn.prepareStatement(
                "SELECT id_plano, nome, valor, duracao_meses FROM planos ORDER BY valor");
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                modelo.addRow(new Object[]{
                    rs.getInt("id_plano"),
                    rs.getString("nome"),
                    rs.getDouble("valor"),
                    rs.getObject("duracao_meses")
                });
            }
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Erro ao carregar: " + ex.getMessage());
        }
    }

    private void inserir() {
        if (campoNome.getText().trim().isEmpty() || campoValor.getText().trim().isEmpty()) {
            JOptionPane.showMessageDialog(this, "Nome e Valor são obrigatórios.");
            return;
        }
        try (Connection conn = ConexaoBD.conectar();
             PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO planos (nome, valor, duracao_meses) VALUES (?, ?, ?)")) {

            ps.setString(1, campoNome.getText().trim());
            ps.setDouble(2, Double.parseDouble(campoValor.getText().trim()));
            ps.setObject(3, campoDuracao.getText().trim().isEmpty()
                ? null : Integer.parseInt(campoDuracao.getText().trim()));
            ps.executeUpdate();

            JOptionPane.showMessageDialog(this, "Plano cadastrado!");
            limparCampos();
            carregarTabela();
            RelatorioService.invalidar();   // REDIS: relatório depende dos planos

        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Valor e Duração devem ser números.");
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Erro ao inserir: " + ex.getMessage());
        }
    }

    private void atualizar() {
        if (idSelecionado == -1) {
            JOptionPane.showMessageDialog(this, "Clique em um plano na tabela para editar.");
            return;
        }
        try (Connection conn = ConexaoBD.conectar();
             PreparedStatement ps = conn.prepareStatement(
                "UPDATE planos SET nome=?, valor=?, duracao_meses=? WHERE id_plano=?")) {

            ps.setString(1, campoNome.getText().trim());
            ps.setDouble(2, Double.parseDouble(campoValor.getText().trim()));
            ps.setObject(3, campoDuracao.getText().trim().isEmpty()
                ? null : Integer.parseInt(campoDuracao.getText().trim()));
            ps.setInt(4, idSelecionado);
            ps.executeUpdate();

            JOptionPane.showMessageDialog(this, "Plano atualizado!");
            limparCampos();
            carregarTabela();
            RelatorioService.invalidar();   // REDIS: relatório depende dos planos

        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Valor e Duração devem ser números.");
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Erro ao atualizar: " + ex.getMessage());
        }
    }

    private void excluir() {
        if (idSelecionado == -1) {
            JOptionPane.showMessageDialog(this, "Clique em um plano na tabela para excluir.");
            return;
        }
        int confirm = JOptionPane.showConfirmDialog(this,
            "Excluir plano selecionado?", "Confirmar", JOptionPane.YES_NO_OPTION);
        if (confirm != JOptionPane.YES_OPTION) return;

        try (Connection conn = ConexaoBD.conectar();
             PreparedStatement ps = conn.prepareStatement(
                "DELETE FROM planos WHERE id_plano=?")) {

            ps.setInt(1, idSelecionado);
            ps.executeUpdate();

            JOptionPane.showMessageDialog(this, "Plano excluído!");
            limparCampos();
            carregarTabela();
            RelatorioService.invalidar();   // REDIS: relatório depende dos planos

        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Erro ao excluir: " + ex.getMessage());
        }
    }

    private void limparCampos() {
        campoNome.setText("");
        campoValor.setText("");
        campoDuracao.setText("");
        idSelecionado = -1;
        tabela.clearSelection();
    }
}