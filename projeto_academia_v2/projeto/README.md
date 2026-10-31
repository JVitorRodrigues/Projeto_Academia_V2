# Sistema de Gestão de Academia — Supabase + Redis

## Identificação
- **Aluno:** João Vitor Rodrigues Santos
- **Disciplina:** Banco de Dados
- **Professor:** Anderson Costa

## Problema que o projeto resolve
Uma academia precisa controlar alunos, planos, matrículas e pagamentos, e acompanhar a receita. Este sistema
centraliza esses dados em um banco na nuvem (**Supabase**) e usa o **Redis** para deixar o relatório financeiro
rápido e para proteger o login contra tentativas repetidas de senha.

## Objetivo e principais funcionalidades
- CRUD de alunos, instrutores e planos.
- Matrícula com pagamento (procedure `sp_matricular_aluno`), cancelamento, desconto de fidelidade (function).
- Relatório Financeiro por plano (view) **com cache no Redis**.
- Login com **limite de tentativas** controlado pelo Redis.

## Tecnologias
Java 17+ · Swing (interface gráfica) · JDBC · **Supabase (PostgreSQL na nuvem)** · **Redis (Upstash)**

## Arquitetura
```
Interface Swing ──JDBC (SSL)──▶ Supabase (PostgreSQL: tabelas, view, functions, procedures)
       │
       └──────RESP/TLS────────▶ Redis (cache do relatório + contador de tentativas de login)
```

## Integração com o Supabase
O Supabase é o banco de dados principal (PostgreSQL gerenciado). A aplicação conecta por JDBC pelo **Session pooler**
(porta 5432, compatível com IPv4) e usa:

| Recurso | Nome | Onde é usado |
|---|---|---|
| Tabelas | `alunos`, `instrutores`, `planos`, `matriculas`, `pagamentos`, `usuarios` | CRUD das telas |
| View | `vw_matriculas_detalhadas` | Tela Matrículas |
| View | `vw_resumo_por_plano` | Tela Relatório Financeiro (via cache) |
| Function | `fn_valor_final_plano`, `fn_situacao_aluno` | Telas Matrículas e Alunos |
| Procedure | `sp_matricular_aluno`, `sp_cancelar_matricula` | Botões Matricular e Cancelar |

Os dados gravados pela aplicação podem ser vistos em tempo real no **Table Editor** do painel do Supabase.

## Onde e por que o Redis foi usado
**1) Cache do Relatório Financeiro** (`RelatorioService.java`, padrão *cache-aside*)
- **Problema:** o relatório agrega matrículas e pagamentos de todos os planos (JOINs + SUM/COUNT) e é consultado
  com frequência, mas os dados mudam pouco. Cada abertura da tela gerava uma ida ao banco na nuvem (~1 s).
- **Solução:** a 1ª consulta lê a view no Supabase e guarda o resultado no Redis com expiração de 60 s
  (`SET ... EX 60`). As próximas leituras vêm do Redis (~1 ms). A tela mostra a origem (`REDIS (cache HIT)` ou
  `SUPABASE (cache MISS)`) e o tempo gasto.
- **Consistência:** ao matricular, cancelar ou excluir/alterar planos e alunos, a chave é apagada (`DEL`), então o
  relatório nunca mostra dado velho depois de uma alteração feita pela aplicação.
- **Benefícios:** menos carga e menos latência no banco, e a aplicação continua funcionando se o Redis cair.

**2) Limite de tentativas de login** (`LimiteLogin.java`)
- **Problema:** nada impedia tentar senhas indefinidamente (força bruta).
- **Solução:** a cada senha errada, `INCR` em uma chave por usuário, com `EXPIRE 60` na primeira falha. Com 5 falhas,
  o login é bloqueado até a chave expirar (`TTL` mostra o tempo restante). Login correto apaga a chave (`DEL`).
- **Por que Redis:** contador atômico com expiração automática, sem tabela extra nem rotina de limpeza.

## Como instalar e executar

