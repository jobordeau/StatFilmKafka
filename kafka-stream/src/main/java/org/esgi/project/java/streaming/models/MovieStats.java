package org.esgi.project.java.streaming.models;

public class MovieStats {
    public int totalViews = 0;
    public int startOnly = 0;
    public int half = 0;
    public int full = 0;

    public double totalScore = 0;
    public int scoresCount = 0;

    public MovieStats incrementViews(View view) {
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

    public double averageScore() {
        return scoresCount > 0 ? totalScore / scoresCount : 0;
    }
}
