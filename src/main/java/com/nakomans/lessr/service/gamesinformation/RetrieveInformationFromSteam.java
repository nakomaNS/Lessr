package com.nakomans.lessr.service.gamesinformation;

import java.util.Optional;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.springframework.stereotype.Service;
import com.nakomans.lessr.dao.GamesDatabase;

@Service
public class RetrieveInformationFromSteam {

    private final GamesDatabase gamesDatabase;

    public RetrieveInformationFromSteam(GamesDatabase gamesDatabase) {
        this.gamesDatabase = gamesDatabase;
    }

    public Document loadSteamPage(WebDriver driver, String rawGameName) throws Exception {
        String appId = gamesDatabase.getGameAppId(rawGameName)
                .orElseThrow(() -> new Exception("Jogo não encontrado no banco de dados: " + rawGameName));

        String url = "https://store.steampowered.com/app/" + appId + "/";
        driver.get(url);
        Thread.sleep(2000);

        if (driver.findElements(By.id("ageYear")).size() > 0) {
            driver.findElement(By.id("ageYear")).sendKeys("2000");
            driver.findElement(By.cssSelector("#view_product_page_btn")).click();
            Thread.sleep(2000);
        }

        return Jsoup.parse(driver.getPageSource());
    }

    public Optional<Element> safeSearch(Document gamePage, String cssSelector) {
        return Optional.ofNullable(gamePage.selectFirst(cssSelector));
    }

    public String gameNameDisplay(Document gamePage) {
        return safeSearch(gamePage, "div#appHubAppName.apphub_AppName")
                .map(Element::text).orElse("Nome não encontrado");
    }

    public String gameDescription(Document gamePage) {
        return safeSearch(gamePage, "div.game_description_snippet")
                .map(Element::text).orElse("Descrição não encontrada");
    }

    public String gameBanner(Document gamePage) {
        return safeSearch(gamePage, "img.game_header_image_full")
                .map(e -> e.attr("abs:src"))
                .orElse("Não foi possível extrair o link do banner");
    }

    public String categorieTags(Document gamePage) {
        Elements rawTags = gamePage.select("a.app_tag:lt(5)");
        return rawTags.isEmpty() ? "Sem categorias" : String.join(", ", rawTags.eachText());
    }

    public String showReviews(Document gamePage) {
        return safeSearch(gamePage, "span.game_review_summary")
                .map(Element::text).orElse("Sem reviews");
    }

    public String showDeveloper(Document gamePage) {
        return safeSearch(gamePage, "div.dev_row a")
                .map(Element::text).orElse("Desenvolvedor desconhecido");
    }

    public String showMetaCriticScore(Document gamePage) {
        return safeSearch(gamePage, "div.score.high")
                .map(Element::text).orElse("N/A");
    }
}