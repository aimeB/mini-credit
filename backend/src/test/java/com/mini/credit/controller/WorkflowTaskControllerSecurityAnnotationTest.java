package com.mini.credit.controller;

import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;

class WorkflowTaskControllerSecurityAnnotationTest {

    @Test
    void endpointAnnotations_shouldRequireTaskPermissions() throws NoSuchMethodException {
        Method getMyActions = WorkflowTaskController.class.getMethod("getMyActions", String.class);
        Method countMyActions = WorkflowTaskController.class.getMethod("countMyActions");
        Method getDashboard = WorkflowTaskController.class.getMethod("getDashboard");
        Method markAsViewed = WorkflowTaskController.class.getMethod("markAsViewed", Long.class, com.mini.credit.dto.workflow.WorkflowTaskUpdateRequest.class);
        Method complete = WorkflowTaskController.class.getMethod("complete", Long.class, com.mini.credit.dto.workflow.WorkflowTaskUpdateRequest.class);
        Method reconcileCaisseTasks = WorkflowTaskController.class.getMethod("reconcileCaisseTasks");

        assertThat(getMyActions.getAnnotation(PreAuthorize.class).value())
                .isEqualTo("hasAnyAuthority('TASK_READ_OWN', 'TASK_READ_ANTENNE', 'TASK_SUPERVISE', 'TASK_AUDIT')");
        assertThat(countMyActions.getAnnotation(PreAuthorize.class).value())
                .isEqualTo("hasAnyAuthority('TASK_READ_OWN', 'TASK_READ_ANTENNE', 'TASK_SUPERVISE', 'TASK_AUDIT')");
        assertThat(getDashboard.getAnnotation(PreAuthorize.class).value())
                .isEqualTo("hasAnyAuthority('TASK_READ_OWN', 'TASK_READ_ANTENNE', 'TASK_SUPERVISE', 'TASK_AUDIT')");
        assertThat(markAsViewed.getAnnotation(PreAuthorize.class).value())
                .isEqualTo("hasAnyAuthority('TASK_READ_OWN', 'TASK_READ_ANTENNE', 'TASK_SUPERVISE', 'TASK_AUDIT')");
        assertThat(complete.getAnnotation(PreAuthorize.class).value())
                .isEqualTo("hasAuthority('TASK_COMPLETE')");
        assertThat(reconcileCaisseTasks.getAnnotation(PreAuthorize.class).value())
                .isEqualTo("hasRole('ADMIN')");
    }
}
