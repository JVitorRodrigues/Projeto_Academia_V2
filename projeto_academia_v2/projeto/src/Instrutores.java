import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import java.sql.*;

public class Instrutores extends JFrame {

    private JTextField campoNome, campoCpf, campoEspecialidade;
    private JTable tabela;
    private DefaultTableModel modelo;
    private int idSelecionado = -1;

    public Instrutores() {
        setTitle("Gerenciar Instrutores");
        setSize(650, 480);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        construirTela();
        carregarTabela();
    }

    private void construirTela() {

        JPanel form = new JPanel(new GridLayout(3, 2, 8, 8));
        form.setBorder(BorderFactory.createTitledBorder("Dados do Instrutor"));
        form.setPreferredSize(new Dimension(0, 130));

        form.add(new JLabel("Nome:"));
        campoNome = new JTextField();
        form.add(campoNome);

        form.add(new JLabel("CPF:"));
        campoCpf = new JTextField();
        form.add(campoCpf);

        form.add(new JLabel("Especialidade:"));
        campoEspecialidade = new JTextField();
        form.add(campoEspecialidade);

        JPanel botoes = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 6));
        JButton btnInserir   = new JButton("➕ Inserir");
        JButton btnAtualizar = new JButton("✏️ Atualizar");
        JButton btnExcluir   = new JButton("🗑 Excluir");
        JButton btnLimpar    = new JButton("🔄 Limpar");

        btnInserir.setBackground(new Color(76, 175, 80));   btnInserir.setForeground(Color.WHITE);
        btnAtualizar.setBackground(new Color(33, 150, 243)); btnAtualizar.setForeground(Color.WHITE);
        btnExcluir.setBackground(new Color(244, 67, 54));   btnExcluir.setForeground(Color.WHITE);

        for (JButton b : new JButton[]{btnInserir, btnAtualizar, btnExcluir, btnLimpar}) {
            b.setFocusPainted(false);
            b.setCursor(new Cursor(Cursor.HAND_CURSOR));
            botoes.add(b);
        }

        modelo = new DefaultTableModel(
            new String[]{"ID", "Nome", "CPF", "Especialidade"}, 0) {
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
                campoCpf.setText((String) modelo.getValueAt(linha, 2));
                campoEspecialidade.setText((String) modelo.getValueAt(linha, 3));
            }
        });
    }

    private void carregarTabela() {
        modelo.setRowCount(0);
        try (Connection conn = ConexaoBD.conectar();
             PreparedStatement ps = conn.prepareStatement(
                "SELECT id_instrutor, nome, cpf, especialidade FROM instrutores ORDER BY nome");
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                modelo.addRow(new Object[]{
                    rs.getInt("id_instrutor"),
                    rs.getString("nome"),
                    rs.getString("cpf"),
                    rs.getString("especialidade")
                });
            }
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Erro ao carregar: " + ex.getMessage());
        }
    }

    private void inserir() {
        if (campoNome.getText().trim().isEmpty() || campoCpf.getText().trim().isEmpty()
                || campoEspecialidade.getText().trim().isEmpty()) {
            JOptionPane.showMessageDialog(this, "Todos os campos são obrigatórios.");
            return;
        }
        try (Connection conn = ConexaoBD.conectar();
             PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO instrutores (nome, cpf, especialidade) VALUES (?, ?, ?)")) {

            ps.setString(1, campoNome.getText().trim());
            ps.setString(2, campoCpf.getText().trim());
            ps.setString(3, campoEspecialidade.getText().trim());
            ps.executeUpdate();

            JOptionPane.showMessageDialog(this, "Instrutor cadastrado!");
            limparCampos();
            carregarTabela();

        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Erro ao inserir: " + ex.getMessage());
        }
    }

    private void atualizar() {
        if (idSelecionado == -1) {
            JOptionPane.showMessageDialog(this, "Clique em um instrutor na tabela para editar.");
            return;
        }
        try (Connection conn = ConexaoBD.conectar();
             PreparedStatement ps = conn.prepareStatement(
                "UPDATE instrutores SET nome=?, cpf=?, especialidade=? WHERE id_instrutor=?")) {

            ps.setString(1, campoNome.getText().trim());
            ps.setString(2, campoCpf.getText().trim());
            ps.setString(3, campoEspecialidade.getText().trim());
            ps.setInt(4, idSelecionado);
            ps.executeUpdate();

            JOptionPane.showMessageDialog(this, "Instrutor atualizado!");
            limparCampos();
            carregarTabela();

        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Erro ao atualizar: " + ex.getMessage());
        }
    }

    private void excluir() {
        if (idSelecionado == -1) {
            JOptionPane.showMessageDialog(this, "Clique em um instrutor na tabela para excluir.");
            return;
        }
        int confirm = JOptionPane.showConfirmDialog(this,
            "Excluir instrutor selecionado?", "Confirmar", JOptionPane.YES_NO_OPTION);
        if (confirm != JOptionPane.YES_OPTION) return;

        try (Connection conn = ConexaoBD.conectar();
             PreparedStatement ps = conn.prepareStatement(
                "DELETE FROM instrutores WHERE id_instrutor=?")) {

            ps.setInt(1, idSelecionado);
            ps.executeUpdate();

            JOptionPane.showMessageDialog(this, "Instrutor excluído!");
            limparCampos();
            carregarTabela();

        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Erro ao excluir: " + ex.getMessage());
        }
    }

    private void limparCampos() {
        campoNome.setText("");
        campoCpf.setText("");
        campoEspecialidade.setText("");
        idSelecionado = -1;
        tabela.clearSelection();
    }
}