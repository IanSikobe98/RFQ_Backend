package com.kingdom_bank.RFQBackend.enums;


import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

@Getter
public enum ApiResponseCode {
    SUCCESS("00"),
    FAIL("01"),
    INFORMATION("02");

    private final String code;
    ApiResponseCode(String code){this.code = code;}

    @JsonValue public String intValue() {return this.getCode();}

}
