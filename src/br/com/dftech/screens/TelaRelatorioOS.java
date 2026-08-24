package br.com.dftech.screens;

import java.sql.*;
import br.com.dftech.dal.Moduloconexao;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import javax.swing.JOptionPane;
import javax.swing.table.DefaultTableModel;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.view.JasperViewer;

/**
 * Tela de Relatório de Ordens de Serviço com filtro por nome de cliente e estatísticas.
 * 
 * @author dario
 */
public class TelaRelatorioOS extends javax.swing.JInternalFrame {

    Connection conexao = null;
    PreparedStatement pst = null;
    ResultSet rs = null;

    private static final DecimalFormat df = new DecimalFormat("R$ #,##0.00");
    private static final SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm");

    // Componentes Swing
    private javax.swing.JButton btnImprimir;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel lblTotalOS;
    private javax.swing.JLabel lblTotalTipos;
    private javax.swing.JLabel lblTotalValor;
    private javax.swing.JPanel panelResumo;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JTable tblRelatorio;
    private javax.swing.JTextField txtFiltroCliente;

    public TelaRelatorioOS() {
        initComponents();
        conexao = Moduloconexao.conector();
        carregarRelatorio();
    }

    private void carregarRelatorio() {
        if (conexao == null)
            return;

        String filtro = txtFiltroCliente.getText().trim().toLowerCase();
        String sql = "SELECT O.os, O.data_os, C.nome_cliente, O.tipo, O.situacao, O.equipamento, O.servico, O.tecnico, O.valor "
                + "FROM tbos O INNER JOIN tbclientes C ON O.id_cliente = C.id_cliente "
                + "WHERE lower(C.nome_cliente) LIKE ? ORDER BY O.os DESC";

        DefaultTableModel model = new DefaultTableModel(
                new Object[][] {},
                new String[] { "Nº OS", "Data/Hora", "Cliente", "Tipo", "Situação", "Equipamento", "Serviço", "Técnico", "Valor (R$)" }) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        int totalOS = 0;
        int qtdOrcamento = 0;
        int qtdOS = 0;
        double totalValorAcumulado = 0.0;

        try {
            pst = conexao.prepareStatement(sql);
            pst.setString(1, "%" + filtro + "%");
            rs = pst.executeQuery();

            while (rs.next()) {
                int osNum = rs.getInt("os");
                Timestamp dataOs = rs.getTimestamp("data_os");
                String dataStr = (dataOs != null) ? sdf.format(dataOs) : "";
                String cliente = rs.getString("nome_cliente");
                String tipoStr = rs.getString("tipo");
                String situacao = rs.getString("situacao");
                String equip = rs.getString("equipamento");
                String servico = rs.getString("servico");
                String tecnico = rs.getString("tecnico");
                double valor = rs.getDouble("valor");

                totalOS++;
                if ("Orçamento".equalsIgnoreCase(tipoStr)) {
                    qtdOrcamento++;
                } else {
                    qtdOS++;
                }
                totalValorAcumulado += valor;

                model.addRow(new Object[] {
                        osNum,
                        dataStr,
                        cliente,
                        tipoStr,
                        situacao,
                        equip,
                        servico,
                        tecnico,
                        df.format(valor)
                });
            }

            tblRelatorio.setModel(model);
            tblRelatorio.setAutoResizeMode(javax.swing.JTable.AUTO_RESIZE_ALL_COLUMNS);

            // Ajustar largura e alinhamento das colunas da tabela
            javax.swing.table.DefaultTableCellRenderer centerRenderer = new javax.swing.table.DefaultTableCellRenderer();
            centerRenderer.setHorizontalAlignment(javax.swing.JLabel.CENTER);

            javax.swing.table.DefaultTableCellRenderer rightRenderer = new javax.swing.table.DefaultTableCellRenderer();
            rightRenderer.setHorizontalAlignment(javax.swing.JLabel.RIGHT);

            if (tblRelatorio.getColumnModel().getColumnCount() >= 9) {
                // Nº OS
                tblRelatorio.getColumnModel().getColumn(0).setPreferredWidth(45);
                tblRelatorio.getColumnModel().getColumn(0).setMaxWidth(60);
                tblRelatorio.getColumnModel().getColumn(0).setCellRenderer(centerRenderer);

                // Data/Hora
                tblRelatorio.getColumnModel().getColumn(1).setPreferredWidth(110);
                tblRelatorio.getColumnModel().getColumn(1).setCellRenderer(centerRenderer);

                // Cliente
                tblRelatorio.getColumnModel().getColumn(2).setPreferredWidth(140);

                // Tipo
                tblRelatorio.getColumnModel().getColumn(3).setPreferredWidth(70);
                tblRelatorio.getColumnModel().getColumn(3).setCellRenderer(centerRenderer);

                // Situação
                tblRelatorio.getColumnModel().getColumn(4).setPreferredWidth(95);

                // Equipamento
                tblRelatorio.getColumnModel().getColumn(5).setPreferredWidth(145);

                // Serviço
                tblRelatorio.getColumnModel().getColumn(6).setPreferredWidth(120);

                // Técnico
                tblRelatorio.getColumnModel().getColumn(7).setPreferredWidth(100);

                // Valor (R$)
                tblRelatorio.getColumnModel().getColumn(8).setPreferredWidth(90);
                tblRelatorio.getColumnModel().getColumn(8).setCellRenderer(rightRenderer);
            }

            lblTotalOS.setText("Total de Registros: " + totalOS);
            lblTotalTipos.setText("Orçamentos: " + qtdOrcamento + " | OS: " + qtdOS);
            lblTotalValor.setText("Valor Total Acumulado: " + df.format(totalValorAcumulado));

        } catch (SQLException e) {
            JOptionPane.showMessageDialog(null, "Erro ao carregar relatório: " + e.getMessage());
        }
    }

