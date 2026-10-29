/**
 * Proteção contra força bruta usando Redis: contador de falhas por usuário com expiração automática.
 *  - INCR + EXPIRE: após 5 erros em 60 s o login fica bloqueado até a chave expirar.
 *  - Se o Redis estiver fora do ar, o login continua funcionando (sem bloqueio).
 */
public class LimiteLogin {
    public static final int MAX_FALHAS = 5;
    public static final int JANELA_SEGUNDOS = 60;

    private static String chave(String usuario) { return "academia:login:falhas:" + usuario.toLowerCase(); }

    /** Retorna os segundos restantes de bloqueio (0 = liberado). */
    public static long segundosBloqueado(String usuario) {
        try {
            String v = RedisClient.get().get(chave(usuario));
            if (v != null && Long.parseLong(v) >= MAX_FALHAS)
                return Math.max(1, RedisClient.get().ttl(chave(usuario)));
        } catch (RuntimeException e) { /* Redis fora: não bloqueia */ }
        return 0;
    }

    /** Registra uma falha e devolve o total de falhas na janela (0 se Redis indisponível). */
    public static long registrarFalha(String usuario) {
        try {
            long n = RedisClient.get().incr(chave(usuario));
            if (n == 1) RedisClient.get().expire(chave(usuario), JANELA_SEGUNDOS);
            return n;
        } catch (RuntimeException e) { return 0; }
    }

    public static void limpar(String usuario) {
        try { RedisClient.get().del(chave(usuario)); } catch (RuntimeException e) { }
    }
}
