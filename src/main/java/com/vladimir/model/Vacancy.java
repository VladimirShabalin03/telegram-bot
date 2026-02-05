package com.vladimir.model;

import com.google.gson.annotations.SerializedName;

public class Vacancy {
    private String id;
    private String name;

    @SerializedName("alternate_url")
    private String alternateUrl;

    @SerializedName("has_test")
    private boolean hasTest;

    private Area area;
    private Salary salary;
    private Employer employer;
    private Experience experience;

    public String getId() {
        return id;
    }
    public String getName() {
        return name;
    }
    public String getAlternateUrl() {
        return alternateUrl;
    }
    public boolean hasTest() {
        return hasTest;
    }
    public Area getArea() {
        return area;
    }
    public Salary getSalary() {
        return salary;
    }
    public String getSalaryFormatted() {
        return salary != null ? salary.format() : "не указана";
    }
    public Employer getEmployer() {
        return employer;
    }
    public Experience getExperience() {
        return experience;
    }

    public String vacancyMessage() {
        String hasTest = hasTest() ? "Да" : "Нет";
        return String.format("Название вакансии: %s%nНазвание компании: %s%nЗарплата: %s%nРегион: %s%nОпыт: %s%nЕсть тестовое - %s%nСсылка: %s",
                name, employer.getName(),getSalaryFormatted(), area.getName(), experience.getName(), hasTest, alternateUrl);

    }

}
