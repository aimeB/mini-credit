package com.mini.credit.controller;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SystemStatusControllerTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    @Test
    void healthReturnsOkWhenDatabaseIsAvailable() {
        when(jdbcTemplate.queryForObject("SELECT 1", Integer.class)).thenReturn(1);
        SystemStatusController controller = new SystemStatusController(jdbcTemplate);

        ResponseEntity<Map<String, Object>> response = controller.health();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsEntry("status", "UP");
        assertThat(checks(response)).containsEntry("database", "UP");
    }

    @Test
    void healthReturnsServiceUnavailableWhenDatabaseIsUnavailable() {
        when(jdbcTemplate.queryForObject("SELECT 1", Integer.class)).thenThrow(new RuntimeException("db unavailable"));
        SystemStatusController controller = new SystemStatusController(jdbcTemplate);

        ResponseEntity<Map<String, Object>> response = controller.health();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(response.getBody()).containsEntry("status", "DOWN");
        assertThat(checks(response)).containsEntry("database", "DOWN");
    }

    @Test
    void statusReturnsNonSensitiveApplicationStatus() {
        when(jdbcTemplate.queryForObject("SELECT 1", Integer.class)).thenReturn(1);
        SystemStatusController controller = new SystemStatusController(jdbcTemplate);

        ResponseEntity<Map<String, Object>> response = controller.status();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsEntry("application", "mini-credit-api");
        assertThat(response.getBody()).containsEntry("status", "UP");
        assertThat(response.getBody()).containsEntry("database", "UP");
        assertThat(response.getBody()).doesNotContainKeys("password", "secret", "token", "jdbcUrl");
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> checks(ResponseEntity<Map<String, Object>> response) {
        return (Map<String, Object>) response.getBody().get("checks");
    }
}
