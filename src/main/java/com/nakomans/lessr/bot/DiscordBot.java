package com.nakomans.lessr.bot;

import com.nakomans.lessr.dao.GamesDatabase;
import com.nakomans.lessr.dao.WishlistDatabase;
import com.nakomans.lessr.dto.EnebaDto;
import com.nakomans.lessr.dto.SteamDto;
import com.nakomans.lessr.dto.WishlistItem;
import com.nakomans.lessr.service.gamesinformation.RetrieveGamesInformation;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.InteractionHook;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.awt.Color;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

@Component
public class DiscordBot extends ListenerAdapter {

    private final RetrieveGamesInformation gameService;
    private final GamesDatabase gamesDatabase;
    private final WishlistDatabase wishlistDatabase;

    @Value("${discord.bot.token}")
    private String token;

    public DiscordBot(RetrieveGamesInformation gameService, GamesDatabase gamesDatabase, WishlistDatabase wishlistDatabase) {
        this.gameService = gameService;
        this.gamesDatabase = gamesDatabase;
        this.wishlistDatabase = wishlistDatabase;
    }

    @PostConstruct
    public void startBot() throws InterruptedException {
        JDA jda = JDABuilder.createDefault(token).addEventListeners(this).build();
        jda.awaitReady();
        
        jda.upsertCommand("check", "Busca os preços de um jogo em várias lojas")
                .addOption(OptionType.STRING, "jogo", "Nome do jogo para buscar", true)
                .queue();

        jda.upsertCommand("wishlist", "Gerencie sua lista de desejos particular")
                .addSubcommands(
                    new SubcommandData("add", "Adiciona um jogo à sua wishlist")
                        .addOption(OptionType.STRING, "jogo", "Nome do jogo", true),
                    new SubcommandData("list", "Mostra sua wishlist com os preços atualizados"),
                    new SubcommandData("remove", "Remove um jogo da sua wishlist")
                        .addOption(OptionType.STRING, "jogo", "Nome do jogo", true)
                ).queue();
    }

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        String commandName = event.getName();

