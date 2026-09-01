/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JInternalFrame.java to edit this template
 */
package br.com.dftech.screens;

import java.sql.*;
import br.com.dftech.dal.Moduloconexao;
import br.com.dftech.utils.MascaraValor;
import java.awt.event.KeyEvent;
import java.util.HashMap;
import javax.swing.JFormattedTextField;
import javax.swing.JOptionPane;
import javax.swing.JTextField;
//import javax.swing.SwingConstants;
import net.proteanit.sql.DbUtils;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.view.JasperViewer;
import java.awt.print.Printable;
import java.awt.print.PrinterJob;
import java.awt.print.PageFormat;
import java.awt.print.PrinterException;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Color;
import java.awt.Font;

public class TelaOS extends javax.swing.JInternalFrame {

    Connection conexao = null;
    PreparedStatement pst = null;
    ResultSet rs = null;

    private String tipo;

    public TelaOS() {
        initComponents();
        conexao = Moduloconexao.conector();
        txtCliId.setHorizontalAlignment(JTextField.CENTER);
        txtOs.setHorizontalAlignment(JTextField.CENTER);
        MascaraValor.aplicar(txtOsValor);
        criarTabelaSeNaoExistir();
        carregarPecasEstoque();
    }

    private static class PecaItem {
        private final int idPeca;
        private final String nomePeca;
        private final int qtdEstoque;
        private final double valorPeca;

        public PecaItem(int idPeca, String nomePeca, int qtdEstoque, double valorPeca) {
            this.idPeca = idPeca;
            this.nomePeca = nomePeca;
            this.qtdEstoque = qtdEstoque;
            this.valorPeca = valorPeca;
        }

        public int getIdPeca() {
            return idPeca;
        }

        public String getNomePeca() {
            return nomePeca;
        }

        public int getQtdEstoque() {
            return qtdEstoque;
        }

        public double getValorPeca() {
            return valorPeca;
        }

        @Override
        public String toString() {
            return String.format("%s (R$ %.2f) - Est: %d", nomePeca, valorPeca, qtdEstoque);
        }
    }

    private void criarTabelaSeNaoExistir() {
        if (conexao == null)
            return;
        String sql = "CREATE TABLE IF NOT EXISTS tbos_pecas ("
                + "id SERIAL PRIMARY KEY, "
                + "os INT NOT NULL, "
                + "id_peca INT NOT NULL, "
                + "qtd INT NOT NULL, "
                + "valor_unit NUMERIC(10,2) NOT NULL"
                + ");";
        try (Statement st = conexao.createStatement()) {
            st.executeUpdate(sql);
        } catch (SQLException e) {
            System.err.println("Erro ao criar/verificar tabela tbos_pecas: " + e.getMessage());
        }
    }

    private void carregarPecasEstoque() {
        if (cboPecas == null)
            return;
        cboPecas.removeAllItems();
        if (conexao == null)
            return;
        String sql = "SELECT id_peca, nome_peca, qtd_estoque, valor_peca FROM tbpecas ORDER BY nome_peca ASC";
        try (PreparedStatement pstP = conexao.prepareStatement(sql);
                ResultSet rsP = pstP.executeQuery()) {
            while (rsP.next()) {
                int id = rsP.getInt("id_peca");
                String nome = rsP.getString("nome_peca");
                int qtd = rsP.getInt("qtd_estoque");
                double valor = rsP.getDouble("valor_peca");
                cboPecas.addItem(new PecaItem(id, nome, qtd, valor));
            }
        } catch (Exception e) {
            System.err.println("Erro ao carregar peças do estoque: " + e.getMessage());
        }
    }

    private void recalcularValorTotal() {
        if (tblOsPecas == null)
            return;
        javax.swing.table.DefaultTableModel dtm = (javax.swing.table.DefaultTableModel) tblOsPecas.getModel();
        double totalPecas = 0.0;
        for (int i = 0; i < dtm.getRowCount(); i++) {
            try {
                Object valObj = dtm.getValueAt(i, 4);
                if (valObj != null) {
                    totalPecas += Double.parseDouble(valObj.toString());
                }
            } catch (NumberFormatException e) {
                // Ignore formatting errors
            }
        }
        MascaraValor.setValor(txtOsValor, totalPecas);
    }

    private void adicionarPecaNaOs() {
        PecaItem item = (PecaItem) cboPecas.getSelectedItem();
        if (item == null) {
            JOptionPane.showMessageDialog(null, "Selecione uma peça do estoque!");
            return;
        }
        int qtdDesejada;
        try {
            qtdDesejada = Integer.parseInt(txtPecaQtd.getText().trim());
            if (qtdDesejada <= 0) {
                JOptionPane.showMessageDialog(null, "Informe uma quantidade maior que zero!");
                return;
            }
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(null, "Quantidade inválida!");
            return;
        }

        if (qtdDesejada > item.getQtdEstoque()) {
            JOptionPane.showMessageDialog(null, "Atenção: Estoque insuficiente! Disponível: " + item.getQtdEstoque());
            return;
        }

        javax.swing.table.DefaultTableModel dtm = (javax.swing.table.DefaultTableModel) tblOsPecas.getModel();
        boolean jaExiste = false;
        double subtotalAdicional = qtdDesejada * item.getValorPeca();

        for (int i = 0; i < dtm.getRowCount(); i++) {
            int idInTable = Integer.parseInt(dtm.getValueAt(i, 0).toString());
            if (idInTable == item.getIdPeca()) {
                int qtdAtual = Integer.parseInt(dtm.getValueAt(i, 2).toString());
                int novaQtd = qtdAtual + qtdDesejada;
                if (novaQtd > item.getQtdEstoque()) {
                    JOptionPane.showMessageDialog(null, "Atenção: A quantidade total solicitada (" + novaQtd
                            + ") excede o estoque disponível (" + item.getQtdEstoque() + ")!");
                    return;
                }
                double subtotalTotal = novaQtd * item.getValorPeca();
                dtm.setValueAt(novaQtd, i, 2);
                dtm.setValueAt(subtotalTotal, i, 4);
                jaExiste = true;
                break;
            }
        }

        if (!jaExiste) {
            dtm.addRow(new Object[] { item.getIdPeca(), item.getNomePeca(), qtdDesejada, item.getValorPeca(),
                    subtotalAdicional });
        }

        // Somar ao valor que já está no campo Valor Total
        double valorAtual = MascaraValor.getValor(txtOsValor);
        MascaraValor.setValor(txtOsValor, valorAtual + subtotalAdicional);

        txtPecaQtd.setText("1");
    }

