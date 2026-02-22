package com.nakomans.lessr;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.boot.autoconfigure.data.jdbc.JdbcRepositoriesAutoConfiguration;
import org.springframework.context.annotation.Bean;

import com.nakomans.lessr.dto.GameInfoDto;
import com.nakomans.lessr.service.parsers.SteamParser;

@SpringBootApplication(exclude = {
    DataSourceAutoConfiguration.class, 
    DataSourceTransactionManagerAutoConfiguration.class, 
    JdbcRepositoriesAutoConfiguration.class
})
public class LessrApplication {

    public static void main(String[] args) {
        SpringApplication.run(LessrApplication.class, args);
    }

    @Bean
public CommandLineRunner teste(SteamParser steamParser) {
    return args -> {
        String gameNameTest = "Elden Ring";

        try {
            GameInfoDto info = steamParser.getGameInformation(gameNameTest);

            if (info != null) {
                System.out.println("======== TESTE RÁPIDO [TEMPORÁRIO ATÉ QUE TESTES UNITÁRIOS SEJAM IMPLEMENTADOS] ========");
                System.out.println("DADOS DO JOGO ENCONTRADOS:");
                System.out.println("--------------------------------------------------");
                System.out.println("Nome:              " + info.gameName());
                System.out.println("Desenvolvedor:     " + info.developer());
                System.out.println("Metacritic:        " + info.metacriticScore());
                System.out.println("Avaliacoes:        " + info.reviews());
                System.out.println("Categorias:        " + info.categories());
                System.out.println("URL do Banner:     " + info.banner());
                System.out.println("Descricao:         " + info.description());
                System.out.println("--------------------------------------------------");
                System.out.println("Preco Original:    " + info.originalGamePrice());
                System.out.println("Em Promocao:       " + (info.inPromotion() ? "Sim" : "Nao"));
                
                if (info.inPromotion()) {
                    System.out.println("Preco Promocional: " + info.promotionPrice());
                }
            }

        } catch (Exception e) {
            System.err.println("ERRO: " + e.getMessage());
        }

        System.out.println("--------------------------------------------------");
    };
}
}