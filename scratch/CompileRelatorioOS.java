import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.util.JRProperties;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

public class CompileRelatorioOS {
    public static void main(String[] args) {
        try {
            JRProperties.setProperty("net.sf.jasperreports.compiler.class", "net.sf.jasperreports.engine.design.JRJavacCompiler");
            
            // 1. Compilar relatorio_os.jrxml no projeto
            File jrxmlProj = new File("reports/relatorio_os/relatorio_os.jrxml");
            File jasperProj = new File("reports/relatorio_os/relatorio_os.jasper");
            if (jrxmlProj.exists()) {
                JasperCompileManager.compileReportToFile(jrxmlProj.getAbsolutePath(), jasperProj.getAbsolutePath());
                System.out.println("Compilado no projeto com sucesso: " + jasperProj.getAbsolutePath());
            }
            
            // 2. Compilar no JaspersoftWorkspace
            File jrxmlWs = new File("C:/Users/dario/JaspersoftWorkspace/relatorio_os/relatorio_os.jrxml");
            File jasperWs = new File("C:/Users/dario/JaspersoftWorkspace/relatorio_os/relatorio_os.jasper");
            if (jrxmlWs.exists()) {
                JasperCompileManager.compileReportToFile(jrxmlWs.getAbsolutePath(), jasperWs.getAbsolutePath());
                System.out.println("Compilado no workspace com sucesso: " + jasperWs.getAbsolutePath());
            }

            // 3. Copiar para dist/reports/relatorio_os/
            File distFolder = new File("dist/reports/relatorio_os");
            if (!distFolder.exists()) distFolder.mkdirs();
            File distJasper = new File("dist/reports/relatorio_os/relatorio_os.jasper");
            File distJrxml = new File("dist/reports/relatorio_os/relatorio_os.jrxml");
            if (jasperProj.exists()) {
                Files.copy(jasperProj.toPath(), distJasper.toPath(), StandardCopyOption.REPLACE_EXISTING);
                Files.copy(jrxmlProj.toPath(), distJrxml.toPath(), StandardCopyOption.REPLACE_EXISTING);
                System.out.println("Copiado para dist/reports/relatorio_os com sucesso!");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
