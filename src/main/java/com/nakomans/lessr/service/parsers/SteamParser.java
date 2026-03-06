package com.nakomans.lessr.service.parsers;

import com.nakomans.lessr.dao.GamesDatabase;
import com.nakomans.lessr.dto.SteamDto;
import com.nakomans.lessr.service.gamesinformation.RetrieveInformationFromSteam;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Service;
import java.io.IOException;
import java.text.Normalizer;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

@Service
public class SteamParser {

    private final GamesDatabase dataBase;
    private final RetrieveInformationFromSteam retrieveInformation;

    public SteamParser(GamesDatabase dataBase, RetrieveInformationFromSteam retrieveInformation) { 
        this.dataBase = dataBase; 
        this.retrieveInformation = retrieveInformation;
    }

    public String getStoreName() {
        return "Steam";
    }

    public SteamDto getGameInformation(String rawGameName, Document gamePage) {
        return new SteamDto(
            retrieveInformation.gameNameDisplay(gamePage),
            retrieveInformation.gameDescription(gamePage),
            retrieveInformation.gameBanner(gamePage),
            retrieveInformation.categorieTags(gamePage),
            retrieveInformation.showReviews(gamePage),
            retrieveInformation.showDeveloper(gamePage),
            retrieveInformation.showMetaCriticScore(gamePage),
            isGameInPromotion(gamePage),
            showGamePrice(gamePage),
            showGamePromotionPrice(gamePage),
            LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
        );
    }

    public Document loadSteamPage(String rawGameName) throws IOException {
        String appId = dataBase.getGameAppId(rawGameName)
                .orElseThrow(() -> new IllegalArgumentException("Jogo não encontrado: " + rawGameName));

        String urlName = Normalizer.normalize(rawGameName, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "").replaceAll("[^a-zA-Z0-9 ]", "").trim().replaceAll("\\s+", "_");

        Map<String, String> cookies = new HashMap<>();
        cookies.put("wants_mature_content", "1");
        cookies.put("birthtime", "978314400");
        cookies.put("lastagecheckage", "1-January-2001");
        cookies.put("Steam_Language", "brazilian");

        return Jsoup.connect("https://store.steampowered.com/app/" + appId + "/" + urlName + "/?l=brazilian")
                .cookies(cookies)
                .userAgent("Mozilla/5.0")
                .timeout(10000)
                .get();
    }

    public String showGamePrice(Document doc) {
        return retrieveInformation.safeSearch(doc, "div.discount_original_price")
                .map(Element::text)
                .orElseGet(() -> retrieveInformation.safeSearch(doc, "div.game_purchase_price.price")
                .map(Element::text).orElse("N/A"));
    }

    public String showGamePromotionPrice(Document doc) {
        return retrieveInformation.safeSearch(doc, "div.discount_final_price")
                .map(Element::text)
                .orElseGet(() -> showGamePrice(doc));
    }

    public Boolean isGameInPromotion(Document doc) {
        return retrieveInformation.safeSearch(doc, "div.discount_original_price").isPresent();
    }
}
