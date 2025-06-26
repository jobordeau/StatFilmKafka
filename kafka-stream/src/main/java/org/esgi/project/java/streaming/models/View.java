package org.esgi.project.java.streaming.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class View {
    public int    id;
    public String title;
    public String view_category;

    public View() {}

    public View(int id, String title, String view_category) {
        this.id            = id;
        this.title         = title;
        this.view_category = view_category;
    }
}
