package com.nakomans.lessr.service.parsers;

import com.nakomans.lessr.dao.GamesDatabase;
import com.nakomans.lessr.dto.GameInfoDto;
import com.nakomans.lessr.service.gamesinformation.RetrieveInformationFromSteam;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.Normalizer;
import java.util.HashMap;
import java.util.Map;
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
            promotionPrice
        );
    }

    public String getGameAppId(String rawGameName) {
        try (Connection conn = dataBase.connectToDatabase();
            PreparedStatement pstmt = conn.prepareStatement("SELECT appid FROM games where name = ?")) {
                
                pstmt.setString(1, rawGameName);

                try (ResultSet rs = pstmt.executeQuery()) {
                    if(rs.next()) {
                        Integer result = rs.getInt("appid");
                        return result.toString();
                    }
                }
            } catch (SQLException e) { e.printStackTrace(); }
            return null;
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
        String gameAppId = getGameAppId(rawGameName);
        return Jsoup.connect("https://store.steampowered.com/app/" + gameAppId + "/" + gameName + "?l=portuguese").cookies(cookiesForSteam).userAgent(userAgent).get();
    }
    
    public String showGamePrice(Document gamePage) {
        Element gamePrice = gamePage.selectFirst("div.game_purchase_price.price");
        if (isGameInPromotion(gamePage)) {
            return gamePage.selectFirst("div.discount_prices div.discount_original_price").text();
        }
        return gamePrice.text();
    }

    public String showGamePromotionPrice(Document gamePage) {
        if (isGameInPromotion(gamePage)) {
            return gamePage.selectFirst("div.discount_prices div.discount_final_price").text();
        }
        return "Não há promoções para este jogo no momento na Steam";
    }

    public Boolean isGameInPromotion(Document gamePage) {
        return gamePage.selectFirst("div.game_area_purchase_game div.discount_prices") != null;
    }

}
