import java.io.File;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.util.JRProperties;

public class CompileReports {
    public static void main(String[] args) {
        try {
            JRProperties.setProperty("net.sf.jasperreports.compiler.class", "net.sf.jasperreports.engine.design.JRJavacCompiler");
            File reportsDir = new File("reports");
            compileRecursive(reportsDir);
            System.out.println("Compilação de relatórios concluída!");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void compileRecursive(File dir) {
        if (!dir.exists()) return;
        File[] files = dir.listFiles();
        if (files == null) return;
        for (File f : files) {
            if (f.isDirectory()) {
                compileRecursive(f);
            } else if (f.getName().endsWith(".jrxml")) {
                String jasperPath = f.getAbsolutePath().substring(0, f.getAbsolutePath().length() - 6) + ".jasper";
                File jasperFile = new File(jasperPath);
                // Força recompilação para testar
                System.out.println("Compilando com JRJavacCompiler: " + f.getName() + " -> " + jasperFile.getName());
                try {
                    JasperCompileManager.compileReportToFile(f.getAbsolutePath(), jasperPath);
                    System.out.println("Sucesso: " + jasperFile.getName());
                } catch (Exception e) {
                    System.err.println("Erro compilando " + f.getName() + ": " + e.getMessage());
                    e.printStackTrace();
                }
            }
        }
    }
}
