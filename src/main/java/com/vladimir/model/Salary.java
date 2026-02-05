package com.vladimir.model;

public class Salary {
    private Integer from, to;
    private String currency;

    public Integer getFrom() {
        return from;
    }
    public Integer getTo() {
        return to;
    }
    public String getCurrency() {
        return currency;
    }

    public String format() {
        if (from != null && to != null) {
            return String.format("от %d до %d %s", from, to, currency);
        } else if (from != null) {
            return String.format("от %d %s", from, currency);
        } else if (to != null) {
            return String.format("до %d %s", to, currency);
        }
        return "не указана";
    }
}
