import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import java.sql.*;

public class Alunos extends JFrame {

    private JTextField campoNome, campoCpf, campoTelefone, campoEmail;
    private JTable tabela;
    private DefaultTableModel modelo;
    private int idSelecionado = -1;

    public Alunos() {
        setTitle("Gerenciar Alunos");
        setSize(750, 520);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        construirTela();
        carregarTabela("");
    }

    private void construirTela() {

        
        JPanel form = new JPanel(new GridLayout(4, 2, 8, 8));
        form.setBorder(BorderFactory.createTitledBorder("Dados do Aluno"));
        form.setPreferredSize(new Dimension(0, 160));

        form.add(new JLabel("Nome:"));
        campoNome = new JTextField();
        form.add(campoNome);

        form.add(new JLabel("CPF:"));
        campoCpf = new JTextField();
        form.add(campoCpf);

        form.add(new JLabel("Telefone:"));
        campoTelefone = new JTextField();
        form.add(campoTelefone);

        form.add(new JLabel("E-mail:"));
        campoEmail = new JTextField();
        form.add(campoEmail);

        
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

        
        JPanel filtroPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        filtroPanel.add(new JLabel("Buscar por nome:"));
        JTextField campoFiltro = new JTextField(20);
        JButton btnBuscar = new JButton("🔍 Buscar");
        JButton btnTodos  = new JButton("Ver Todos");
        filtroPanel.add(campoFiltro);
        filtroPanel.add(btnBuscar);
        filtroPanel.add(btnTodos);

     
        modelo = new DefaultTableModel(
            new String[]{"ID", "Nome", "CPF", "Telefone", "E-mail", "Cadastro", "Situação"}, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };
        tabela = new JTable(modelo);
        tabela.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tabela.getColumnModel().getColumn(0).setMaxWidth(50);
        tabela.setRowHeight(24);

        
        JPanel topo = new JPanel(new BorderLayout());
        topo.add(form,        BorderLayout.CENTER);
        topo.add(botoes,      BorderLayout.SOUTH);

        JPanel centro = new JPanel(new BorderLayout(0, 4));
        centro.add(filtroPanel,          BorderLayout.NORTH);
        centro.add(new JScrollPane(tabela), BorderLayout.CENTER);

        setLayout(new BorderLayout(0, 8));
        add(topo,   BorderLayout.NORTH);
        add(centro, BorderLayout.CENTER);

       
        btnInserir.addActionListener(e   -> inserir());
        btnAtualizar.addActionListener(e -> atualizar());
        btnExcluir.addActionListener(e   -> excluir());
        btnLimpar.addActionListener(e    -> limparCampos());
        btnBuscar.addActionListener(e    -> carregarTabela(campoFiltro.getText().trim()));
        btnTodos.addActionListener(e     -> { campoFiltro.setText(""); carregarTabela(""); });

        tabela.getSelectionModel().addListSelectionListener(e -> {
            int linha = tabela.getSelectedRow();
            if (linha >= 0) {
                idSelecionado = (int) modelo.getValueAt(linha, 0);
                campoNome.setText((String) modelo.getValueAt(linha, 1));
                campoCpf.setText((String) modelo.getValueAt(linha, 2));
                campoTelefone.setText(modelo.getValueAt(linha, 3) != null
                    ? modelo.getValueAt(linha, 3).toString() : "");
                campoEmail.setText(modelo.getValueAt(linha, 4) != null
                    ? modelo.getValueAt(linha, 4).toString() : "");
            }
        });
    }

    private void carregarTabela(String filtro) {
        modelo.setRowCount(0);
        String sql = "SELECT id_aluno, nome, cpf, telefone, email, data_cadastro, "
                   + "       fn_situacao_aluno(id_aluno) AS situacao "   // FUNCTION do banco
                   + "FROM alunos WHERE nome ILIKE ? ORDER BY nome";
        try (Connection conn = ConexaoBD.conectar();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, "%" + filtro + "%");
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                modelo.addRow(new Object[]{
                    rs.getInt("id_aluno"),
                    rs.getString("nome"),
                    rs.getString("cpf"),
                    rs.getString("telefone"),
                    rs.getString("email"),
                    rs.getTimestamp("data_cadastro"),
                    rs.getString("situacao")
                });
            }
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Erro ao carregar: " + ex.getMessage());
        }
    }

    private void inserir() {
        if (campoNome.getText().trim().isEmpty() || campoCpf.getText().trim().isEmpty()) {
            JOptionPane.showMessageDialog(this, "Nome e CPF são obrigatórios.");
            return;
        }
        String sql = "INSERT INTO alunos (nome, cpf, telefone, email) VALUES (?, ?, ?, ?)";
        try (Connection conn = ConexaoBD.conectar();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, campoNome.getText().trim());
            ps.setString(2, campoCpf.getText().trim());
            ps.setString(3, campoTelefone.getText().trim());
            ps.setString(4, campoEmail.getText().trim());
            ps.executeUpdate();

            JOptionPane.showMessageDialog(this, "Aluno cadastrado com sucesso!");
            limparCampos();
            carregarTabela("");
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Erro ao inserir: " + ex.getMessage());
        }
    }

    private void atualizar() {
        if (idSelecionado == -1) {
            JOptionPane.showMessageDialog(this, "Clique em um aluno na tabela para editar.");
            return;
        }
        String sql = "UPDATE alunos SET nome=?, cpf=?, telefone=?, email=? WHERE id_aluno=?";
        try (Connection conn = ConexaoBD.conectar();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, campoNome.getText().trim());
            ps.setString(2, campoCpf.getText().trim());
            ps.setString(3, campoTelefone.getText().trim());
            ps.setString(4, campoEmail.getText().trim());
            ps.setInt(5, idSelecionado);
            ps.executeUpdate();

            JOptionPane.showMessageDialog(this, "Aluno atualizado com sucesso!");
            limparCampos();
            carregarTabela("");
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Erro ao atualizar: " + ex.getMessage());
        }
    }

    private void excluir() {
        if (idSelecionado == -1) {
            JOptionPane.showMessageDialog(this, "Clique em um aluno na tabela para excluir.");
            return;
        }
        int confirm = JOptionPane.showConfirmDialog(this,
            "Excluir aluno selecionado? Suas matrículas também serão removidas.",
            "Confirmar Exclusão", JOptionPane.YES_NO_OPTION);
        if (confirm != JOptionPane.YES_OPTION) return;

        try (Connection conn = ConexaoBD.conectar();
             PreparedStatement ps = conn.prepareStatement(
                "DELETE FROM alunos WHERE id_aluno=?")) {

            ps.setInt(1, idSelecionado);
            ps.executeUpdate();

            JOptionPane.showMessageDialog(this, "Aluno excluído!");
            limparCampos();
            carregarTabela("");
            RelatorioService.invalidar();   // REDIS: excluir aluno remove matrículas/pagamentos
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Erro ao excluir: " + ex.getMessage());
        }
    }

    private void limparCampos() {
        campoNome.setText("");
        campoCpf.setText("");
        campoTelefone.setText("");
        campoEmail.setText("");
        idSelecionado = -1;
        tabela.clearSelection();
    }
}
