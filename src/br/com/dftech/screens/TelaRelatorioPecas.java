/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package br.com.dftech.screens;

import java.sql.*;
import br.com.dftech.dal.Moduloconexao;
import java.awt.print.PrinterException;
import java.text.DecimalFormat;
import java.text.MessageFormat;
import javax.swing.JOptionPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.view.JasperViewer;

/**
 * Tela de Relatório de Peças em Estoque com filtro por nome e estatísticas.
 * 
 * @author dario
 */
public class TelaRelatorioPecas extends javax.swing.JInternalFrame {

    Connection conexao = null;
    PreparedStatement pst = null;
    ResultSet rs = null;

    private static final DecimalFormat df = new DecimalFormat("R$ #,##0.00");

    // Componentes Swing
    private javax.swing.JButton btnImprimir;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel lblTotalItens;
    private javax.swing.JLabel lblTotalQtd;
    private javax.swing.JLabel lblTotalValor;
    private javax.swing.JPanel panelResumo;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JTable tblRelatorio;
    private javax.swing.JTextField txtFiltroNome;

    public TelaRelatorioPecas() {
        initComponents();
        conexao = Moduloconexao.conector();
        carregarRelatorio();
    }

    private void carregarRelatorio() {
        if (conexao == null)
            return;

        String filtro = txtFiltroNome.getText().trim().toLowerCase();
        String sql = "SELECT id_peca, nome_peca, qtd_estoque, valor_peca, (qtd_estoque * valor_peca) AS total_item "
                + "FROM tbpecas WHERE lower(nome_peca) LIKE ? ORDER BY nome_peca ASC";

        DefaultTableModel model = new DefaultTableModel(
                new Object[][] {},
                new String[] { "ID", "Nome da Peça", "Qtd Estoque", "Valor Unitário (R$)", "Valor Total (R$)" }) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        int totalItens = 0;
        int totalQtd = 0;
        double totalValorAcumulado = 0.0;

        try {
            pst = conexao.prepareStatement(sql);
            pst.setString(1, "%" + filtro + "%");
            rs = pst.executeQuery();

            while (rs.next()) {
                int id = rs.getInt("id_peca");
                String nome = rs.getString("nome_peca");
                int qtd = rs.getInt("qtd_estoque");
                double valorUnit = rs.getDouble("valor_peca");
                double totalItem = rs.getDouble("total_item");

                totalItens++;
                totalQtd += qtd;
                totalValorAcumulado += totalItem;

                model.addRow(new Object[] {
                        id,
                        nome,
                        qtd,
                        df.format(valorUnit),
                        df.format(totalItem)
                });
            }

            tblRelatorio.setModel(model);
            tblRelatorio.setAutoResizeMode(javax.swing.JTable.AUTO_RESIZE_ALL_COLUMNS);

            // Ajustar largura e alinhamento das colunas da tabela
            javax.swing.table.DefaultTableCellRenderer centerRenderer = new javax.swing.table.DefaultTableCellRenderer();
            centerRenderer.setHorizontalAlignment(javax.swing.JLabel.CENTER);

            javax.swing.table.DefaultTableCellRenderer rightRenderer = new javax.swing.table.DefaultTableCellRenderer();
            rightRenderer.setHorizontalAlignment(javax.swing.JLabel.RIGHT);

            if (tblRelatorio.getColumnModel().getColumnCount() >= 5) {
                // Coluna ID (3 dígitos)
                tblRelatorio.getColumnModel().getColumn(0).setPreferredWidth(35);
                tblRelatorio.getColumnModel().getColumn(0).setMaxWidth(45);
                tblRelatorio.getColumnModel().getColumn(0).setCellRenderer(centerRenderer);

                // Coluna Nome da Peça
                tblRelatorio.getColumnModel().getColumn(1).setPreferredWidth(210);

                // Coluna Qtd Estoque (3 dígitos)
                tblRelatorio.getColumnModel().getColumn(2).setPreferredWidth(80);
                tblRelatorio.getColumnModel().getColumn(2).setCellRenderer(centerRenderer);

                // Coluna Valor Unitario (R$)
                tblRelatorio.getColumnModel().getColumn(3).setPreferredWidth(105);
                tblRelatorio.getColumnModel().getColumn(3).setCellRenderer(rightRenderer);

                // Coluna Total Item (R$)
                tblRelatorio.getColumnModel().getColumn(4).setPreferredWidth(105);
                tblRelatorio.getColumnModel().getColumn(4).setCellRenderer(rightRenderer);
            }

            lblTotalItens.setText("Tipos de Peça: " + totalItens);
            lblTotalQtd.setText("Qtd Total em Estoque: " + totalQtd);
            lblTotalValor.setText("Valor Total Acumulado: " + df.format(totalValorAcumulado));

        } catch (SQLException e) {
            JOptionPane.showMessageDialog(null, "Erro ao carregar relatório: " + e.getMessage());
        }
    }

