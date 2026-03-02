package com.nakomans.lessr.service.parsers;

import java.io.IOException;

import com.nakomans.lessr.dto.GameInfoDto;

public interface GameParser {
    GameInfoDto getGameInformation(String rawGameName) throws IOException;
    String getStoreName();
}