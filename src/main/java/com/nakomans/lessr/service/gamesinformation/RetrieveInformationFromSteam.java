package com.nakomans.lessr.service.gamesinformation;

import java.util.List;

import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;

@Service
public class RetrieveInformationFromSteam {

    public String gameNameDisplay(Document gamePage) {
        return gamePage.selectFirst("div#appHubAppName.apphub_AppName").text();
    }

    public String gameDescription(Document gamePage) {
        return gamePage.selectFirst("div.game_background_glow div.game_description_snippet").text();
    }

    public String gameBanner(Document gamePage) {
        return gamePage.selectFirst("div.game_background_glow .game_header_image_full").attr("abs:src");
    }

    public String categorieTags(Document gamePage) {
        Elements rawTags = gamePage.select("#glanceCtnResponsiveRight a.app_tag:lt(5)");
        List<String> tags = rawTags.eachText();
        return String.join(", ", tags);
    }

    public String showReviews(Document gamePage) {
        return gamePage.selectFirst("div.game_background_glow div.summary.column span.game_review_summary.positive").text();
    }

    public String showDeveloper(Document gamePage) {
        return gamePage.selectFirst("div.dev_row a").text();
    }

    public String showMetaCriticScore(Document gamePage) {
        return gamePage.selectFirst("div.score.high").text();
    }

}