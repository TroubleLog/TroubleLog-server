package com.example.trouble_log.domain.ai;

import com.example.trouble_log.domain.ai.dto.AnswerFeedbackResult;
import com.example.trouble_log.domain.ai.dto.CodeEvaluationResult;
import com.example.trouble_log.domain.ai.dto.RadarScore;
import com.example.trouble_log.domain.ai.service.AzureOpenAiPromptService;
import com.example.trouble_log.domain.ai.service.RadarScoreCalculator;
import com.example.trouble_log.TroubleLogApplication;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = TroubleLogApplication.class)
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@Disabled("Azure OpenAI 키 필요 — 로컬에서만 실행")
class PromptIntegrationTest {

    @Autowired AzureOpenAiPromptService promptService;
    @Autowired RadarScoreCalculator radarCalculator;

    // ── 공통 입력값 ──────────────────────────────────────────
    static final String CODE = """
            @Service
            @RequiredArgsConstructor
            public class OrderService {

                private final OrderRepository orderRepository;
                private final InventoryRepository inventoryRepository;

                @Transactional
                public OrderResult createOrder(Long userId, List<OrderItem> items) {
                    for (OrderItem item : items) {
                        Inventory inventory = inventoryRepository.findByProductId(item.getProductId())
                                .orElseThrow(() -> new RuntimeException("상품을 찾을 수 없습니다"));
                        if (inventory.getStock() < item.getQuantity()) {
                            throw new IllegalStateException("재고가 부족합니다");
                        }
                    }
                    Order order = Order.builder()
                            .userId(userId).items(items).status(OrderStatus.PENDING).build();
                    orderRepository.save(order);
                    for (OrderItem item : items) {
                        inventoryRepository.decrementStock(item.getProductId(), item.getQuantity());
                    }
                    return new OrderResult(order.getId(), OrderStatus.CONFIRMED);
                }
            }
            """;

    static final String PURPOSE   = "주문 생성 API의 핵심 서비스 로직입니다. 재고 확인 후 주문을 저장하고 재고를 차감합니다.";
    static final String TECH      = "Spring Boot + JPA를 선택했습니다. 팀 전원이 익숙하고 @Transactional로 트랜잭션 관리가 편리해서 선택했습니다.";
    static final String EXCEPTION = "재고 부족 시 IllegalStateException, 상품 없을 때 RuntimeException을 던집니다. 전역 예외 처리는 아직 없습니다.";
    static final String SCALE     = "3주, 4인 팀 프로젝트입니다.";

    static final String QUESTION    = "재고 확인 루프와 재고 차감 루프를 분리해서 두 번 순회하도록 설계한 이유가 있나요?";
    static final String ANSWER_LOW  = "그냥 그렇게 짰어요. 먼저 다 확인하고 나서 차감하는 게 맞는 것 같아서요.";
    static final String ANSWER_HIGH = """
            루프를 합치면 3번째 상품에서 재고 부족이 발생했을 때 앞의 두 상품은 이미 차감된 상태가 됩니다.
            검증을 먼저 전부 통과한 뒤에 차감을 시작해야 부분 차감 문제를 막을 수 있어서 분리했습니다.
            @Transactional이 있어도 차감 전에 예외가 나면 롤백되지만,
            차감 도중 예외가 나면 일부만 차감된 상태로 남을 수 있기 때문입니다.
            """;

    // qaPairs — STEP 5 리포트 생성에 사용할 Q&A 문자열
    static final String QA_PAIRS = """
            Q1. 재고 확인 루프와 재고 차감 루프를 분리해서 두 번 순회하도록 설계한 이유가 있나요?
            A1. 루프를 합치면 3번째 상품에서 재고 부족이 발생했을 때 앞의 두 상품은 이미 차감된 상태가 됩니다. \
            검증을 먼저 전부 통과한 뒤에 차감을 시작해야 부분 차감 문제를 막을 수 있어서 분리했습니다. \
            @Transactional이 있어도 차감 전에 예외가 나면 롤백되지만, 차감 도중 예외가 나면 일부만 차감된 상태로 남을 수 있기 때문입니다.

            Q2. 전역 예외 처리가 없는 상태에서 RuntimeException이 호출자에게 전파될 때 어떤 문제가 발생하나요?
            A2. HTTP 상태코드가 500으로 고정되어 클라이언트가 오류 원인을 파악하기 어렵습니다. \
            추후 @ControllerAdvice를 도입해서 재고 부족은 400, 상품 없음은 404로 분리할 계획입니다.

            Q3. 현재 재고 확인과 차감 사이에 락이 없는데, 동시 주문이 들어오면 어떤 문제가 생기나요?
            A3. 두 요청이 동시에 재고 확인을 통과하면 실제 재고보다 많은 수량이 차감되는 오버셀링이 발생합니다. \
            낙관적 잠금이나 비관적 잠금을 도입해서 해결할 수 있습니다.
            """;

