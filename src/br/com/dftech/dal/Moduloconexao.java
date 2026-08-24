/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package br.com.dftech.dal;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.sql.*;
import java.util.Properties;

/**
 *
 * @author dario
 */
public class Moduloconexao {

    private static File obterArquivoConfig() {
        File localFile = new File("db.properties");
        if (localFile.exists()) {
            return localFile;
        }
        String userHome = System.getProperty("user.home");
        File userDir = new File(userHome, ".sistemaos");
        File userFile = new File(userDir, "db.properties");
        if (userFile.exists()) {
            return userFile;
        }
        return localFile;
    }

    public static Properties carregarPropriedades() {
        Properties props = new Properties();
        File file = obterArquivoConfig();

        if (!file.exists()) {
            props.setProperty("host", "localhost");
            props.setProperty("port", "5432");
            props.setProperty("database", "dbinfox");
            props.setProperty("user", "postgres");
            props.setProperty("password", "Mae191161");
            salvarPropriedades(props);
        } else {
            try (FileInputStream fis = new FileInputStream(file)) {
                props.load(fis);
            } catch (IOException e) {
                System.out.println("Erro ao carregar db.properties: " + e.getMessage());
                props.setProperty("host", "localhost");
                props.setProperty("port", "5432");
                props.setProperty("database", "dbinfox");
                props.setProperty("user", "postgres");
                props.setProperty("password", "Mae191161");
            }
        }
        return props;
    }

    public static boolean salvarPropriedades(Properties props) {
        File file = new File("db.properties");
        try (FileOutputStream fos = new FileOutputStream(file)) {
            props.store(fos, "Configuracao de Conexao Banco de Dados PostgreSQL - DFtech SistemaOS");
            return true;
        } catch (IOException e) {
            try {
                String userHome = System.getProperty("user.home");
                File userDir = new File(userHome, ".sistemaos");
                if (!userDir.exists()) {
                    userDir.mkdirs();
                }
                File userFile = new File(userDir, "db.properties");
                try (FileOutputStream fosUser = new FileOutputStream(userFile)) {
                    props.store(fosUser, "Configuracao de Conexao Banco de Dados PostgreSQL - DFtech SistemaOS");
                    return true;
                }
            } catch (IOException ex) {
                System.out.println("Erro ao salvar db.properties: " + ex.getMessage());
                return false;
            }
        }
    }

    public static Connection testarConexao(String host, String port, String database, String user, String password) throws Exception {
        String driver = "org.postgresql.Driver";
        Class.forName(driver);
        String url = "jdbc:postgresql://" + host.trim() + ":" + port.trim() + "/" + database.trim();
        return DriverManager.getConnection(url, user.trim(), password.trim());
    }

    public static Connection conector() {
        Connection conexao = null;
        String driver = "org.postgresql.Driver";

        Properties props = carregarPropriedades();
        String host = props.getProperty("host", "localhost").trim();
        String port = props.getProperty("port", "5432").trim();
        String database = props.getProperty("database", "dbinfox").trim();
        String user = props.getProperty("user", "postgres").trim();
        String password = props.getProperty("password", "Mae191161").trim();

        String url = "jdbc:postgresql://" + host + ":" + port + "/" + database;

        try {
            Class.forName(driver);
            conexao = DriverManager.getConnection(url, user, password);
            return conexao;
        } catch (Exception e) {
            System.out.println("Erro na conexao com o banco de dados (" + url + "): " + e.getMessage());
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

