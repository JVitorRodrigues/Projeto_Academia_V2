import javax.net.ssl.SSLSocketFactory;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Cliente Redis mínimo (protocolo RESP), sem dependências externas.
 * Aceita redis://[usuario:senha@]host:porta e rediss:// (TLS, usado pelo Upstash/Redis Cloud).
 * Se o Redis ficar indisponível, as chamadas falham rápido e a aplicação continua usando só o banco.
 */
public class RedisClient {

    /** Erro devolvido pelo servidor Redis ou indisponibilidade do serviço. */
    public static class RedisErro extends RuntimeException {
        public RedisErro(String msg) { super(msg); }
    }

    private static RedisClient instancia;

    public static synchronized RedisClient get() {
        if (instancia == null) instancia = new RedisClient(Config.get("REDIS_URL", "redis://localhost:6379"));
        return instancia;
    }

    private final String host, usuario, senha;
    private final int porta;
    private final boolean tls;
    private Socket socket;
    private BufferedInputStream in;
    private OutputStream out;
    private long tentarNovamenteApos = 0;

    public RedisClient(String url) {
        try {
            URI u = new URI(url);
            tls = "rediss".equalsIgnoreCase(u.getScheme());
            host = u.getHost();
            porta = u.getPort() == -1 ? 6379 : u.getPort();
            String info = u.getUserInfo();
            if (info == null) { usuario = null; senha = null; }
            else if (info.contains(":")) {
                String[] p = info.split(":", 2);
                usuario = p[0].isEmpty() ? null : p[0];
                senha = p[1];
            } else { usuario = null; senha = info; }
        } catch (URISyntaxException e) {
            throw new RedisErro("REDIS_URL inválida: " + e.getMessage());
        }
    }

    // ── Comandos usados pela aplicação ──────────────────────────────
    public String get(String chave)                     { return (String) command("GET", chave); }
    public void   setComTtl(String chave, String valor, int segundos) { command("SET", chave, valor, "EX", String.valueOf(segundos)); }
    public long   del(String chave)                     { return (Long) command("DEL", chave); }
    public long   incr(String chave)                    { return (Long) command("INCR", chave); }
    public void   expire(String chave, int segundos)    { command("EXPIRE", chave, String.valueOf(segundos)); }
    public long   ttl(String chave)                     { return (Long) command("TTL", chave); }
    public String ping()                                { return (String) command("PING"); }

    // ── Infraestrutura ─────────────────────────────────────────────
    public synchronized Object command(String... args) {
        if (System.currentTimeMillis() < tentarNovamenteApos)
            throw new RedisErro("Redis indisponível (nova tentativa em instantes)");
        try {
            try {
                garantirConexao();
                return enviar(args);
            } catch (IOException e) {          // conexão caiu: reconecta uma vez
                fechar();
                garantirConexao();
                return enviar(args);
            }
        } catch (IOException e) {
            fechar();
            tentarNovamenteApos = System.currentTimeMillis() + 15_000;
            throw new RedisErro("Redis indisponível: " + e.getMessage());
        }
    }

    private void garantirConexao() throws IOException {
        if (socket != null && !socket.isClosed()) return;
        Socket s = new Socket();
        s.connect(new InetSocketAddress(host, porta), 3000);
        s.setSoTimeout(3000);
        if (tls) s = ((SSLSocketFactory) SSLSocketFactory.getDefault()).createSocket(s, host, porta, true);
        socket = s;
        in = new BufferedInputStream(s.getInputStream());
        out = s.getOutputStream();
        if (senha != null) {
            if (usuario != null) enviar("AUTH", usuario, senha); else enviar("AUTH", senha);
        }
    }

    private void fechar() {
        try { if (socket != null) socket.close(); } catch (IOException ignorado) { }
        socket = null;
    }

    private Object enviar(String... args) throws IOException {
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        buf.write(("*" + args.length + "\r\n").getBytes(StandardCharsets.UTF_8));
        for (String a : args) {
            byte[] b = a.getBytes(StandardCharsets.UTF_8);
            buf.write(("$" + b.length + "\r\n").getBytes(StandardCharsets.UTF_8));
            buf.write(b);
            buf.write("\r\n".getBytes(StandardCharsets.UTF_8));
        }
        out.write(buf.toByteArray());
        out.flush();
        return lerResposta();
    }

    private Object lerResposta() throws IOException {
        int tipo = in.read();
        if (tipo < 0) throw new EOFException("conexão encerrada");
        String linha = lerLinha();
        switch (tipo) {
            case '+': return linha;
            case '-': throw new RedisErro(linha);
            case ':': return Long.parseLong(linha);
            case '$': {
                int n = Integer.parseInt(linha);
                if (n < 0) return null;
                byte[] dados = in.readNBytes(n);
                in.read(); in.read();           // CRLF final
                return new String(dados, StandardCharsets.UTF_8);
            }
            case '*': {
                int n = Integer.parseInt(linha);
                List<Object> lista = new ArrayList<>();
                for (int i = 0; i < n; i++) lista.add(lerResposta());
                return lista;
            }
            default: throw new IOException("Resposta Redis inesperada: " + (char) tipo);
        }
    }

    private String lerLinha() throws IOException {
        StringBuilder sb = new StringBuilder();
        int c;
        while ((c = in.read()) != -1) {
            if (c == '\r') { in.read(); break; }
            sb.append((char) c);
        }
        return sb.toString();
    }
}
