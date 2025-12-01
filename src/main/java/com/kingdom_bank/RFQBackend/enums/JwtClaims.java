package com.kingdom_bank.RFQBackend.enums;

public enum JwtClaims {
    USER_ID("user");

    private final String value;
    JwtClaims(String value){ this.value = value;}

    public String getValue() {return value;}
}
