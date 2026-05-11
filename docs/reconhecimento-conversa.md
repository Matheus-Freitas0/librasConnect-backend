# Reconhecimento no modo conversa (alinhamento API / produto)

## Escopo

- **Conversa:** expressões curtas em texto (ex.: “Oi”, “Tchau”, “Sim”) e amostras de **gesto com movimento** (gravação manual com início/fim).
- Clips são **sequências temporais**: cada frame tem `t` (ms desde o início da gravação) e `hands` com landmarks normalizados.

## Pipeline de inferência

- Entrada: `durationMs` + `frames` ordenados por `t` monotônico.
- Antes de comparar, remove-se **no início e no fim** apenas frames **sem mão** (buracos nas bordas).
- Para cada amostra salva (mesmo formato JSON no banco), extraem-se só frames **com pelo menos uma mão**. Frames sem mão no meio do tempo permanecem fora da série usada no DTW (são ignorados; não há interpolação).
- A distância entre dois clips é um **DTW** (Dynamic Time Warping) sobre o custo médio por frame: para cada par de frames alinhados pelo DTW, calcula-se MSE dos landmarks **por papel** (`left` com `left`, `right` com `right`). Se não houver par compatível, usa-se um custo fixo moderado (desalinhamento de papéis).
- Se `query` ou amostra tiverem muitos frames, ambas as séries são **subamostradas uniformemente no eixo `t`** até no máximo `app.recognizer.dtw-max-series-points`, para limitar CPU/memória.
- Por `sign_id`, conserva-se a **melhor** distância entre o clip de entrada e **qualquer** amostra daquele sinal; depois aplica-se ranking global, `max-mean-distance` e margem ao segundo colocado.

## Treino (`POST /v1/samples`)

- Mesmo schema de clip que no reconhecimento; validações idênticas garantem hipótese temporal alinhada entre treino e inferência.

## Limites e erros

- **413:** corpo da requisição acima de `app.api.max-body-bytes` (filtro JWT / Tomcat).
- **Frames:** entre `MIN_FRAMES` e `MAX_FRAMES` (ver `ClipPayloadValidator`); pelo menos **dois** frames com mão detectada.
- **400:** formato JSON do clip, `t`, landmarks, duração — mensagens indicam o problema (sequência temporal, duração, etc.).

## Label e `sign.id`

- **Exibição:** `sign.label` na resposta de sucesso; o primeiro cadastro define o texto para aquele id canônico.
- **Agrupamento:** trim + espaços; `sign.id` slug (acentos removidos no slug, minúsculas). Variações de caixa/espaco colapsam na mesma expressão.

## Contratos HTTP

- `POST /v1/recognize` — `200` com `recognized` + `sign` ou `recognized: false` + `message`.
- **Léxico vazio:** **`409 Conflict`**, código `CATALOG_EMPTY` (cliente: orientar Treinar conversa).

## Ajuste fino

- `app.recognizer.max-mean-distance` e `app.recognizer.min-gap-next-sign` calibram aceitar vs rejeitar (métrica agora é média ao longo do caminho DTW — pode exigir recom calibragem frente ao antigo alinhamento por índice).
- `app.recognizer.dtw-max-series-points`: mais pontos = mais fidelidade e mais custo.
