package com.flowchat.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public class AnalysisData {
    
    @JsonProperty("type")
    private String analysisType;
    
    @JsonProperty("roomId")
    private Long roomId;
    
    @JsonProperty("timestamp")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime timestamp;
    
    @JsonProperty("keywords")
    private KeywordAnalysis keywords;
    
    @JsonProperty("participation")
    private ParticipationAnalysis participation;
    
    @JsonProperty("hourlyActivity")
    private HourlyAnalysis hourlyActivity;
    
    public static class KeywordAnalysis {
        @JsonProperty("labels")
        private List<String> labels;
        
        @JsonProperty("data")
        private List<Integer> data;
        
        @JsonProperty("backgroundColor")
        private List<String> backgroundColor;
        
        @JsonProperty("topKeywords")
        private List<Map<String, Object>> topKeywords;
        
        @JsonProperty("totalKeywords")
        private Integer totalKeywords;

        public KeywordAnalysis() {}

        public KeywordAnalysis(List<String> labels, List<Integer> data, List<Map<String, Object>> topKeywords, Integer totalKeywords) {
            this.labels = labels;
            this.data = data;
            this.topKeywords = topKeywords;
            this.totalKeywords = totalKeywords;
            // Chart.js용 기본 색상 설정
            this.backgroundColor = List.of(
                "#FF6384", "#36A2EB", "#FFCE56", "#4BC0C0", "#9966FF",
                "#FF9F40", "#FF6384", "#C9CBCF", "#4BC0C0", "#FF6384"
            );
        }

        // Getters and Setters
        public List<String> getLabels() { return labels; }
        public void setLabels(List<String> labels) { this.labels = labels; }
        
        public List<Integer> getData() { return data; }
        public void setData(List<Integer> data) { this.data = data; }
        
        public List<String> getBackgroundColor() { return backgroundColor; }
        public void setBackgroundColor(List<String> backgroundColor) { this.backgroundColor = backgroundColor; }
        
        public List<Map<String, Object>> getTopKeywords() { return topKeywords; }
        public void setTopKeywords(List<Map<String, Object>> topKeywords) { this.topKeywords = topKeywords; }
        
        public Integer getTotalKeywords() { return totalKeywords; }
        public void setTotalKeywords(Integer totalKeywords) { this.totalKeywords = totalKeywords; }
    }
    
    public static class ParticipationAnalysis {
        @JsonProperty("labels")
        private List<String> labels;
        
        @JsonProperty("data")
        private List<Integer> data;
        
        @JsonProperty("backgroundColor")
        private List<String> backgroundColor;
        
        @JsonProperty("userParticipation")
        private List<Map<String, Object>> userParticipation;
        
        @JsonProperty("totalUsers")
        private Integer totalUsers;

        public ParticipationAnalysis() {}

        public ParticipationAnalysis(List<String> labels, List<Integer> data, List<Map<String, Object>> userParticipation, Integer totalUsers) {
            this.labels = labels;
            this.data = data;
            this.userParticipation = userParticipation;
            this.totalUsers = totalUsers;
            // Chart.js용 기본 색상 설정
            this.backgroundColor = List.of(
                "#36A2EB", "#FF6384", "#FFCE56", "#4BC0C0", "#9966FF",
                "#FF9F40", "#FF6384", "#C9CBCF", "#4BC0C0", "#FF6384"
            );
        }

        // Getters and Setters
        public List<String> getLabels() { return labels; }
        public void setLabels(List<String> labels) { this.labels = labels; }
        
        public List<Integer> getData() { return data; }
        public void setData(List<Integer> data) { this.data = data; }
        
        public List<String> getBackgroundColor() { return backgroundColor; }
        public void setBackgroundColor(List<String> backgroundColor) { this.backgroundColor = backgroundColor; }
        
        public List<Map<String, Object>> getUserParticipation() { return userParticipation; }
        public void setUserParticipation(List<Map<String, Object>> userParticipation) { this.userParticipation = userParticipation; }
        
        public Integer getTotalUsers() { return totalUsers; }
        public void setTotalUsers(Integer totalUsers) { this.totalUsers = totalUsers; }
    }
    
    public static class HourlyAnalysis {
        @JsonProperty("labels")
        private List<String> labels;
        
        @JsonProperty("data")
        private List<Integer> data;
        
        @JsonProperty("borderColor")
        private String borderColor = "#36A2EB";
        
        @JsonProperty("backgroundColor")
        private String backgroundColor = "rgba(54, 162, 235, 0.2)";
        
        @JsonProperty("hourlyActivity")
        private List<Map<String, Object>> hourlyActivity;

        public HourlyAnalysis() {}

        public HourlyAnalysis(List<String> labels, List<Integer> data, List<Map<String, Object>> hourlyActivity) {
            this.labels = labels;
            this.data = data;
            this.hourlyActivity = hourlyActivity;
        }

        // Getters and Setters
        public List<String> getLabels() { return labels; }
        public void setLabels(List<String> labels) { this.labels = labels; }
        
        public List<Integer> getData() { return data; }
        public void setData(List<Integer> data) { this.data = data; }
        
        public String getBorderColor() { return borderColor; }
        public void setBorderColor(String borderColor) { this.borderColor = borderColor; }
        
        public String getBackgroundColor() { return backgroundColor; }
        public void setBackgroundColor(String backgroundColor) { this.backgroundColor = backgroundColor; }
        
        public List<Map<String, Object>> getHourlyActivity() { return hourlyActivity; }
        public void setHourlyActivity(List<Map<String, Object>> hourlyActivity) { this.hourlyActivity = hourlyActivity; }
    }
    
    public AnalysisData() {}
    
    public AnalysisData(String analysisType, Long roomId, LocalDateTime timestamp) {
        this.analysisType = analysisType;
        this.roomId = roomId;
        this.timestamp = timestamp;
    }

    // Static factory methods
    public static AnalysisData createKeywordUpdate(Long roomId, KeywordAnalysis keywords) {
        AnalysisData data = new AnalysisData("KEYWORD_UPDATE", roomId, LocalDateTime.now());
        data.setKeywords(keywords);
        return data;
    }
    
    public static AnalysisData createParticipationUpdate(Long roomId, ParticipationAnalysis participation) {
        AnalysisData data = new AnalysisData("PARTICIPATION_UPDATE", roomId, LocalDateTime.now());
        data.setParticipation(participation);
        return data;
    }
    
    public static AnalysisData createHourlyUpdate(Long roomId, HourlyAnalysis hourlyActivity) {
        AnalysisData data = new AnalysisData("HOURLY_UPDATE", roomId, LocalDateTime.now());
        data.setHourlyActivity(hourlyActivity);
        return data;
    }
    
    public static AnalysisData createFullUpdate(Long roomId, KeywordAnalysis keywords, 
                                              ParticipationAnalysis participation, 
                                              HourlyAnalysis hourlyActivity) {
        AnalysisData data = new AnalysisData("FULL_UPDATE", roomId, LocalDateTime.now());
        data.setKeywords(keywords);
        data.setParticipation(participation);
        data.setHourlyActivity(hourlyActivity);
        return data;
    }

    // Getters and Setters
    public String getAnalysisType() { return analysisType; }
    public void setAnalysisType(String analysisType) { this.analysisType = analysisType; }
    
    public Long getRoomId() { return roomId; }
    public void setRoomId(Long roomId) { this.roomId = roomId; }
    
    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
    
    public KeywordAnalysis getKeywords() { return keywords; }
    public void setKeywords(KeywordAnalysis keywords) { this.keywords = keywords; }
    
    public ParticipationAnalysis getParticipation() { return participation; }
    public void setParticipation(ParticipationAnalysis participation) { this.participation = participation; }
    
    public HourlyAnalysis getHourlyActivity() { return hourlyActivity; }
    public void setHourlyActivity(HourlyAnalysis hourlyActivity) { this.hourlyActivity = hourlyActivity; }
}