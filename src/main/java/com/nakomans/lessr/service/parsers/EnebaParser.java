package com.nakomans.lessr.service.parsers;

import com.nakomans.lessr.dto.GameInfoDto;
import com.nakomans.lessr.service.gamesinformation.RetrieveInformationFromSteam;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Service;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Service
public class EnebaParser implements GameParser {

    private final RetrieveInformationFromSteam retrieveInformation;
    private final SteamParser steamParser;

    public EnebaParser(RetrieveInformationFromSteam retrieveInformation, SteamParser steamParser) {
        this.retrieveInformation = retrieveInformation;
        this.steamParser = steamParser;
    }

    @Override
    public String getStoreName() {
        return "Eneba";
    }

    @Override
    public GameInfoDto getGameInformation(String rawGameName) throws IOException {
        Document steamDoc = steamParser.loadSteamPage(rawGameName);

        boolean inPromotion = true; 
        String originalPrice = "R$ 230,26";
        String promoPrice = "193.42 BRL";

        return new GameInfoDto(
            retrieveInformation.gameNameDisplay(steamDoc),
            retrieveInformation.gameDescription(steamDoc),
            retrieveInformation.gameBanner(steamDoc),
            retrieveInformation.categorieTags(steamDoc),
            retrieveInformation.showReviews(steamDoc),
            retrieveInformation.showDeveloper(steamDoc),
            retrieveInformation.showMetaCriticScore(steamDoc),
            inPromotion,
            originalPrice,
            promoPrice,
            LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
        );
    }
}