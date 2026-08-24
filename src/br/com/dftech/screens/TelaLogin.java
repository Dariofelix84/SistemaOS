/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JFrame.java to edit this template
 */
package br.com.dftech.screens;

import java.sql.*;
import br.com.dftech.dal.Moduloconexao;
import java.awt.Color;
import java.awt.event.KeyEvent;
import javax.swing.JOptionPane;

public class TelaLogin extends javax.swing.JFrame {

    Connection conexao = null;
    PreparedStatement pst = null;
    ResultSet rs = null;

    public void logar() {
        String user = txtUsuario.getText().trim();
        String senha = new String(txtSenha.getPassword());

        if (user.isEmpty()) {
            JOptionPane.showMessageDialog(null, "Preencha o campo Usuário!");
            txtUsuario.requestFocus();
            return;
        }

        if (senha.isEmpty()) {
            JOptionPane.showMessageDialog(null, "Preencha o campo Senha!");
            txtSenha.requestFocus();
            return;
        }

        String sqlUser = "select * from tbusuarios where login=?";
        try {
            pst = conexao.prepareStatement(sqlUser);
            pst.setString(1, user);
            rs = pst.executeQuery();

            if (!rs.next()) {
                JOptionPane.showMessageDialog(null, "Usuário incorreto!");
                txtUsuario.setText("");
                txtSenha.setText("");
                txtUsuario.requestFocus();
                return;
            }

            String senhaDb = rs.getString(5);
            if (!senhaDb.equals(senha)) {
                JOptionPane.showMessageDialog(null, "Senha incorreta!");
                txtSenha.setText("");
                txtSenha.requestFocus();
                return;
            }

            String perfil = rs.getString(6);
            TelaPrincipal principal = new TelaPrincipal();
            principal.setVisible(true);
            TelaPrincipal.lblUsuario.setText(rs.getString(2));

            if ("admin".equalsIgnoreCase(perfil)) {
                TelaPrincipal.menRel.setEnabled(true);
                TelaPrincipal.menCadUsu.setEnabled(true);
                TelaPrincipal.lblUsuario.setForeground(Color.red);
            }

            this.dispose();
            if (conexao != null && !conexao.isClosed()) {
                conexao.close();
            }

        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, e);
        }
    }

    public void atualizarStatusConexao() {
        try {
            if (conexao != null && !conexao.isClosed()) {
                conexao.close();
            }
        } catch (Exception e) {
        }

        conexao = Moduloconexao.conector();
        java.util.Properties props = Moduloconexao.carregarPropriedades();
        String host = props.getProperty("host", "localhost");
        if (conexao != null) {
            lblStatus.setIcon(new javax.swing.ImageIcon(getClass().getResource("/br/com/dftech/icons/dbconectado.png")));
            lblStatus.setToolTipText("Conectado ao Banco (" + host + "). Clique para alterar o IP do Servidor.");
        } else {
            lblStatus.setIcon(new javax.swing.ImageIcon(getClass().getResource("/br/com/dftech/icons/dbnaoconectado.png")));
            lblStatus.setToolTipText("Não Conectado (" + host + ")! Clique para configurar o IP do Servidor.");
        }
    }

    private void configurarConexaoRede() {
        java.util.Properties props = Moduloconexao.carregarPropriedades();
        String currentHost = props.getProperty("host", "localhost");
        String currentPort = props.getProperty("port", "5432");
        String currentDb = props.getProperty("database", "dbinfox");
        String currentUser = props.getProperty("user", "postgres");
        String currentPass = props.getProperty("password", "Mae191161");

        javax.swing.JTextField txtHost = new javax.swing.JTextField(currentHost);
        javax.swing.JTextField txtPort = new javax.swing.JTextField(currentPort);
        javax.swing.JTextField txtDb = new javax.swing.JTextField(currentDb);
        javax.swing.JTextField txtUser = new javax.swing.JTextField(currentUser);
        javax.swing.JPasswordField txtPass = new javax.swing.JPasswordField(currentPass);

        Object[] message = {
            "IP / Servidor:", txtHost,
            "Porta:", txtPort,
            "Nome do Banco:", txtDb,
            "Usuário:", txtUser,
            "Senha:", txtPass
        };

        int option = javax.swing.JOptionPane.showConfirmDialog(
                this,
                message,
                "Configuração de Conexão em Rede (PostgreSQL)",
                javax.swing.JOptionPane.OK_CANCEL_OPTION,
                javax.swing.JOptionPane.QUESTION_MESSAGE
        );

        if (option == javax.swing.JOptionPane.OK_OPTION) {
            String newHost = txtHost.getText().trim();
            String newPort = txtPort.getText().trim();
            String newDb = txtDb.getText().trim();
            String newUser = txtUser.getText().trim();
            String newPass = new String(txtPass.getPassword());

            if (newHost.isEmpty() || newDb.isEmpty()) {
                javax.swing.JOptionPane.showMessageDialog(this, "IP/Servidor e Nome do Banco não podem ser vazios!");
                return;
            }

            try {
                Connection testConn = Moduloconexao.testarConexao(newHost, newPort, newDb, newUser, newPass);
                if (testConn != null) {
                    testConn.close();
                    props.setProperty("host", newHost);
                    props.setProperty("port", newPort);
                    props.setProperty("database", newDb);
                    props.setProperty("user", newUser);
                    props.setProperty("password", newPass);
                    Moduloconexao.salvarPropriedades(props);

                    atualizarStatusConexao();
                    javax.swing.JOptionPane.showMessageDialog(this, "Conexão estabelecida e salva com sucesso!");
                }
            } catch (Exception e) {
                javax.swing.JOptionPane.showMessageDialog(this, "Erro ao conectar ao banco:\n" + e.getMessage(), "Falha de Conexão", javax.swing.JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    public TelaLogin() {
        initComponents();
        // Ícone da janela
        setIconImage(new javax.swing.ImageIcon(getClass().getResource("/br/com/dftech/icons/x.png")).getImage());

        lblStatus.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        lblStatus.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                configurarConexaoRede();
            }
        });

        atualizarStatusConexao();
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated
    // Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jLabel1 = new javax.swing.JLabel();
        jLabel2 = new javax.swing.JLabel();
        txtUsuario = new javax.swing.JTextField();
        btnLogin = new javax.swing.JButton();
        txtSenha = new javax.swing.JPasswordField();
        lblStatus = new javax.swing.JLabel();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setTitle("DFtech - Login");
        setResizable(false);

        jLabel1.setText("Usuário");

        jLabel2.setText("Senha");

        txtUsuario.setText("");
        txtUsuario.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                txtUsuarioActionPerformed(evt);
            }
        });
        txtUsuario.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyPressed(java.awt.event.KeyEvent evt) {
                txtUsuarioKeyPressed(evt);
            }
        });

        btnLogin.setText("Login");
        btnLogin.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnLoginActionPerformed(evt);
            }
        });
        btnLogin.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyPressed(java.awt.event.KeyEvent evt) {
                btnLoginKeyPressed(evt);
            }
        });

        txtSenha.setText("");
        txtSenha.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyPressed(java.awt.event.KeyEvent evt) {
                txtSenhaKeyPressed(evt);
            }
        });

        lblStatus.setIcon(new javax.swing.ImageIcon(getClass().getResource("/br/com/dftech/icons/dbnaoconectado.png"))); // NOI18N

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
                layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(layout.createSequentialGroup()
                                .addContainerGap(40, Short.MAX_VALUE)
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                        .addComponent(jLabel1, javax.swing.GroupLayout.Alignment.TRAILING)
                                        .addComponent(jLabel2)
                                        .addComponent(lblStatus, javax.swing.GroupLayout.PREFERRED_SIZE, 32,
                                                javax.swing.GroupLayout.PREFERRED_SIZE))
                                .addGap(18, 18, 18)
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                                .addComponent(txtUsuario, javax.swing.GroupLayout.DEFAULT_SIZE, 220, Short.MAX_VALUE)
                                                .addComponent(txtSenha, javax.swing.GroupLayout.DEFAULT_SIZE, 220, Short.MAX_VALUE))
                                        .addComponent(btnLogin, javax.swing.GroupLayout.PREFERRED_SIZE, 90, javax.swing.GroupLayout.PREFERRED_SIZE))
                                .addContainerGap(40, Short.MAX_VALUE)));
        layout.setVerticalGroup(
                layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(layout.createSequentialGroup()
                                .addGap(25, 25, 25)
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                        .addComponent(jLabel1)
                                        .addComponent(txtUsuario, javax.swing.GroupLayout.PREFERRED_SIZE,
                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                javax.swing.GroupLayout.PREFERRED_SIZE))
                                .addGap(18, 18, 18)
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                        .addComponent(jLabel2)
                                        .addComponent(txtSenha, javax.swing.GroupLayout.PREFERRED_SIZE,
                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                javax.swing.GroupLayout.PREFERRED_SIZE))
                                .addGap(18, 18, 18)
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                        .addComponent(lblStatus, javax.swing.GroupLayout.PREFERRED_SIZE, 37,
                                                javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addComponent(btnLogin))
                                .addContainerGap(25, Short.MAX_VALUE)));

        setSize(new java.awt.Dimension(376, 210));
        setLocationRelativeTo(null);
    }// </editor-fold>//GEN-END:initComponents

    private void txtUsuarioActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_txtUsuarioActionPerformed

    }// GEN-LAST:event_txtUsuarioActionPerformed

    private void btnLoginActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_btnLoginActionPerformed
        logar(); // TODO add your handling code here:
    }// GEN-LAST:event_btnLoginActionPerformed

    private void btnLoginKeyPressed(java.awt.event.KeyEvent evt) {// GEN-FIRST:event_btnLoginKeyPressed
        if (evt.getKeyCode() == KeyEvent.VK_ENTER) {
            btnLogin.doClick();
        }
    }// GEN-LAST:event_btnLoginKeyPressed

    private void txtUsuarioKeyPressed(java.awt.event.KeyEvent evt) {// GEN-FIRST:event_txtUsuarioKeyPressed
        if (evt.getKeyCode() == KeyEvent.VK_ENTER) {
            txtSenha.requestFocus();
        }
    }// GEN-LAST:event_txtUsuarioKeyPressed

    private void txtSenhaKeyPressed(java.awt.event.KeyEvent evt) {// GEN-FIRST:event_txtSenhaKeyPressed
        if (evt.getKeyCode() == KeyEvent.VK_ENTER) {
            logar();
        }
    }// GEN-LAST:event_txtSenhaKeyPressed

    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        /* Set the Nimbus look and feel */
        // <editor-fold defaultstate="collapsed" desc=" Look and feel setting code
        // (optional) ">
        /*
         * If Nimbus (introduced in Java SE 6) is not available, stay with the default
         * look and feel.
         * For details see
         * http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html
         */
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException
                | javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(TelaLogin.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        // </editor-fold>

        // </editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(new Runnable() {
            @Override
            public void run() {
                new TelaLogin().setVisible(true);
            }
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btnLogin;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel lblStatus;
    private javax.swing.JPasswordField txtSenha;
    private javax.swing.JTextField txtUsuario;
    // End of variables declaration//GEN-END:variables

}
