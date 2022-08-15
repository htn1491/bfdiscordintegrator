package htn.bfdiscordintegration;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class BfdiscordintegrationApplication {

	public static void main(String[] args) {
		SpringApplication.run(BfdiscordintegrationApplication.class, args);
	}

}
