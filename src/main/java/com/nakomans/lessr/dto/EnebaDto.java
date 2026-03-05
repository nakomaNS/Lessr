package com.nakomans.lessr.dto;

public record EnebaDto (
    String gameName,
    String description,
    String banner,
    String categories,
    String reviews,
    String developer,
    String metacriticScore,
    Boolean inPromotion,
    String originalGamePrice,
    String promotionPrice,
    String date
 ) {}