    // ── STEP 1: 면접 질문 생성 ────────────────────────────────
    @Test
    @Order(1)
    @DisplayName("STEP1 | 면접 질문 3개 생성 — List<String> 파싱 및 내용 확인")
    void step1_generateQuestions() {
        var result = promptService.generateInterviewQuestions(
                CODE, PURPOSE, TECH, EXCEPTION, SCALE
        );

        System.out.println("\n=== STEP1 면접 질문 생성 결과 ===");
        for (int i = 0; i < result.size(); i++) {
            System.out.println("Q" + (i + 1) + ": " + result.get(i));
        }

        assertThat(result).as("질문이 3개가 아님").hasSize(3);
        result.forEach(q -> assertThat(q).as("빈 질문이 포함됨").isNotBlank());
        result.forEach(q -> assertThat(q.length()).as("질문이 너무 짧음: " + q).isGreaterThan(10));
    }

    // ── STEP 2-A: 낮은 수준 답변 피드백 ─────────────────────
    @Test
    @Order(2)
    @DisplayName("STEP2-A | 낮은 수준 답변 — scores 낮음 + improvement 존재")
    void step2_feedbackLowAnswer() {
        AnswerFeedbackResult result = promptService.evaluateAnswer(QUESTION, ANSWER_LOW);

        System.out.println("\n=== STEP2-A 낮은 답변 피드백 ===");
        printFeedbackResult(result);

        assertThat(result.getScores()).as("scores 필드가 null").isNotNull();
        assertScoreRange(result);
        assertThat(result.getScores().getSpecificity()).as("낮은 답변인데 specificity가 너무 높음").isLessThanOrEqualTo(2);
        assertThat(result.getScores().getKeyword()).as("낮은 답변인데 keyword가 너무 높음").isLessThanOrEqualTo(2);
        assertThat(result.getImprovement()).as("낮은 답변인데 improvement가 null 또는 빈값").isNotNull().isNotBlank();
    }

    // ── STEP 2-B: 높은 수준 답변 피드백 ─────────────────────
    @Test
    @Order(3)
    @DisplayName("STEP2-B | 높은 수준 답변 — 주요 scores 높음 + warning null")
    void step2_feedbackHighAnswer() {
        AnswerFeedbackResult result = promptService.evaluateAnswer(QUESTION, ANSWER_HIGH);

        System.out.println("\n=== STEP2-B 높은 답변 피드백 ===");
        printFeedbackResult(result);

        assertThat(result.getScores()).as("scores 필드가 null").isNotNull();
        assertScoreRange(result);
        assertThat(result.getScores().getRelevance()).as("좋은 답변인데 relevance가 낮음").isGreaterThanOrEqualTo(4);
        assertThat(result.getScores().getStructure()).as("좋은 답변인데 structure가 낮음").isGreaterThanOrEqualTo(3);
        assertThat(result.getWarning()).as("warning이 불필요하게 출력됨").isNull();
    }

    // ── STEP 3: 코드 정량 평가 ───────────────────────────────
    @Test
    @Order(4)
    @DisplayName("STEP3 | 코드 평가 — 5축 점수 0~20 범위 및 comment 존재 확인")
    void step3_codeEvaluation() {
        CodeEvaluationResult result = promptService.evaluateCode(CODE);

        System.out.println("\n=== STEP3 코드 정량 평가 ===");
        printCodeEvalResult(result);

        assertThat(result.getNaming())               .as("naming null").isNotNull();
        assertThat(result.getSingleResponsibility()) .as("singleResponsibility null").isNotNull();
        assertThat(result.getErrorHandling())        .as("errorHandling null").isNotNull();
        assertThat(result.getDuplication())          .as("duplication null").isNotNull();
        assertThat(result.getCommentQuality())       .as("commentQuality null").isNotNull();

        assertThat(result.getNaming().getScore())               .as("naming 범위 초과").isBetween(0, 20);
        assertThat(result.getSingleResponsibility().getScore()) .as("singleResponsibility 범위 초과").isBetween(0, 20);
        assertThat(result.getErrorHandling().getScore())        .as("errorHandling 범위 초과").isBetween(0, 20);
        assertThat(result.getDuplication().getScore())          .as("duplication 범위 초과").isBetween(0, 20);
        assertThat(result.getCommentQuality().getScore())       .as("commentQuality 범위 초과").isBetween(0, 20);

        assertThat(result.getNaming().getComment())               .as("naming comment 없음").isNotBlank();
        assertThat(result.getSingleResponsibility().getComment()) .as("singleResponsibility comment 없음").isNotBlank();
        assertThat(result.getErrorHandling().getComment())        .as("errorHandling comment 없음").isNotBlank();
        assertThat(result.getDuplication().getComment())          .as("duplication comment 없음").isNotBlank();
        assertThat(result.getCommentQuality().getComment())       .as("commentQuality comment 없음").isNotBlank();

        assertThat(result.getCommentQuality().getScore()).as("주석 없는 코드인데 commentQuality가 너무 높음").isLessThan(15);
        assertThat(result.getSingleResponsibility().getScore()).as("책임 혼재 코드인데 singleResponsibility가 너무 높음").isLessThan(15);
    }

