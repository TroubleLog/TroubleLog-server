package com.example.trouble_log.domain.projectSession.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.example.trouble_log.domain.ai.service.AzureOpenAiPromptService;
import com.example.trouble_log.domain.audit.service.AuditLogService;
import com.example.trouble_log.domain.interview.exception.PersonalInfoDetectedException;
import com.example.trouble_log.domain.interview.repository.InterviewQuestionRepository;
import com.example.trouble_log.domain.interview.service.PersonalInfoDetectionService;
import com.example.trouble_log.domain.projectSession.dto.ProjectSessionRequest;
import com.example.trouble_log.domain.projectSession.entity.ProjectSession;
import com.example.trouble_log.domain.projectSession.repository.PreContextRepository;
import com.example.trouble_log.domain.projectSession.repository.ProjectSessionRepository;
import com.example.trouble_log.domain.user.repository.MemberRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ProjectSessionServiceTest {

    private ProjectSessionRepository projectSessionRepository;
    private ProjectSessionService projectSessionService;

    @BeforeEach
    void setUp() {
        projectSessionRepository = mock(ProjectSessionRepository.class);
        projectSessionService = new ProjectSessionService(
                mock(MemberRepository.class),
                projectSessionRepository,
                mock(PreContextRepository.class),
                mock(InterviewQuestionRepository.class),
                mock(AzureOpenAiPromptService.class),
                mock(AuditLogService.class),
                new PersonalInfoDetectionService()
        );
    }

    @Test
    void createBlocksWhenCodeContentContainsPersonalInfo() {
        ProjectSessionRequest request = new ProjectSessionRequest(
                1L,
                "const email = \"test@example.com\";",
                null
        );

        assertThatThrownBy(() -> projectSessionService.create(request))
                .isInstanceOf(PersonalInfoDetectedException.class);

        verify(projectSessionRepository, never()).save(any(ProjectSession.class));
    }
}
