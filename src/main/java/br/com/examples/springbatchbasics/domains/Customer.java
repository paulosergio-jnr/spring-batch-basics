package br.com.examples.springbatchbasics.domains;

import br.com.examples.springbatchbasics.enums.Gender;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.UUID;

@Getter
@Setter
public class Customer {

  private UUID id;

  private String firstName;

  private String lastName;

  private Gender gender;

  private LocalDate birthday;

  private Address address;

  private boolean foreign;

}
