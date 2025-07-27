package dev.alsalman.agenticworkflowengine.domain;

public enum ParameterType {
    STRING("text"),
    NUMBER("number"),
    DATE("date"),
    BOOLEAN("boolean"),
    SELECTION("select"),
    LOCATION("location"),
    DURATION("duration"),
    EMAIL("email"),
    URL("url"),
    CURRENCY("currency");
    
    private final String inputType;
    
    ParameterType(String inputType) {
        this.inputType = inputType;
    }
    
    public String getInputType() {
        return inputType;
    }
}