    private void removerPecaDaOs() {
        int linhaSelecionada = tblOsPecas.getSelectedRow();
        if (linhaSelecionada >= 0) {
            javax.swing.table.DefaultTableModel dtm = (javax.swing.table.DefaultTableModel) tblOsPecas.getModel();
            double subtotalRemovido = 0.0;
            try {
                Object valObj = dtm.getValueAt(linhaSelecionada, 4);
                if (valObj != null) {
                    subtotalRemovido = Double.parseDouble(valObj.toString());
                }
            } catch (Exception e) {
                // ignore
            }

            dtm.removeRow(linhaSelecionada);

            // Subtrair do valor que já está no campo Valor Total
            double valorAtual = MascaraValor.getValor(txtOsValor);
            double novoValor = Math.max(0.0, valorAtual - subtotalRemovido);
            MascaraValor.setValor(txtOsValor, novoValor);
        } else {
            JOptionPane.showMessageDialog(null, "Selecione uma peça na tabela para remover!");
        }
    }

    private void salvarPecasOs(int osId) throws SQLException {
        javax.swing.table.DefaultTableModel dtm = (javax.swing.table.DefaultTableModel) tblOsPecas.getModel();
        String sqlInsert = "INSERT INTO tbos_pecas (os, id_peca, qtd, valor_unit) VALUES (?, ?, ?, ?)";
        String sqlUpdateEstoque = "UPDATE tbpecas SET qtd_estoque = qtd_estoque - ? WHERE id_peca = ?";

        for (int i = 0; i < dtm.getRowCount(); i++) {
            int idPeca = Integer.parseInt(dtm.getValueAt(i, 0).toString());
            int qtd = Integer.parseInt(dtm.getValueAt(i, 2).toString());
            double valorUnit = Double.parseDouble(dtm.getValueAt(i, 3).toString());

            try (PreparedStatement pstInsert = conexao.prepareStatement(sqlInsert)) {
                pstInsert.setInt(1, osId);
                pstInsert.setInt(2, idPeca);
                pstInsert.setInt(3, qtd);
                pstInsert.setDouble(4, valorUnit);
                pstInsert.executeUpdate();
            }

            try (PreparedStatement pstEstoque = conexao.prepareStatement(sqlUpdateEstoque)) {
                pstEstoque.setInt(1, qtd);
                pstEstoque.setInt(2, idPeca);
                pstEstoque.executeUpdate();
            }
        }
        carregarPecasEstoque();
    }

    private void restaurarEstoquePecasOs(int osId) {
        String sqlSelect = "SELECT id_peca, qtd FROM tbos_pecas WHERE os = ?";
        String sqlUpdateEstoque = "UPDATE tbpecas SET qtd_estoque = qtd_estoque + ? WHERE id_peca = ?";
        String sqlDelete = "DELETE FROM tbos_pecas WHERE os = ?";

        try {
            try (PreparedStatement pstSelect = conexao.prepareStatement(sqlSelect)) {
                pstSelect.setInt(1, osId);
                try (ResultSet rsPecas = pstSelect.executeQuery()) {
                    while (rsPecas.next()) {
                        int idPeca = rsPecas.getInt("id_peca");
                        int qtd = rsPecas.getInt("qtd");
                        try (PreparedStatement pstEstoque = conexao.prepareStatement(sqlUpdateEstoque)) {
                            pstEstoque.setInt(1, qtd);
                            pstEstoque.setInt(2, idPeca);
                            pstEstoque.executeUpdate();
                        }
                    }
                }
            }

            try (PreparedStatement pstDelete = conexao.prepareStatement(sqlDelete)) {
                pstDelete.setInt(1, osId);
                pstDelete.executeUpdate();
            }
        } catch (Exception e) {
            System.err.println("Erro ao restaurar estoque de peças da OS " + osId + ": " + e.getMessage());
        }
    }

    private void carregarPecasDaOs(int osId) {
        if (tblOsPecas == null)
            return;
        javax.swing.table.DefaultTableModel dtm = (javax.swing.table.DefaultTableModel) tblOsPecas.getModel();
        dtm.setRowCount(0);
        String sql = "SELECT op.id_peca, p.nome_peca, op.qtd, op.valor_unit, (op.qtd * op.valor_unit) AS subtotal "
                + "FROM tbos_pecas op JOIN tbpecas p ON op.id_peca = p.id_peca "
                + "WHERE op.os = ?";
        try (PreparedStatement pstPecas = conexao.prepareStatement(sql)) {
            pstPecas.setInt(1, osId);
            try (ResultSet rsPecas = pstPecas.executeQuery()) {
                while (rsPecas.next()) {
                    dtm.addRow(new Object[] {
                            rsPecas.getInt("id_peca"),
                            rsPecas.getString("nome_peca"),
                            rsPecas.getInt("qtd"),
                            rsPecas.getDouble("valor_unit"),
                            rsPecas.getDouble("subtotal")
                    });
                }
            }
        } catch (Exception e) {
            System.err.println("Erro ao carregar peças da OS: " + e.getMessage());
        }
    }

    private void limparCampos() {
        txtOs.setText(null);
        txtData.setText(null);
        txtCliId.setText(null);
        txtOsEquip.setText(null);
        txtOsDef.setText(null);
        txtOsServ.setText(null);
        txtOsTec.setText(null);
        txtOsValor.setText(null);
        if (tblOsPecas != null && tblOsPecas.getModel() instanceof javax.swing.table.DefaultTableModel) {
            ((javax.swing.table.DefaultTableModel) tblOsPecas.getModel()).setRowCount(0);
        }
        btnOsAdicionar.setEnabled(true);
        txtCliPesquisar.setEnabled(true);
        tblClientes.setVisible(true);
    }

