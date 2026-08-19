/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package br.com.dftech.screens;

import java.sql.*;
import br.com.dftech.dal.Moduloconexao;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JTextField;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableModel;
import net.proteanit.sql.DbUtils;

/**
 * Tela de Cadastro de Peças com foto e quantidade em estoque.
 * 
 * @author dario
 */
public class TelaPeca extends javax.swing.JInternalFrame {

    Connection conexao = null;
    PreparedStatement pst = null;
    ResultSet rs = null;

    private byte[] bytesFoto = null;

    // Declaração de componentes Swing
    private javax.swing.JButton btnPecaAdicionar;
    private javax.swing.JButton btnPecaAlterar;
    private javax.swing.JButton btnPecaRemover;
    private javax.swing.JButton btnPecaLimpar;
    private javax.swing.JButton btnCarregarFoto;
    private javax.swing.JButton btnRemoverFoto;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JLabel lblFoto;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JTable tblPecas;
    private javax.swing.JTextField txtPecaId;
    private javax.swing.JTextField txtPecaNome;
    private javax.swing.JTextField txtPecaQtd;
    private javax.swing.JTextField txtPecaValor;
    public static javax.swing.JTextField txtPecaPesquisar;

    public TelaPeca() {
        initComponents();
        conexao = Moduloconexao.conector();
        criarTabelaSeNaoExistir();
        txtPecaId.setHorizontalAlignment(JTextField.CENTER);
        txtPecaQtd.setHorizontalAlignment(JTextField.CENTER);
    }

    private void criarTabelaSeNaoExistir() {
        if (conexao == null) return;
        String sql = "CREATE TABLE IF NOT EXISTS tbpecas ("
                   + "id_peca SERIAL PRIMARY KEY, "
                   + "nome_peca VARCHAR(100) NOT NULL, "
                   + "qtd_estoque INT NOT NULL DEFAULT 0, "
                   + "valor_peca NUMERIC(10,2) DEFAULT 0.00, "
                   + "foto_peca BYTEA"
                   + ");";
        try (Statement st = conexao.createStatement()) {
            st.executeUpdate(sql);
        } catch (SQLException e) {
            System.err.println("Erro ao criar/verificar tabela tbpecas: " + e.getMessage());
        }
    }

    private void carregarFoto() {
        JFileChooser chooser = new JFileChooser();
        FileNameExtensionFilter filter = new FileNameExtensionFilter("Imagens (JPG, PNG, GIF, BMP)", "jpg", "jpeg", "png", "gif", "bmp");
        chooser.setFileFilter(filter);
        int res = chooser.showOpenDialog(this);
        if (res == JFileChooser.APPROVE_OPTION) {
            File file = chooser.getSelectedFile();
            try (FileInputStream fis = new FileInputStream(file)) {
                bytesFoto = new byte[(int) file.length()];
                fis.read(bytesFoto);
                exibirImagem(bytesFoto);
            } catch (Exception e) {
                JOptionPane.showMessageDialog(null, "Erro ao carregar a imagem: " + e.getMessage());
            }
        }
    }

    private void removerFoto() {
        bytesFoto = null;
        lblFoto.setIcon(null);
        lblFoto.setText("Sem Foto");
    }

    private void exibirImagem(byte[] imgBytes) {
        if (imgBytes != null && imgBytes.length > 0) {
            try {
                ByteArrayInputStream bais = new ByteArrayInputStream(imgBytes);
                BufferedImage img = ImageIO.read(bais);
                if (img != null) {
                    int w = lblFoto.getWidth() > 0 ? lblFoto.getWidth() : 140;
                    int h = lblFoto.getHeight() > 0 ? lblFoto.getHeight() : 140;
                    Image scaledImg = img.getScaledInstance(w, h, Image.SCALE_SMOOTH);
                    lblFoto.setIcon(new ImageIcon(scaledImg));
                    lblFoto.setText(null);
                } else {
                    lblFoto.setIcon(null);
                    lblFoto.setText("Foto inválida");
                }
            } catch (Exception e) {
                lblFoto.setIcon(null);
                lblFoto.setText("Erro foto");
            }
        } else {
            lblFoto.setIcon(null);
            lblFoto.setText("Sem Foto");
        }
    }

