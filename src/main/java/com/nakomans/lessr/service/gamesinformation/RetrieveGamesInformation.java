package com.nakomans.lessr.service.gamesinformation;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import org.springframework.cache.annotation.Cacheable;
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

    @Cacheable(value = "gamesInfo", key = "#gameName.trim().toLowerCase()")
    public Map<String, Object> getAllStoresInfo(String gameName) throws IOException {
        System.out.println(">>> Fora do Cache: " + gameName);

        Map<String, Object> fullReport = new LinkedHashMap<>();

        org.jsoup.nodes.Document sharedSteamDoc = steamParser.loadSteamPage(gameName);

        CompletableFuture<Object> enebaTask = CompletableFuture.<Object>supplyAsync(() -> {
            try {
                return enebaParser.getGameInformation(gameName, sharedSteamDoc);
            } catch (Exception e) {
                return null;
            }
        }).completeOnTimeout(null, 15, TimeUnit.SECONDS);

        CompletableFuture<Object> nuuvemTask = CompletableFuture.<Object>supplyAsync(() -> {
            try {
                return nuuvemParser.getGameInformation(gameName, sharedSteamDoc);
            } catch (Exception e) {
                return null;
            }
        }).completeOnTimeout(null, 5, TimeUnit.SECONDS);

        Object steamInfo = steamParser.getGameInformation(gameName, sharedSteamDoc);

        fullReport.put("steam", steamInfo);
        fullReport.put("eneba", enebaTask.join());
        fullReport.put("nuuvem", nuuvemTask.join());

        return fullReport;
    }
}