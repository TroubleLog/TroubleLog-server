package com.example.trouble_log.domain.projectSession.service;

import com.example.trouble_log.domain.projectSession.dto.ProjectSessionRequest;
import com.example.trouble_log.domain.projectSession.dto.ProjectSessionResponse;
import com.example.trouble_log.domain.projectSession.entity.ProjectSession;
import com.example.trouble_log.domain.projectSession.repository.ProjectSessionRepository;
import com.example.trouble_log.domain.user.entity.Member;
import com.example.trouble_log.domain.user.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProjectSessionService {

    private final MemberRepository memberRepository;
    private final ProjectSessionRepository projectSessionRepository;

    @Transactional
    public ProjectSessionResponse create(ProjectSessionRequest request) {
        validateCreateRequest(request);

        Member member = memberRepository.findById(request.getMemberId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "유저를 찾지 못했습니다."));

        ProjectSession projectSession = new ProjectSession(
                member,
                request.getCodeContent(),
                request.getGithubUrl()
        );

        ProjectSession savedProjectSession = projectSessionRepository.save(projectSession);

        return new ProjectSessionResponse(savedProjectSession.getId());
    }

    private void validateCreateRequest(ProjectSessionRequest request) {
        if (request == null || request.getMemberId() == null || isBlank(request.getCodeContent())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "회원 ID와 소스코드는 필수입니다.");
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
