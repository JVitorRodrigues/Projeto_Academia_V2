import javax.swing.*;
import java.awt.*;

public class MenuPrincipal extends JFrame {

    public MenuPrincipal() {
        setTitle("Academia PRO — Menu Principal");
        setSize(400, 440);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        JPanel painel = new JPanel(new GridLayout(7, 1, 10, 10));
        painel.setBorder(BorderFactory.createEmptyBorder(30, 50, 30, 50));

        JLabel titulo = new JLabel("Menu Principal", SwingConstants.CENTER);
        titulo.setFont(new Font("Arial", Font.BOLD, 18));
        painel.add(titulo);

        JButton btnAlunos      = new JButton("👤 Gerenciar Alunos");
        JButton btnInstrutores = new JButton("👨‍🏫 Gerenciar Instrutores");
        JButton btnPlanos      = new JButton("📋 Gerenciar Planos");
        JButton btnMatriculas  = new JButton("📝 Gerenciar Matrículas");
        JButton btnRelatorio   = new JButton("📊 Relatório Financeiro");
        JButton btnSair        = new JButton("Sair");

        for (JButton btn : new JButton[]{btnAlunos, btnInstrutores, btnPlanos, btnMatriculas, btnRelatorio}) {
            btn.setFont(new Font("Arial", Font.PLAIN, 13));
            btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
            painel.add(btn);
        }
        painel.add(btnSair);

        add(painel);

        btnAlunos.addActionListener(e      -> new Alunos().setVisible(true));
        btnInstrutores.addActionListener(e -> new Instrutores().setVisible(true));
        btnPlanos.addActionListener(e      -> new Planos().setVisible(true));
        btnMatriculas.addActionListener(e  -> new Matriculas().setVisible(true));
        btnRelatorio.addActionListener(e   -> new Relatorios().setVisible(true));
        btnSair.addActionListener(e -> {
            dispose();
            new Login().setVisible(true);
        });
    }
}