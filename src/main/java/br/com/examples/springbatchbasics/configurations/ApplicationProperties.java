package br.com.examples.springbatchbasics.configurations;

import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@Configuration
public class ApplicationProperties {

  @Value("${batch.input.file-name}")
  private String inputFile;

  @Value("${batch.output.americans.file-name}")
  private String americansFile;

}
