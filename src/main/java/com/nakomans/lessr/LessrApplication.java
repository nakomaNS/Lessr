package com.nakomans.lessr;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.boot.autoconfigure.data.jdbc.JdbcRepositoriesAutoConfiguration;
import org.springframework.context.annotation.Bean;
import com.nakomans.lessr.dto.GameInfoDto;
import com.nakomans.lessr.service.parsers.EnebaParser;

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
    public CommandLineRunner teste(EnebaParser enebaParser) {
        return args -> {
            String gameNameTest = "the first berserker khazan";
            try {
                GameInfoDto info = enebaParser.getGameInformation(gameNameTest);

                if (info != null) {
                    System.out.println("======== TESTE RÁPIDO ========");
                    System.out.println("DADOS DO JOGO ENCONTRADOS:");
                    System.out.println("--------------------------------------------------");
                    System.out.println("Nome:              " + info.gameName());
                    System.out.println("Desenvolvedor:     " + info.developer());
                    System.out.println("Metacritic:        " + info.metacriticScore());
                    System.out.println("Avaliacoes:        " + info.reviews());
                    System.out.println("Categorias:        " + info.categories());
                    System.out.println("URL do Banner:     " + info.banner());
                    System.out.println("--------------------------------------------------");
                    System.out.println("Preco Original:    " + info.originalGamePrice());
                    System.out.println("Em Promocao:       " + (info.inPromotion() ? "Sim" : "Nao"));
                    if (info.inPromotion()) {
                        System.out.println("Preco Promocional: " + info.promotionPrice());
                    }
                    System.out.println("Data do registro:  " + info.date());
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        };
    }
}