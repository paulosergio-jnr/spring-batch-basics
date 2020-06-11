package br.com.examples.springbatchbasics.enums;

public enum Gender {

  MALE("male"),
  FEMALE("female");

  private String value;

  Gender(final String gender) {
    this.value = gender;
  }

  public String getValue() {
    return value;
  }

  public static Gender entryOf(final String gender) {
    for (Gender g : Gender.values()) {
      if (g.getValue().equals(gender)) {
        return g;
      }
    }
    return null;
  }
}
