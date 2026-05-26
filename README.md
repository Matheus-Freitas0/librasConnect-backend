# librasConnect — Backend

Backend do **librasConnect**, plataforma de reconhecimento de **Libras** (Língua Brasileira de Sinais) a partir de clipes de _landmarks_ de mãos. Permite cadastrar amostras de treino para um léxico de sinais e reconhecer expressões enviadas pelo cliente com comparação temporal baseada em DTW (Dynamic Time Warping).

## Stack

- **Java 21**
- **Spring Boot 4.0.6** (Web MVC, Security, Data JPA, Validation)
- **Spring Security + JWT** (`jjwt 0.12.5`)
- **PostgreSQL 16+** com **Flyway** para migrações
- **Lombok**
- **Maven** (wrapper incluído)
- **Docker / docker-compose** para deploy

## Arquitetura

```
src/main/java/com/librasConnect/system
├── SystemApplication.java          → entrypoint Spring Boot
├── config/                         → CORS, Security, Jackson, Password
├── controllers/                    → AuthController + v1 (Recognize, Samples)
├── dto/v1 + dtos/                  → contratos de entrada/saída da API
├── enums/                          → Rule (USER, ADMIN)
├── exception/                      → ApiException e handler global
├── models/                         → entidades JPA (User, Sign, SignSample, Domain)
├── repositories/                   → Spring Data JPA
├── security/                       → JwtService, JwtAuthFilter, failure handler
├── services/ + services/impl       → UserService, TrainingSampleService,
│                                     RecognitionOrchestrator, LexiconCache
└── signs/                          → pipeline de comparação de clipes
                                     (DTW, subsampling, bimanual, validação)
```

### Pipeline de reconhecimento

1. `ClipPayloadValidator` valida o clipe recebido (frames, mãos, landmarks).
2. `ClipSegmentTailTrim` remove frames finais sem mãos detectadas.
3. `RecognitionLexiconCache` mantém em memória os sinais cadastrados e suas amostras.
4. `ClipTemporalDistance` calcula custo entre query e amostras usando DTW normalizado.
5. Uma fase _coarse_ opcional (subsampling agressivo) seleciona os top-N sinais candidatos antes do DTW completo.
6. `RecognitionOrchestrator` aplica thresholds (`max-mean-distance`, `min-gap-next-sign`) e devolve o melhor sinal ou `notRecognized`.

## Pré-requisitos

- JDK 21
- Maven 3.9+ (ou usar o wrapper `./mvnw`)
- PostgreSQL acessível
- Docker (opcional, para subir via container)

## Configuração

A aplicação é parametrizada por variáveis de ambiente (com defaults em `application.properties`):

| Variável | Default | Descrição |
| --- | --- | --- |
| `DATASOURCE_URL` | `jdbc:postgresql://localhost:5432/librasconnect` | URL JDBC do Postgres |
| `DATASOURCE_USERNAME` | `postgres` | Usuário do banco |
| `DATASOURCE_PASSWORD` | _(vazio)_ | Senha do banco |
| `HIKARI_MAX_LIFETIME` | `600000` | Vida máxima de conexão (ms) |
| `HIKARI_KEEPALIVE_TIME` | `120000` | Keepalive de conexão (ms) |
| `URL_CORS_PERMISSION` | `http://localhost:5173` | Origem permitida no CORS |
| `JWT_SECRET` | _(default longo, **trocar em produção**)_ | Segredo HMAC para assinar JWT |
| `app.api.max-body-bytes` | `4194304` | Limite do corpo (bytes) para rotas `/api/v1/*` |
| `app.recognizer.enabled` | `true` | Liga/desliga o reconhecedor |
| `app.recognizer.max-mean-distance` | `0.012` | Distância média máxima aceita |
| `app.recognizer.min-gap-next-sign` | `0.004` | Gap mínimo entre 1º e 2º sinal candidato |
| `app.recognizer.dtw-max-series-points` | `96` | Pontos máximos no DTW fino |
| `app.recognizer.coarse-series-points` | `48` | Pontos no DTW _coarse_ (0 desliga) |
| `app.recognizer.coarse-top-signs` | `8` | Top-N sinais após fase _coarse_ |

Crie um arquivo `.env` na raiz (não commitado) para o `docker-compose`:

```env
DATASOURCE_URL=jdbc:postgresql://postgres:5432/librasconnect
DATASOURCE_USERNAME=postgres
DATASOURCE_PASSWORD=postgres
URL_CORS_PERMISSION=https://librasconnect.exemplo.com
JWT_SECRET=substitua_por_um_segredo_de_ao_menos_32_caracteres
```

## Como rodar

### Local (Maven wrapper)

```bash
./mvnw spring-boot:run
```

A aplicação sobe em `http://localhost:8080`. O Flyway aplica as migrations automaticamente em `src/main/resources/db/migration`.

### Build do jar

```bash
./mvnw clean package
java -jar target/system-0.0.1-SNAPSHOT.jar
```

### Docker

```bash
docker compose up -d --build
```

O `docker-compose.yml` espera duas networks externas (`nginx-proxy` e `n8n-kk4n_default`). Ajuste conforme a sua infraestrutura.

## API

Base URL: `http://localhost:8080`

### Autenticação

`POST /auth/login`

```json
{
  "email": "usuario@exemplo.com",
  "password": "senha"
}
```

Resposta:

```json
{ "token": "<jwt>" }
```

Use o token nas demais rotas no header `Authorization: Bearer <jwt>`.

### Reconhecer um clipe

`POST /v1/recognize`

```json
{
  "durationMs": 1500,
  "frames": [
    {
      "t": 0,
      "hands": [
        {
          "role": "RIGHT",
          "landmarks": [[0.51, 0.32, 0.0], "... 21 pontos x,y,z ..."]
        }
      ]
    }
  ]
}
```

Resposta de sucesso:

```json
{ "recognized": true, "sign": { "id": "ola", "label": "Olá" } }
```

Resposta quando não reconhecido:

```json
{ "recognized": false, "message": "Expressão não encontrada no léxico cadastrado." }
```

### Cadastrar amostra de treino

`POST /v1/samples`

```json
{
  "label": "Olá",
  "description": "Sinal de saudação",
  "durationMs": 1200,
  "frames": [/* mesmo formato do recognize */]
}
```

Resposta `201 Created`:

```json
{
  "id": "ola-001",
  "signId": "ola",
  "createdAt": "2026-05-26T18:00:00Z",
  "durationMs": 1200,
  "frameCount": 36
}
```

### Formato de `landmarks`

Cada mão envia 21 pontos no formato `[x, y, z]`, compatível com o MediaPipe Hands. `role` aceita identificadores de mão (`LEFT`, `RIGHT`). A propriedade `t` em cada frame é o timestamp em milissegundos relativo ao início do clipe.

## Migrations

Localizadas em `src/main/resources/db/migration`, seguem o padrão Flyway `V{n}__descricao.sql`. Para adicionar uma nova migração, crie `V{n+1}__nome.sql` — nunca edite arquivos já aplicados em ambientes ativos.

## Testes

```bash
./mvnw test
```

## Licença

Projeto interno do librasConnect.