    private void adicionar() {
        String sql = "insert into tbpecas(nome_peca, qtd_estoque, valor_peca, foto_peca) values(?,?,?,?)";
        try {
            if (txtPecaNome.getText().trim().isEmpty() || txtPecaQtd.getText().trim().isEmpty()) {
                JOptionPane.showMessageDialog(null, "Preencha todos os campos obrigatórios (*)");
                return;
            }

            int qtd;
            try {
                qtd = Integer.parseInt(txtPecaQtd.getText().trim());
            } catch (NumberFormatException e) {
                JOptionPane.showMessageDialog(null, "A Quantidade em Estoque deve ser um número inteiro!");
                return;
            }

            double valor = 0.0;
            if (!txtPecaValor.getText().trim().isEmpty()) {
                try {
                    valor = Double.parseDouble(txtPecaValor.getText().trim().replace(",", "."));
                } catch (NumberFormatException e) {
                    JOptionPane.showMessageDialog(null, "O Valor deve ser um número válido (ex: 15.50)!");
                    return;
                }
            }

            pst = conexao.prepareStatement(sql);
            pst.setString(1, txtPecaNome.getText().trim());
            pst.setInt(2, qtd);
            pst.setDouble(3, valor);
            pst.setBytes(4, bytesFoto);

            int adicionado = pst.executeUpdate();
            if (adicionado > 0) {
                JOptionPane.showMessageDialog(null, "Peça cadastrada com sucesso!");
                limpar();
            }

        } catch (SQLException e) {
            JOptionPane.showMessageDialog(null, e.getMessage());
        }
    }

    private void pesquisarPeca() {
        String sql = "select id_peca as id, nome_peca as peça, qtd_estoque as quantidade, valor_peca as valor from tbpecas where lower(nome_peca) like ? order by id_peca";
        try {
            pst = conexao.prepareStatement(sql);
            pst.setString(1, txtPecaPesquisar.getText().toLowerCase() + "%");
            rs = pst.executeQuery();
            tblPecas.setModel(DbUtils.resultSetToTableModel(rs));
            
            tblPecas.getSelectionModel().addListSelectionListener(new javax.swing.event.ListSelectionListener() {
                @Override
                public void valueChanged(javax.swing.event.ListSelectionEvent e) {
                    if (!e.getValueIsAdjusting() && tblPecas.getSelectedRow() >= 0) {
                        setarCampo();
                    }
                }
            });
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(null, e.getMessage());
        }

        if (txtPecaPesquisar.getText().isEmpty()) {
            limpar();
        }
    }

    public void setarCampo() {
        int setar = tblPecas.getSelectedRow();
        if (setar < 0) return;

        txtPecaId.setText(tblPecas.getModel().getValueAt(setar, 0).toString());
        txtPecaNome.setText(tblPecas.getModel().getValueAt(setar, 1).toString());
        txtPecaQtd.setText(tblPecas.getModel().getValueAt(setar, 2) != null ? tblPecas.getModel().getValueAt(setar, 2).toString() : "0");
        txtPecaValor.setText(tblPecas.getModel().getValueAt(setar, 3) != null ? tblPecas.getModel().getValueAt(setar, 3).toString() : "0.00");
        
        btnPecaAdicionar.setEnabled(false);

        // Buscar foto da peça
        String sql = "select foto_peca from tbpecas where id_peca=?";
        try {
            int pecaId = Integer.parseInt(txtPecaId.getText());
            pst = conexao.prepareStatement(sql);
            pst.setInt(1, pecaId);
            rs = pst.executeQuery();
            if (rs.next()) {
                bytesFoto = rs.getBytes("foto_peca");
                exibirImagem(bytesFoto);
            } else {
                removerFoto();
            }
        } catch (Exception e) {
            removerFoto();
        }
    }