    private void imprimir() {
        btnImprimir.setEnabled(false);
        final String filtro = txtFiltroCliente.getText().trim();

        new javax.swing.SwingWorker<JasperPrint, Void>() {
            @Override
            protected JasperPrint doInBackground() throws Exception {
                Map<String, Object> params = new HashMap<>();
                params.put("nome_filtro", filtro.isEmpty() ? null : filtro);

                String jasperPath = Moduloconexao.obterCaminhoJasper("relatorio_os_cliente", "relatorio_os_cliente");

                try (Connection conn = Moduloconexao.conector()) {
                    if (conn == null) throw new Exception("Não foi possível conectar ao banco de dados!");
                    return JasperFillManager.fillReport(jasperPath, params, conn);
                }
            }

            @Override
            protected void done() {
                btnImprimir.setEnabled(true);
                try {
                    JasperPrint print = get();
                    if (print != null) {
                        JasperViewer viewer = new JasperViewer(print, false);
                        viewer.setTitle("Relatório de OS por Cliente");
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
        txtFiltroCliente = new javax.swing.JTextField();
        btnImprimir = new javax.swing.JButton();
        jScrollPane1 = new javax.swing.JScrollPane();
        tblRelatorio = new javax.swing.JTable();
        panelResumo = new javax.swing.JPanel();
        lblTotalOS = new javax.swing.JLabel();
        lblTotalTipos = new javax.swing.JLabel();
        lblTotalValor = new javax.swing.JLabel();

        setClosable(true);
        setIconifiable(true);
        setMaximizable(true);
        setResizable(true);
        setTitle("Relatório de OS por Cliente");
        setMinimumSize(new java.awt.Dimension(640, 480));
        setPreferredSize(new java.awt.Dimension(640, 480));

        jLabel1.setFont(new java.awt.Font("Segoe UI", 1, 14));
        jLabel1.setText("Filtrar por nome do cliente:");

        txtFiltroCliente.setFont(new java.awt.Font("Segoe UI", 0, 14));
        txtFiltroCliente.addKeyListener(new java.awt.event.KeyAdapter() {
            @Override
            public void keyReleased(java.awt.event.KeyEvent evt) {
                carregarRelatorio();
            }
            @Override
            public void keyPressed(java.awt.event.KeyEvent evt) {
                if (evt.getKeyCode() == java.awt.event.KeyEvent.VK_ENTER) {
                    imprimir();
                }
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
        btnImprimir.addKeyListener(new java.awt.event.KeyAdapter() {
            @Override
            public void keyPressed(java.awt.event.KeyEvent evt) {
                if (evt.getKeyCode() == java.awt.event.KeyEvent.VK_ENTER) {
                    imprimir();
                }
            }
        });

        tblRelatorio.setModel(new javax.swing.table.DefaultTableModel(
                new Object[][] {},
                new String[] {
                        "Nº OS", "Data/Hora", "Cliente", "Tipo", "Situação", "Equipamento", "Serviço", "Técnico", "Valor (R$)"
                }));
        tblRelatorio.setRowHeight(22);
        jScrollPane1.setViewportView(tblRelatorio);

        panelResumo.setBorder(javax.swing.BorderFactory.createTitledBorder(
                javax.swing.BorderFactory.createEtchedBorder(), "Resumo das Ordens de Serviço",
                javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION,
                javax.swing.border.TitledBorder.DEFAULT_POSITION,
                new java.awt.Font("Segoe UI", 1, 12)));

        lblTotalOS.setFont(new java.awt.Font("Segoe UI", 1, 13));
        lblTotalOS.setText("Total de Registros: 0");

        lblTotalTipos.setFont(new java.awt.Font("Segoe UI", 1, 13));
        lblTotalTipos.setForeground(new java.awt.Color(0, 102, 204));
        lblTotalTipos.setText("Orçamentos: 0 | OS: 0");

        lblTotalValor.setFont(new java.awt.Font("Segoe UI", 1, 13));
        lblTotalValor.setForeground(new java.awt.Color(0, 128, 0));
        lblTotalValor.setText("Valor Total Acumulado: R$ 0,00");

        javax.swing.GroupLayout panelResumoLayout = new javax.swing.GroupLayout(panelResumo);
        panelResumo.setLayout(panelResumoLayout);
        panelResumoLayout.setHorizontalGroup(
                panelResumoLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(panelResumoLayout.createSequentialGroup()
                                .addGap(10, 10, 10)
                                .addComponent(lblTotalOS, javax.swing.GroupLayout.PREFERRED_SIZE, 140,
                                        javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(15, 15, 15)
                                .addComponent(lblTotalTipos, javax.swing.GroupLayout.PREFERRED_SIZE, 175,
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
                                        .addComponent(lblTotalOS)
                                        .addComponent(lblTotalTipos)
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
                                                .addComponent(txtFiltroCliente, javax.swing.GroupLayout.PREFERRED_SIZE,
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
                                        .addComponent(txtFiltroCliente, javax.swing.GroupLayout.PREFERRED_SIZE,
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
