import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.util.JRProperties;
import java.io.File;

public class CompileReports {
    public static void main(String[] args) {
        try {
            JRProperties.setProperty("net.sf.jasperreports.compiler.class", "net.sf.jasperreports.engine.design.JRJavacCompiler");
            
            // Compilar relatorio_servicos.jrxml no projeto
            File jrxmlProj = new File("reports/relatorio_servicos/relatorio_servicos.jrxml");
            File jasperProj = new File("reports/relatorio_servicos/relatorio_servicos.jasper");
            if (jrxmlProj.exists()) {
                JasperCompileManager.compileReportToFile(jrxmlProj.getAbsolutePath(), jasperProj.getAbsolutePath());
                System.out.println("Compilado com sucesso: " + jasperProj.getAbsolutePath());
            }
            
            // Compilar relatorio_servicos.jrxml no JaspersoftWorkspace
            File jrxmlWs = new File("C:/Users/dario/JaspersoftWorkspace/relatorio_servicos/relatorio_servicos.jrxml");
            File jasperWs = new File("C:/Users/dario/JaspersoftWorkspace/relatorio_servicos/relatorio_servicos.jasper");
            if (jrxmlWs.exists()) {
                JasperCompileManager.compileReportToFile(jrxmlWs.getAbsolutePath(), jasperWs.getAbsolutePath());
                System.out.println("Compilado com sucesso: " + jasperWs.getAbsolutePath());
            }

            // Copiar jasper compilado para dist/reports/relatorio_servicos/
            File distFolder = new File("dist/reports/relatorio_servicos");
            if (!distFolder.exists()) distFolder.mkdirs();
            File distJasper = new File("dist/reports/relatorio_servicos/relatorio_servicos.jasper");
            File distJrxml = new File("dist/reports/relatorio_servicos/relatorio_servicos.jrxml");
            java.nio.file.Files.copy(jasperProj.toPath(), distJasper.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            java.nio.file.Files.copy(jrxmlProj.toPath(), distJrxml.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            System.out.println("Copiado para dist/reports/relatorio_servicos com sucesso!");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
