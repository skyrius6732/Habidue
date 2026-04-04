package com.habidue.app.dto.admin;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class DbConnectionMetricsDto {
    private int totalConnections;
    private int activeConnections;
    private int idleConnections;
    private int threadsAwaitingConnection;
    private int maxPoolSize;
}
