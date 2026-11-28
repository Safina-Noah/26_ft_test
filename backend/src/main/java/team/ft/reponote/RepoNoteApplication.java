package team.ft.reponote;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class RepoNoteApplication {
  public static void main(String[] args) {
    SpringApplication.run(RepoNoteApplication.class, args);
  }
}
