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

import com.nakomans.lessr.dto.SteamDto;
import com.nakomans.lessr.service.gamesinformation.RetrieveInformationFromSteam;

@Service
public class NuuvemParser {

    private final RetrieveInformationFromSteam steamService;
    private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36";

    public NuuvemParser(RetrieveInformationFromSteam steamService) {
        this.steamService = steamService;
    }

    public String getStoreName() {
        return "Nuuvem";
    }

    public SteamDto getGameInformation(String rawGameName, Document steamDoc) {
        Document nuuvemDoc;

        try {
            nuuvemDoc = loadNuuvemPage(rawGameName);
        } catch (Exception e) {
            nuuvemDoc = Jsoup.parse("<html></html>");
        }

        boolean inPromotion = isGameInPromotion(nuuvemDoc);
        String originalPrice = showGamePrice(nuuvemDoc);
        String promotionPrice = showGamePromotionPrice(nuuvemDoc);
        
        String gameName = Optional.ofNullable(steamService.gameNameDisplay(steamDoc))
                .orElse("Nome indisponível")
                .replace("?", "").replace("™", "").replace("®", "").trim();
        
        String dateFormatted = LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));

        return new SteamDto(
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

    public Boolean isGameInPromotion(Document doc) {
        if (doc == null) return false;
        return doc.selectFirst(".product-buy .product-price--discount") != null;
    }

    public String showGamePrice(Document doc) {
        if (doc == null) return "N/A";
        Element priceOld = doc.selectFirst(".product-buy .product-price--old");
        if (priceOld != null) {
            return priceOld.text().trim();
        }
        return showGamePromotionPrice(doc);
    }

    public String showGamePromotionPrice(Document doc) {
        if (doc == null) return "Preço não encontrado";
        Element priceVal = doc.selectFirst(".product-buy .product-price--val");
        if (priceVal != null) {
            Element cleanPrice = priceVal.clone();
            cleanPrice.select(".product-price--old").remove();
            cleanPrice.select(".product-price--discount").remove();
            return cleanPrice.text().replace(" ,", ",").trim();
        }
        return "Preço não encontrado";
    }
}