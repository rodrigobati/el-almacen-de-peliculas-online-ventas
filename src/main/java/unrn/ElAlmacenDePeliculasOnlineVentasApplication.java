package unrn;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class ElAlmacenDePeliculasOnlineVentasApplication {

    public static void main(String[] args) {
        SpringApplication.run(ElAlmacenDePeliculasOnlineVentasApplication.class, args);
    }

}
