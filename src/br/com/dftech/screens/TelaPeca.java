/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package br.com.dftech.screens;

import java.sql.*;
import br.com.dftech.dal.Moduloconexao;
import br.com.dftech.utils.MascaraValor;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import javax.swing.ImageIcon;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JTextField;
import javax.swing.SwingWorker;
import javax.swing.Timer;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;
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

    private static class CachedFoto {
        final byte[] bytes;
        final ImageIcon icon;
        CachedFoto(byte[] bytes, ImageIcon icon) {
            this.bytes = bytes;
            this.icon = icon;
        }
    }

    private final Map<Integer, CachedFoto> fotoCache = new ConcurrentHashMap<>();
    private Timer searchTimer = null;
    private SwingWorker<CachedFoto, Void> imageWorker = null;
    private SwingWorker<TableModel, Void> searchWorker = null;
    private SwingWorker<Void, Void> preCarregarWorker = null;

    // Declaração de componentes Swing
    private javax.swing.JButton btnPecaAdicionar;
    private javax.swing.JButton btnPecaAlterar;
    private javax.swing.JButton btnPecaRemover;
    private javax.swing.JButton btnPecaLimpar;
    private javax.swing.JButton btnCarregarFoto;
    private javax.swing.JButton btnRemoverFoto;
    private javax.swing.JLabel lblPecaAdicionar;
    private javax.swing.JLabel lblPecaAlterar;
    private javax.swing.JLabel lblPecaRemover;
    private javax.swing.JLabel lblPecaLimpar;
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
        otimizarFotosExistentesDoBanco();
        txtPecaId.setHorizontalAlignment(JTextField.CENTER);
        txtPecaQtd.setHorizontalAlignment(JTextField.CENTER);
        MascaraValor.aplicar(txtPecaValor);
        configurarLarguraColunasTabela();
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

    private byte[] compressFoto(byte[] originalBytes, int maxDimension, float quality) {
        if (originalBytes == null || originalBytes.length == 0) return null;
        try {
            ByteArrayInputStream bais = new ByteArrayInputStream(originalBytes);
            BufferedImage originalImage = ImageIO.read(bais);
            if (originalImage == null) return originalBytes;

            int origW = originalImage.getWidth();
            int origH = originalImage.getHeight();

            // Se já for pequena o suficiente (< 100KB e dimensão <= maxDimension), não precisa re-comprimir
            if (origW <= maxDimension && origH <= maxDimension && originalBytes.length < 100 * 1024) {
                return originalBytes;
            }

            // Calcular novas dimensões mantendo a proporção de aspecto (aspect ratio)
            int newW = origW;
            int newH = origH;
            if (origW > maxDimension || origH > maxDimension) {
                if (origW > origH) {
                    newW = maxDimension;
                    newH = (int) ((double) origH / origW * maxDimension);
                } else {
                    newH = maxDimension;
                    newW = (int) ((double) origW / origH * maxDimension);
                }
            }

            BufferedImage resizedImage = new BufferedImage(newW, newH, BufferedImage.TYPE_INT_RGB);
            Graphics2D g2d = resizedImage.createGraphics();
            g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            // Fundo branco caso seja um PNG com transparência
            g2d.setColor(java.awt.Color.WHITE);
            g2d.fillRect(0, 0, newW, newH);

            g2d.drawImage(originalImage, 0, 0, newW, newH, null);
            g2d.dispose();

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageWriter writer = ImageIO.getImageWritersByFormatName("jpg").next();
            ImageWriteParam param = writer.getDefaultWriteParam();
            if (param.canWriteCompressed()) {
                param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
                param.setCompressionQuality(quality);
            }
            try (ImageOutputStream ios = ImageIO.createImageOutputStream(baos)) {
                writer.setOutput(ios);
                writer.write(null, new IIOImage(resizedImage, null, null), param);
            }
            writer.dispose();
            return baos.toByteArray();
        } catch (Exception e) {
            System.err.println("Erro ao comprimir foto: " + e.getMessage());
            return originalBytes;
        }
    }

    private void otimizarFotosExistentesDoBanco() {
        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                String sqlSelect = "select id_peca, foto_peca from tbpecas where foto_peca is not null";
                String sqlUpdate = "update tbpecas set foto_peca=? where id_peca=?";
                try (Connection conn = Moduloconexao.conector()) {
                    if (conn == null) return null;
                    try (PreparedStatement psSelect = conn.prepareStatement(sqlSelect);
                         ResultSet r = psSelect.executeQuery()) {
                        while (r.next()) {
                            int id = r.getInt("id_peca");
                            byte[] imgBytes = r.getBytes("foto_peca");
                            if (imgBytes != null && imgBytes.length > 100 * 1024) {
                                byte[] compressed = compressFoto(imgBytes, 500, 0.80f);
                                if (compressed != null && compressed.length < imgBytes.length) {
                                    try (PreparedStatement psUpdate = conn.prepareStatement(sqlUpdate)) {
                                        psUpdate.setBytes(1, compressed);
                                        psUpdate.setInt(2, id);
                                        psUpdate.executeUpdate();
                                    }
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    System.err.println("Erro ao otimizar fotos existentes: " + e.getMessage());
                }
                return null;
            }
        }.execute();
    }

    private void carregarFoto() {
        JFileChooser chooser = new JFileChooser();
        FileNameExtensionFilter filter = new FileNameExtensionFilter("Imagens (JPG, PNG, GIF, BMP)", "jpg", "jpeg", "png", "gif", "bmp");
        chooser.setFileFilter(filter);
        int res = chooser.showOpenDialog(this);
        if (res == JFileChooser.APPROVE_OPTION) {
            File file = chooser.getSelectedFile();
            try (FileInputStream fis = new FileInputStream(file)) {
                byte[] raw = new byte[(int) file.length()];
                fis.read(raw);
                // Reduzir resolução para no máximo 500px e comprimir para ~30-50KB
                bytesFoto = compressFoto(raw, 500, 0.80f);
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

    private void dispararPesquisaComDebounce() {
        if (searchTimer == null) {
            searchTimer = new Timer(200, evt -> pesquisarPeca());
            searchTimer.setRepeats(false);
        }
        searchTimer.restart();
    }

    private ImageIcon redimensionarImagem(BufferedImage original, int targetWidth, int targetHeight) {
        if (original == null) return null;
        int w = targetWidth > 0 ? targetWidth : 140;
        int h = targetHeight > 0 ? targetHeight : 140;

        int origW = original.getWidth();
        int origH = original.getHeight();
        BufferedImage current = original;

        // Pré-redução rápida se a imagem for muito grande para evitar alto consumo de CPU e RAM
        if (origW > w * 2 || origH > h * 2) {
            int fastW = Math.max(w, origW / 4);
            int fastH = Math.max(h, origH / 4);
            BufferedImage fastResized = new BufferedImage(fastW, fastH, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = fastResized.createGraphics();
            g.drawImage(current, 0, 0, fastW, fastH, null);
            g.dispose();
            current = fastResized;
        }

        BufferedImage resized = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = resized.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_SPEED);
        g2d.drawImage(current, 0, 0, w, h, null);
        g2d.dispose();
        return new ImageIcon(resized);
    }

    private void exibirImagem(byte[] imgBytes) {
        if (imgBytes != null && imgBytes.length > 0) {
            try {
                ByteArrayInputStream bais = new ByteArrayInputStream(imgBytes);
                BufferedImage img = ImageIO.read(bais);
                if (img != null) {
                    int w = lblFoto.getWidth() > 0 ? lblFoto.getWidth() : 200;
                    int h = lblFoto.getHeight() > 0 ? lblFoto.getHeight() : 200;
                    lblFoto.setIcon(redimensionarImagem(img, w, h));
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

            double valor = MascaraValor.getValor(txtPecaValor);

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

    private void configurarLarguraColunasTabela() {
        if (tblPecas.getColumnCount() >= 4) {
            tblPecas.getColumnModel().getColumn(0).setPreferredWidth(45);
            tblPecas.getColumnModel().getColumn(0).setMaxWidth(60);
            tblPecas.getColumnModel().getColumn(0).setMinWidth(35);

            tblPecas.getColumnModel().getColumn(1).setPreferredWidth(320);

            tblPecas.getColumnModel().getColumn(2).setPreferredWidth(90);
            tblPecas.getColumnModel().getColumn(2).setMaxWidth(110);

            tblPecas.getColumnModel().getColumn(3).setPreferredWidth(90);
            tblPecas.getColumnModel().getColumn(3).setMaxWidth(110);
        }
    }

    private String removerAcentos(String str) {
        if (str == null) return "";
        return java.text.Normalizer.normalize(str, java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "")
                .toLowerCase();
    }

    private void pesquisarPeca() {
        final String rawSearchText = txtPecaPesquisar.getText().trim();
        
        if (searchWorker != null && !searchWorker.isDone()) {
            searchWorker.cancel(true);
        }
        if (preCarregarWorker != null && !preCarregarWorker.isDone()) {
            preCarregarWorker.cancel(true);
        }

        if (rawSearchText.isEmpty()) {
            limpar();
            return;
        }

        final String cleanText = removerAcentos(rawSearchText);
        final String[] terms = cleanText.split("\\s+");

        searchWorker = new SwingWorker<TableModel, Void>() {
            @Override
            protected TableModel doInBackground() throws Exception {
                StringBuilder sql = new StringBuilder(
                    "select id_peca as id, nome_peca as peça, qtd_estoque as quantidade, valor_peca as valor from tbpecas where "
                );

                // 1. Condição por ID ou busca por termos insensível a acentos
                sql.append("(cast(id_peca as text) like ? or (");

                String normCol = "translate(lower(nome_peca), 'áàâãäéèêëíìîïóòôõöúùûüç', 'aaaaaeeeeiiiiooooouuuuc')";
                for (int i = 0; i < terms.length; i++) {
                    if (i > 0) sql.append(" and ");
                    sql.append(normCol).append(" like ?");
                }
                sql.append(")) order by id_peca");

                try (Connection conn = Moduloconexao.conector()) {
                    if (conn == null) return null;
                    try (PreparedStatement ps = conn.prepareStatement(sql.toString())) {
                        ps.setString(1, rawSearchText + "%");
                        for (int i = 0; i < terms.length; i++) {
                            ps.setString(i + 2, "%" + terms[i] + "%");
                        }
                        try (ResultSet r = ps.executeQuery()) {
                            return DbUtils.resultSetToTableModel(r);
                        }
                    }
                }
            }

            @Override
            protected void done() {
                if (isCancelled()) return;
                try {
                    TableModel model = get();
                    if (model != null) {
                        tblPecas.setModel(model);
                        configurarLarguraColunasTabela();
                        if (txtPecaId.getText().isEmpty()) {
                            limparFormulario();
                        }
                        if (tblPecas.getRowCount() > 0) {
                            int targetRow = 0;
                            try {
                                int idPesquisado = Integer.parseInt(rawSearchText);
                                for (int i = 0; i < tblPecas.getRowCount(); i++) {
                                    Object val = tblPecas.getModel().getValueAt(i, 0);
                                    if (val != null && Integer.parseInt(val.toString()) == idPesquisado) {
                                        targetRow = i;
                                        break;
                                    }
                                }
                            } catch (NumberFormatException ignored) {}

                            tblPecas.setRowSelectionInterval(targetRow, targetRow);
                        }
                        preCarregarFotosTabela();
                    }
                } catch (Exception e) {
                    // Ignore exceções de busca cancelada ou conexão
                }
            }
        };
        searchWorker.execute();
    }

    private void preCarregarFotosTabela() {
        int rowCount = tblPecas.getRowCount();
        if (rowCount == 0) return;

        if (preCarregarWorker != null && !preCarregarWorker.isDone()) {
            preCarregarWorker.cancel(true);
        }

        List<Integer> idsParaCarregar = new ArrayList<>();
        int limit = Math.min(rowCount, 100);
        for (int i = 0; i < limit; i++) {
            Object val = tblPecas.getModel().getValueAt(i, 0);
            if (val != null) {
                try {
                    int id = Integer.parseInt(val.toString());
                    if (!fotoCache.containsKey(id)) {
                        idsParaCarregar.add(id);
                    }
                } catch (NumberFormatException ignored) {}
            }
        }

        if (idsParaCarregar.isEmpty()) return;

        preCarregarWorker = new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                try (Connection conn = Moduloconexao.conector()) {
                    if (conn == null) return null;

                    int chunkSize = 10;
                    for (int start = 0; start < idsParaCarregar.size(); start += chunkSize) {
                        if (isCancelled()) break;
                        int end = Math.min(start + chunkSize, idsParaCarregar.size());
                        List<Integer> chunk = idsParaCarregar.subList(start, end);

                        StringBuilder sb = new StringBuilder("select id_peca, foto_peca from tbpecas where id_peca in (");
                        for (int i = 0; i < chunk.size(); i++) {
                            sb.append(i == 0 ? "?" : ", ?");
                        }
                        sb.append(")");

                        try (PreparedStatement ps = conn.prepareStatement(sb.toString())) {
                            for (int i = 0; i < chunk.size(); i++) {
                                ps.setInt(i + 1, chunk.get(i));
                            }
                            try (ResultSet r = ps.executeQuery()) {
                                int w = lblFoto.getWidth() > 0 ? lblFoto.getWidth() : 200;
                                int h = lblFoto.getHeight() > 0 ? lblFoto.getHeight() : 200;

                                while (r.next()) {
                                    if (isCancelled()) break;
                                    int id = r.getInt("id_peca");
                                    byte[] imgBytes = r.getBytes("foto_peca");
                                    ImageIcon icon = null;
                                    if (imgBytes != null && imgBytes.length > 0) {
                                        ByteArrayInputStream bais = new ByteArrayInputStream(imgBytes);
                                        BufferedImage img = ImageIO.read(bais);
                                        if (img != null) {
                                            icon = redimensionarImagem(img, w, h);
                                        }
                                    }
                                    fotoCache.put(id, new CachedFoto(imgBytes, icon));
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    System.err.println("Erro no pré-carregamento de fotos: " + e.getMessage());
                }
                return null;
            }

            @Override
            protected void done() {
                if (isCancelled()) return;
                if (tblPecas.getSelectedRow() >= 0) {
                    Object selectedIdObj = tblPecas.getModel().getValueAt(tblPecas.getSelectedRow(), 0);
                    if (selectedIdObj != null) {
                        try {
                            int selectedId = Integer.parseInt(selectedIdObj.toString());
                            if (fotoCache.containsKey(selectedId)) {
                                CachedFoto cached = fotoCache.get(selectedId);
                                if (cached != null && txtPecaId.getText().equals(String.valueOf(selectedId))) {
                                    bytesFoto = cached.bytes;
                                    lblFoto.setIcon(cached.icon);
                                    lblFoto.setText(cached.icon == null ? "Sem Foto" : null);
                                }
                            }
                        } catch (NumberFormatException ignored) {}
                    }
                }
            }
        };
        preCarregarWorker.execute();
    }

    public void setarCampo() {
        if (searchTimer != null && searchTimer.isRunning()) {
            searchTimer.stop();
        }
        int setar = tblPecas.getSelectedRow();
        if (setar < 0) return;

        Object idObj = tblPecas.getModel().getValueAt(setar, 0);
        Object nomeObj = tblPecas.getModel().getValueAt(setar, 1);
        Object qtdObj = tblPecas.getModel().getValueAt(setar, 2);
        Object valorObj = tblPecas.getModel().getValueAt(setar, 3);

        if (idObj == null) return;

        txtPecaId.setText(idObj.toString());
        txtPecaNome.setText(nomeObj != null ? nomeObj.toString() : "");
        txtPecaQtd.setText(qtdObj != null ? qtdObj.toString() : "0");
        if (valorObj != null) {
            try {
                double val = Double.parseDouble(valorObj.toString().replace(",", "."));
                MascaraValor.setValor(txtPecaValor, val);
            } catch (Exception e) {
                txtPecaValor.setText("");
            }
        } else {
            txtPecaValor.setText("");
        }
        
        btnPecaAdicionar.setEnabled(false);

        int pecaId;
        try {
            pecaId = Integer.parseInt(idObj.toString());
        } catch (NumberFormatException e) {
            removerFoto();
            return;
        }

        // 1. HIT no cache: exibição instantânea 0ms!
        if (fotoCache.containsKey(pecaId)) {
            CachedFoto cached = fotoCache.get(pecaId);
            if (cached != null) {
                bytesFoto = cached.bytes;
                lblFoto.setIcon(cached.icon);
                lblFoto.setText(cached.icon == null ? "Sem Foto" : null);
            } else {
                bytesFoto = null;
                lblFoto.setIcon(null);
                lblFoto.setText("Sem Foto");
            }
            return;
        }

        // 2. MISS no cache: mostrar indicação imediata e buscar em segundo plano via conexão isolada
        lblFoto.setIcon(null);
        lblFoto.setText("Carregando...");

        if (imageWorker != null && !imageWorker.isDone()) {
            imageWorker.cancel(true);
        }

        imageWorker = new SwingWorker<CachedFoto, Void>() {
            @Override
            protected CachedFoto doInBackground() throws Exception {
                String sql = "select foto_peca from tbpecas where id_peca=?";
                try (Connection conn = Moduloconexao.conector()) {
                    if (conn == null) return new CachedFoto(null, null);
                    try (PreparedStatement ps = conn.prepareStatement(sql)) {
                        ps.setInt(1, pecaId);
                        try (ResultSet r = ps.executeQuery()) {
                            if (r.next()) {
                                byte[] imgBytes = r.getBytes("foto_peca");
                                if (imgBytes != null && imgBytes.length > 0) {
                                    ByteArrayInputStream bais = new ByteArrayInputStream(imgBytes);
                                    BufferedImage img = ImageIO.read(bais);
                                    if (img != null) {
                                        int w = lblFoto.getWidth() > 0 ? lblFoto.getWidth() : 200;
                                        int h = lblFoto.getHeight() > 0 ? lblFoto.getHeight() : 200;
                                        ImageIcon icon = redimensionarImagem(img, w, h);
                                        return new CachedFoto(imgBytes, icon);
                                    }
                                }
                            }
                        }
                    }
                }
                return new CachedFoto(null, null);
            }

            @Override
            protected void done() {
                if (isCancelled()) return;
                try {
                    CachedFoto res = get();
                    fotoCache.put(pecaId, res);
                    if (!txtPecaId.getText().isEmpty()) {
                        try {
                            int currentId = Integer.parseInt(txtPecaId.getText());
                            if (currentId == pecaId) {
                                bytesFoto = res.bytes;
                                lblFoto.setIcon(res.icon);
                                lblFoto.setText(res.icon == null ? "Sem Foto" : null);
                            }
                        } catch (NumberFormatException ignored) {}
                    }
                } catch (Exception e) {
                    lblFoto.setIcon(null);
                    lblFoto.setText("Sem Foto");
                }
            }
        };
        imageWorker.execute();
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

        double valor = MascaraValor.getValor(txtPecaValor);

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
                fotoCache.remove(pecaId);
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
                    fotoCache.remove(pecaId);
                    JOptionPane.showMessageDialog(null, "Peça removida com sucesso!");
                    limpar();
                }
            } catch (SQLException e) {
                JOptionPane.showMessageDialog(null, e.getMessage());
            }
        }
    }

    private void limparFormulario() {
        if (imageWorker != null && !imageWorker.isDone()) {
            imageWorker.cancel(true);
        }
        txtPecaId.setText(null);
        txtPecaNome.setText(null);
        txtPecaQtd.setText(null);
        txtPecaValor.setText(null);
        removerFoto();
        btnPecaAdicionar.setEnabled(true);
    }

    private void limpar() {
        if (imageWorker != null && !imageWorker.isDone()) {
            imageWorker.cancel(true);
        }
        if (searchWorker != null && !searchWorker.isDone()) {
            searchWorker.cancel(true);
        }
        if (preCarregarWorker != null && !preCarregarWorker.isDone()) {
            preCarregarWorker.cancel(true);
        }
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
        configurarLarguraColunasTabela();
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
                if (evt.getKeyCode() == java.awt.event.KeyEvent.VK_ENTER) {
                    if (searchTimer != null && searchTimer.isRunning()) {
                        searchTimer.stop();
                    }
                    pesquisarPeca();
                } else if (evt.getKeyCode() != java.awt.event.KeyEvent.VK_DOWN && evt.getKeyCode() != java.awt.event.KeyEvent.VK_UP) {
                    dispararPesquisaComDebounce();
                }
            }
            @Override
            public void keyPressed(java.awt.event.KeyEvent evt) {
                if (evt.getKeyCode() == java.awt.event.KeyEvent.VK_ENTER) {
                    if (searchTimer != null && searchTimer.isRunning()) {
                        searchTimer.stop();
                    }
                    pesquisarPeca();
                    if (tblPecas.getRowCount() > 0) {
                        tblPecas.requestFocus();
                        if (tblPecas.getSelectedRow() < 0) {
                            tblPecas.setRowSelectionInterval(0, 0);
                        }
                        setarCampo();
                    }
                } else if (evt.getKeyCode() == java.awt.event.KeyEvent.VK_TAB) {
                    if (!txtPecaId.getText().isEmpty()) {
                        evt.consume();
                        txtPecaId.requestFocus();
                    }
                } else if (evt.getKeyCode() == java.awt.event.KeyEvent.VK_DOWN || evt.getKeyCode() == java.awt.event.KeyEvent.VK_UP) {
                    if (tblPecas.getRowCount() > 0) {
                        tblPecas.requestFocus();
                        if (tblPecas.getSelectedRow() < 0) {
                            tblPecas.setRowSelectionInterval(0, 0);
                        }
                        setarCampo();
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
            public void mousePressed(java.awt.event.MouseEvent evt) {
                if (tblPecas.getSelectedRow() >= 0) {
                    setarCampo();
                }
            }
            @Override
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                if (tblPecas.getSelectedRow() >= 0) {
                    setarCampo();
                }
            }
        });
        tblPecas.addKeyListener(new java.awt.event.KeyAdapter() {
            @Override
            public void keyPressed(java.awt.event.KeyEvent evt) {
                if (evt.getKeyCode() == java.awt.event.KeyEvent.VK_ENTER) {
                    evt.consume();
                    if (tblPecas.getSelectedRow() >= 0) {
                        setarCampo();
                    }
                } else if (evt.getKeyCode() == java.awt.event.KeyEvent.VK_TAB) {
                    evt.consume();
                    if (tblPecas.getSelectedRow() >= 0) {
                        setarCampo();
                    }
                    txtPecaId.requestFocus();
                }
            }
            @Override
            public void keyReleased(java.awt.event.KeyEvent evt) {
                if (evt.getKeyCode() == java.awt.event.KeyEvent.VK_UP || evt.getKeyCode() == java.awt.event.KeyEvent.VK_DOWN) {
                    if (tblPecas.getSelectedRow() >= 0) {
                        setarCampo();
                    }
                }
            }
        });
        jScrollPane1.setViewportView(tblPecas);

        jLabel1.setText("* Campos obrigatórios");

        jLabel7.setFont(new java.awt.Font("Segoe UI", 0, 14));
        jLabel7.setText("ID");

        txtPecaId.setEditable(false);
        txtPecaId.addKeyListener(new java.awt.event.KeyAdapter() {
            @Override
            public void keyPressed(java.awt.event.KeyEvent evt) {
                if (evt.getKeyCode() == java.awt.event.KeyEvent.VK_ENTER || evt.getKeyCode() == java.awt.event.KeyEvent.VK_TAB) {
                    evt.consume();
                    txtPecaNome.requestFocus();
                }
            }
        });

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
        lblFoto.setPreferredSize(new java.awt.Dimension(200, 200));

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

        lblPecaAdicionar = new javax.swing.JLabel("Adicionar", javax.swing.SwingConstants.CENTER);
        lblPecaAdicionar.setFont(new java.awt.Font("Segoe UI", java.awt.Font.BOLD, 12));

        lblPecaAlterar = new javax.swing.JLabel("Alterar", javax.swing.SwingConstants.CENTER);
        lblPecaAlterar.setFont(new java.awt.Font("Segoe UI", java.awt.Font.BOLD, 12));

        lblPecaRemover = new javax.swing.JLabel("Excluir", javax.swing.SwingConstants.CENTER);
        lblPecaRemover.setFont(new java.awt.Font("Segoe UI", java.awt.Font.BOLD, 12));

        lblPecaLimpar = new javax.swing.JLabel("Limpar", javax.swing.SwingConstants.CENTER);
        lblPecaLimpar.setFont(new java.awt.Font("Segoe UI", java.awt.Font.BOLD, 12));

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
                                .addGap(15, 15, 15)
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.CENTER)
                                    .addComponent(btnPecaAdicionar, javax.swing.GroupLayout.PREFERRED_SIZE, 70, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addComponent(lblPecaAdicionar, javax.swing.GroupLayout.PREFERRED_SIZE, 70, javax.swing.GroupLayout.PREFERRED_SIZE))
                                .addGap(15, 15, 15)
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.CENTER)
                                    .addComponent(btnPecaAlterar, javax.swing.GroupLayout.PREFERRED_SIZE, 70, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addComponent(lblPecaAlterar, javax.swing.GroupLayout.PREFERRED_SIZE, 70, javax.swing.GroupLayout.PREFERRED_SIZE))
                                .addGap(15, 15, 15)
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.CENTER)
                                    .addComponent(btnPecaRemover, javax.swing.GroupLayout.PREFERRED_SIZE, 70, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addComponent(lblPecaRemover, javax.swing.GroupLayout.PREFERRED_SIZE, 70, javax.swing.GroupLayout.PREFERRED_SIZE))
                                .addGap(15, 15, 15)
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.CENTER)
                                    .addComponent(btnPecaLimpar, javax.swing.GroupLayout.PREFERRED_SIZE, 70, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addComponent(lblPecaLimpar, javax.swing.GroupLayout.PREFERRED_SIZE, 70, javax.swing.GroupLayout.PREFERRED_SIZE))))
                        .addGap(30, 30, Short.MAX_VALUE)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.CENTER)
                            .addComponent(lblFoto, javax.swing.GroupLayout.PREFERRED_SIZE, 200, javax.swing.GroupLayout.PREFERRED_SIZE)
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
                            .addGroup(layout.createSequentialGroup()
                                .addComponent(btnPecaAdicionar, javax.swing.GroupLayout.PREFERRED_SIZE, 70, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(lblPecaAdicionar))
                            .addGroup(layout.createSequentialGroup()
                                .addComponent(btnPecaAlterar, javax.swing.GroupLayout.PREFERRED_SIZE, 70, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(lblPecaAlterar))
                            .addGroup(layout.createSequentialGroup()
                                .addComponent(btnPecaRemover, javax.swing.GroupLayout.PREFERRED_SIZE, 70, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(lblPecaRemover))
                            .addGroup(layout.createSequentialGroup()
                                .addComponent(btnPecaLimpar, javax.swing.GroupLayout.PREFERRED_SIZE, 70, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(lblPecaLimpar))))
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(lblFoto, javax.swing.GroupLayout.PREFERRED_SIZE, 200, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(btnCarregarFoto)
                            .addComponent(btnRemoverFoto))))
                .addContainerGap(20, Short.MAX_VALUE))
        );

        setBounds(0, 0, 640, 480);
    }
}
