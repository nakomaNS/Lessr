package com.nakomans.lessr.controller;

import java.io.IOException;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import com.nakomans.lessr.service.gamesinformation.RetrieveGamesInformation;

@RestController
public class GamesController {
    
    private final RetrieveGamesInformation retrieveGamesInformation;

    public GamesController(RetrieveGamesInformation retrieveGamesInformation) {
        this.retrieveGamesInformation = retrieveGamesInformation;
    }

    @GetMapping("/getinfo/{rawGameName}")
    public ResponseEntity<Map<String, Object>> gameInformation(@PathVariable String rawGameName) {
        try {
            Map<String, Object> info = retrieveGamesInformation.getAllStoresInfo(rawGameName);
            return ResponseEntity.ok(info);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(404).build();
        } catch (IOException e) {
            return ResponseEntity.status(500).build();
        }
    }
}