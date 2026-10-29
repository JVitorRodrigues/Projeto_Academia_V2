import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Conexão JDBC com o PostgreSQL. Em produção/entrega aponta para o SUPABASE (Postgres na nuvem).
 * Variáveis: SUPABASE_DB_URL, SUPABASE_DB_USER, SUPABASE_DB_PASSWORD (veja config.properties.example).
 * Sem configuração, usa um PostgreSQL local (útil para desenvolvimento).
 */
public class ConexaoBD {

    public static Connection conectar() throws SQLException {
        String url  = Config.get("SUPABASE_DB_URL", "jdbc:postgresql://localhost:5432/academia");
        String user = Config.get("SUPABASE_DB_USER", null);
        String pass = Config.get("SUPABASE_DB_PASSWORD", null);

        // A string JDBC do Supabase já pode trazer user e password dentro da URL
        if (user == null && url.contains("localhost")) { user = "postgres"; pass = "1234"; }
        if (user == null) return DriverManager.getConnection(url);
        return DriverManager.getConnection(url, user, pass);
    }
}
