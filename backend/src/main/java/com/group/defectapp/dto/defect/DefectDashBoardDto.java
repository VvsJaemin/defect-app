package com.group.defectapp.dto.defect;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "결함 대시보드(통계) DTO")
public class DefectDashBoardDto {

    @Schema(description = "통계 데이터(Overview)", implementation = StatisticData.class)
    private StatisticData statisticData;

    @Schema(description = "주간 결함 통계 데이터 리스트", implementation = WeeklyData.class)
    private List<WeeklyData> weeklyStats;

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @Schema(description = "대시보드 통계 데이터 집계")
    public static class StatisticData {
        @Schema(description = "오늘 발생한 결함 총 건수", example = "8")
        private long todayTotalDefect;

        @Schema(description = "오늘 처리된 결함 건수", example = "5")
        private long todayProcessedDefect;

        @Schema(description = "오늘 결함 조치율 (%)", example = "62.5")
        private double todayProcessRate;

        @Schema(description = "누적 총 결함 수", example = "124")
        private long totalDefect;

        @Schema(description = "누적 결함 해제 건수", example = "20")
        private long defectCanceled;

        @Schema(description = "누적 결함 종료 건수", example = "100")
        private long defectClosed;

        @Schema(description = "누적 결함 해제율 (%)", example = "16.1")
        private double cancelRate;

        @Schema(description = "누적 결함 종료율 (%)", example = "80.6")
        private double closeRate;
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @Schema(description = "주간(Weekly) 결함 통계 데이터")
    public static class WeeklyData {
        @Schema(description = "주 시작 날짜 (yyyy-MM-dd)", example = "2024-08-01")
        private String startDate;

        @Schema(description = "주 종료 날짜 (yyyy-MM-dd)", example = "2024-08-07")
        private String endDate;

        @Schema(description = "결함 발생 일자 (yyyy-MM-dd)", example = "2024-08-03")
        private String defectDate;

        @Schema(description = "해당 주 총 결함 건수", example = "22")
        private long totalDefects;

        @Schema(description = "해당 주 완료된 결함 건수", example = "18")
        private long completedDefects;

        @Schema(description = "해당 주 완료율 (%)", example = "81.8")
        private double completionRate;
    }
}