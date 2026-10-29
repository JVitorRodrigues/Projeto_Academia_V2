import javax.swing.*;
import java.awt.*;
import java.sql.*;

public class Login extends JFrame {

    private JTextField campoUsuario;
    private JPasswordField campoSenha;
    private JLabel lblErro;

    public Login() {
        setTitle("Login — Academia");
        setSize(320, 250);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setResizable(false);

        JPanel painel = new JPanel(new GridLayout(5, 1, 8, 8));
        painel.setBorder(BorderFactory.createEmptyBorder(20, 30, 20, 30));

        painel.add(new JLabel("Usuário:"));
        campoUsuario = new JTextField();
        painel.add(campoUsuario);

        painel.add(new JLabel("Senha:"));
        campoSenha = new JPasswordField();
        painel.add(campoSenha);

        JButton btnEntrar = new JButton("Entrar");
        painel.add(btnEntrar);

        lblErro = new JLabel("", SwingConstants.CENTER);
        lblErro.setForeground(Color.RED);

        add(painel, BorderLayout.CENTER);
        add(lblErro, BorderLayout.SOUTH);

        btnEntrar.addActionListener(e -> fazerLogin());
        campoSenha.addActionListener(e -> fazerLogin());
    }

    private void fazerLogin() {
        String user = campoUsuario.getText().trim();
        String pass = new String(campoSenha.getPassword()).trim();

        // REDIS: bloqueia o usuário após muitas tentativas erradas
        long bloqueio = LimiteLogin.segundosBloqueado(user);
        if (bloqueio > 0) {
            lblErro.setText("Muitas tentativas. Aguarde " + bloqueio + " s.");
            return;
        }

        try (Connection conn = ConexaoBD.conectar();
             PreparedStatement ps = conn.prepareStatement(
                "SELECT id_usuario FROM usuarios WHERE username=? AND senha=?")) {

            ps.setString(1, user);
            ps.setString(2, pass);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                LimiteLogin.limpar(user);
                dispose();
                new MenuPrincipal().setVisible(true);
            } else {
                long n = LimiteLogin.registrarFalha(user);
                lblErro.setText("Usuário ou senha incorretos."
                    + (n > 0 ? " (tentativa " + n + " de " + LimiteLogin.MAX_FALHAS + ")" : ""));
            }

        } catch (SQLException ex) {
            lblErro.setText("Erro: " + ex.getMessage());
        }
    }

    public static void main(String[] args) {
        new Login().setVisible(true);
    }
}