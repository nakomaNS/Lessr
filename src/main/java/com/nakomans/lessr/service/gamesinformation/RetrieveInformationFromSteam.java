package com.nakomans.lessr.service.gamesinformation;

import java.util.List;
import java.util.Optional;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;

@Service
public class RetrieveInformationFromSteam {

    public Optional<Element> safeSearch (Document gamePage, String cssSelector) {
        return Optional.ofNullable(gamePage.selectFirst(cssSelector));
    }

    public String gameNameDisplay(Document gamePage) {
        Optional<Element> element = safeSearch(gamePage, "div#appHubAppName.apphub_AppName");
        return element.isPresent() ? element.get().text() : "Nome não encontrado";
    }

    public String gameDescription(Document gamePage) {
        Optional<Element> element = safeSearch(gamePage, "div.game_background_glow div.game_description_snippet");
        return element.isPresent() ? element.get().text() : "Descrição não encontrada";
    }

    public String gameBanner(Document gamePage) {
        String imgattr = "abs:src";
        Optional<Element> element = safeSearch(gamePage, "div.game_background_glow .game_header_image_full");
        
        if(element.isPresent()) {
            String url = element.get().attr(imgattr);
            if(!url.isBlank()){
                return url;
            }
        } return "Não foi possível extrair o link do banner";
    } 

    public String categorieTags(Document gamePage) {
        Elements rawTags = gamePage.select("#glanceCtnResponsiveRight a.app_tag:lt(5)");
        if(!rawTags.isEmpty()) {
            List<String> extractedTags = rawTags.eachText();
            return String.join(", ", extractedTags);
        } return "Não foi possível capturar as categorias deste jogo";
    }

    public String showReviews(Document gamePage) {
        Optional<Element> element = safeSearch(gamePage, "div.game_background_glow div.summary.column span.game_review_summary");
        return element.isPresent() ? element.get().text() : "Não foi possível extrair as reviews deste jogo";
    }

    public String showDeveloper(Document gamePage) {
        Optional<Element> element = safeSearch(gamePage, "div.dev_row a");
        return element.isPresent() ? element.get().text() : "Não foi possível conseguir o nome do(a) desenvolvedor(a)";
    }

    public String showMetaCriticScore(Document gamePage) {
        Optional<Element> element = safeSearch(gamePage, "div.score.high");
        return element.isPresent() ? element.get().text() : "Não foi possível visualizar a nota do Metacritic";
    }

}