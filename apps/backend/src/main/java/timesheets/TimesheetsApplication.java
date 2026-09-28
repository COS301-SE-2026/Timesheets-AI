package timesheets;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class TimesheetsApplication {

  public static void main(String[] args) {
    SpringApplication.run(TimesheetsApplication.class, args);
  }
}
