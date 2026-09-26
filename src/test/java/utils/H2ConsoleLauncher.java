package utils;

import files.ConfigManager;
import org.h2.tools.Server;
import java.nio.file.Path;
import java.nio.file.Paths;

public class H2ConsoleLauncher {



    public static void main(String[] args) throws Exception {
        //DBUtil.createSchema();
        //DBUtil.insertData();
        //ConfigManager CONFIG = new ConfigManager(Path.of(".//src///test//resources//config.properties")) ;
        ConfigManager.load(java.nio.file.Paths.get("src/test/resources/config.properties"));
        Server webServer = Server.createWebServer("-webPort", "8082").start();

        System.out.println("H2 console started at: " + webServer.getURL());
        System.out.println("JDBC URL: " + toAbsoluteJdbcUrl(ConfigManager.getProperty("db.url")));
        System.out.println("Database files: " + Paths.get("data").toAbsolutePath());
        System.out.println("User: " + ConfigManager.getProperty("db.user"));
        System.out.println("Password: <empty>");
        System.out.println("Press Enter to stop the H2 console...");

        System.in.read();
        webServer.stop();
    }

    private static String toAbsoluteJdbcUrl(String jdbcUrl) {
        String filePrefix = "jdbc:h2:file:";
        if (!jdbcUrl.startsWith(filePrefix)) {
            return jdbcUrl;
        }

        String filePath = jdbcUrl.substring(filePrefix.length());
        Path resolvedPath = Paths.get(filePath).toAbsolutePath().normalize();
        return filePrefix + resolvedPath;
    }
}
