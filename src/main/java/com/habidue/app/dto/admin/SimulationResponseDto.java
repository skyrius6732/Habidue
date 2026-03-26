package com.habidue.app.dto.admin;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SimulationResponseDto {
    private String actionName;
    private Long targetId;
    
    // Subject(보상 대상자)의 전후 상태
    private SimulationUserStatsDto subjectBefore;
    private SimulationUserStatsDto subjectAfter;
    
    // Actor(액션 수행자)의 전후 상태
    private SimulationUserStatsDto actorBefore;
    private SimulationUserStatsDto actorAfter;
}
