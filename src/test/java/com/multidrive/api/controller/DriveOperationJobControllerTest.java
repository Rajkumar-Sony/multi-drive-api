package com.multidrive.api.controller;

import com.multidrive.api.dto.DriveOperationJobResponse;
import com.multidrive.api.dto.DriveOperationJobsPageResponse;
import com.multidrive.api.entity.DriveConflictStrategy;
import com.multidrive.api.entity.DriveOperationJobStatus;
import com.multidrive.api.entity.DriveOperationType;
import com.multidrive.api.model.DriveOperationStrategyType;
import com.multidrive.api.service.DriveOperationJobService;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oidcLogin;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        DriveOperationJobController.class
)
class DriveOperationJobControllerTest {

    private static final String GOOGLE_SUBJECT_ID =
            "google-subject-123";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DriveOperationJobService driveOperationJobService;

    @Test
    void submitCreatesQueuedJob() throws Exception {

        DriveOperationJobResponse response =
                response(
                        10L,
                        DriveOperationJobStatus.QUEUED
                );

        when(
                driveOperationJobService
                        .submit(
                                eq(
                                        GOOGLE_SUBJECT_ID
                                ),
                                eq(
                                        "submit-key-1"
                                ),
                                any()
                        )
        )
                .thenReturn(
                        response
                );

        mockMvc.perform(
                        post(
                                "/api/drive/operation-jobs"
                        )
                                .with(
                                        oidcLogin()
                                                .idToken(
                                                        token -> token
                                                                .subject(
                                                                        GOOGLE_SUBJECT_ID
                                                                )
                                                )
                                )
                                .with(
                                        csrf()
                                )
                                .header(
                                        "Idempotency-Key",
                                        "submit-key-1"
                                )
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(
                                        """
                                                {
                                                  "operationType": "COPY",
                                                  "sourceItemId": 123,
                                                  "destinationSourceId": 456,
                                                  "destinationParentItemId": 789,
                                                  "name": "Quarterly Report Copy",
                                                  "conflictStrategy": "KEEP_BOTH"
                                                }
                                                """
                                )
                )
                .andExpect(
                        status()
                                .isAccepted()
                )
                .andExpect(
                        jsonPath(
                                "$.id"
                        )
                                .value(
                                        10
                                )
                )
                .andExpect(
                        jsonPath(
                                "$.status"
                        )
                                .value(
                                        "QUEUED"
                                )
                )
                .andExpect(
                        jsonPath(
                                "$.operationType"
                        )
                                .value(
                                        "COPY"
                                )
                );

        ArgumentCaptor<com.multidrive.api.dto.DriveOperationJobSubmitRequest>
                requestCaptor =
                        ArgumentCaptor.forClass(
                                com.multidrive.api.dto.DriveOperationJobSubmitRequest.class
                        );

        verify(
                driveOperationJobService
        )
                .submit(
                        eq(
                                GOOGLE_SUBJECT_ID
                        ),
                        eq(
                                "submit-key-1"
                        ),
                        requestCaptor.capture()
                );

        assertThat(
                requestCaptor
                        .getValue()
                        .operationType()
        )
                .isEqualTo(
                        DriveOperationType.COPY
                );

        assertThat(
                requestCaptor
                        .getValue()
                        .destinationParentItemId()
        )
                .isEqualTo(
                        789L
                );
    }

    @Test
    void getJobReturnsSingleJob() throws Exception {

        when(
                driveOperationJobService
                        .getJob(
                                GOOGLE_SUBJECT_ID,
                                10L
                        )
        )
                .thenReturn(
                        response(
                                10L,
                                DriveOperationJobStatus.QUEUED
                        )
                );

        mockMvc.perform(
                        get(
                                "/api/drive/operation-jobs/{jobId}",
                                10L
                        )
                                .with(
                                        oidcLogin()
                                                .idToken(
                                                        token -> token
                                                                .subject(
                                                                        GOOGLE_SUBJECT_ID
                                                                )
                                                )
                                )
                )
                .andExpect(
                        status()
                                .isOk()
                )
                .andExpect(
                        jsonPath(
                                "$.id"
                        )
                                .value(
                                        10
                                )
                );

        verify(
                driveOperationJobService
        )
                .getJob(
                        GOOGLE_SUBJECT_ID,
                        10L
                );
    }