    private void pesquisar_cliente() {
        String sql = "select id_cliente as Id, nome_cliente as Nome, fone_cliente as Fone from tbclientes where lower (nome_cliente) like ?";
        try {
            pst = conexao.prepareStatement(sql);
            pst.setString(1, txtCliPesquisar.getText().toLowerCase() + "%");
            rs = pst.executeQuery();
            tblClientes.setModel(DbUtils.resultSetToTableModel(rs));
            configurarLarguraColunasTabelaClientes();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, e);
        }

    }

    private void configurarLarguraColunasTabelaClientes() {
        if (tblClientes.getColumnCount() >= 3) {
            tblClientes.getColumnModel().getColumn(0).setPreferredWidth(50);
            tblClientes.getColumnModel().getColumn(0).setMaxWidth(60);
            tblClientes.getColumnModel().getColumn(0).setMinWidth(40);

            javax.swing.table.DefaultTableCellRenderer centerRenderer = new javax.swing.table.DefaultTableCellRenderer();
            centerRenderer.setHorizontalAlignment(javax.swing.JLabel.CENTER);
            tblClientes.getColumnModel().getColumn(0).setCellRenderer(centerRenderer);

            tblClientes.getColumnModel().getColumn(1).setPreferredWidth(250);
            tblClientes.getColumnModel().getColumn(2).setPreferredWidth(120);
        }
    }

    private void setar_campos() {
        int setar = tblClientes.getSelectedRow();
        txtCliId.setText(tblClientes.getModel().getValueAt(setar, 0).toString());
    }

    private void emitir_os() {
        String sql = "insert into tbos (data_os, equipamento, defeito, servico, tecnico, valor, id_cliente, tipo, situacao) values(?,?,?,?,?,?,?,?,?)";
        try {
            if ((txtCliId.getText().isEmpty()) || (txtOsEquip.getText().isEmpty()) || (txtOsDef.getText().isEmpty())) {
                JOptionPane.showMessageDialog(null, "Preencha todos os campos obrigatórios (*)");
                return;
            }
            int cliId;
            try {
                cliId = Integer.parseInt(txtCliId.getText());
            } catch (NumberFormatException e) {
                JOptionPane.showMessageDialog(null, "ID do cliente inválido!");
                return;
            }
            double valor = MascaraValor.getValor(txtOsValor);

            pst = conexao.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            Timestamp dataAtual = new java.sql.Timestamp(System.currentTimeMillis());
            pst.setTimestamp(1, dataAtual);
            pst.setString(2, txtOsEquip.getText());
            pst.setString(3, txtOsDef.getText());
            pst.setString(4, txtOsServ.getText());
            pst.setString(5, txtOsTec.getText());
            pst.setDouble(6, valor);
            pst.setInt(7, cliId);
            pst.setString(8, tipo);
            pst.setString(9, cboOsSit.getSelectedItem().toString());

            int adicionado = pst.executeUpdate();
            if (adicionado > 0) {
                String osGerada = null;
                try (ResultSet rsKeys = pst.getGeneratedKeys()) {
                    if (rsKeys.next()) {
                        osGerada = rsKeys.getString(1);
                    }
                } catch (Exception eKey) {
                    // Fallback
                }

                if (osGerada == null || osGerada.trim().isEmpty()) {
                    String sqlMax = "select max(os) from tbos";
                    try (PreparedStatement pstMax = conexao.prepareStatement(sqlMax);
                            ResultSet rsMax = pstMax.executeQuery()) {
                        if (rsMax.next()) {
                            osGerada = rsMax.getString(1);
                        }
                    }
                }

                txtOs.setText(osGerada);
                java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                txtData.setText(sdf.format(dataAtual));

                if (osGerada != null && !osGerada.trim().isEmpty()) {
                    try {
                        salvarPecasOs(Integer.parseInt(osGerada.trim()));
                    } catch (Exception exPecas) {
                        System.err.println("Erro ao salvar peças da OS: " + exPecas.getMessage());
                    }
                }

                btnOsAdicionar.setEnabled(false);
                txtCliPesquisar.setEnabled(false);
                tblClientes.setVisible(false);

                JOptionPane.showMessageDialog(null, "OS emitida com sucesso");
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, e);
        }
    }

    private void pesquisar_os() {
        String num_os = JOptionPane.showInputDialog("Número da OS");
        if (num_os == null || num_os.trim().isEmpty()) {
            return;
        }
        String sql = "select * from tbos where os = ?";
        try {
            pst = conexao.prepareStatement(sql);
            pst.setInt(1, Integer.parseInt(num_os.trim()));
            rs = pst.executeQuery();
            if (rs.next()) {
                txtOs.setText(rs.getString(1));
                Timestamp dataOs = rs.getTimestamp(2);
                if (dataOs != null) {
                    java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                    txtData.setText(sdf.format(dataOs));
                } else {
                    txtData.setText(rs.getString(2));
                }
                String rbtTipo = rs.getString(9);
                if (rbtTipo.equals("OS")) {
                    rbtOs.setSelected(true);
                    tipo = "OS";
                } else {
                    rbtOrc.setSelected(true);
                    tipo = "Orçamento";
                }
                cboOsSit.setSelectedItem(rs.getString(10));
                txtOsEquip.setText(rs.getString(3));
                txtOsDef.setText(rs.getString(4));
                txtOsServ.setText(rs.getString(5));
                txtOsTec.setText(rs.getString(6));
                double valOs = rs.getDouble(7);
                MascaraValor.setValor(txtOsValor, valOs);
                txtCliId.setText(rs.getString(8));
                btnOsAdicionar.setEnabled(false);
                txtCliPesquisar.setEnabled(false);
                tblClientes.setVisible(false);

                carregarPecasDaOs(Integer.parseInt(num_os.trim()));

            } else {
                JOptionPane.showMessageDialog(null, "OS não cadastrada");
            }
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(null, "Número de OS inválido!");
        } catch (Exception e2) {
            JOptionPane.showMessageDialog(null, e2);
        }
    }

    private void alterar_os() {
        if (txtOs.getText().isEmpty()) {
            JOptionPane.showMessageDialog(null, "Nenhuma OS carregada para alterar!");
            return;
        }
        if ((txtCliId.getText().isEmpty()) || (txtOsEquip.getText().isEmpty()) || (txtOsDef.getText().isEmpty())) {
            JOptionPane.showMessageDialog(null, "Preencha todos os campos obrigatórios (*)");
            return;
        }
        int osNum;
        try {
            osNum = Integer.parseInt(txtOs.getText());
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(null, "Número da OS inválido!");
            return;
        }
        restaurarEstoquePecasOs(osNum);
        double valor = MascaraValor.getValor(txtOsValor);
        String sql = "update tbos set equipamento=?, defeito=?, servico=?, tecnico=?, valor=?, tipo=?, situacao=? where os=?";
        try {
            pst = conexao.prepareStatement(sql);
            pst.setString(1, txtOsEquip.getText());
            pst.setString(2, txtOsDef.getText());
            pst.setString(3, txtOsServ.getText());
            pst.setString(4, txtOsTec.getText());
            pst.setDouble(5, valor);
            pst.setString(6, tipo);
            pst.setString(7, cboOsSit.getSelectedItem().toString());
            pst.setInt(8, osNum);

            int adicionado = pst.executeUpdate();
            if (adicionado > 0) {
                salvarPecasOs(osNum);
                JOptionPane.showMessageDialog(null, "OS alterada com sucesso");
                limparCampos();
                txtCliPesquisar.requestFocus();
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, e);
        }
    }

    private void excluir_os() {
        if (txtOs.getText().isEmpty()) {
            JOptionPane.showMessageDialog(null, "Nenhuma OS carregada para excluir!");
            return;
        }
        int osNum;
        try {
            osNum = Integer.parseInt(txtOs.getText());
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(null, "Número da OS inválido!");
            return;
        }
        int confirma = JOptionPane.showConfirmDialog(null, "Tem certeza que deseja excluir esta OS?", "Atenção",
                JOptionPane.YES_NO_OPTION);
        if (confirma == JOptionPane.YES_OPTION) {
            restaurarEstoquePecasOs(osNum);
            String sql = "delete from tbos where os=?";
            try {
                pst = conexao.prepareStatement(sql);
                pst.setInt(1, osNum);
                int apagado = pst.executeUpdate();
                if (apagado > 0) {
                    JOptionPane.showMessageDialog(null, "OS excluída com sucesso");
                    limparCampos();
                }
            } catch (Exception e) {
                JOptionPane.showMessageDialog(null, e);
            }
        }
    }

    private void imprimir_os() {
        if (txtOs.getText().isEmpty()) {
            JOptionPane.showMessageDialog(null, "Nenhuma OS carregada para imprimir!");
            return;
        }
        int confirma = JOptionPane.showConfirmDialog(null, "Confirma a visualização/impressão desta OS?", "Atenção",
                JOptionPane.YES_NO_OPTION);
        if (confirma == JOptionPane.YES_OPTION) {
            try {
                HashMap<String, Object> filtro = new HashMap<>();
                filtro.put("os", Integer.parseInt(txtOs.getText()));
                String jasperPath = Moduloconexao.obterCaminhoJasper("relatorio_os", "relatorio_os");
                JasperPrint print = JasperFillManager.fillReport(jasperPath, filtro, conexao);
                JasperViewer.viewReport(print, false);
            } catch (Exception e) {
                JOptionPane.showMessageDialog(null, e);
            }
        }
    }

    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated
    // Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        buttonGroup1 = new javax.swing.ButtonGroup();
        jPanel2 = new javax.swing.JPanel();
        jPanel1 = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        jLabel2 = new javax.swing.JLabel();
        txtOs = new javax.swing.JTextField();
        txtData = new javax.swing.JTextField();
        rbtOrc = new javax.swing.JRadioButton();
        rbtOs = new javax.swing.JRadioButton();
        jLabel3 = new javax.swing.JLabel();
        cboOsSit = new javax.swing.JComboBox<>();
        jPanel3 = new javax.swing.JPanel();
        txtCliPesquisar = new javax.swing.JTextField();
        jLabel4 = new javax.swing.JLabel();
        jLabel5 = new javax.swing.JLabel();
        txtCliId = new javax.swing.JTextField();
        jScrollPane1 = new javax.swing.JScrollPane();
        tblClientes = new javax.swing.JTable();
        jLabel6 = new javax.swing.JLabel();
        txtOsEquip = new javax.swing.JTextField();
        jLabel7 = new javax.swing.JLabel();
        txtOsDef = new javax.swing.JTextField();
        jLabel8 = new javax.swing.JLabel();
        txtOsServ = new javax.swing.JTextField();
        jLabel9 = new javax.swing.JLabel();
        txtOsTec = new javax.swing.JTextField();
        jLabel10 = new javax.swing.JLabel();
        txtOsValor = new javax.swing.JFormattedTextField();
        btnOsAdicionar = new javax.swing.JButton();
        btnOsPesquisar = new javax.swing.JButton();
        btnOsAlterar = new javax.swing.JButton();
        btnOsExluir = new javax.swing.JButton();
        btnOsImprimir = new javax.swing.JButton();

        jPanelPecas = new javax.swing.JPanel();
        jLabelPeca = new javax.swing.JLabel();
        cboPecas = new javax.swing.JComboBox<>();
        jLabelQtd = new javax.swing.JLabel();
        txtPecaQtd = new javax.swing.JTextField();
        btnAdicionarPeca = new javax.swing.JButton();
        btnRemoverPeca = new javax.swing.JButton();
        jScrollPaneOsPecas = new javax.swing.JScrollPane();
        tblOsPecas = new javax.swing.JTable();

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
                jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGap(0, 100, Short.MAX_VALUE));
        jPanel2Layout.setVerticalGroup(
                jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGap(0, 100, Short.MAX_VALUE));

        setClosable(true);
        setIconifiable(true);
        setMaximizable(true);
        setResizable(true);
        setTitle("Ordem de Serviços");
        setName("OS"); // NOI18N
        setMinimumSize(new java.awt.Dimension(640, 620));
        setPreferredSize(new java.awt.Dimension(640, 620));
        addInternalFrameListener(new javax.swing.event.InternalFrameListener() {
            public void internalFrameActivated(javax.swing.event.InternalFrameEvent evt) {
            }

            public void internalFrameClosed(javax.swing.event.InternalFrameEvent evt) {
            }

            public void internalFrameClosing(javax.swing.event.InternalFrameEvent evt) {
            }

            public void internalFrameDeactivated(javax.swing.event.InternalFrameEvent evt) {
            }

            public void internalFrameDeiconified(javax.swing.event.InternalFrameEvent evt) {
            }

            public void internalFrameIconified(javax.swing.event.InternalFrameEvent evt) {
            }

            public void internalFrameOpened(javax.swing.event.InternalFrameEvent evt) {
                formInternalFrameOpened(evt);
            }
        });

        jPanel1.setBorder(javax.swing.BorderFactory.createEtchedBorder());

        jLabel1.setText("Nº OS");

        jLabel2.setText("Data");

        txtOs.setEditable(false);
        txtOs.setFont(new java.awt.Font("Segoe UI", 0, 14)); // NOI18N
        txtOs.setDisabledTextColor(new java.awt.Color(51, 51, 51));
        txtOs.setEnabled(false);

        txtData.setEditable(false);
        txtData.setFont(new java.awt.Font("Segoe UI", 0, 14)); // NOI18N
        txtData.setDisabledTextColor(new java.awt.Color(0, 0, 0));
        txtData.setEnabled(false);

        buttonGroup1.add(rbtOrc);
        rbtOrc.setText("Orçamento");
        rbtOrc.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                rbtOrcActionPerformed(evt);
            }
        });

        buttonGroup1.add(rbtOs);
        rbtOs.setText("OS");
        rbtOs.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                rbtOsActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
                jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(jPanel1Layout.createSequentialGroup()
                                .addContainerGap()
                                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                        .addGroup(jPanel1Layout.createSequentialGroup()
                                                .addComponent(rbtOrc)
                                                .addGap(27, 27, 27)
                                                .addComponent(rbtOs)
                                                .addGap(0, 0, Short.MAX_VALUE))
                                        .addGroup(jPanel1Layout.createSequentialGroup()
                                                .addGroup(jPanel1Layout
                                                        .createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                                        .addComponent(jLabel1)
                                                        .addComponent(txtOs, javax.swing.GroupLayout.PREFERRED_SIZE, 56,
                                                                javax.swing.GroupLayout.PREFERRED_SIZE))
                                                .addGap(18, 18, 18)
                                                .addGroup(jPanel1Layout
                                                        .createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                                        .addGroup(jPanel1Layout.createSequentialGroup()
                                                                .addComponent(jLabel2)
                                                                .addGap(0, 0, Short.MAX_VALUE))
                                                        .addComponent(txtData))))
                                .addContainerGap()));
        jPanel1Layout.setVerticalGroup(
                jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(jPanel1Layout.createSequentialGroup()
                                .addContainerGap()
                                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                        .addComponent(jLabel1)
                                        .addComponent(jLabel2))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                        .addComponent(txtOs, javax.swing.GroupLayout.PREFERRED_SIZE,
                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addComponent(txtData, javax.swing.GroupLayout.PREFERRED_SIZE,
                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                javax.swing.GroupLayout.PREFERRED_SIZE))
                                .addGap(18, 18, 18)
                                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                        .addComponent(rbtOrc)
                                        .addComponent(rbtOs))
                                .addContainerGap(34, Short.MAX_VALUE)));

        jLabel3.setText("Situação");

        cboOsSit.setModel(new javax.swing.DefaultComboBoxModel<>(
                new String[] { "Na bancada", "Entrega OK", "Orçamento APROVADO", "Orçamento REPROVADO",
                        "Aguardando Aprovação", "Aguardando peças", "Abandonado pelo cliente", "Retornou" }));

        jPanel3.setBorder(javax.swing.BorderFactory.createTitledBorder("Cliente"));

        txtCliPesquisar.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyPressed(java.awt.event.KeyEvent evt) {
                txtCliPesquisarKeyPressed(evt);
            }

            public void keyReleased(java.awt.event.KeyEvent evt) {
                txtCliPesquisarKeyReleased(evt);
            }
        });

        jLabel4.setIcon(new javax.swing.ImageIcon(getClass().getResource("/br/com/dftech/icons/pesquisar.png"))); // NOI18N

        jLabel5.setText("* Id");

        txtCliId.setFont(new java.awt.Font("Segoe UI", 0, 14)); // NOI18N
        txtCliId.setDisabledTextColor(new java.awt.Color(0, 0, 0));
        txtCliId.setEnabled(false);

        tblClientes.setModel(new javax.swing.table.DefaultTableModel(
                new Object[][] {
                        { null, null, null },
                        { null, null, null },
                        { null, null, null },
                        { null, null, null }
                },
                new String[] {
                        "Id", "Nome", "Fone"
                }));
        tblClientes.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                tblClientesMouseClicked(evt);
            }
        });
        tblClientes.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyPressed(java.awt.event.KeyEvent evt) {
                tblClientesKeyPressed(evt);
            }
        });
        jScrollPane1.setViewportView(tblClientes);

        javax.swing.GroupLayout jPanel3Layout = new javax.swing.GroupLayout(jPanel3);
        jPanel3.setLayout(jPanel3Layout);
        jPanel3Layout.setHorizontalGroup(
                jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(jPanel3Layout.createSequentialGroup()
                                .addContainerGap()
                                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                        .addGroup(jPanel3Layout.createSequentialGroup()
                                                .addComponent(txtCliPesquisar)
                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                                .addComponent(jLabel4)
                                                .addGap(18, 18, 18)
                                                .addComponent(jLabel5)
                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                .addComponent(txtCliId, javax.swing.GroupLayout.PREFERRED_SIZE, 48,
                                                        javax.swing.GroupLayout.PREFERRED_SIZE))
                                        .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 358,
                                                Short.MAX_VALUE))
                                .addContainerGap()));
        jPanel3Layout.setVerticalGroup(
                jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(jPanel3Layout.createSequentialGroup()
                                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                        .addComponent(jLabel4)
                                        .addGroup(jPanel3Layout.createSequentialGroup()
                                                .addContainerGap()
                                                .addGroup(jPanel3Layout
                                                        .createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                                        .addGroup(jPanel3Layout
                                                                .createParallelGroup(
                                                                        javax.swing.GroupLayout.Alignment.BASELINE)
                                                                .addComponent(txtCliId,
                                                                        javax.swing.GroupLayout.PREFERRED_SIZE,
                                                                        javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                        javax.swing.GroupLayout.PREFERRED_SIZE)
                                                                .addComponent(jLabel5))
                                                        .addComponent(txtCliPesquisar,
                                                                javax.swing.GroupLayout.PREFERRED_SIZE,
                                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                javax.swing.GroupLayout.PREFERRED_SIZE))))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 102, Short.MAX_VALUE)
                                .addContainerGap()));

        jLabel6.setText("* Equipamento");

        jLabel7.setText("* Defeito");

        jLabel8.setText("Serviço");

        jLabel9.setText("Técnico");

        jLabel10.setText("Valor Total");

        txtOsValor.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                txtOsValorActionPerformed(evt);
            }
        });

        btnOsAdicionar
                .setIcon(new javax.swing.ImageIcon(getClass().getResource("/br/com/dftech/icons/btnAdicionar.png"))); // NOI18N
        btnOsAdicionar.setToolTipText("Adicionar OS");
        btnOsAdicionar.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        btnOsAdicionar.setPreferredSize(new java.awt.Dimension(80, 80));
        btnOsAdicionar.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnOsAdicionarActionPerformed(evt);
            }
        });
        btnOsAdicionar.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyPressed(java.awt.event.KeyEvent evt) {
                btnOsAdicionarKeyPressed(evt);
            }
        });

        btnOsPesquisar.setIcon(new javax.swing.ImageIcon(getClass().getResource("/br/com/dftech/icons/read.png"))); // NOI18N
        btnOsPesquisar.setToolTipText("Pesquisar OS");
        btnOsPesquisar.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        btnOsPesquisar.setPreferredSize(new java.awt.Dimension(80, 80));
        btnOsPesquisar.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnOsPesquisarActionPerformed(evt);
            }
        });
        btnOsPesquisar.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyPressed(java.awt.event.KeyEvent evt) {
                btnOsPesquisarKeyPressed(evt);
            }
        });

        btnOsAlterar.setIcon(new javax.swing.ImageIcon(getClass().getResource("/br/com/dftech/icons/update.png"))); // NOI18N
        btnOsAlterar.setToolTipText("Alterar OS");
        btnOsAlterar.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        btnOsAlterar.setPreferredSize(new java.awt.Dimension(80, 80));
        btnOsAlterar.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnOsAlterarActionPerformed(evt);
            }
        });
        btnOsAlterar.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyPressed(java.awt.event.KeyEvent evt) {
                btnOsAlterarKeyPressed(evt);
            }
        });

        btnOsExluir.setIcon(new javax.swing.ImageIcon(getClass().getResource("/br/com/dftech/icons/delete.png"))); // NOI18N
        btnOsExluir.setToolTipText("Excluir OS");
        btnOsExluir.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        btnOsExluir.setPreferredSize(new java.awt.Dimension(80, 80));
        btnOsExluir.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnOsExluirActionPerformed(evt);
            }
        });
        btnOsExluir.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyPressed(java.awt.event.KeyEvent evt) {
                btnOsExluirKeyPressed(evt);
            }
        });

        btnOsImprimir.setIcon(new javax.swing.ImageIcon(getClass().getResource("/br/com/dftech/icons/print.png"))); // NOI18N
        btnOsImprimir.setToolTipText("Imprimir OS");
        btnOsImprimir.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        btnOsImprimir.setPreferredSize(new java.awt.Dimension(80, 80));
        btnOsImprimir.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnOsImprimirActionPerformed(evt);
            }
        });
        btnOsImprimir.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyPressed(java.awt.event.KeyEvent evt) {
                btnOsImprimirKeyPressed(evt);
            }
        });

        lblOsAdicionar = new javax.swing.JLabel("Adicionar", javax.swing.SwingConstants.CENTER);
        lblOsAdicionar.setFont(new java.awt.Font("Segoe UI", java.awt.Font.BOLD, 12));

        lblOsPesquisar = new javax.swing.JLabel("Pesquisar", javax.swing.SwingConstants.CENTER);
        lblOsPesquisar.setFont(new java.awt.Font("Segoe UI", java.awt.Font.BOLD, 12));

        lblOsAlterar = new javax.swing.JLabel("Alterar", javax.swing.SwingConstants.CENTER);
        lblOsAlterar.setFont(new java.awt.Font("Segoe UI", java.awt.Font.BOLD, 12));

        lblOsExcluir = new javax.swing.JLabel("Excluir", javax.swing.SwingConstants.CENTER);
        lblOsExcluir.setFont(new java.awt.Font("Segoe UI", java.awt.Font.BOLD, 12));

        lblOsImprimir = new javax.swing.JLabel("Imprimir", javax.swing.SwingConstants.CENTER);
        lblOsImprimir.setFont(new java.awt.Font("Segoe UI", java.awt.Font.BOLD, 12));

        jPanelPecas.setBorder(javax.swing.BorderFactory.createTitledBorder("Peças Utilizadas do Estoque"));

        jLabelPeca.setText("Peça:");
        jLabelQtd.setText("Qtd:");

        txtPecaQtd.setText("1");
        txtPecaQtd.setHorizontalAlignment(javax.swing.JTextField.CENTER);

        btnAdicionarPeca.setText("Adicionar Peça");
        btnAdicionarPeca.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnAdicionarPecaActionPerformed(evt);
            }
        });

        btnRemoverPeca.setText("Remover Peça");
        btnRemoverPeca.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnRemoverPecaActionPerformed(evt);
            }
        });

        tblOsPecas.setModel(new javax.swing.table.DefaultTableModel(
                new Object[][] {},
                new String[] {
                        "Id Peça", "Peça", "Qtd", "Valor Unit. (R$)", "Subtotal (R$)"
                }) {
            boolean[] canEdit = new boolean[] {
                    false, false, false, false, false
            };

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit[columnIndex];
            }
        });
        jScrollPaneOsPecas.setViewportView(tblOsPecas);

        javax.swing.GroupLayout jPanelPecasLayout = new javax.swing.GroupLayout(jPanelPecas);
        jPanelPecas.setLayout(jPanelPecasLayout);
        jPanelPecasLayout.setHorizontalGroup(
                jPanelPecasLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(jPanelPecasLayout.createSequentialGroup()
                                .addContainerGap()
                                .addGroup(jPanelPecasLayout
                                        .createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                        .addComponent(jScrollPaneOsPecas)
                                        .addGroup(jPanelPecasLayout.createSequentialGroup()
                                                .addComponent(jLabelPeca)
                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                .addComponent(cboPecas, 0, 240, Short.MAX_VALUE)
                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                                .addComponent(jLabelQtd)
                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                .addComponent(txtPecaQtd, javax.swing.GroupLayout.PREFERRED_SIZE, 45,
                                                        javax.swing.GroupLayout.PREFERRED_SIZE)
                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                                .addComponent(btnAdicionarPeca)
                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                .addComponent(btnRemoverPeca)))
                                .addContainerGap()));
        jPanelPecasLayout.setVerticalGroup(
                jPanelPecasLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(jPanelPecasLayout.createSequentialGroup()
                                .addContainerGap()
                                .addGroup(jPanelPecasLayout
                                        .createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                        .addComponent(jLabelPeca)
                                        .addComponent(cboPecas, javax.swing.GroupLayout.PREFERRED_SIZE,
                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addComponent(jLabelQtd)
                                        .addComponent(txtPecaQtd, javax.swing.GroupLayout.PREFERRED_SIZE,
                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addComponent(btnAdicionarPeca)
                                        .addComponent(btnRemoverPeca))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jScrollPaneOsPecas, javax.swing.GroupLayout.PREFERRED_SIZE, 110,
                                        javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)));

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
                layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(layout.createSequentialGroup()
                                .addContainerGap()
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                        .addComponent(jPanelPecas, javax.swing.GroupLayout.DEFAULT_SIZE,
                                                javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                        .addGroup(layout.createSequentialGroup()
                                                .addGroup(layout
                                                        .createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING,
                                                                false)
                                                        .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                                        .addGroup(layout.createSequentialGroup()
                                                                .addComponent(jLabel3)
                                                                .addGap(18, 18, 18)
                                                                .addComponent(cboOsSit, 0,
                                                                        javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                        Short.MAX_VALUE)))
                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                .addComponent(jPanel3, javax.swing.GroupLayout.DEFAULT_SIZE,
                                                        javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                                        .addGroup(layout.createSequentialGroup()
                                                .addGroup(layout
                                                        .createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                                        .addGroup(layout.createSequentialGroup()
                                                                .addGap(33, 33, 33)
                                                                .addGroup(layout.createParallelGroup(
                                                                        javax.swing.GroupLayout.Alignment.TRAILING)
                                                                        .addComponent(jLabel7)
                                                                        .addGroup(layout.createSequentialGroup()
                                                                                .addComponent(jLabel8)
                                                                                .addGap(4, 4, 4))))
                                                        .addComponent(jLabel6)
                                                        .addComponent(jLabel9,
                                                                javax.swing.GroupLayout.Alignment.TRAILING))
                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                .addGroup(layout
                                                        .createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                                        .addGroup(layout.createSequentialGroup()
                                                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.CENTER)
                                                                        .addComponent(btnOsAdicionar,
                                                                                javax.swing.GroupLayout.PREFERRED_SIZE,
                                                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                javax.swing.GroupLayout.PREFERRED_SIZE)
                                                                        .addComponent(lblOsAdicionar,
                                                                                javax.swing.GroupLayout.PREFERRED_SIZE, 80,
                                                                                javax.swing.GroupLayout.PREFERRED_SIZE))
                                                                .addPreferredGap(
                                                                        javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.CENTER)
                                                                        .addComponent(btnOsPesquisar,
                                                                                javax.swing.GroupLayout.PREFERRED_SIZE,
                                                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                javax.swing.GroupLayout.PREFERRED_SIZE)
                                                                        .addComponent(lblOsPesquisar,
                                                                                javax.swing.GroupLayout.PREFERRED_SIZE, 80,
                                                                                javax.swing.GroupLayout.PREFERRED_SIZE))
                                                                .addPreferredGap(
                                                                        javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.CENTER)
                                                                        .addComponent(btnOsAlterar,
                                                                                javax.swing.GroupLayout.PREFERRED_SIZE,
                                                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                javax.swing.GroupLayout.PREFERRED_SIZE)
                                                                        .addComponent(lblOsAlterar,
                                                                                javax.swing.GroupLayout.PREFERRED_SIZE, 80,
                                                                                javax.swing.GroupLayout.PREFERRED_SIZE))
                                                                .addPreferredGap(
                                                                        javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.CENTER)
                                                                        .addComponent(btnOsExluir,
                                                                                javax.swing.GroupLayout.PREFERRED_SIZE,
                                                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                javax.swing.GroupLayout.PREFERRED_SIZE)
                                                                        .addComponent(lblOsExcluir,
                                                                                javax.swing.GroupLayout.PREFERRED_SIZE, 80,
                                                                                javax.swing.GroupLayout.PREFERRED_SIZE))
                                                                .addPreferredGap(
                                                                        javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.CENTER)
                                                                        .addComponent(btnOsImprimir,
                                                                                javax.swing.GroupLayout.PREFERRED_SIZE,
                                                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                javax.swing.GroupLayout.PREFERRED_SIZE)
                                                                        .addComponent(lblOsImprimir,
                                                                                javax.swing.GroupLayout.PREFERRED_SIZE, 80,
                                                                                javax.swing.GroupLayout.PREFERRED_SIZE))
                                                                .addGap(0, 0, Short.MAX_VALUE))
                                                        .addGroup(layout.createSequentialGroup()
                                                                .addGroup(layout.createParallelGroup(
                                                                        javax.swing.GroupLayout.Alignment.TRAILING)
                                                                        .addComponent(txtOsServ,
                                                                                javax.swing.GroupLayout.Alignment.LEADING)
                                                                        .addComponent(txtOsEquip,
                                                                                javax.swing.GroupLayout.Alignment.LEADING)
                                                                        .addGroup(layout.createSequentialGroup()
                                                                                .addComponent(txtOsTec,
                                                                                        javax.swing.GroupLayout.PREFERRED_SIZE,
                                                                                        304,
                                                                                        javax.swing.GroupLayout.PREFERRED_SIZE)
                                                                                .addGap(37, 37, 37)
                                                                                .addComponent(jLabel10)
                                                                                .addPreferredGap(
                                                                                        javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                                                .addComponent(txtOsValor))
                                                                        .addComponent(txtOsDef))
                                                                .addGap(11, 11, 11)))))
                                .addContainerGap()));
        layout.setVerticalGroup(
                layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(layout.createSequentialGroup()
                                .addContainerGap()
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                                        .addComponent(jPanel3, javax.swing.GroupLayout.PREFERRED_SIZE,
                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addGroup(layout.createSequentialGroup()
                                                .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE,
                                                        javax.swing.GroupLayout.DEFAULT_SIZE,
                                                        javax.swing.GroupLayout.PREFERRED_SIZE)
                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                .addGroup(layout
                                                        .createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                                        .addComponent(jLabel3)
                                                        .addComponent(cboOsSit, javax.swing.GroupLayout.PREFERRED_SIZE,
                                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                javax.swing.GroupLayout.PREFERRED_SIZE))
                                                .addGap(12, 12, 12)))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                        .addComponent(txtOsEquip, javax.swing.GroupLayout.PREFERRED_SIZE,
                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addComponent(jLabel6))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                        .addComponent(txtOsDef, javax.swing.GroupLayout.PREFERRED_SIZE,
                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addComponent(jLabel7))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                        .addComponent(txtOsServ, javax.swing.GroupLayout.PREFERRED_SIZE,
                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addComponent(jLabel8))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                        .addComponent(txtOsTec, javax.swing.GroupLayout.PREFERRED_SIZE,
                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addComponent(jLabel10)
                                        .addComponent(txtOsValor, javax.swing.GroupLayout.PREFERRED_SIZE,
                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addComponent(jLabel9))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(jPanelPecas, javax.swing.GroupLayout.PREFERRED_SIZE,
                                        javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                        .addGroup(layout.createSequentialGroup()
                                                .addComponent(btnOsAdicionar, javax.swing.GroupLayout.PREFERRED_SIZE,
                                                        javax.swing.GroupLayout.DEFAULT_SIZE,
                                                        javax.swing.GroupLayout.PREFERRED_SIZE)
                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                .addComponent(lblOsAdicionar))
                                        .addGroup(layout.createSequentialGroup()
                                                .addComponent(btnOsPesquisar, javax.swing.GroupLayout.PREFERRED_SIZE,
                                                        javax.swing.GroupLayout.DEFAULT_SIZE,
                                                        javax.swing.GroupLayout.PREFERRED_SIZE)
                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                .addComponent(lblOsPesquisar))
                                        .addGroup(layout.createSequentialGroup()
                                                .addComponent(btnOsAlterar, javax.swing.GroupLayout.PREFERRED_SIZE,
                                                        javax.swing.GroupLayout.DEFAULT_SIZE,
                                                        javax.swing.GroupLayout.PREFERRED_SIZE)
                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                .addComponent(lblOsAlterar))
                                        .addGroup(layout.createSequentialGroup()
                                                .addComponent(btnOsExluir, javax.swing.GroupLayout.PREFERRED_SIZE,
                                                        javax.swing.GroupLayout.DEFAULT_SIZE,
                                                        javax.swing.GroupLayout.PREFERRED_SIZE)
                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                .addComponent(lblOsExcluir))
                                        .addGroup(layout.createSequentialGroup()
                                                .addComponent(btnOsImprimir, javax.swing.GroupLayout.PREFERRED_SIZE,
                                                        javax.swing.GroupLayout.DEFAULT_SIZE,
                                                        javax.swing.GroupLayout.PREFERRED_SIZE)
                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                .addComponent(lblOsImprimir)))
                                .addContainerGap(15, Short.MAX_VALUE)));

        setBounds(0, 0, 640, 640);
    }// </editor-fold>//GEN-END:initComponents

    private void txtOsValorActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_txtOsValorActionPerformed
        // TODO add your handling code here:
    }// GEN-LAST:event_txtOsValorActionPerformed

    private void txtCliPesquisarKeyReleased(java.awt.event.KeyEvent evt) {// GEN-FIRST:event_txtCliPesquisarKeyReleased
        pesquisar_cliente();
    }// GEN-LAST:event_txtCliPesquisarKeyReleased

    private void tblClientesMouseClicked(java.awt.event.MouseEvent evt) {// GEN-FIRST:event_tblClientesMouseClicked
        setar_campos();
        txtOsEquip.requestFocus();
    }// GEN-LAST:event_tblClientesMouseClicked

    private void rbtOrcActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_rbtOrcActionPerformed
        tipo = "Orçamento";
    }// GEN-LAST:event_rbtOrcActionPerformed

    private void rbtOsActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_rbtOsActionPerformed
        tipo = "OS";
    }// GEN-LAST:event_rbtOsActionPerformed

    private void formInternalFrameOpened(javax.swing.event.InternalFrameEvent evt) {// GEN-FIRST:event_formInternalFrameOpened
        rbtOrc.setSelected(true);
        tipo = "Orçamento";
    }// GEN-LAST:event_formInternalFrameOpened

    private void btnOsAdicionarActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_btnOsAdicionarActionPerformed
        emitir_os();
    }// GEN-LAST:event_btnOsAdicionarActionPerformed

    private void btnOsPesquisarActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_btnOsPesquisarActionPerformed
        pesquisar_os();
    }// GEN-LAST:event_btnOsPesquisarActionPerformed

    private void btnOsAdicionarKeyPressed(java.awt.event.KeyEvent evt) {// GEN-FIRST:event_btnOsAdicionarKeyPressed
        if (evt.getKeyCode() == KeyEvent.VK_ENTER) {
            emitir_os();
        }
    }// GEN-LAST:event_btnOsAdicionarKeyPressed

    private void btnOsPesquisarKeyPressed(java.awt.event.KeyEvent evt) {// GEN-FIRST:event_btnOsPesquisarKeyPressed
        if (evt.getKeyCode() == KeyEvent.VK_ENTER) {
            pesquisar_os();
        }
    }// GEN-LAST:event_btnOsPesquisarKeyPressed

    private void tblClientesKeyPressed(java.awt.event.KeyEvent evt) {// GEN-FIRST:event_tblClientesKeyPressed
        if (evt.getKeyCode() == KeyEvent.VK_ENTER) {
            setar_campos();
            txtOsEquip.requestFocus();
        }
    }// GEN-LAST:event_tblClientesKeyPressed

    private void btnOsAlterarActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_btnOsAlterarActionPerformed
        alterar_os();
    }// GEN-LAST:event_btnOsAlterarActionPerformed

    private void btnOsAlterarKeyPressed(java.awt.event.KeyEvent evt) {// GEN-FIRST:event_btnOsAlterarKeyPressed
        if (evt.getKeyCode() == KeyEvent.VK_ENTER) {
            alterar_os();
        }
    }// GEN-LAST:event_btnOsAlterarKeyPressed

    private void btnOsExluirActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_btnOsExluirActionPerformed
        excluir_os();
    }// GEN-LAST:event_btnOsExluirActionPerformed

    private void btnOsExluirKeyPressed(java.awt.event.KeyEvent evt) {// GEN-FIRST:event_btnOsExluirKeyPressed
        if (evt.getKeyCode() == KeyEvent.VK_ENTER) {
            excluir_os();
            txtCliPesquisar.requestFocus();
        }
    }// GEN-LAST:event_btnOsExluirKeyPressed

    private void txtCliPesquisarKeyPressed(java.awt.event.KeyEvent evt) {// GEN-FIRST:event_txtCliPesquisarKeyPressed
        if (evt.getKeyCode() == KeyEvent.VK_ENTER) {
            tblClientes.requestFocus();
        }
    }// GEN-LAST:event_txtCliPesquisarKeyPressed

    private void btnOsImprimirActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_btnOsImprimirActionPerformed
        imprimir_os();
    }// GEN-LAST:event_btnOsImprimirActionPerformed

    private void btnOsImprimirKeyPressed(java.awt.event.KeyEvent evt) {// GEN-FIRST:event_btnOsImprimirKeyPressed
        if (evt.getKeyCode() == KeyEvent.VK_ENTER) {
            imprimir_os();
        }
    }// GEN-LAST:event_btnOsImprimirKeyPressed

    private void btnAdicionarPecaActionPerformed(java.awt.event.ActionEvent evt) {
        adicionarPecaNaOs();
    }

    private void btnRemoverPecaActionPerformed(java.awt.event.ActionEvent evt) {
        removerPecaDaOs();
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btnAdicionarPeca;
    private javax.swing.JButton btnOsAdicionar;
    private javax.swing.JButton btnOsAlterar;
    private javax.swing.JButton btnOsExluir;
    private javax.swing.JButton btnOsImprimir;
    private javax.swing.JButton btnOsPesquisar;
    private javax.swing.JButton btnRemoverPeca;
    private javax.swing.ButtonGroup buttonGroup1;
    private javax.swing.JComboBox<PecaItem> cboPecas;
    private javax.swing.JComboBox<String> cboOsSit;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel10;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JLabel jLabel9;
    private javax.swing.JLabel jLabelPeca;
    private javax.swing.JLabel jLabelQtd;
    private javax.swing.JLabel lblOsAdicionar;
    private javax.swing.JLabel lblOsPesquisar;
    private javax.swing.JLabel lblOsAlterar;
    private javax.swing.JLabel lblOsExcluir;
    private javax.swing.JLabel lblOsImprimir;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JPanel jPanelPecas;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPaneOsPecas;
    private javax.swing.JRadioButton rbtOrc;
    private javax.swing.JRadioButton rbtOs;
    private javax.swing.JTable tblClientes;
    private javax.swing.JTable tblOsPecas;
    private javax.swing.JTextField txtCliId;
    public static javax.swing.JTextField txtCliPesquisar;
    private javax.swing.JTextField txtData;
    private javax.swing.JTextField txtOs;
    private javax.swing.JTextField txtOsDef;
    private javax.swing.JTextField txtOsEquip;
    private javax.swing.JTextField txtOsServ;
    private javax.swing.JTextField txtOsTec;
    private javax.swing.JFormattedTextField txtOsValor;
    private javax.swing.JTextField txtPecaQtd;
    // End of variables declaration//GEN-END:variables
}
