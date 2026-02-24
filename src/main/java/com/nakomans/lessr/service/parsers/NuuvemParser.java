package com.nakomans.lessr.service.parsers;

import java.io.IOException;
import java.text.Normalizer;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Service;

import com.nakomans.lessr.dto.GameInfoDto;
import com.nakomans.lessr.service.gamesinformation.RetrieveInformationFromSteam;

@Service
public class NuuvemParser implements GameParser {

    private final RetrieveInformationFromSteam steamService;
    private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36";

    public NuuvemParser(RetrieveInformationFromSteam steamService) {
        this.steamService = steamService;
    }

    @Override
    public String getStoreName() {
        return "Nuuvem";
    }

    @Override
    public GameInfoDto getGameInformation(String rawGameName) {
        Document nuuvemDoc = null;
        Document steamDoc = null;

        try {
            nuuvemDoc = loadNuuvemPage(rawGameName);
        } catch (Exception e) {
            nuuvemDoc = Jsoup.parse("<html></html>");
        }

        try {
            steamDoc = loadSteamPage(rawGameName);
        } catch (Exception e) {
            steamDoc = Jsoup.parse("<html></html>");
        }

        boolean inPromotion = isGameInPromotion(nuuvemDoc);
        String promotionPrice = showGamePromotionPrice(nuuvemDoc);
        String originalPrice = inPromotion ? showGamePrice(nuuvemDoc) : promotionPrice;
        String gameName = Optional.ofNullable(steamService.gameNameDisplay(steamDoc))
                .orElse("Nome indisponível")
                .replace("?", "").replace("™", "").replace("®", "").trim();
        String dateFormatted = LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));

        return new GameInfoDto(
                gameName,
                steamService.gameDescription(steamDoc),
                steamService.gameBanner(steamDoc),
                steamService.categorieTags(steamDoc),
                steamService.showReviews(steamDoc),
                steamService.showDeveloper(steamDoc),
                steamService.showMetaCriticScore(steamDoc),
                inPromotion,
                originalPrice,
                promotionPrice,
                dateFormatted
        );
    }

    private Document loadNuuvemPage(String rawGameName) throws IOException {
        String slug = Normalizer.normalize(rawGameName, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .replaceAll("[^a-zA-Z0-9 ]", "")
                .trim()
                .replaceAll("\\s+", "-")
                .toLowerCase();
        
        return Jsoup.connect("https://www.nuuvem.com/br-pt/item/" + slug)
                .userAgent(USER_AGENT)
                .timeout(15000)
                .get();
    }

    private Document loadSteamPage(String rawGameName) throws IOException {
        String searchUrl = "https://store.steampowered.com/search/?term=" + rawGameName;
        Document searchPage = Jsoup.connect(searchUrl).userAgent(USER_AGENT).timeout(10000).get();

        Element firstResult = searchPage.selectFirst("a.search_result_row");
        if (firstResult != null) {
            String gameUrl = firstResult.attr("href");
            return Jsoup.connect(gameUrl)
                    .userAgent(USER_AGENT)
                    .cookie("lastagecheckage", "1-0-1900")
                    .cookie("birthtime", "283993201")
                    .header("Accept-Language", "pt-BR,pt;q=0.9")
                    .timeout(10000)
                    .get();
        }
        return searchPage;
    }

    public Boolean isGameInPromotion(Document doc) {
        if (doc == null) return false;
        return doc.selectFirst(".product-price--discount") != null;
    }

    public String showGamePrice(Document doc) {
        if (doc == null) return "N/A";
        Element priceOld = doc.selectFirst(".product-price--old");
        if (priceOld != null) {
            return priceOld.text().trim();
        }
        return showGamePromotionPrice(doc);
    }

    public String showGamePromotionPrice(Document doc) {
        if (doc == null) return "Preço não encontrado";
        Element priceVal = doc.selectFirst(".product-price--val");
        if (priceVal != null) {
            Element cleanPrice = priceVal.clone();
            cleanPrice.select(".product-price--old").remove();
            cleanPrice.select(".product-price--discount").remove();
            return cleanPrice.text().trim();
        }
        return "Preço não encontrado";
    }
}