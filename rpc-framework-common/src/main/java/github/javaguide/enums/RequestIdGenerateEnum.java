package github.javaguide.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum RequestIdGenerateEnum {

    UUID("uuid"),
    SNOWFLAKE("snowflake");

    private final String name;

}