    // ── STEP 4: RadarScore 통합 계산 ────────────────────────
    @Test
    @Order(5)
    @DisplayName("STEP4 | RadarScore 통합 — STEP2-B + STEP3 연결 후 축별 범위 확인")
    void step4_radarScore() {
        AnswerFeedbackResult feedback = promptService.evaluateAnswer(QUESTION, ANSWER_HIGH);
        CodeEvaluationResult codeEval = promptService.evaluateCode(CODE);
        RadarScore radar = radarCalculator.calculate(feedback, codeEval);

        System.out.println("\n=== STEP4 RadarScore 최종 ===");
        System.out.println("problemSolving  (문제해결력):  " + radar.getProblemSolving());
        System.out.println("techJudgment    (기술 판단력): " + radar.getTechJudgment());
        System.out.println("codeReliability (코드 신뢰성): " + radar.getCodeReliability());
        System.out.println("communication   (커뮤니케이션):" + radar.getCommunication());
        System.out.println("designThinking  (설계 사고력): " + radar.getDesignThinking());

        assertThat(radar.getProblemSolving())  .as("problemSolving 범위 초과").isBetween(0, 100);
        assertThat(radar.getTechJudgment())    .as("techJudgment 범위 초과").isBetween(0, 100);
        assertThat(radar.getCodeReliability()) .as("codeReliability 범위 초과").isBetween(0, 100);
        assertThat(radar.getCommunication())   .as("communication 범위 초과").isBetween(0, 100);
        assertThat(radar.getDesignThinking())  .as("designThinking 범위 초과").isBetween(0, 100);

        assertThat(radar.getProblemSolving())
                .as("좋은 답변인데 problemSolving이 codeReliability보다 낮음")
                .isGreaterThanOrEqualTo(radar.getCodeReliability());
    }

    // ── STEP 5: 트러블슈팅 리포트 생성 ──────────────────────
    @Test
    @Order(6)
    @DisplayName("STEP5 | 트러블슈팅 리포트 — 마크다운 형식 및 5개 섹션 포함 확인")
    void step5_generateReport() {
        String report = promptService.generateReport(
                CODE, PURPOSE, TECH, EXCEPTION, SCALE, QA_PAIRS
        );

        System.out.println("\n=== STEP5 트러블슈팅 리포트 ===");
        System.out.println(report);

        // 리포트가 비어있지 않음
        assertThat(report)
                .as("리포트가 비어있음")
                .isNotNull()
                .isNotBlank();

        // 5개 섹션 헤더 존재 확인
        assertThat(report).as("Background 섹션 없음").contains("## Background");
        assertThat(report).as("Problem 섹션 없음").contains("## Problem");
        assertThat(report).as("Root Cause 섹션 없음").contains("## Root Cause");
        assertThat(report).as("Resolution 섹션 없음").contains("## Resolution");
        assertThat(report).as("Result 섹션 없음").contains("## Result");

        // 책임있는 AI 문구 존재 확인
        assertThat(report)
                .as("data_basis 문구 없음")
                .contains("이 리포트는 사용자가 제출한 코드와 면접 Q&A 답변만을 근거로 생성되었습니다");

        // 없는 내용 추가 금지 확인 — QA_PAIRS에 없는 수치가 들어오면 안 됨
        assertThat(report)
                .as("없는 수치가 포함됨 (99.97% 등)")
                .doesNotContain("99.97%")
                .doesNotContain("1,000 TPS")
                .doesNotContain("100% → 0%");
    }

    // ── 헬퍼 메서드 ──────────────────────────────────────────
    private void printFeedbackResult(AnswerFeedbackResult result) {
        if (result.getScores() != null) {
            System.out.println("specificity: " + result.getScores().getSpecificity());
            System.out.println("structure:   " + result.getScores().getStructure());
            System.out.println("relevance:   " + result.getScores().getRelevance());
            System.out.println("keyword:     " + result.getScores().getKeyword());
        }
        System.out.println("improvement: " + result.getImprovement());
        System.out.println("warning:     " + result.getWarning());
    }

    private void printCodeEvalResult(CodeEvaluationResult result) {
        System.out.println("naming:               "
                + result.getNaming().getScore() + "점 | " + result.getNaming().getComment());
        System.out.println("singleResponsibility: "
                + result.getSingleResponsibility().getScore() + "점 | " + result.getSingleResponsibility().getComment());
        System.out.println("errorHandling:        "
                + result.getErrorHandling().getScore() + "점 | " + result.getErrorHandling().getComment());
        System.out.println("duplication:          "
                + result.getDuplication().getScore() + "점 | " + result.getDuplication().getComment());
        System.out.println("commentQuality:       "
                + result.getCommentQuality().getScore() + "점 | " + result.getCommentQuality().getComment());
    }

    private void assertScoreRange(AnswerFeedbackResult result) {
        assertThat(result.getScores().getSpecificity()).as("specificity 범위 초과").isBetween(1, 5);
        assertThat(result.getScores().getStructure())  .as("structure 범위 초과").isBetween(1, 5);
        assertThat(result.getScores().getRelevance())  .as("relevance 범위 초과").isBetween(1, 5);
        assertThat(result.getScores().getKeyword())    .as("keyword 범위 초과").isBetween(1, 5);
    }
}
