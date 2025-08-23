package com.group.defectapp.dto.defect;

import lombok.*;

import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DefectDashBoardDto {

    private StatisticData statisticData;

    private List<WeeklyData> weeklyStats;

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class StatisticData {
        private long todayTotalDefect;

        private long todayProcessedDefect;

        private double todayProcessRate;

        private long totalDefect;

        private long defectCanceled;

        private long defectClosed;

        private double cancelRate;

        private double closeRate;
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class WeeklyData {
        private String startDate;

        private String endDate;

        private String defectDate;

        private long totalDefects;

        private long completedDefects;

        private double completionRate;
    }
}