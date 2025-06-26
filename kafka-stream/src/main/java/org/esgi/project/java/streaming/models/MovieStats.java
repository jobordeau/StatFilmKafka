package org.esgi.project.java.streaming.models;

public class MovieStats {
    public String title;
    public int totalViews = 0;
    public int startOnly = 0;
    public int half = 0;
    public int full = 0;

    public double totalScore = 0;
    public int scoresCount = 0;

    public MovieStats incrementViews(View view) {
        title = view.title;
        totalViews++;
        switch (view.view_category) {
            case "start_only": startOnly++; break;
            case "half": half++; break;
            case "full": full++; break;
        }
        return this;
    }

    public MovieStats addScore(double score) {
        totalScore += score;
        scoresCount++;
        return this;
    }

    public double avgScore() {
        return scoresCount > 0 ? totalScore / scoresCount : 0;
    }

    public MovieStats merge(MovieStats o){
        totalViews += o.totalViews;
        startOnly  += o.startOnly;
        half       += o.half;
        full       += o.full;
        totalScore += o.totalScore;
        scoresCount+= o.scoresCount;
        if(title==null) title=o.title;
        return this;
    }
}
