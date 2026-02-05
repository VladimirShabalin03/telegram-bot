package com.vladimir.model;

import java.util.List;

public class VacancyResponse {
    private List<Vacancy> items;
    private Integer found;
    private Integer pages;
    private Integer page;

    public List<Vacancy> getItems() {
        return items;
    }
    public Integer getFound() {
        return found;
    }
    public Integer getPages() {
        return pages;
    }
    public Integer getPage() {
        return page;
    }
}
