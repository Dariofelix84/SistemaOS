/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package br.com.dftech.dal;

import java.sql.*;
import java.sql.DriverManager;

/**
 *
 * @author dario
 */
public class Moduloconexao {

    public static Connection conector() {
        Connection conexao = null;
        String driver = "org.postgresql.Driver";

        String url = "jdbc:postgresql://localhost:5432/dbinfox";
        // String url = "jdbc:postgresql://eloquently-humble-meerkat.data-1.use1.tembo.io/dftech";
        String user = "postgres";
        String password = "Mae191161";
        // String password = "4F7Y4T0j7MIWbz8P";

        try {
            Class.forName(driver);
            conexao = DriverManager.getConnection(url, user, password);
            return conexao;
        } catch (Exception e) {
            System.out.println(e);
            return null;
        }
    }

    public static String obterCaminhoJasper(String folderName, String fileName) throws Exception {
        net.sf.jasperreports.engine.util.JRProperties.setProperty("net.sf.jasperreports.compiler.class", "net.sf.jasperreports.engine.design.JRJavacCompiler");

        String[] candidateJasperPaths = new String[] {
            "app/reports/" + folderName + "/" + fileName + ".jasper",
            "reports/" + folderName + "/" + fileName + ".jasper",
            "dist/reports/" + folderName + "/" + fileName + ".jasper",
            "C:\\Users\\dario\\JaspersoftWorkspace\\" + folderName + "\\" + fileName + ".jasper"
        };

        for (String path : candidateJasperPaths) {
            java.io.File f = new java.io.File(path);
            if (f.exists()) {
                return f.getAbsolutePath();
            }
        }

        String[] candidateJrxmlPaths = new String[] {
            "app/reports/" + folderName + "/" + fileName + ".jrxml",
            "reports/" + folderName + "/" + fileName + ".jrxml",
            "dist/reports/" + folderName + "/" + fileName + ".jrxml",
            "C:\\Users\\dario\\JaspersoftWorkspace\\" + folderName + "\\" + fileName + ".jrxml"
        };

        for (String jrxmlPath : candidateJrxmlPaths) {
            java.io.File jrxmlFile = new java.io.File(jrxmlPath);
            if (jrxmlFile.exists()) {
                java.io.File targetJasper = new java.io.File("reports/" + folderName + "/" + fileName + ".jasper");
                if (!targetJasper.getParentFile().exists()) {
                    targetJasper.getParentFile().mkdirs();
                }
                net.sf.jasperreports.engine.JasperCompileManager.compileReportToFile(jrxmlFile.getAbsolutePath(), targetJasper.getAbsolutePath());
                return targetJasper.getAbsolutePath();
            }
        }

        throw new Exception("Arquivo de relatório " + fileName + ".jasper não foi localizado!");
    }
}

