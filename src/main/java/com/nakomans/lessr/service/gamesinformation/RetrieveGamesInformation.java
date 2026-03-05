package com.nakomans.lessr.service.gamesinformation;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Service;
import com.nakomans.lessr.service.parsers.EnebaParser;
import com.nakomans.lessr.service.parsers.NuuvemParser;
import com.nakomans.lessr.service.parsers.SteamParser;

@Service
public class RetrieveGamesInformation {

    private final SteamParser steamParser;
    private final EnebaParser enebaParser;
    private final NuuvemParser nuuvemParser;

    public RetrieveGamesInformation(SteamParser steam, EnebaParser eneba, NuuvemParser nuuvem) {
        this.steamParser = steam;
        this.enebaParser = eneba;
        this.nuuvemParser = nuuvem;
    }

    public Map<String, Object> getAllStoresInfo(String gameName) throws IOException {
        Map<String, Object> fullReport = new LinkedHashMap<>();

        org.jsoup.nodes.Document sharedSteamDoc = steamParser.loadSteamPage(gameName);

        fullReport.put("steam", steamParser.getGameInformation(gameName, sharedSteamDoc));
        fullReport.put("eneba", enebaParser.getGameInformation(gameName, sharedSteamDoc));
        fullReport.put("nuuvem", nuuvemParser.getGameInformation(gameName, sharedSteamDoc));

        return fullReport;
    }
}


