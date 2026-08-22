package br.com.dftech.utils;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;
import javax.swing.JTextField;
import javax.swing.text.AbstractDocument;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DocumentFilter;

/**
 * Utilitário para aplicar máscara de valor monetário em tempo real (R$ #.##0,00)
 * em campos JTextField ou JFormattedTextField no Swing.
 */
public class MascaraValor {

    private static final DecimalFormat df;

    static {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(new Locale("pt", "BR"));
        symbols.setGroupingSeparator('.');
        symbols.setDecimalSeparator(',');
        df = new DecimalFormat("#,##0.00", symbols);
    }

    /**
     * Aplica a máscara monetária automática ao campo informado.
     * Os dígitos digitados são formatados dinamicamente com pontos e vírgulas.
     *
     * @param field O JTextField ou JFormattedTextField a ser formatado.
     */
    public static void aplicar(JTextField field) {
        if (field == null) return;
        if (field instanceof javax.swing.JFormattedTextField) {
            javax.swing.JFormattedTextField ftf = (javax.swing.JFormattedTextField) field;
            ftf.setFormatterFactory(null);
        }
        if (field.getDocument() instanceof AbstractDocument) {
            AbstractDocument doc = (AbstractDocument) field.getDocument();
            doc.setDocumentFilter(new MonetaryDocumentFilter(field));
        }
        field.setHorizontalAlignment(JTextField.RIGHT);
    }

    /**
     * Extrai o valor numérico (double) do campo formatado.
     * Exemplo: "1.500,50" -> 1500.50
     *
     * @param field O campo de texto
     * @return O valor como double (retorna 0.0 se vazio ou inválido)
     */
    public static double getValor(JTextField field) {
        if (field == null || field.getText() == null) return 0.0;
        String text = field.getText().trim();
        if (text.isEmpty()) return 0.0;
        try {
            String clean = text.replaceAll("[^0-9,]", "").replace(",", ".");
            return Double.parseDouble(clean);
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }

    /**
     * Define um valor numérico (double) no campo, formatando-o automaticamente.
     * Exemplo: 1500.50 -> "1.500,50"
     *
     * @param field O campo de texto
     * @param valor O valor numérico a ser exibido
     */
    public static void setValor(JTextField field, double valor) {
        if (field != null) {
            if (valor <= 0.0001) {
                field.setText("");
            } else {
                field.setText(df.format(valor));
            }
        }
    }

    /**
     * Formata um valor double diretamente para String (ex: 1500.50 -> "1.500,50")
     */
    public static String formatar(double valor) {
        return df.format(valor);
    }

    private static class MonetaryDocumentFilter extends DocumentFilter {
        private final JTextField field;
        private boolean updating = false;

        public MonetaryDocumentFilter(JTextField field) {
            this.field = field;
        }

        @Override
        public void insertString(FilterBypass fb, int offset, String string, AttributeSet attr) throws BadLocationException {
            if (updating) {
                super.insertString(fb, offset, string, attr);
                return;
            }
            updateText(fb, getNewText(fb, offset, 0, string));
        }

        @Override
        public void replace(FilterBypass fb, int offset, int length, String text, AttributeSet attrs) throws BadLocationException {
            if (updating) {
                super.replace(fb, offset, length, text, attrs);
                return;
            }
            updateText(fb, getNewText(fb, offset, length, text));
        }

        @Override
        public void remove(FilterBypass fb, int offset, int length) throws BadLocationException {
            if (updating) {
                super.remove(fb, offset, length);
                return;
            }
            updateText(fb, getNewText(fb, offset, length, ""));
        }

        private String getNewText(FilterBypass fb, int offset, int length, String text) throws BadLocationException {
            String current = fb.getDocument().getText(0, fb.getDocument().getLength());
            StringBuilder sb = new StringBuilder(current);
            sb.replace(offset, offset + length, text != null ? text : "");
            return sb.toString();
        }

        private void updateText(FilterBypass fb, String newText) throws BadLocationException {
            updating = true;
            try {
                if (newText == null || newText.trim().isEmpty()) {
                    fb.remove(0, fb.getDocument().getLength());
                    return;
                }

                String digitsOnly = newText.replaceAll("[^0-9]", "");
                if (digitsOnly.isEmpty()) {
                    fb.remove(0, fb.getDocument().getLength());
                    return;
                }

                // Limita a 12 dígitos para evitar overflow em Long.parseLong
                if (digitsOnly.length() > 12) {
                    digitsOnly = digitsOnly.substring(0, 12);
                }

                long cents = Long.parseLong(digitsOnly);
                double val = cents / 100.0;
                String formatted = df.format(val);

                fb.remove(0, fb.getDocument().getLength());
                fb.insertString(0, formatted, null);
            } finally {
                updating = false;
            }
        }
    }
}
