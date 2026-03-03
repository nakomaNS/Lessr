package com.nakomans.lessr.service.parsers;

import java.io.IOException;
import java.text.Normalizer;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.regex.Pattern;
import java.util.Optional;
import com.google.gson.*;
import com.nakomans.lessr.dto.GameInfoDto;
import com.nakomans.lessr.service.gamesinformation.RetrieveInformationFromSteam;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.springframework.stereotype.Service;

@Service
public class EnebaParser implements GameParser {

    private final RetrieveInformationFromSteam steamInfo;
    private final WebDriver driver;

    public EnebaParser(RetrieveInformationFromSteam steamInfo) {
        this.steamInfo = steamInfo;
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--headless=new"); 
        options.addArguments("--disable-gpu");
        options.addArguments("--window-size=1920,1080");
        options.addArguments("--user-agent=Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36");
        this.driver = new ChromeDriver(options);
    }

    @Override
    public String getStoreName() {
        return "Eneba";
    }

    @Override
    public GameInfoDto getGameInformation(String rawGameName) throws IOException {
        try {
            Document steamDoc = steamInfo.loadSteamPage(driver, rawGameName);

            Document enebaDoc = loadEnebaPage(rawGameName);

            return new GameInfoDto(
                steamInfo.gameNameDisplay(steamDoc),
                steamInfo.gameDescription(steamDoc),
                steamInfo.gameBanner(steamDoc),
                steamInfo.categorieTags(steamDoc),
                steamInfo.showReviews(steamDoc),
                steamInfo.showDeveloper(steamDoc),
                steamInfo.showMetaCriticScore(steamDoc),
                isGameInPromotion(enebaDoc),
                showGamePrice(enebaDoc),
                showGamePromotionPrice(enebaDoc),
                LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
            );
        } catch (Exception e) {
            throw new IOException("Erro na orquestração: " + e.getMessage(), e);
        }
    }

    public String gameNameFormatterForEneba(String rawGameName) {
        String normalized = Normalizer.normalize(rawGameName, Normalizer.Form.NFD);
        String withoutAccents = Pattern.compile("\\p{M}").matcher(normalized).replaceAll("");
        return withoutAccents.trim().replaceAll("[^a-zA-Z0-9 ]","").replaceAll("\\s+", "-").toLowerCase();
    }

    public Document loadEnebaPage(String rawGameName) throws InterruptedException {
        String formattedName = gameNameFormatterForEneba(rawGameName);
        
        String[] urlTemplates = {
            "https://www.eneba.com/br/steam-" + formattedName + "-steam-key-pc-latam",
            "https://www.eneba.com/br/" + formattedName + "-pc-steam-key-latam",
            "https://www.eneba.com/br/steam-" + formattedName + "-pc-steam-key-latam",
            "https://www.eneba.com/br/" + formattedName + "-steam-key-latam"
        };

        for (String url : urlTemplates) {
            driver.get(url);
            Thread.sleep(2000);
            
            if (driver.getPageSource().contains("application/ld+json")) {
                return Jsoup.parse(driver.getPageSource());
            }
        }

        return Jsoup.parse(driver.getPageSource());
    }

    public String showGamePrice(Document enebaPage) {
        double priceWithDiscount = 0;
        Element script = enebaPage.selectFirst("script[type=application/ld+json]");
        if (script != null) {
            JsonObject root = JsonParser.parseString(script.html()).getAsJsonObject();
            priceWithDiscount = root.getAsJsonObject("offers").get("lowPrice").getAsDouble();
        }
        Optional<Element> discountElement = Optional.ofNullable(enebaPage.selectFirst("span.MjI1ZB"));
        if (discountElement.isPresent() && priceWithDiscount > 0) {
            String discountText = discountElement.get().text().replaceAll("[^0-9]", "");
            if (!discountText.isEmpty()) {
                double discountPercent = Double.parseDouble(discountText);
                double originalPrice = priceWithDiscount / (1 - (discountPercent / 100));
                return String.format("R$ %.2f", originalPrice);
            }
        }
        return priceWithDiscount > 0 ? String.format("R$ %.2f", priceWithDiscount) : "Preço não disponível";
    }

    public String showGamePromotionPrice(Document enebaPage) {
        Element script = enebaPage.selectFirst("script[type=application/ld+json]");
        if (script == null) return "Preço não disponível";
        JsonObject root = JsonParser.parseString(script.html()).getAsJsonObject();
        double lowPrice = root.getAsJsonObject("offers").get("lowPrice").getAsDouble();
        return String.format("R$ %.2f", lowPrice);
    }

    public Boolean isGameInPromotion(Document enebaPage) {
        return Optional.ofNullable(enebaPage.selectFirst("span.MjI1ZB")).isPresent();
    }
}