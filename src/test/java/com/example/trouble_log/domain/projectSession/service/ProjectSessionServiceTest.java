package com.example.trouble_log.domain.projectSession.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.trouble_log.domain.ai.service.AzureOpenAiPromptService;
import com.example.trouble_log.domain.audit.service.AuditLogService;
import com.example.trouble_log.domain.interview.exception.PersonalInfoDetectedException;
import com.example.trouble_log.domain.interview.repository.InterviewQuestionRepository;
import com.example.trouble_log.domain.interview.service.PersonalInfoDetectionService;
import com.example.trouble_log.domain.projectSession.dto.ProjectSessionRequest;
import com.example.trouble_log.domain.projectSession.dto.ProjectSessionResponse;
import com.example.trouble_log.domain.projectSession.entity.ProjectSession;
import com.example.trouble_log.domain.projectSession.repository.PreContextRepository;
import com.example.trouble_log.domain.projectSession.repository.ProjectSessionRepository;
import com.example.trouble_log.domain.user.entity.Member;
import com.example.trouble_log.domain.user.repository.MemberRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class ProjectSessionServiceTest {

    private MemberRepository memberRepository;
    private ProjectSessionRepository projectSessionRepository;
    private ProjectSessionService projectSessionService;

    @BeforeEach
    void setUp() {
        memberRepository = mock(MemberRepository.class);
        projectSessionRepository = mock(ProjectSessionRepository.class);
        projectSessionService = new ProjectSessionService(
                memberRepository,
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

    @Test
    void createSavesProjectSessionWhenCodeContentDoesNotContainPersonalInfo() {
        Member member = new Member("user@example.com", "password", "user");
        ReflectionTestUtils.setField(member, "id", 1L);

        ProjectSession savedProjectSession = new ProjectSession(
                member,
                "public class Main { public static void main(String[] args) { } }",
                "https://github.com/user/project"
        );
        ReflectionTestUtils.setField(savedProjectSession, "id", 10L);

        ProjectSessionRequest request = new ProjectSessionRequest(
                1L,
                "public class Main { public static void main(String[] args) { } }",
                "https://github.com/user/project"
        );

        when(memberRepository.findById(1L)).thenReturn(Optional.of(member));
        when(projectSessionRepository.save(any(ProjectSession.class))).thenReturn(savedProjectSession);

        ProjectSessionResponse response = projectSessionService.create(request);

        assertThat(response.getSessionId()).isEqualTo(10L);
        verify(projectSessionRepository).save(any(ProjectSession.class));
    }
}
