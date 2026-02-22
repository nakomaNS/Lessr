package com.nakomans.lessr.dto;

public record GameInfoDto (
    String gameName,
    String description,
    String banner,
    String categories,
    String reviews,
    String developer,
    String metacriticScore,
    Boolean inPromotion,
    String originalGamePrice,
    String promotionPrice
) {}
