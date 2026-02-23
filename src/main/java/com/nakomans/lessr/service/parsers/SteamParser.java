package com.nakomans.lessr.service.parsers;

import com.nakomans.lessr.dao.GamesDatabase;
import com.nakomans.lessr.dto.GameInfoDto;
import com.nakomans.lessr.service.gamesinformation.RetrieveInformationFromSteam;
import java.io.IOException;
import java.text.Normalizer;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.jsoup.nodes.Element;
import java.util.regex.Pattern;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Service;

@Service
public class SteamParser {

    private final GamesDatabase dataBase;
    private final RetrieveInformationFromSteam retrieveInformation;
    public SteamParser(GamesDatabase dataBase, RetrieveInformationFromSteam retrieveInformation) { this.dataBase = dataBase; this.retrieveInformation = retrieveInformation;}

    public GameInfoDto getGameInformation(String rawGameName) throws IOException {
        Document gamePage = loadSteamPage(rawGameName);
        
        String gameName = retrieveInformation.gameNameDisplay(gamePage);
        String description = retrieveInformation.gameDescription(gamePage);
        String banner = retrieveInformation.gameBanner(gamePage);
        String categories = retrieveInformation.categorieTags(gamePage);
        String reviews = retrieveInformation.showReviews(gamePage);
        String developer = retrieveInformation.showDeveloper(gamePage);
        String metacriticScore = retrieveInformation.showMetaCriticScore(gamePage);
        Boolean inPromotion = isGameInPromotion(gamePage);
        String originalGamePrice = showGamePrice(gamePage);
        String promotionPrice = showGamePromotionPrice(gamePage);
        LocalDate date = LocalDate.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        String dateFormatted = date.format(formatter);

        return new GameInfoDto(
            gameName,
            description,
            banner,
            categories,
            reviews,
            developer,
            metacriticScore,
            inPromotion,
            originalGamePrice,
            promotionPrice,
            dateFormatted
        );
    }

    public String gameNameFormatterForSteam(String rawGameName) {
        String gameNameWithAccents = Normalizer.normalize(rawGameName, Normalizer.Form.NFD);
        Pattern accents = Pattern.compile("\\p{M}");
        String gameNameWithoutAccents = accents.matcher(gameNameWithAccents).replaceAll("");
        String gameNameFormatted = gameNameWithoutAccents.trim().replaceAll("[^a-zA-Z0-9 ]","");
        return gameNameFormatted.trim().replaceAll("\s+", "_");
    }

    public Document loadSteamPage(String rawGameName) throws IOException {
        String userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36";
        Map<String,String> cookiesForSteam = new HashMap<>();
        cookiesForSteam.put("wants_mature_content","1");
        cookiesForSteam.put("birthtime","978314400");
        cookiesForSteam.put("lastagecheckage","1-January-2001");
        cookiesForSteam.put("timezoneName", "America/Sao_Paulo");
        cookiesForSteam.put("steamCountry", "BR%7C6701a4f67d7b861f33919b5a1c1df1ba");

        String gameName = gameNameFormatterForSteam(rawGameName);
        String gameAppId = dataBase.getGameAppId(rawGameName).orElseThrow(() -> new IllegalArgumentException("Erro: O jogo '" + rawGameName + "' não existe no banco de dados."));

        return Jsoup.connect("https://store.steampowered.com/app/" + gameAppId + "/" + gameName + "?l=portuguese")
        .cookies(cookiesForSteam)
        .userAgent(userAgent)
        .timeout(10000)
        .get();
    }

    
    public String showGamePrice(Document gamePage) {
    Optional<Element> originalPriceInDiscount = retrieveInformation.safeSearch(gamePage,
        "div.game_area_purchase_game:not(.game_area_purchase_game_dropdown_subscription) div.discount_original_price");
    if (originalPriceInDiscount.isPresent()) {
        return originalPriceInDiscount.get().text();
    }
    Optional<Element> normalPrice = retrieveInformation.safeSearch(gamePage,
        "div.game_area_purchase_game:not(.game_area_purchase_game_dropdown_subscription) div.game_purchase_price.price");
    return normalPrice
        .map(Element::text)
        .orElse("Preço não encontrado");
}

    public String showGamePromotionPrice(Document gamePage) {
    String cssSelector = "div.game_area_purchase_game:not(.game_area_purchase_game_dropdown_subscription) div.discount_final_price";
    Optional<Element> promoPrice = retrieveInformation.safeSearch(gamePage, cssSelector);
    return promoPrice.map(Element::text).orElse("Não há promoções para este jogo no momento na Steam");
}

    public Boolean isGameInPromotion(Document gamePage) {
    String cssSelector = "div.game_area_purchase_game:not(.game_area_purchase_game_dropdown_subscription) div.discount_original_price";
    Optional<Element> element = retrieveInformation.safeSearch(gamePage, cssSelector);
    return element.isPresent();
}
}