/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JInternalFrame.java to edit this template
 */
package br.com.dftech.screens;

import java.sql.*;
import br.com.dftech.dal.Moduloconexao;
import java.awt.event.KeyEvent;
import javax.swing.JOptionPane;
import javax.swing.JTextField;
import net.proteanit.sql.DbUtils;

public class TelaUsuario extends javax.swing.JInternalFrame {

    Connection conexao = null;
    PreparedStatement pst = null;
    ResultSet rs = null;

    /**
     * Creates new form TelaUsuario
     */
    public TelaUsuario() {
        initComponents();
        conexao = Moduloconexao.conector();
        txtUsuId.setHorizontalAlignment(JTextField.CENTER);
        pesquisarUsuario();
    }

    private void pesquisarUsuario() {
        String sql = "select iduser as ID, usuario as Nome, fone as Fone, login as Login, perfil as Perfil from tbusuarios where lower(usuario) like ? order by usuario";
        try {
            if (conexao == null || conexao.isClosed()) {
                conexao = Moduloconexao.conector();
            }
            pst = conexao.prepareStatement(sql);
            pst.setString(1, "%" + txtUsuPesquisar.getText().toLowerCase() + "%");

            rs = pst.executeQuery();
            tblUsuarios.setModel(DbUtils.resultSetToTableModel(rs));
            configurarLarguraColunasTabela();

        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, e);
        }
    }

    private void configurarLarguraColunasTabela() {
        if (tblUsuarios.getColumnCount() >= 5) {
            tblUsuarios.getColumnModel().getColumn(0).setPreferredWidth(50);
            tblUsuarios.getColumnModel().getColumn(0).setMaxWidth(60);
            tblUsuarios.getColumnModel().getColumn(0).setMinWidth(40);

            javax.swing.table.DefaultTableCellRenderer centerRenderer = new javax.swing.table.DefaultTableCellRenderer();
            centerRenderer.setHorizontalAlignment(javax.swing.JLabel.CENTER);
            tblUsuarios.getColumnModel().getColumn(0).setCellRenderer(centerRenderer);

            tblUsuarios.getColumnModel().getColumn(1).setPreferredWidth(180);
            tblUsuarios.getColumnModel().getColumn(2).setPreferredWidth(110);
            tblUsuarios.getColumnModel().getColumn(3).setPreferredWidth(120);
            tblUsuarios.getColumnModel().getColumn(4).setPreferredWidth(80);
        }
    }

    public void setarCampos() {
        int setar = tblUsuarios.getSelectedRow();
        if (setar < 0)
            return;

        String userIdStr = tblUsuarios.getModel().getValueAt(setar, 0).toString();
        String sql = "select * from tbusuarios where iduser=?";
        try {
            if (conexao == null || conexao.isClosed()) {
                conexao = Moduloconexao.conector();
            }
            pst = conexao.prepareStatement(sql);
            pst.setInt(1, Integer.parseInt(userIdStr));
            rs = pst.executeQuery();
            if (rs.next()) {
                txtUsuId.setText(rs.getString(1));
                txtUsuNome.setText(rs.getString(2));
                txtUsuFone.setText(rs.getString(3));
                txtUsuLogin.setText(rs.getString(4));
                txtUsuSenha.setText(rs.getString(5));
                cboUsuPerfil.setSelectedItem(rs.getString(6));
                btnUsuCreate.setEnabled(false);
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, e);
        }
    }

    private void consultar() {
        if (txtUsuId.getText().trim().isEmpty()) {
            if (!txtUsuPesquisar.getText().trim().isEmpty() || !txtUsuNome.getText().trim().isEmpty()) {
                if (txtUsuPesquisar.getText().trim().isEmpty()) {
                    txtUsuPesquisar.setText(txtUsuNome.getText().trim());
                }
                pesquisarUsuario();
                return;
            }
            JOptionPane.showMessageDialog(null, "Preencha o ID ou o Nome do usuário para consultar!");
            return;
        }

        String sql = "select * from tbusuarios where iduser=?";
        try {
            int userId;
            try {
                userId = Integer.parseInt(txtUsuId.getText().trim());
            } catch (NumberFormatException e) {
                JOptionPane.showMessageDialog(null, "O ID deve ser um número inteiro!");
                return;
            }
            if (conexao == null || conexao.isClosed()) {
                conexao = Moduloconexao.conector();
            }
            pst = conexao.prepareStatement(sql);

            pst.setInt(1, userId);
            rs = pst.executeQuery();
            if (rs.next()) {
                txtUsuNome.setText(rs.getString(2));
                txtUsuFone.setText(rs.getString(3));
                txtUsuLogin.setText(rs.getString(4));
                txtUsuSenha.setText(rs.getString(5));
                cboUsuPerfil.setSelectedItem(rs.getString(6));
                txtUsuId.requestFocus();
                btnUsuCreate.setEnabled(false);

            } else {
                JOptionPane.showMessageDialog(null, "Usuário não cadastrado");
                txtUsuNome.setText(null);
                txtUsuFone.setText(null);
                txtUsuLogin.setText(null);
                txtUsuSenha.setText(null);
                txtUsuNome.requestFocus();
                btnUsuCreate.setEnabled(true);
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, e);
        }
    }

    private int obterProximoId() {
        String sql = "select coalesce(max(iduser), 0) + 1 from tbusuarios";
        try {
            if (conexao == null || conexao.isClosed()) {
                conexao = Moduloconexao.conector();
            }
            pst = conexao.prepareStatement(sql);
            rs = pst.executeQuery();
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "Erro ao obter próximo ID: " + e.getMessage());
        }
        return 1;
    }

    private void novoUsuario() {
        int proximoId = obterProximoId();
        txtUsuId.setText(String.valueOf(proximoId));
        txtUsuNome.setText(null);
        txtUsuFone.setText(null);
        txtUsuLogin.setText(null);
        txtUsuSenha.setText(null);
        cboUsuPerfil.setSelectedIndex(0);
        txtUsuPesquisar.setText(null);
        pesquisarUsuario();
        btnUsuCreate.setEnabled(true);
        txtUsuNome.requestFocus();
    }

    private void adicionar() {
        String sql = "insert into tbusuarios(iduser,usuario,fone,login,senha,perfil) values(?,?,?,?,?,?)";
        try {
            if (txtUsuId.getText().trim().isEmpty()) {
                txtUsuId.setText(String.valueOf(obterProximoId()));
            }
            if ((txtUsuId.getText().isEmpty()) || (txtUsuNome.getText().isEmpty()) || (txtUsuLogin.getText().isEmpty())
                    || (txtUsuSenha.getText().isEmpty())) {
                JOptionPane.showMessageDialog(null, "Prencha todos os campos obrigatórios!");
                return;
            }
            int userId;
            try {
                userId = Integer.parseInt(txtUsuId.getText().trim());
            } catch (NumberFormatException e) {
                JOptionPane.showMessageDialog(null, "O ID deve ser um número inteiro!");
                return;
            }
            if (conexao == null || conexao.isClosed()) {
                conexao = Moduloconexao.conector();
            }
            pst = conexao.prepareStatement(sql);
            pst.setInt(1, userId);
            pst.setString(2, txtUsuNome.getText());
            pst.setString(3, txtUsuFone.getText());
            pst.setString(4, txtUsuLogin.getText());
            pst.setString(5, txtUsuSenha.getText());
            pst.setString(6, (String) cboUsuPerfil.getSelectedItem());

            int adicionado = pst.executeUpdate();
            if (adicionado > 0) {
                JOptionPane.showMessageDialog(null, "Usuário cadastrado com sucesso");
                txtUsuId.setText(null);
                txtUsuNome.setText(null);
                txtUsuFone.setText(null);
                txtUsuLogin.setText(null);
                txtUsuSenha.setText(null);
                pesquisarUsuario();
                txtUsuId.requestFocus();
            }

        } catch (SQLException e) {
            JOptionPane.showMessageDialog(null, e);
        }
    }

    private void alterar() {
        String sql = "update tbusuarios set usuario=?,fone=?,login=?,senha=?,perfil=? where iduser=?";
        try {
            if ((txtUsuId.getText().isEmpty()) || (txtUsuNome.getText().isEmpty()) || (txtUsuLogin.getText().isEmpty())
                    || (txtUsuSenha.getText().isEmpty())) {
                JOptionPane.showMessageDialog(null, "Prencha todos os campos obrigatórios!");
                return;
            }
            int userId;
            try {
                userId = Integer.parseInt(txtUsuId.getText());
            } catch (NumberFormatException e) {
                JOptionPane.showMessageDialog(null, "O ID deve ser um número inteiro!");
                return;
            }
            if (conexao == null || conexao.isClosed()) {
                conexao = Moduloconexao.conector();
            }
            pst = conexao.prepareStatement(sql);
            pst.setString(1, txtUsuNome.getText());
            pst.setString(2, txtUsuFone.getText());
            pst.setString(3, txtUsuLogin.getText());
            pst.setString(4, txtUsuSenha.getText());
            pst.setString(5, (String) cboUsuPerfil.getSelectedItem());
            pst.setInt(6, userId);

            int adicionado = pst.executeUpdate();
            if (adicionado > 0) {
                JOptionPane.showMessageDialog(null, "Dados do usuário alterado com sucesso");
                txtUsuId.setText(null);
                txtUsuNome.setText(null);
                txtUsuFone.setText(null);
                txtUsuLogin.setText(null);
                txtUsuSenha.setText(null);
                btnUsuCreate.setEnabled(true);
                pesquisarUsuario();
                txtUsuId.requestFocus();
            }

        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, e);
        }
    }

    private void remover() {
        if (txtUsuId.getText().isEmpty()) {
            JOptionPane.showMessageDialog(null, "Preencha o ID do usuário para remover!");
            return;
        }
        int userId;
        try {
            userId = Integer.parseInt(txtUsuId.getText());
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(null, "O ID deve ser um número inteiro!");
            return;
        }
        int confirma = JOptionPane.showConfirmDialog(null, "Tem certeza que deseja remover esse usuário?", "Atenção",
                JOptionPane.YES_NO_OPTION);
        if (confirma == JOptionPane.YES_OPTION) {
            String sql = "delete from tbusuarios where iduser=?";
            try {
                if (conexao == null || conexao.isClosed()) {
                    conexao = Moduloconexao.conector();
                }
                pst = conexao.prepareStatement(sql);
                pst.setInt(1, userId);
                int apagado = pst.executeUpdate();
                if (apagado > 0) {
                    JOptionPane.showMessageDialog(null, "Usuário removido com sucesso");
                    txtUsuId.setText(null);
                    txtUsuNome.setText(null);
                    txtUsuFone.setText(null);
                    txtUsuLogin.setText(null);
                    txtUsuSenha.setText(null);
                    btnUsuCreate.setEnabled(true);
                    pesquisarUsuario();
                    txtUsuId.requestFocus();
                }

            } catch (Exception e) {
                JOptionPane.showMessageDialog(null, e);
            }

        }

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

        jLabel8 = new javax.swing.JLabel();
        lblIconPesquisar = new javax.swing.JLabel();
        txtUsuPesquisar = new javax.swing.JTextField();
        jScrollPane1 = new javax.swing.JScrollPane();
        tblUsuarios = new javax.swing.JTable();
        jLabel2 = new javax.swing.JLabel();
        jLabel3 = new javax.swing.JLabel();
        jLabel4 = new javax.swing.JLabel();
        jLabel5 = new javax.swing.JLabel();
        txtUsuId = new javax.swing.JTextField();
        txtUsuNome = new javax.swing.JTextField();
        txtUsuLogin = new javax.swing.JTextField();
        txtUsuSenha = new javax.swing.JTextField();
        cboUsuPerfil = new javax.swing.JComboBox<>();
        jLabel6 = new javax.swing.JLabel();
        btnUsuCreate = new javax.swing.JButton();
        btnUsuConsulta = new javax.swing.JButton();
        btnUsuUpdate = new javax.swing.JButton();
        btnUsuDelete = new javax.swing.JButton();
        btnUsuNovo = new javax.swing.JButton();
        jLabel7 = new javax.swing.JLabel();
        jLabel1 = new javax.swing.JLabel();
        txtUsuFone = new javax.swing.JFormattedTextField();

        setClosable(true);
        setIconifiable(true);
        setMaximizable(true);
        setResizable(true);
        setTitle("Usuários");
        setMinimumSize(new java.awt.Dimension(640, 530));
        setPreferredSize(new java.awt.Dimension(640, 530));

        jLabel8.setFont(new java.awt.Font("Segoe UI", 0, 14)); // NOI18N
        jLabel8.setText("Pesquisar Usuário por Nome");

        lblIconPesquisar
                .setIcon(new javax.swing.ImageIcon(getClass().getResource("/br/com/dftech/icons/pesquisar.png"))); // NOI18N

        txtUsuPesquisar.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                txtUsuPesquisarKeyReleased(evt);
            }
        });

        tblUsuarios.setModel(new javax.swing.table.DefaultTableModel(
                new Object[][] {},
                new String[] {
                        "ID", "Nome", "Fone", "Login", "Perfil"
                }));
        tblUsuarios.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        tblUsuarios.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                tblUsuariosMouseClicked(evt);
            }
        });
        tblUsuarios.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                tblUsuariosKeyReleased(evt);
            }
        });
        jScrollPane1.setViewportView(tblUsuarios);

        jLabel2.setFont(new java.awt.Font("sansserif", 0, 14)); // NOI18N
        jLabel2.setText("* Nome");

        jLabel3.setFont(new java.awt.Font("sansserif", 0, 14)); // NOI18N
        jLabel3.setText("* Login");

        jLabel4.setFont(new java.awt.Font("sansserif", 0, 14)); // NOI18N
        jLabel4.setText("* Senha");

        jLabel5.setFont(new java.awt.Font("sansserif", 0, 14)); // NOI18N
        jLabel5.setText("* Perfil");

        txtUsuId.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyPressed(java.awt.event.KeyEvent evt) {
                txtUsuIdKeyPressed(evt);
            }
        });

        cboUsuPerfil.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "user", "admin" }));

        jLabel6.setFont(new java.awt.Font("sansserif", 0, 14)); // NOI18N
        jLabel6.setText("Fone");

        btnUsuCreate.setIcon(new javax.swing.ImageIcon(getClass().getResource("/br/com/dftech/icons/create.png"))); // NOI18N
        btnUsuCreate.setToolTipText("Adicionar");
        btnUsuCreate.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        btnUsuCreate.setPreferredSize(new java.awt.Dimension(80, 80));
        btnUsuCreate.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnUsuCreateActionPerformed(evt);
            }
        });
        btnUsuCreate.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyPressed(java.awt.event.KeyEvent evt) {
                btnUsuCreateKeyPressed(evt);
            }
        });

        btnUsuConsulta.setIcon(new javax.swing.ImageIcon(getClass().getResource("/br/com/dftech/icons/read.png"))); // NOI18N
        btnUsuConsulta.setToolTipText("Consultar");
        btnUsuConsulta.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        btnUsuConsulta.setPreferredSize(new java.awt.Dimension(80, 80));
        btnUsuConsulta.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnUsuConsultaActionPerformed(evt);
            }
        });
        btnUsuConsulta.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyPressed(java.awt.event.KeyEvent evt) {
                btnUsuConsultaKeyPressed(evt);
            }
        });

        btnUsuUpdate.setIcon(new javax.swing.ImageIcon(getClass().getResource("/br/com/dftech/icons/update.png"))); // NOI18N
        btnUsuUpdate.setToolTipText("Alterar");
        btnUsuUpdate.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        btnUsuUpdate.setPreferredSize(new java.awt.Dimension(80, 80));
        btnUsuUpdate.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnUsuUpdateActionPerformed(evt);
            }
        });
        btnUsuUpdate.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyPressed(java.awt.event.KeyEvent evt) {
                btnUsuUpdateKeyPressed(evt);
            }
        });

        btnUsuDelete.setIcon(new javax.swing.ImageIcon(getClass().getResource("/br/com/dftech/icons/delete.png"))); // NOI18N
        btnUsuDelete.setToolTipText("Remover");
        btnUsuDelete.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        btnUsuDelete.setPreferredSize(new java.awt.Dimension(80, 80));
        btnUsuDelete.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnUsuDeleteActionPerformed(evt);
            }
        });
        btnUsuDelete.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyPressed(java.awt.event.KeyEvent evt) {
                btnUsuDeleteKeyPressed(evt);
            }
        });

        lblUsuCreate = new javax.swing.JLabel("Adicionar", javax.swing.SwingConstants.CENTER);
        lblUsuCreate.setFont(new java.awt.Font("Segoe UI", java.awt.Font.BOLD, 12));

        lblUsuConsulta = new javax.swing.JLabel("Consultar", javax.swing.SwingConstants.CENTER);
        lblUsuConsulta.setFont(new java.awt.Font("Segoe UI", java.awt.Font.BOLD, 12));

        lblUsuUpdate = new javax.swing.JLabel("Alterar", javax.swing.SwingConstants.CENTER);
        lblUsuUpdate.setFont(new java.awt.Font("Segoe UI", java.awt.Font.BOLD, 12));

        lblUsuDelete = new javax.swing.JLabel("Excluir", javax.swing.SwingConstants.CENTER);
        lblUsuDelete.setFont(new java.awt.Font("Segoe UI", java.awt.Font.BOLD, 12));

        jLabel7.setFont(new java.awt.Font("Segoe UI", 0, 14)); // NOI18N
        jLabel7.setText("* ID");

        btnUsuNovo.setText("Novo Usuário");
        btnUsuNovo.setToolTipText("Gerar Novo ID Automático");
        btnUsuNovo.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        btnUsuNovo.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnUsuNovoActionPerformed(evt);
            }
        });
        btnUsuNovo.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyPressed(java.awt.event.KeyEvent evt) {
                btnUsuNovoKeyPressed(evt);
            }
        });

        jLabel1.setText("* campos obrigatórios");

        try {
            txtUsuFone.setFormatterFactory(new javax.swing.text.DefaultFormatterFactory(
                    new javax.swing.text.MaskFormatter("(##) #####-####")));
        } catch (java.text.ParseException ex) {
            ex.printStackTrace();
        }
        txtUsuFone.setCursor(new java.awt.Cursor(java.awt.Cursor.TEXT_CURSOR));

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
                layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(layout.createSequentialGroup()
                                .addGap(25, 25, 25)
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                        .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 574,
                                                Short.MAX_VALUE)
                                        .addGroup(layout.createSequentialGroup()
                                                .addGroup(layout
                                                        .createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                                        .addComponent(jLabel8)
                                                        .addGroup(layout.createSequentialGroup()
                                                                .addComponent(txtUsuPesquisar,
                                                                        javax.swing.GroupLayout.PREFERRED_SIZE, 300,
                                                                        javax.swing.GroupLayout.PREFERRED_SIZE)
                                                                .addPreferredGap(
                                                                        javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                                .addComponent(lblIconPesquisar)))
                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED,
                                                        javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                                .addComponent(jLabel1))
                                        .addGroup(layout.createSequentialGroup()
                                                .addGap(15, 15, 15)
                                                .addGroup(layout
                                                        .createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                                                        .addComponent(jLabel7)
                                                        .addComponent(jLabel2)
                                                        .addComponent(jLabel6)
                                                        .addComponent(jLabel4))
                                                .addGap(18, 18, 18)
                                                .addGroup(layout
                                                        .createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                                        .addGroup(layout.createSequentialGroup()
                                                                .addComponent(txtUsuId,
                                                                        javax.swing.GroupLayout.PREFERRED_SIZE, 50,
                                                                        javax.swing.GroupLayout.PREFERRED_SIZE)
                                                                .addPreferredGap(
                                                                        javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                                .addComponent(btnUsuNovo,
                                                                        javax.swing.GroupLayout.PREFERRED_SIZE, 120,
                                                                        javax.swing.GroupLayout.PREFERRED_SIZE))
                                                        .addComponent(txtUsuNome, javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                420, Short.MAX_VALUE)
                                                        .addGroup(layout.createSequentialGroup()
                                                                .addGroup(layout.createParallelGroup(
                                                                        javax.swing.GroupLayout.Alignment.LEADING,
                                                                        false)
                                                                        .addComponent(txtUsuFone,
                                                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                130, Short.MAX_VALUE)
                                                                        .addComponent(txtUsuSenha))
                                                                .addGap(30, 30, 30)
                                                                .addGroup(layout.createParallelGroup(
                                                                        javax.swing.GroupLayout.Alignment.TRAILING)
                                                                        .addComponent(jLabel3)
                                                                        .addComponent(jLabel5))
                                                                .addGap(18, 18, 18)
                                                                .addGroup(layout.createParallelGroup(
                                                                        javax.swing.GroupLayout.Alignment.LEADING)
                                                                        .addComponent(txtUsuLogin)
                                                                        .addComponent(cboUsuPerfil, 0, 195,
                                                                                Short.MAX_VALUE)))
                                                        .addGroup(layout.createSequentialGroup()
                                                                .addGap(20, 20, 20)
                                                                .addGroup(layout.createParallelGroup(
                                                                        javax.swing.GroupLayout.Alignment.CENTER)
                                                                        .addComponent(btnUsuCreate,
                                                                                javax.swing.GroupLayout.PREFERRED_SIZE,
                                                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                javax.swing.GroupLayout.PREFERRED_SIZE)
                                                                        .addComponent(lblUsuCreate,
                                                                                javax.swing.GroupLayout.PREFERRED_SIZE,
                                                                                80,
                                                                                javax.swing.GroupLayout.PREFERRED_SIZE))
                                                                .addGap(30, 30, 30)
                                                                .addGroup(layout.createParallelGroup(
                                                                        javax.swing.GroupLayout.Alignment.CENTER)
                                                                        .addComponent(btnUsuConsulta,
                                                                                javax.swing.GroupLayout.PREFERRED_SIZE,
                                                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                javax.swing.GroupLayout.PREFERRED_SIZE)
                                                                        .addComponent(lblUsuConsulta,
                                                                                javax.swing.GroupLayout.PREFERRED_SIZE,
                                                                                80,
                                                                                javax.swing.GroupLayout.PREFERRED_SIZE))
                                                                .addGap(30, 30, 30)
                                                                .addGroup(layout.createParallelGroup(
                                                                        javax.swing.GroupLayout.Alignment.CENTER)
                                                                        .addComponent(btnUsuUpdate,
                                                                                javax.swing.GroupLayout.PREFERRED_SIZE,
                                                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                javax.swing.GroupLayout.PREFERRED_SIZE)
                                                                        .addComponent(lblUsuUpdate,
                                                                                javax.swing.GroupLayout.PREFERRED_SIZE,
                                                                                80,
                                                                                javax.swing.GroupLayout.PREFERRED_SIZE))
                                                                .addGap(30, 30, 30)
                                                                .addGroup(layout.createParallelGroup(
                                                                        javax.swing.GroupLayout.Alignment.CENTER)
                                                                        .addComponent(btnUsuDelete,
                                                                                javax.swing.GroupLayout.PREFERRED_SIZE,
                                                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                javax.swing.GroupLayout.PREFERRED_SIZE)
                                                                        .addComponent(lblUsuDelete,
                                                                                javax.swing.GroupLayout.PREFERRED_SIZE,
                                                                                80,
                                                                                javax.swing.GroupLayout.PREFERRED_SIZE))))))
                                .addGap(25, 25, 25)));
        layout.setVerticalGroup(
                layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(layout.createSequentialGroup()
                                .addGap(15, 15, 15)
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                                        .addGroup(layout.createSequentialGroup()
                                                .addComponent(jLabel8)
                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                .addGroup(layout
                                                        .createParallelGroup(javax.swing.GroupLayout.Alignment.CENTER)
                                                        .addComponent(txtUsuPesquisar,
                                                                javax.swing.GroupLayout.PREFERRED_SIZE,
                                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                javax.swing.GroupLayout.PREFERRED_SIZE)
                                                        .addComponent(lblIconPesquisar)))
                                        .addComponent(jLabel1))
                                .addGap(10, 10, 10)
                                .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 110,
                                        javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(15, 15, 15)
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                        .addComponent(jLabel7)
                                        .addComponent(txtUsuId, javax.swing.GroupLayout.PREFERRED_SIZE,
                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addComponent(btnUsuNovo, javax.swing.GroupLayout.PREFERRED_SIZE,
                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                javax.swing.GroupLayout.PREFERRED_SIZE))
                                .addGap(15, 15, 15)
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                        .addComponent(jLabel2)
                                        .addComponent(txtUsuNome, javax.swing.GroupLayout.PREFERRED_SIZE,
                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                javax.swing.GroupLayout.PREFERRED_SIZE))
                                .addGap(15, 15, 15)
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                        .addComponent(jLabel6)
                                        .addComponent(txtUsuFone, javax.swing.GroupLayout.PREFERRED_SIZE,
                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addComponent(jLabel3)
                                        .addComponent(txtUsuLogin, javax.swing.GroupLayout.PREFERRED_SIZE,
                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                javax.swing.GroupLayout.PREFERRED_SIZE))
                                .addGap(15, 15, 15)
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                        .addComponent(jLabel4)
                                        .addComponent(txtUsuSenha, javax.swing.GroupLayout.PREFERRED_SIZE,
                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addComponent(jLabel5)
                                        .addComponent(cboUsuPerfil, javax.swing.GroupLayout.PREFERRED_SIZE,
                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                javax.swing.GroupLayout.PREFERRED_SIZE))
                                .addGap(20, 20, 20)
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                        .addGroup(layout.createSequentialGroup()
                                                .addComponent(btnUsuCreate, javax.swing.GroupLayout.PREFERRED_SIZE,
                                                        javax.swing.GroupLayout.DEFAULT_SIZE,
                                                        javax.swing.GroupLayout.PREFERRED_SIZE)
                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                .addComponent(lblUsuCreate))
                                        .addGroup(layout.createSequentialGroup()
                                                .addComponent(btnUsuConsulta, javax.swing.GroupLayout.PREFERRED_SIZE,
                                                        javax.swing.GroupLayout.DEFAULT_SIZE,
                                                        javax.swing.GroupLayout.PREFERRED_SIZE)
                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                .addComponent(lblUsuConsulta))
                                        .addGroup(layout.createSequentialGroup()
                                                .addComponent(btnUsuUpdate, javax.swing.GroupLayout.PREFERRED_SIZE,
                                                        javax.swing.GroupLayout.DEFAULT_SIZE,
                                                        javax.swing.GroupLayout.PREFERRED_SIZE)
                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                .addComponent(lblUsuUpdate))
                                        .addGroup(layout.createSequentialGroup()
                                                .addComponent(btnUsuDelete, javax.swing.GroupLayout.PREFERRED_SIZE,
                                                        javax.swing.GroupLayout.DEFAULT_SIZE,
                                                        javax.swing.GroupLayout.PREFERRED_SIZE)
                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                .addComponent(lblUsuDelete)))
                                .addContainerGap(20, Short.MAX_VALUE)));

        setBounds(0, 0, 640, 530);
    }// </editor-fold>//GEN-END:initComponents

    private void txtUsuPesquisarKeyReleased(java.awt.event.KeyEvent evt) {
        pesquisarUsuario();
    }

    private void tblUsuariosMouseClicked(java.awt.event.MouseEvent evt) {
        setarCampos();
    }

    private void tblUsuariosKeyReleased(java.awt.event.KeyEvent evt) {
        setarCampos();
    }

    private void btnUsuDeleteActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_btnUsuDeleteActionPerformed
        remover();
    }// GEN-LAST:event_btnUsuDeleteActionPerformed

    private void btnUsuConsultaActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_btnUsuConsultaActionPerformed
        consultar();
    }// GEN-LAST:event_btnUsuConsultaActionPerformed

    private void btnUsuCreateActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_btnUsuCreateActionPerformed
        adicionar();
    }// GEN-LAST:event_btnUsuCreateActionPerformed

    private void btnUsuUpdateActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_btnUsuUpdateActionPerformed
        alterar();
    }// GEN-LAST:event_btnUsuUpdateActionPerformed

    private void txtUsuIdKeyPressed(java.awt.event.KeyEvent evt) {// GEN-FIRST:event_txtUsuIdKeyPressed
        if (evt.getKeyCode() == KeyEvent.VK_ENTER) {
            consultar();
        }

    }// GEN-LAST:event_txtUsuIdKeyPressed

    private void btnUsuCreateKeyPressed(java.awt.event.KeyEvent evt) {// GEN-FIRST:event_btnUsuCreateKeyPressed
        if (evt.getKeyCode() == KeyEvent.VK_ENTER) {
            adicionar();
        }
    }// GEN-LAST:event_btnUsuCreateKeyPressed

    private void btnUsuConsultaKeyPressed(java.awt.event.KeyEvent evt) {// GEN-FIRST:event_btnUsuConsultaKeyPressed
        if (evt.getKeyCode() == KeyEvent.VK_ENTER) {
            consultar();
        }
    }// GEN-LAST:event_btnUsuConsultaKeyPressed

    private void btnUsuUpdateKeyPressed(java.awt.event.KeyEvent evt) {// GEN-FIRST:event_btnUsuUpdateKeyPressed
        if (evt.getKeyCode() == KeyEvent.VK_ENTER) {
            alterar();
        }
    }// GEN-LAST:event_btnUsuUpdateKeyPressed

    private void btnUsuDeleteKeyPressed(java.awt.event.KeyEvent evt) {// GEN-FIRST:event_btnUsuDeleteKeyPressed
        if (evt.getKeyCode() == KeyEvent.VK_ENTER) {
            remover();
        }
    }// GEN-LAST:event_btnUsuDeleteKeyPressed

    private void btnUsuNovoActionPerformed(java.awt.event.ActionEvent evt) {
        novoUsuario();
    }

    private void btnUsuNovoKeyPressed(java.awt.event.KeyEvent evt) {
        if (evt.getKeyCode() == KeyEvent.VK_ENTER) {
            novoUsuario();
        }
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btnUsuConsulta;
    private javax.swing.JButton btnUsuCreate;
    private javax.swing.JButton btnUsuDelete;
    private javax.swing.JButton btnUsuNovo;
    private javax.swing.JButton btnUsuUpdate;
    private javax.swing.JComboBox<String> cboUsuPerfil;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JLabel lblIconPesquisar;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JTable tblUsuarios;
    private javax.swing.JTextField txtUsuPesquisar;
    private javax.swing.JLabel lblUsuConsulta;
    private javax.swing.JLabel lblUsuCreate;
    private javax.swing.JLabel lblUsuDelete;
    private javax.swing.JLabel lblUsuUpdate;
    private javax.swing.JFormattedTextField txtUsuFone;
    public static javax.swing.JTextField txtUsuId;
    private javax.swing.JTextField txtUsuLogin;
    private javax.swing.JTextField txtUsuNome;
    private javax.swing.JTextField txtUsuSenha;
    // End of variables declaration//GEN-END:variables
}
