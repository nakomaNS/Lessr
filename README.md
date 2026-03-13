# Lessr Bot

API que consulta diariamente promoções de jogos em lojas confiáveis, retorna o resultado organizado, com registro de histórico das promoções, pico de valores, plug & play para bots do Discord e Telegram.

![Java](https://img.shields.io/badge/Java-21-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.4.3-6DB33F?style=for-the-badge&logo=spring&logoColor=white)
![SQLite](https://img.shields.io/badge/SQLite-07405E?style=for-the-badge&logo=sqlite&logoColor=white)
![Selenium](https://img.shields.io/badge/Selenium-43B02A?style=for-the-badge&logo=selenium&logoColor=white)
![Discord](https://img.shields.io/badge/Discord_JDA-5865F2?style=for-the-badge&logo=discord&logoColor=white)

---

## Visão Geral

O Lessr é um assistente focado em procurar o melhor preço para jogos de PC Steam. Ele extrai informações oficiais (Metacritic, tags, descrições) e varre o mercado de keys em tempo real. 
O sistema foi desenhado para ser resiliente a quedas de sites de terceiros, rápido através de requisições assíncronas e altamente tolerante a erros de digitação dos usuários através de algoritmos de aproximação de texto.

---

## Stack

- **Linguagem:** Java 21  
- **Framework:** Spring Boot 3.4.3  
- **API do Discord:** JDA (Java Discord API) 5.x  
- **Banco de Dados:** SQLite (Armazenamento leve de AppIDs e Wishlists)  
- **Web Scraping:** Jsoup (Parsing estático) & Selenium WebDriver (Parsing dinâmico / Anti-bot bypass)
- **JSON Parsing:** Gson
- **Build Tool:** Maven

---

## Estrutura do Projeto

```text
src/main/java/com/nakomans/lessr
├── bot
│   └── DiscordBot.java                 # Listeners, Slash Commands e Renderização de UI (Embeds/Botões)
├── dao
│   ├── GamesDatabase.java              # Acesso ao SQLite (Motor Fuzzy/Levenshtein)
│   └── WishlistDatabase.java           # Persistência isolada das listas de usuários
├── dto
│   ├── SteamDto.java                   # Record padronizado de resposta da Steam/Nuuvem
│   ├── EnebaDto.java                   # Record padronizado de resposta da Eneba
│   └── WishlistItem.java               # Record de retorno da wishlist
└── service
    ├── gamesinformation
    │   └── RetrieveGamesInformation.java # Orquestrador Assíncrono (CompletableFutures)
    └── parsers
        ├── SteamParser.java            # Web scraper via Jsoup
        ├── NuuvemParser.java           # Web scraper via Jsoup
        └── EnebaParser.java            # Web scraper via Selenium
```
---
## Comandos Principais

### 1. Checar Preços
Varre as lojas simultaneamente e retorna um Embed rico com capa, informações do jogo e os preços lado a lado. Caso o nome esteja incorreto, devolve botões interativos com sugestões corrigidas.

**Comando:** `/check jogo:[nome]`

**Exemplo de Resposta:**
```text
[Capa do Jogo]
DARK SOULS III
Descrição: DARK SOULS™ continua a ultrapassar seus próprios limites...
Steam: R$ 229,90  |  Nuuvem: R$ 229,90  |  Eneba: R$ 98,50
Informações: Metacritic: 89 | Reviews: Muito positivas
```

### 2. Lista de desejos
Gerencia uma lista de desejos atrelada ao ID único do usuário no Discord. Mensagens de resposta são efêmeras. O sistema bloqueia duplicatas e impõe limites.
#### Comandos disponíveis:
- **Adicionar jogo a lista de desejos:**
  `/wishlist add [NomeDoJogo]`
- **Remover jogo da lista:**
  `/wishlist remove [NomeDoJogo]`
- **Listar wishlist:**
  `/wishlist list` → Realiza scrape de todos os jogos da lista e retorna os **preços atualizados** da Steam, Nuuvem e Eneba.
---

## Fluxo de Processamento — Comando `/check`

1. Usuário executa:  
   `/check Elden Ring`

2. **Lessr** responde imediatamente:  
   "Buscando..."

3. **RetrieveGamesInformation** dispara buscas paralelas:  
   - Thread 1 → `SteamParser` (Jsoup)  
   - Thread 2 → `NuuvemParser` (Jsoup)  
   - Thread 3 → `EnebaParser` (Selenium)

4. Aguarda todas as threads com `CompletableFuture.join()`

5. Resultado:  
   - **Se encontrou**: monta Embed bonito (capa, preços, Metacritic, etc.) e edita a mensagem  
   - **Se falhou / não encontrou**:  
     → Consulta `GamesDatabase` (base em memória)  
     → Calcula **Distância de Levenshtein**  
     → Gera botões interativos de sugestão ("Você quis dizer?")  
     → Envia pro Discord para que o usuário possa selecionar a opção correta
---
