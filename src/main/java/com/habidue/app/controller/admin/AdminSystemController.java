package com.habidue.app.controller.admin;

import com.habidue.app.dto.ApiResponse;
import com.habidue.app.dto.admin.DbConnectionMetricsDto;
import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.HikariPoolMXBean;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;

@Slf4j
@RestController
@RequestMapping("/api/admin/system")
@RequiredArgsConstructor
public class AdminSystemController {

    private final DataSource dataSource;

    @GetMapping("/db-metrics")
    public ResponseEntity<ApiResponse<DbConnectionMetricsDto>> getDbMetrics() {
        if (dataSource instanceof HikariDataSource hikariDataSource) {
            HikariPoolMXBean poolMXBean = hikariDataSource.getHikariPoolMXBean();
            
            if (poolMXBean != null) {
                DbConnectionMetricsDto metrics = DbConnectionMetricsDto.builder()
                        .totalConnections(poolMXBean.getTotalConnections())
                        .activeConnections(poolMXBean.getActiveConnections())
                        .idleConnections(poolMXBean.getIdleConnections())
                        .threadsAwaitingConnection(poolMXBean.getThreadsAwaitingConnection())
                        .maxPoolSize(hikariDataSource.getMaximumPoolSize())
                        .build();
                
                return ApiResponse.success(metrics);
            }
        }
        
        return ApiResponse.error(HttpStatus.SERVICE_UNAVAILABLE, "HikariCP metrics are not available.", "METRICS_UNAVAILABLE");
    }
}