### 1. Supabase (banco)
1. Crie um projeto em [supabase.com](https://supabase.com) e guarde a **senha do banco**.
2. Abra **SQL Editor**, cole o conteúdo de `database/setup_supabase.sql` e clique em **Run**.
3. Clique em **Connect** → **Session pooler** → aba **JDBC** e copie host, usuário (`postgres.<ref>`) e porta.

### 2. Redis (Upstash)
1. Crie um banco em [upstash.com](https://upstash.com) (plano gratuito).
2. Copie **Endpoint**, **Porta** e **Senha**. A URL fica `rediss://default:SENHA@ENDPOINT:PORTA`.

### 3. Configurar (sem publicar senhas)
Copie `config.properties.example` para `config.properties` (já está no `.gitignore`) e preencha. Alternativa:
definir as variáveis de ambiente `SUPABASE_DB_URL`, `SUPABASE_DB_USER`, `SUPABASE_DB_PASSWORD` e `REDIS_URL`.

### 4. Executar (na raiz do projeto)
- Windows: `executar.bat`
- Linux/macOS: `./executar.sh`

Login: `admin` / `admin123`.

> Sem configuração, a aplicação usa PostgreSQL e Redis locais (`localhost`), útil para desenvolvimento.

## Estrutura do repositório
```
/src                  código-fonte Java (telas, Config, RedisClient, RelatorioService, LimiteLogin)
/lib                  driver JDBC do PostgreSQL
/database             scripts SQL por tipo + setup_supabase.sql (tudo em um arquivo)
/docs                 diagrama (DER)
config.properties.example   modelo de configuração (sem segredos)
```

## Capturas de tela 
### Aplicação (Java Swing)

**1. Login** — acesso protegido por usuário e senha, com bloqueio temporário controlado pelo Redis.
![Login](docs/prints/01_login.png)

**2. Menu principal** — acesso a alunos, instrutores, planos, matrículas e ao relatório financeiro.
![Menu](docs/prints/02_menu.png)

**3. Matrículas (View + Function + Procedure)** — a tabela é lida da view `vw_matriculas_detalhadas` (com situação e total pago); o *valor final* de R$ 809,91 é calculado pela function `fn_valor_final_plano` no banco; os botões *Matricular* e *Cancelar matrícula* chamam as procedures.
![Matrículas](docs/prints/03_matriculas.png)

**4. Relatório Financeiro (Supabase + Redis)** — dados da view `vw_resumo_por_plano`. A linha laranja mostra **cache MISS**: a consulta foi ao Supabase (1214 ms) e o resultado foi salvo no Redis por 60 s.
![Relatório - cache MISS](docs/prints/04_relatorio_cache_miss.png)

### Integração com o Supabase

**5. Cadastro de instrutor na aplicação** — o instrutor *Roberto Alves* é cadastrado pela interface.
![Instrutor cadastrado](docs/prints/05_instrutor_cadastrado.png)

**6. Table Editor do Supabase (tabela `instrutores`)** — o mesmo registro, *Roberto Alves*, aparece gravado no banco na nuvem, o que comprova a comunicação entre a aplicação e o Supabase. Na lateral, estão as tabelas e as duas views do sistema.
![Supabase - instrutores](docs/prints/06_supabase_instrutores.png)

**7. Table Editor do Supabase (tabela `alunos`)** — alunos de exemplo e um aluno cadastrado pela aplicação.
![Supabase - alunos](docs/prints/07_supabase_alunos.png)

### Integração com o Redis

**8. Painel do Upstash (aba Uso)** — o banco Redis `academia` (região São Paulo, plano gratuito) já registra comandos recebidos da aplicação (6 no momento da captura), o que comprova que a conexão com o Redis está ativa.
![Upstash - uso](docs/prints/08_upstash_uso.png)

## Link do vídeo: 
https://drive.google.com/file/d/1rx-vBGw4irm1yYLhBGFGTrrZCZIGBu9_/view?usp=sharing

## Link do vídeo compactado:
https://drive.google.com/file/d/1DQHsLVGHWcTlWKsGdjhu1YA3soSZPiCe/view?usp=drive_link