    @Test
    void getJobsReturnsPage() throws Exception {

        DriveOperationJobResponse job =
                response(
                        10L,
                        DriveOperationJobStatus.QUEUED
                );

        when(
                driveOperationJobService
                        .getJobs(
                                GOOGLE_SUBJECT_ID,
                                null,
                                1,
                                2
                        )
        )
                .thenReturn(
                        new DriveOperationJobsPageResponse(
                                List.of(
                                        job
                                ),
                                1,
                                2,
                                3,
                                2,
                                false,
                                true
                        )
                );

        mockMvc.perform(
                        get(
                                "/api/drive/operation-jobs"
                        )
                                .param(
                                        "page",
                                        "1"
                                )
                                .param(
                                        "size",
                                        "2"
                                )
                                .with(
                                        oidcLogin()
                                                .idToken(
                                                        token -> token
                                                                .subject(
                                                                        GOOGLE_SUBJECT_ID
                                                                )
                                                )
                                )
                )
                .andExpect(
                        status()
                                .isOk()
                )
                .andExpect(
                        jsonPath(
                                "$.items[0].id"
                        )
                                .value(
                                        10
                                )
                )
                .andExpect(
                        jsonPath(
                                "$.page"
                        )
                                .value(
                                        1
                                )
                )
                .andExpect(
                        jsonPath(
                                "$.totalElements"
                        )
                                .value(
                                        3
                                )
                );

        verify(
                driveOperationJobService
        )
                .getJobs(
                        GOOGLE_SUBJECT_ID,
                        null,
                        1,
                        2
                );
    }

    @Test
    void getJobsFiltersByStatus() throws Exception {

        when(
                driveOperationJobService
                        .getJobs(
                                GOOGLE_SUBJECT_ID,
                                DriveOperationJobStatus.CANCELLED,
                                null,
                                null
                        )
        )
                .thenReturn(
                        new DriveOperationJobsPageResponse(
                                List.of(),
                                0,
                                20,
                                0,
                                0,
                                true,
                                true
                        )
                );

        mockMvc.perform(
                        get(
                                "/api/drive/operation-jobs"
                        )
                                .param(
                                        "status",
                                        "CANCELLED"
                                )
                                .with(
                                        oidcLogin()
                                                .idToken(
                                                        token -> token
                                                                .subject(
                                                                        GOOGLE_SUBJECT_ID
                                                                )
                                                )
                                )
                )
                .andExpect(
                        status()
                                .isOk()
                )
                .andExpect(
                        jsonPath(
                                "$.items"
                        )
                                .isArray()
                );

        verify(
                driveOperationJobService
        )
                .getJobs(
                        GOOGLE_SUBJECT_ID,
                        DriveOperationJobStatus.CANCELLED,
                        null,
                        null
                );
    }

    @Test
    void cancelRequestsCancellation() throws Exception {

        when(
                driveOperationJobService
                        .cancel(
                                GOOGLE_SUBJECT_ID,
                                10L
                        )
        )
                .thenReturn(
                        response(
                                10L,
                                DriveOperationJobStatus.CANCELLED
                        )
                );

        mockMvc.perform(
                        post(
                                "/api/drive/operation-jobs/{jobId}/cancel",
                                10L
                        )
                                .with(
                                        oidcLogin()
                                                .idToken(
                                                        token -> token
                                                                .subject(
                                                                        GOOGLE_SUBJECT_ID
                                                                )
                                                )
                                )
                                .with(
                                        csrf()
                                )
                )
                .andExpect(
                        status()
                                .isOk()
                )
                .andExpect(
                        jsonPath(
                                "$.status"
                        )
                                .value(
                                        "CANCELLED"
                                )
                );

        verify(
                driveOperationJobService
        )
                .cancel(
                        GOOGLE_SUBJECT_ID,
                        10L
                );
    }

    @Test
    void endpointRequiresAuthentication() throws Exception {

        mockMvc.perform(
                        get(
                                "/api/drive/operation-jobs"
                        )
                )
                .andExpect(
                        status()
                                .is3xxRedirection()
                )
                .andExpect(
                        redirectedUrlPattern(
                                "/oauth2/authorization/*"
                        )
                );
    }

    private DriveOperationJobResponse response(
            Long jobId,
            DriveOperationJobStatus status
    ) {

        LocalDateTime now =
                LocalDateTime.of(
                        2026,
                        8,
                        16,
                        12,
                        0
                );

        return new DriveOperationJobResponse(
                jobId,
                DriveOperationType.COPY,
                DriveOperationStrategyType.NATIVE_COPY,
                status,
                DriveConflictStrategy.KEEP_BOTH,
                123L,
                "Quarterly Report",
                "application/pdf",
                1L,
                2L,
                789L,
                "destination-google-folder",
                "Quarterly Report Copy",
                null,
                null,
                1L,
                0L,
                0L,
                2048L,
                0L,
                0,
                3,
                status == DriveOperationJobStatus.CANCELLED,
                null,
                null,
                null,
                null,
                status == DriveOperationJobStatus.CANCELLED
                        ? now
                        : null,
                now,
                now
        );
    }
}