    private void imprimir() {
        btnImprimir.setEnabled(false);
        final String filtro = txtFiltroNome.getText().trim();

        new javax.swing.SwingWorker<JasperPrint, Void>() {
            @Override
            protected JasperPrint doInBackground() throws Exception {
                net.sf.jasperreports.engine.util.JRProperties.setProperty("net.sf.jasperreports.compiler.class", "net.sf.jasperreports.engine.design.JRJavacCompiler");
                Map<String, Object> params = new HashMap<>();
                params.put("nome_filtro", filtro.isEmpty() ? null : filtro);

                File f1 = new File("reports/relatorio_pecas/relatorio_pecas.jasper");
                File f2 = new File("dist/reports/relatorio_pecas/relatorio_pecas.jasper");
                File f3 = new File("C:\\Users\\dario\\JaspersoftWorkspace\\relatorio_pecas\\relatorio_pecas.jasper");

                String jasperPath = null;
                if (f1.exists()) jasperPath = f1.getAbsolutePath();
                else if (f2.exists()) jasperPath = f2.getAbsolutePath();
                else if (f3.exists()) jasperPath = f3.getAbsolutePath();

                try (Connection conn = Moduloconexao.conector()) {
                    if (conn == null) throw new Exception("Não foi possível conectar ao banco de dados!");

                    if (jasperPath != null && new File(jasperPath).exists()) {
                        return JasperFillManager.fillReport(jasperPath, params, conn);
                    } else {
                        // Fallback com consulta direta
                        String sql = "SELECT id_peca, nome_peca, qtd_estoque, valor_peca, (qtd_estoque * valor_peca) AS total_item "
                                   + "FROM tbpecas WHERE lower(nome_peca) LIKE ? ORDER BY nome_peca ASC";
                        try (PreparedStatement ps = conn.prepareStatement(sql)) {
                            ps.setString(1, "%" + filtro.toLowerCase() + "%");
                            try (ResultSet r = ps.executeQuery()) {
                                net.sf.jasperreports.engine.JRResultSetDataSource ds = new net.sf.jasperreports.engine.JRResultSetDataSource(r);
                                File jrxml = new File("reports/relatorio_pecas/relatorio_pecas.jrxml");
                                if (jrxml.exists()) {
                                    net.sf.jasperreports.engine.JasperReport jr = net.sf.jasperreports.engine.JasperCompileManager.compileReport(jrxml.getAbsolutePath());
                                    return JasperFillManager.fillReport(jr, params, ds);
                                }
                                throw new Exception("Arquivo relatorio_pecas.jasper não foi localizado!");
                            }
                        }
                    }
                }
            }

            @Override
            protected void done() {
                btnImprimir.setEnabled(true);
                try {
                    JasperPrint print = get();
                    if (print != null) {
                        JasperViewer viewer = new JasperViewer(print, false);
                        viewer.setTitle("Relatório de Peças em Estoque");
                        viewer.setVisible(true);
                        viewer.toFront();
                    }
                } catch (Throwable t) {
                    t.printStackTrace();
                    Throwable cause = t;
                    while (cause.getCause() != null) cause = cause.getCause();
                    JOptionPane.showMessageDialog(null, "Erro ao gerar relatório:\n" + cause.getMessage(), "Erro no Relatório", JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }

    private void initComponents() {
        jLabel1 = new javax.swing.JLabel();
        txtFiltroNome = new javax.swing.JTextField();
        btnImprimir = new javax.swing.JButton();
        jScrollPane1 = new javax.swing.JScrollPane();
        tblRelatorio = new javax.swing.JTable();
        panelResumo = new javax.swing.JPanel();
        lblTotalItens = new javax.swing.JLabel();
        lblTotalQtd = new javax.swing.JLabel();
        lblTotalValor = new javax.swing.JLabel();

        setClosable(true);
        setIconifiable(true);
        setMaximizable(true);
        setResizable(true);
        setTitle("Relatório de Peças em Estoque");
        setMinimumSize(new java.awt.Dimension(640, 480));
        setPreferredSize(new java.awt.Dimension(640, 480));

        jLabel1.setFont(new java.awt.Font("Segoe UI", 1, 14));
        jLabel1.setText("Filtrar por nome:");

        txtFiltroNome.setFont(new java.awt.Font("Segoe UI", 0, 14));
        txtFiltroNome.addKeyListener(new java.awt.event.KeyAdapter() {
            @Override
            public void keyReleased(java.awt.event.KeyEvent evt) {
                carregarRelatorio();
            }
        });

        javax.swing.ImageIcon iconPrintRaw = new javax.swing.ImageIcon(
                getClass().getResource("/br/com/dftech/icons/print.png"));
        java.awt.Image imgPrintScaled = iconPrintRaw.getImage().getScaledInstance(16, 16,
                java.awt.Image.SCALE_SMOOTH);
        btnImprimir.setIcon(new javax.swing.ImageIcon(imgPrintScaled));
        btnImprimir.setFont(new java.awt.Font("Segoe UI", 0, 14));
        btnImprimir.setMargin(new java.awt.Insets(2, 10, 2, 10));
        btnImprimir.setText("Imprimir");
        btnImprimir.setToolTipText("Imprimir relatório");
        btnImprimir.addActionListener(new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                imprimir();
            }
        });

        tblRelatorio.setModel(new javax.swing.table.DefaultTableModel(
                new Object[][] {},
                new String[] {
                        "ID", "Nome da Peça", "Qtd Estoque", "Valor Unitário (R$)", "Valor Total (R$)"
                }));
        tblRelatorio.setRowHeight(22);
        jScrollPane1.setViewportView(tblRelatorio);

        panelResumo.setBorder(javax.swing.BorderFactory.createTitledBorder(
                javax.swing.BorderFactory.createEtchedBorder(), "Resumo do Estoque",
                javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION,
                javax.swing.border.TitledBorder.DEFAULT_POSITION,
                new java.awt.Font("Segoe UI", 1, 12)));

        lblTotalItens.setFont(new java.awt.Font("Segoe UI", 1, 13));
        lblTotalItens.setText("Tipos de Peça: 0");

        lblTotalQtd.setFont(new java.awt.Font("Segoe UI", 1, 13));
        lblTotalQtd.setForeground(new java.awt.Color(0, 102, 204));
        lblTotalQtd.setText("Qtd Total em Estoque: 0");

        lblTotalValor.setFont(new java.awt.Font("Segoe UI", 1, 13));
        lblTotalValor.setForeground(new java.awt.Color(0, 128, 0));
        lblTotalValor.setText("Valor Total Acumulado: R$ 0,00");

        javax.swing.GroupLayout panelResumoLayout = new javax.swing.GroupLayout(panelResumo);
        panelResumo.setLayout(panelResumoLayout);
        panelResumoLayout.setHorizontalGroup(
                panelResumoLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(panelResumoLayout.createSequentialGroup()
                                .addGap(10, 10, 10)
                                .addComponent(lblTotalItens, javax.swing.GroupLayout.PREFERRED_SIZE, 135,
                                        javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(15, 15, 15)
                                .addComponent(lblTotalQtd, javax.swing.GroupLayout.PREFERRED_SIZE, 175,
                                        javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(15, 15, 15)
                                .addComponent(lblTotalValor, javax.swing.GroupLayout.DEFAULT_SIZE, 200, Short.MAX_VALUE)
                                .addGap(10, 10, 10)));
        panelResumoLayout.setVerticalGroup(
                panelResumoLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(panelResumoLayout.createSequentialGroup()
                                .addGap(8, 8, 8)
                                .addGroup(panelResumoLayout
                                        .createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                        .addComponent(lblTotalItens)
                                        .addComponent(lblTotalQtd)
                                        .addComponent(lblTotalValor))
                                .addContainerGap(10, Short.MAX_VALUE)));

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);

        layout.setHorizontalGroup(
                layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(layout.createSequentialGroup()
                                .addGap(18, 18, 18)
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                        .addComponent(panelResumo, javax.swing.GroupLayout.DEFAULT_SIZE,
                                                javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                        .addComponent(jScrollPane1)
                                        .addGroup(layout.createSequentialGroup()
                                                .addComponent(jLabel1)
                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                                .addComponent(txtFiltroNome, javax.swing.GroupLayout.PREFERRED_SIZE,
                                                        240, javax.swing.GroupLayout.PREFERRED_SIZE)
                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                                .addComponent(btnImprimir)))
                                .addGap(18, 18, 18)));

        layout.setVerticalGroup(
                layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(layout.createSequentialGroup()
                                .addGap(18, 18, 18)
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                        .addComponent(jLabel1)
                                        .addComponent(txtFiltroNome, javax.swing.GroupLayout.PREFERRED_SIZE,
                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addComponent(btnImprimir))
                                .addGap(15, 15, 15)
                                .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 330, Short.MAX_VALUE)
                                .addGap(12, 12, 12)
                                .addComponent(panelResumo, javax.swing.GroupLayout.PREFERRED_SIZE,
                                        javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(15, 15, 15)));

        setBounds(0, 0, 640, 480);
    }
}
