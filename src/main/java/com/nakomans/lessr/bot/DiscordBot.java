package com.nakomans.lessr.bot;

import com.nakomans.lessr.dto.EnebaDto;
import com.nakomans.lessr.dto.SteamDto;
import com.nakomans.lessr.service.gamesinformation.RetrieveGamesInformation;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import jakarta.annotation.PostConstruct;
import java.awt.Color;
import java.util.Map;

@Component
public class DiscordBot extends ListenerAdapter {

    private final RetrieveGamesInformation gameService;

    @Value("${discord.bot.token}")
    private String token;

    public DiscordBot(RetrieveGamesInformation gameService) {
        this.gameService = gameService;
    }

    @PostConstruct
    public void startBot() throws InterruptedException {
        JDA jda = JDABuilder.createDefault(token)
                .addEventListeners(this)
                .build();

        jda.awaitReady();
        jda.upsertCommand("check", "Busca os preços de um jogo em várias lojas")
                .addOption(OptionType.STRING, "jogo", "Nome do jogo para buscar", true)
                .queue();
    }

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        if (!event.getName().equals("check")) return;

        String gameName = event.getOption("jogo").getAsString();
        event.deferReply().queue();

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

            String steamPrice = buildPriceString(steam.inPromotion(), steam.originalGamePrice(), steam.promotionPrice());
            String nuuvemPrice = buildPriceString(nuuvem.inPromotion(), nuuvem.originalGamePrice(), nuuvem.promotionPrice());
            String enebaPrice = buildPriceString(eneba.inPromotion(), eneba.originalGamePrice(), eneba.promotionPrice());

            embed.addField("Steam", steamPrice, true);
            embed.addField("Nuuvem", nuuvemPrice, true);
            embed.addField("Eneba", enebaPrice, true);

            embed.addField("Informações:", "**Metacritic:** " + steam.metacriticScore() + "\n**Reviews:** " + steam.reviews() + "\n**Tags:** " + steam.categories(), false);

            embed.setFooter("Buscador Lessr")
                 .setTimestamp(java.time.Instant.now());

            event.getHook().sendMessageEmbeds(embed.build()).queue();

        } catch (Exception e) {
            event.getHook().sendMessage("❌ Falha na busca. O jogo existe mesmo? Erro: " + e.getMessage()).queue();
        }
    }

    private String buildPriceString(boolean inPromotion, String originalPrice, String promoPrice) {
        if (originalPrice == null || originalPrice.toLowerCase().contains("gratuito") || originalPrice.toLowerCase().contains("free")) {
            return "**De graça!**";
        }
        if (originalPrice.contains("não encontrado") || originalPrice.contains("N/A") || originalPrice.isBlank()) {
            return "**Indisponível**";
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