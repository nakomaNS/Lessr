package com.nakomans.lessr.controller;

import java.io.IOException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import com.nakomans.lessr.dto.GameInfoDto;
import com.nakomans.lessr.service.parsers.SteamParser;

@RestController
public class GamesController {
    private final SteamParser steamParser;

    public GamesController(SteamParser steamParser) {this.steamParser = steamParser;}

    @GetMapping("/getinfo/{rawGameName}")
    public ResponseEntity<GameInfoDto> gameInformation(@PathVariable String rawGameName) {
        try {
        GameInfoDto info = steamParser.getGameInformation(rawGameName);
        return ResponseEntity.ok(info);
    } catch (IOException e) {
        return ResponseEntity.status(500).build();
    }
}
}