import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Cache-aside do Relatório Financeiro.
 *  - 1ª leitura: consulta a VIEW vw_resumo_por_plano no Supabase e guarda o resultado no Redis (TTL 60 s).
 *  - Leituras seguintes: vêm direto do Redis (sem tocar no banco).
 *  - Ao matricular/cancelar/excluir, o cache é apagado (invalidação) para o relatório nunca ficar desatualizado.
 */
public class RelatorioService {

    static final String CHAVE = "academia:relatorio:resumo_por_plano";
    static final int TTL_SEGUNDOS = 60;

    public static class Resultado {
        public List<String[]> linhas = new ArrayList<>(); // plano, total, ativas, canceladas, receita
        public boolean doCache;
        public boolean redisOk = true;
        public long ttlRestante = -1;
        public long milissegundos;
    }

    public static Resultado buscar() throws SQLException {
        Resultado r = new Resultado();
        long inicio = System.nanoTime();
        RedisClient redis = RedisClient.get();

        try {
            String json = redis.get(CHAVE);
            if (json != null) {
                for (String l : json.split("\n")) r.linhas.add(l.split("\t", -1));
                r.doCache = true;
                r.ttlRestante = redis.ttl(CHAVE);
                r.milissegundos = (System.nanoTime() - inicio) / 1_000_000;
                return r;
            }
        } catch (RedisClient.RedisErro e) {
            r.redisOk = false;
        }

        // CACHE MISS: consulta a VIEW no Supabase
        String sql = "SELECT plano, total_matriculas, ativas, canceladas, receita_total "
                   + "FROM vw_resumo_por_plano ORDER BY receita_total DESC";
        try (Connection conn = ConexaoBD.conectar();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                r.linhas.add(new String[]{
                    rs.getString("plano"),
                    String.valueOf(rs.getInt("total_matriculas")),
                    String.valueOf(rs.getInt("ativas")),
                    String.valueOf(rs.getInt("canceladas")),
                    rs.getBigDecimal("receita_total").toPlainString()});
            }
        }

        if (r.redisOk) {
            try {
                StringBuilder sb = new StringBuilder();
                for (String[] l : r.linhas) {
                    if (sb.length() > 0) sb.append('\n');
                    sb.append(String.join("\t", l));
                }
                if (sb.length() > 0) redis.setComTtl(CHAVE, sb.toString(), TTL_SEGUNDOS);
                r.ttlRestante = TTL_SEGUNDOS;
            } catch (RedisClient.RedisErro e) {
                r.redisOk = false;
            }
        }
        r.milissegundos = (System.nanoTime() - inicio) / 1_000_000;
        return r;
    }

    /** Apaga o cache. Chamado sempre que os dados do relatório mudam. */
    public static boolean invalidar() {
        try { return RedisClient.get().del(CHAVE) > 0; }
        catch (RuntimeException e) { return false; }
    }
}