    private void alterar() {
        if (txtPecaId.getText().isEmpty()) {
            JOptionPane.showMessageDialog(null, "Selecione uma peça da tabela para alterar!");
            return;
        }

        int pecaId;
        try {
            pecaId = Integer.parseInt(txtPecaId.getText());
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(null, "O ID deve ser um número inteiro!");
            return;
        }

        if (txtPecaNome.getText().trim().isEmpty() || txtPecaQtd.getText().trim().isEmpty()) {
            JOptionPane.showMessageDialog(null, "Preencha todos os campos obrigatórios (*)");
            return;
        }

        int qtd;
        try {
            qtd = Integer.parseInt(txtPecaQtd.getText().trim());
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(null, "A Quantidade em Estoque deve ser um número inteiro!");
            return;
        }

        double valor = 0.0;
        if (!txtPecaValor.getText().trim().isEmpty()) {
            try {
                valor = Double.parseDouble(txtPecaValor.getText().trim().replace(",", "."));
            } catch (NumberFormatException e) {
                JOptionPane.showMessageDialog(null, "O Valor deve ser um número válido (ex: 15.50)!");
                return;
            }
        }

        String sql = "update tbpecas set nome_peca=?, qtd_estoque=?, valor_peca=?, foto_peca=? where id_peca=?";
        try {
            pst = conexao.prepareStatement(sql);
            pst.setString(1, txtPecaNome.getText().trim());
            pst.setInt(2, qtd);
            pst.setDouble(3, valor);
            pst.setBytes(4, bytesFoto);
            pst.setInt(5, pecaId);

            int alterado = pst.executeUpdate();
            if (alterado > 0) {
                JOptionPane.showMessageDialog(null, "Dados da peça alterados com sucesso!");
                limpar();
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(null, e.getMessage());
        }
    }

    private void remover() {
        if (txtPecaId.getText().isEmpty()) {
            JOptionPane.showMessageDialog(null, "Selecione uma peça da tabela para remover!");
            return;
        }

        int pecaId;
        try {
            pecaId = Integer.parseInt(txtPecaId.getText());
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(null, "O ID deve ser um número inteiro!");
            return;
        }

        int confirma = JOptionPane.showConfirmDialog(null, "Tem certeza que deseja remover esta peça?", "Atenção", JOptionPane.YES_NO_OPTION);
        if (confirma == JOptionPane.YES_OPTION) {
            String sql = "delete from tbpecas where id_peca=?";
            try {
                pst = conexao.prepareStatement(sql);
                pst.setInt(1, pecaId);
                int apagado = pst.executeUpdate();
                if (apagado > 0) {
                    JOptionPane.showMessageDialog(null, "Peça removida com sucesso!");
                    limpar();
                }
            } catch (SQLException e) {
                JOptionPane.showMessageDialog(null, e.getMessage());
            }
        }
    }

    private void limpar() {
        txtPecaPesquisar.setText(null);
        txtPecaId.setText(null);
        txtPecaNome.setText(null);
        txtPecaQtd.setText(null);
        txtPecaValor.setText(null);
        removerFoto();
        btnPecaAdicionar.setEnabled(true);
        txtPecaPesquisar.requestFocus();
        if (tblPecas.getModel() instanceof DefaultTableModel) {
            ((DefaultTableModel) tblPecas.getModel()).setRowCount(0);
        } else {
            tblPecas.setModel(new DefaultTableModel(
                new Object[][] {},
                new String[] {"ID", "Peça", "Quantidade", "Valor"}
            ));
        }
    }

    private void initComponents() {
        txtPecaPesquisar = new javax.swing.JTextField();
        jLabel6 = new javax.swing.JLabel();
        jScrollPane1 = new javax.swing.JScrollPane();
        tblPecas = new javax.swing.JTable();
        
        jLabel1 = new javax.swing.JLabel();
        jLabel7 = new javax.swing.JLabel();
        txtPecaId = new javax.swing.JTextField();
        jLabel2 = new javax.swing.JLabel();
        txtPecaNome = new javax.swing.JTextField();
        jLabel3 = new javax.swing.JLabel();
        txtPecaQtd = new javax.swing.JTextField();
        jLabel4 = new javax.swing.JLabel();
        txtPecaValor = new javax.swing.JTextField();

        lblFoto = new javax.swing.JLabel();
        btnCarregarFoto = new javax.swing.JButton();
        btnRemoverFoto = new javax.swing.JButton();

        btnPecaAdicionar = new javax.swing.JButton();
        btnPecaAlterar = new javax.swing.JButton();
        btnPecaRemover = new javax.swing.JButton();
        btnPecaLimpar = new javax.swing.JButton();

        setClosable(true);
        setIconifiable(true);
        setMaximizable(true);
        setResizable(true);
        setTitle("Peças");
        setMinimumSize(new java.awt.Dimension(640, 480));
        setPreferredSize(new java.awt.Dimension(640, 480));

        txtPecaPesquisar.addKeyListener(new java.awt.event.KeyAdapter() {
            @Override
            public void keyReleased(java.awt.event.KeyEvent evt) {
                pesquisarPeca();
            }
            @Override
            public void keyPressed(java.awt.event.KeyEvent evt) {
                if (evt.getKeyCode() == java.awt.event.KeyEvent.VK_ENTER) {
                    txtPecaNome.requestFocus();
                } else if (evt.getKeyCode() == java.awt.event.KeyEvent.VK_DOWN || evt.getKeyCode() == java.awt.event.KeyEvent.VK_UP) {
                    if (tblPecas.getRowCount() > 0) {
                        tblPecas.requestFocus();
                        if (tblPecas.getSelectedRow() < 0) {
                            tblPecas.setRowSelectionInterval(0, 0);
                        }
                    }
                }
            }
        });

        jLabel6.setIcon(new javax.swing.ImageIcon(getClass().getResource("/br/com/dftech/icons/pesquisar.png")));

        tblPecas.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null}
            },
            new String [] {
                "ID", "Peça", "Quantidade", "Valor (R$)"
            }
        ));
        tblPecas.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                setarCampo();
            }
        });
        tblPecas.addKeyListener(new java.awt.event.KeyAdapter() {
            @Override
            public void keyReleased(java.awt.event.KeyEvent evt) {
                if (evt.getKeyCode() == java.awt.event.KeyEvent.VK_ENTER) {
                    txtPecaNome.requestFocus();
                }
            }
        });
        tblPecas.getSelectionModel().addListSelectionListener(new javax.swing.event.ListSelectionListener() {
            @Override
            public void valueChanged(javax.swing.event.ListSelectionEvent e) {
                if (!e.getValueIsAdjusting() && tblPecas.getSelectedRow() >= 0) {
                    setarCampo();
                }
            }
        });
        jScrollPane1.setViewportView(tblPecas);

        jLabel1.setText("* Campos obrigatórios");

        jLabel7.setFont(new java.awt.Font("Segoe UI", 0, 14));
        jLabel7.setText("ID");

        txtPecaId.setEditable(false);

        jLabel2.setFont(new java.awt.Font("Segoe UI", 0, 14));
        jLabel2.setText("*Nome da Peça");

        txtPecaNome.addKeyListener(new java.awt.event.KeyAdapter() {
            @Override
            public void keyPressed(java.awt.event.KeyEvent evt) {
                if (evt.getKeyCode() == java.awt.event.KeyEvent.VK_ENTER) {
                    txtPecaQtd.requestFocus();
                }
            }
        });

        jLabel3.setFont(new java.awt.Font("Segoe UI", 0, 14));
        jLabel3.setText("*Qtd Estoque");

        txtPecaQtd.addKeyListener(new java.awt.event.KeyAdapter() {
            @Override
            public void keyPressed(java.awt.event.KeyEvent evt) {
                if (evt.getKeyCode() == java.awt.event.KeyEvent.VK_ENTER) {
                    txtPecaValor.requestFocus();
                }
            }
        });

        jLabel4.setFont(new java.awt.Font("Segoe UI", 0, 14));
        jLabel4.setText("Valor (R$)");

        txtPecaValor.addKeyListener(new java.awt.event.KeyAdapter() {
            @Override
            public void keyPressed(java.awt.event.KeyEvent evt) {
                if (evt.getKeyCode() == java.awt.event.KeyEvent.VK_ENTER) {
                    if (btnPecaAdicionar.isEnabled()) {
                        btnPecaAdicionar.requestFocus();
                    } else {
                        btnPecaAlterar.requestFocus();
                    }
                }
            }
        });

        lblFoto.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        lblFoto.setText("Sem Foto");
        lblFoto.setBorder(javax.swing.BorderFactory.createEtchedBorder());

        btnCarregarFoto.setText("Foto...");
        btnCarregarFoto.addActionListener(new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                carregarFoto();
            }
        });

        btnRemoverFoto.setText("Remover");
        btnRemoverFoto.addActionListener(new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                removerFoto();
            }
        });

        btnPecaAdicionar.setIcon(new javax.swing.ImageIcon(getClass().getResource("/br/com/dftech/icons/create.png")));
        btnPecaAdicionar.setToolTipText("Adicionar Peça");
        btnPecaAdicionar.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        btnPecaAdicionar.setPreferredSize(new java.awt.Dimension(70, 70));
        btnPecaAdicionar.addActionListener(new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                adicionar();
            }
        });
        btnPecaAdicionar.addKeyListener(new java.awt.event.KeyAdapter() {
            @Override
            public void keyPressed(java.awt.event.KeyEvent evt) {
                if (evt.getKeyCode() == java.awt.event.KeyEvent.VK_ENTER) {
                    adicionar();
                }
            }
        });

        btnPecaAlterar.setIcon(new javax.swing.ImageIcon(getClass().getResource("/br/com/dftech/icons/update.png")));
        btnPecaAlterar.setToolTipText("Alterar Peça");
        btnPecaAlterar.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        btnPecaAlterar.setPreferredSize(new java.awt.Dimension(70, 70));
        btnPecaAlterar.addActionListener(new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                alterar();
            }
        });
        btnPecaAlterar.addKeyListener(new java.awt.event.KeyAdapter() {
            @Override
            public void keyPressed(java.awt.event.KeyEvent evt) {
                if (evt.getKeyCode() == java.awt.event.KeyEvent.VK_ENTER) {
                    alterar();
                }
            }
        });

        btnPecaRemover.setIcon(new javax.swing.ImageIcon(getClass().getResource("/br/com/dftech/icons/delete.png")));
        btnPecaRemover.setToolTipText("Remover Peça");
        btnPecaRemover.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        btnPecaRemover.setPreferredSize(new java.awt.Dimension(70, 70));
        btnPecaRemover.addActionListener(new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                remover();
            }
        });
        btnPecaRemover.addKeyListener(new java.awt.event.KeyAdapter() {
            @Override
            public void keyPressed(java.awt.event.KeyEvent evt) {
                if (evt.getKeyCode() == java.awt.event.KeyEvent.VK_ENTER) {
                    remover();
                }
            }
        });

        btnPecaLimpar.setIcon(new javax.swing.ImageIcon(getClass().getResource("/br/com/dftech/icons/read.png")));
        btnPecaLimpar.setToolTipText("Limpar Campos");
        btnPecaLimpar.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        btnPecaLimpar.setPreferredSize(new java.awt.Dimension(70, 70));
        btnPecaLimpar.addActionListener(new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                limpar();
            }
        });
        btnPecaLimpar.addKeyListener(new java.awt.event.KeyAdapter() {
            @Override
            public void keyPressed(java.awt.event.KeyEvent evt) {
                if (evt.getKeyCode() == java.awt.event.KeyEvent.VK_ENTER) {
                    limpar();
                }
            }
        });

        // Layout de Grupos (GroupLayout) combinando com as telas existentes
        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);

        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGap(20, 20, 20)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(txtPecaPesquisar, javax.swing.GroupLayout.PREFERRED_SIZE, 280, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jLabel6)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(jLabel1))
                    .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 584, Short.MAX_VALUE)
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(layout.createSequentialGroup()
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(jLabel7)
                                    .addComponent(jLabel2)
                                    .addComponent(jLabel3)
                                    .addComponent(jLabel4))
                                .addGap(15, 15, 15)
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                    .addComponent(txtPecaId, javax.swing.GroupLayout.PREFERRED_SIZE, 50, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addComponent(txtPecaNome, javax.swing.GroupLayout.DEFAULT_SIZE, 260, Short.MAX_VALUE)
                                    .addComponent(txtPecaQtd, javax.swing.GroupLayout.PREFERRED_SIZE, 50, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addComponent(txtPecaValor, javax.swing.GroupLayout.PREFERRED_SIZE, 120, javax.swing.GroupLayout.PREFERRED_SIZE)))
                            .addGroup(layout.createSequentialGroup()
                                .addGap(30, 30, 30)
                                .addComponent(btnPecaAdicionar, javax.swing.GroupLayout.PREFERRED_SIZE, 70, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(20, 20, 20)
                                .addComponent(btnPecaAlterar, javax.swing.GroupLayout.PREFERRED_SIZE, 70, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(20, 20, 20)
                                .addComponent(btnPecaRemover, javax.swing.GroupLayout.PREFERRED_SIZE, 70, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(20, 20, 20)
                                .addComponent(btnPecaLimpar, javax.swing.GroupLayout.PREFERRED_SIZE, 70, javax.swing.GroupLayout.PREFERRED_SIZE)))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 30, Short.MAX_VALUE)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.CENTER)
                            .addComponent(lblFoto, javax.swing.GroupLayout.PREFERRED_SIZE, 140, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addGroup(layout.createSequentialGroup()
                                .addComponent(btnCarregarFoto)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(btnRemoverFoto)))
                        .addGap(15, 15, 15)))
                .addGap(20, 20, 20))
        );

        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGap(15, 15, 15)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.CENTER)
                    .addComponent(txtPecaPesquisar, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel6)
                    .addComponent(jLabel1))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 130, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(15, 15, 15)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel7)
                            .addComponent(txtPecaId, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel2)
                            .addComponent(txtPecaNome, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel3)
                            .addComponent(txtPecaQtd, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel4)
                            .addComponent(txtPecaValor, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGap(20, 20, 20)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(btnPecaAdicionar, javax.swing.GroupLayout.PREFERRED_SIZE, 70, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(btnPecaAlterar, javax.swing.GroupLayout.PREFERRED_SIZE, 70, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(btnPecaRemover, javax.swing.GroupLayout.PREFERRED_SIZE, 70, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(btnPecaLimpar, javax.swing.GroupLayout.PREFERRED_SIZE, 70, javax.swing.GroupLayout.PREFERRED_SIZE)))
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(lblFoto, javax.swing.GroupLayout.PREFERRED_SIZE, 140, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(btnCarregarFoto)
                            .addComponent(btnRemoverFoto))))
                .addContainerGap(20, Short.MAX_VALUE))
        );

        setBounds(0, 0, 640, 480);
    }
}
