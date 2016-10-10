package com.xored.javafx.packeteditor.data;

public class FieldRules {
    Integer min;
    Integer max;
    String regex;

    public FieldRules(Integer min, Integer max, String regexPattern) {
        this.min = min;
        this.max = max;
        this.regex = regexPattern;
    }

    public Integer getMin() {
        return min;
    }

    public Integer getMax() {
        return max;
    }

    public String getRegex() {
        return regex;
    }

    public boolean hasSpecifiedInterval() {
        return min != null && max != null;
    }
    
    public boolean hasRegex() {
        return regex != null;
    }
    
}
