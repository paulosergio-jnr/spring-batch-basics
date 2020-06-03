package br.com.examples.springbatchbasics.enums;

import java.util.Arrays;

public enum Gender {

    MALE("male"),
    FEMALE("female");

    private String value;

    Gender(String gender) {
        this.value = gender;
    }

    public String getValue() {
        return value;
    }

    public static Gender entryOf(String gender) {
        for (Gender g : Gender.values()) {
            if (g.getValue().equals(gender)) {
                return g;
            }
        }
        return null;
    }
}
