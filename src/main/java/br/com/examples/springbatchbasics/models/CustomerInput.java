package br.com.examples.springbatchbasics.models;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CustomerInput {

    private String id;

    private String firstName;

    private String lastName;

    private String gender;

    private String birthday;

    private String address;

    private String country;

}