        if (commandName.equals("check")) {
            String gameName = event.getOption("jogo").getAsString();
            event.deferReply().queue();
            CompletableFuture.runAsync(() -> executeSearch(event.getHook(), gameName));
        } 
        else if (commandName.equals("wishlist")) {
            handleWishlistCommand(event);
        }
    }

    private void handleWishlistCommand(SlashCommandInteractionEvent event) {
        String subCmd = event.getSubcommandName();
        String userId = event.getUser().getId();

        if ("add".equals(subCmd)) {
            String gameName = event.getOption("jogo").getAsString();
            boolean success = wishlistDatabase.addGame(userId, gameName);
            if (success) {
                event.reply("✅ **" + formatTitle(gameName) + "** adicionado à sua wishlist!").setEphemeral(true).queue();
            } else {
                event.reply("❌ Falha! Ou você já tem esse jogo na lista, ou já bateu o limite de 5 jogos.").setEphemeral(true).queue();
            }
        } 
        else if ("remove".equals(subCmd)) {
            String gameName = event.getOption("jogo").getAsString();
            boolean success = wishlistDatabase.removeGame(userId, gameName);
            if (success) {
                event.reply("🗑️ **" + formatTitle(gameName) + "** removido da sua wishlist.").setEphemeral(true).queue();
            } else {
                event.reply("❌ Jogo não encontrado na sua lista.").setEphemeral(true).queue();
            }
        } 
        else if ("list".equals(subCmd)) {
            event.deferReply(true).queue();
            
            CompletableFuture.runAsync(() -> {
                List<WishlistItem> items = wishlistDatabase.getUserWishlist(userId);
                
                if (items.isEmpty()) {
                    event.getHook().editOriginal("Sua wishlist está vazia! Use `/wishlist add` para começar.").queue();
                    return;
                }

                StringBuilder receipt = new StringBuilder();
                receipt.append("🛒 **SUA WISHLIST (").append(items.size()).append("/5 Slots Usados)**\n");
                receipt.append("━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n");

                for (WishlistItem item : items) {
                    receipt.append("🎮 **").append(formatTitle(item.gameName())).append("**\n");
                    
                    try {
                        Map<String, Object> info = gameService.getAllStoresInfo(item.gameName());
                        SteamDto steam = (SteamDto) info.get("steam");
                        EnebaDto eneba = (EnebaDto) info.get("eneba");
                        SteamDto nuuvem = (SteamDto) info.get("nuuvem");

                        String steamPrice = steam != null ? extractSimplePrice(steam.promotionPrice(), steam.originalGamePrice()) : "Indisponível";
                        String nuuvemPrice = nuuvem != null ? extractSimplePrice(nuuvem.promotionPrice(), nuuvem.originalGamePrice()) : "Indisponível";
                        String enebaPrice = eneba != null ? extractSimplePrice(eneba.promotionPrice(), eneba.originalGamePrice()) : "Indisponível";

                        receipt.append("Steam: ").append(steamPrice)
                               .append("  |  Nuuvem: ").append(nuuvemPrice)
                               .append("  |  Eneba: ").append(enebaPrice).append("\n\n");
                    } catch (Exception e) {
                        receipt.append("❌ *Erro ao buscar preços deste jogo.*\n\n");
                    }
                }
                receipt.append("━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
                
                event.getHook().editOriginal(receipt.toString()).queue();
            });
        }
    }

    private String extractSimplePrice(String promoPrice, String originalPrice) {
        if (originalPrice == null || originalPrice.isBlank() || originalPrice.contains("não encontrado") || originalPrice.contains("N/A") || originalPrice.toLowerCase().contains("não disponível")) {
            return "Indisponível";
        }
        if (originalPrice.toLowerCase().contains("gratuito") || originalPrice.toLowerCase().contains("free")) {
            return "Grátis";
        }
        if (promoPrice != null && !promoPrice.isBlank()) {
            return "**" + promoPrice + "**";
        }
        return originalPrice;
    }

    @Override
    public void onButtonInteraction(ButtonInteractionEvent event) {
        String componentId = event.getComponentId();
        
        if (componentId.startsWith("retry_")) {
            String gameName = componentId.replace("retry_", "");
            event.editMessage("🔍 Buscando detalhes de **" + formatTitle(gameName) + "**...").setComponents().queue();
            CompletableFuture.runAsync(() -> executeSearch(event.getHook(), gameName));
        } 
        else if (componentId.startsWith("page_")) {
            String[] parts = componentId.split("_", 3);
            int targetPage = Integer.parseInt(parts[1]);
            String originalSearch = parts[2];
            event.deferEdit().queue(); 
            sendSuggestionMenu(event.getHook(), originalSearch, targetPage);
        }
    }

    private void executeSearch(InteractionHook hook, String gameName) {
        try {
            Map<String, Object> info = gameService.getAllStoresInfo(gameName);

            SteamDto steam = (SteamDto) info.get("steam");
            EnebaDto eneba = (EnebaDto) info.get("eneba");
            SteamDto nuuvem = (SteamDto) info.get("nuuvem");

            EmbedBuilder embed = new EmbedBuilder();
            
            String steamSearchUrl = "https://store.steampowered.com/search/?term=" + steam.gameName().replace(" ", "+");
            embed.setTitle(steam.gameName(), steamSearchUrl);
            embed.setDescription(">>> **Descrição:**\n" + steam.description() + "\n\u200B");
            embed.setImage(steam.banner());
            embed.setColor(getMetacriticColor(steam.metacriticScore()));

            String steamPrice = steam != null ? buildPriceString(steam.inPromotion(), steam.originalGamePrice(), steam.promotionPrice()) : "**Indisponível**";
            String nuuvemPrice = nuuvem != null ? buildPriceString(nuuvem.inPromotion(), nuuvem.originalGamePrice(), nuuvem.promotionPrice()) : "**Indisponível**";
            String enebaPrice = eneba != null ? buildPriceString(eneba.inPromotion(), eneba.originalGamePrice(), eneba.promotionPrice()) : "**Indisponível**";

            embed.addField("Steam", steamPrice, true);
            embed.addField("Nuuvem", nuuvemPrice, true);
            embed.addField("Eneba", enebaPrice, true);
            embed.addField("Informações:", "**Metacritic:** " + steam.metacriticScore() + "\n**Reviews:** " + steam.reviews() + "\n**Tags:** " + steam.categories(), false);
            embed.setFooter("Buscador Lessr").setTimestamp(java.time.Instant.now());

            hook.editOriginalEmbeds(embed.build()).queue();

        } catch (Exception e) {
            sendSuggestionMenu(hook, gameName, 0);
        }
    }

    private void sendSuggestionMenu(InteractionHook hook, String gameName, int page) {
        List<String> suggestions = gamesDatabase.suggestSimilarGames(gameName);
        
        if (suggestions.isEmpty()) {
            hook.editOriginal("❌ Nada encontrado nem parecido com isso. Tem certeza do nome?").setComponents().queue();
            return;
        }

        int pageSize = 5;
        int startIndex = page * pageSize;
        int endIndex = Math.min(startIndex + pageSize, suggestions.size());
        List<String> currentList = suggestions.subList(startIndex, endIndex);

        List<Button> gameButtons = new ArrayList<>();
        Set<String> usedIds = new HashSet<>(); 

        for (String sug : currentList) {
            String buttonValue = sug.length() > 80 ? sug.substring(0, 80) : sug;
            String customId = "retry_" + buttonValue;
            
            if (!usedIds.contains(customId)) {
                gameButtons.add(Button.secondary(customId, formatTitle(buttonValue)).withEmoji(Emoji.fromUnicode("🎮")));
                usedIds.add(customId);
            }
        }

        List<Button> navButtons = new ArrayList<>();
        if (page > 0) {
            navButtons.add(Button.primary("page_" + (page - 1) + "_" + gameName, "◀️ Anterior"));
        }
        if (endIndex < suggestions.size()) {
            navButtons.add(Button.primary("page_" + (page + 1) + "_" + gameName, "Próximo ▶️"));
        }

        String msg = "❌ Jogo não encontrado exatamente com esse nome.\n\n**Você quis dizer algum desses?** (Página " + (page + 1) + ")\n\u200B";

        if (navButtons.isEmpty()) {
            hook.editOriginal(msg).setComponents(ActionRow.of(gameButtons)).queue();
        } else {
            hook.editOriginal(msg).setComponents(ActionRow.of(gameButtons), ActionRow.of(navButtons)).queue();
        }
    }

    private String formatTitle(String text) {
        if (text == null || text.isEmpty()) return text;
        String[] words = text.split(" ");
        StringBuilder sb = new StringBuilder();
        for (String word : words) {
            if (!word.isEmpty()) {
                sb.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1).toLowerCase()).append(" ");
            }
        }
        return sb.toString().trim();
    }

    private String buildPriceString(boolean inPromotion, String originalPrice, String promoPrice) {
        if (originalPrice == null || originalPrice.isBlank() || originalPrice.contains("não encontrado") || originalPrice.contains("N/A") || originalPrice.toLowerCase().contains("não disponível")) {
            return "**Indisponível**";
        }
        if (originalPrice.toLowerCase().contains("gratuito") || originalPrice.toLowerCase().contains("free")) {
            return "**De graça!**";
        }
        if (!inPromotion) {
            return "**" + originalPrice + "**";
        }
        return "De: ~~" + originalPrice + "~~\nPor: **" + promoPrice + "**";
    }

    private Color getMetacriticColor(String scoreText) {
        try {
            int score = Integer.parseInt(scoreText.trim());
            if (score >= 80) return Color.decode("#44ce1b");
            if (score >= 50) return Color.decode("#f2d011");
            return Color.decode("#f22311");
        } catch (NumberFormatException e) {
            return Color.decode("#2b2d31");
        }
    }
}