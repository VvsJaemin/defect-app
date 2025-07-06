package com.group.defectapp.dto.defect;

import lombok.*;

import java.time.LocalDate;
import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DefectDashBoardDto {

    /**
     * 통계 데이터 (Overview 컴포넌트용)
     */
    private StatisticData statisticData;

    /**
     * 주간 결함 통계 데이터 리스트
     */
    private List<WeeklyData> weeklyStats;


    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class StatisticData {
        /**
         * 오늘 발생한 결함 총 건수
         */
        private long todayTotalDefect;

        /**
         * 오늘 처리된 결함 건수
         */
        private long todayProcessedDefect;

        /**
         * 오늘 결함 조치율 (%)
         */
        private double todayProcessRate;

        /**
         * 누적 총 결함 수
         */
        private long totalDefect;

        /**
         * 누적 결함 해제 건수
         */
        private long defectCanceled;

        /**
         * 누적 결함 종료 건수
         */
        private long defectClosed;

        /**
         * 누적 결함 해제율 (%)
         */
        private double cancelRate;

        /**
         * 누적 결함 종료율 (%)
         */
        private double closeRate;
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class WeeklyData {
        /**
         * 주의 시작 날짜
         */
        private String startDate;

        /**
         * 주의 종료 날짜
         */
        private String endDate;

        /**
        * 결함 발생 일자
        */
        private String defectDate;

        /**
         * 해당 주 총 결함 건수
         */
        private long totalDefects;

        /**
         * 해당 주 완료된 결함 건수
         */
        private long completedDefects;

        /**
         * 해당 주 완료율 (%)
         */
        private double completionRate;

    }
}