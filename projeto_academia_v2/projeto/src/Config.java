import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

/**
 * Lê configurações sensíveis (senhas, URLs) sem colocá-las no código:
 * 1) variável de ambiente; 2) arquivo config.properties (ignorado pelo Git); 3) valor padrão (ambiente local).
 */
public class Config {
    private static final Properties props = new Properties();

    static {
        for (String caminho : new String[]{"config.properties", "../config.properties"}) {
            try (FileInputStream in = new FileInputStream(caminho)) {
                props.load(new java.io.InputStreamReader(in, java.nio.charset.StandardCharsets.UTF_8));
                break;
            } catch (IOException ignorado) { /* tenta o próximo */ }
        }
    }

    public static String get(String nome, String padrao) {
        String v = System.getenv(nome);
        if (v == null || v.isBlank()) v = props.getProperty(nome);
        return (v == null || v.isBlank()) ? padrao : v.trim();
    }
}
