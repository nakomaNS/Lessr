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
            String gameNameTest = "lies of p";
            try {
                System.out.println("Iniciando busca na Eneba para: " + gameNameTest + "...");
                
                GameInfoDto info = enebaParser.getGameInformation(gameNameTest);

                if (info != null) {
                    System.out.println("\n======== TESTE RÁPIDO (ENEBA) ========");
                    System.out.println("DADOS DO JOGO:");
                    System.out.println("--------------------------------------------------");
                    System.out.println("Nome:              " + info.gameName());
                    System.out.println("Desenvolvedor:     " + info.developer());
                    System.out.println("Metacritic:        " + info.metacriticScore());
                    System.out.println("Avaliacoes:        " + info.reviews());
                    System.out.println("Categorias:        " + info.categories());
                    System.out.println("URL do Banner:     " + info.banner());
                    System.out.println("--------------------------------------------------");
                    System.out.println("PREÇOS EXTRAÍDOS:");
                    System.out.println("Preço Base:        " + info.originalGamePrice());
                    System.out.println("Em Promoção:       " + (info.inPromotion() ? "SIM" : "NÃO"));
                    
                    if (info.inPromotion()) {
                        System.out.println("Preço c/ Desconto: " + info.promotionPrice());
                    }
                    
                    System.out.println("Data da Consulta:  " + info.date());
                    System.out.println("======================================\n");
                } else {
                    System.out.println("Nenhum dado encontrado para o jogo especificado.");
                }
            } catch (Exception e) {
                System.err.println("ERRO DURANTE O TESTE: " + e.getMessage());
                e.printStackTrace();
            }
        };
    }
}