package com.kasiefm.api;

public class Show {
    private Long id;
    private String name;
    private String presenter;
    private String startTime;
    private String endTime;
    private String description;
    private String dayOfWeek;
    private Boolean isCurrent;

    public Show() {
    }

    public Show(Long id, String name, String presenter, String startTime, String endTime,
                String description, Boolean isCurrent) {
        this.id = id;
        this.name = name;
        this.presenter = presenter;
        this.startTime = startTime;
        this.endTime = endTime;
        this.description = description;
        this.isCurrent = isCurrent;
    }

    public Show(Long id, String name, String presenter, String startTime, String endTime,
                String description, String dayOfWeek, Boolean isCurrent) {
        this(id, name, presenter, startTime, endTime, description, isCurrent);
        this.dayOfWeek = dayOfWeek;
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public String getPresenter() { return presenter; }
    public String getStartTime() { return startTime; }
    public String getEndTime() { return endTime; }
    public String getDescription() { return description; }
    public String getDayOfWeek() { return dayOfWeek; }
    public boolean isCurrent() { return Boolean.TRUE.equals(isCurrent); }
}
