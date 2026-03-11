package com.examples.bankservice.service;

import com.examples.bankservice.model.CreditRequest;
import com.examples.bankservice.model.CreditResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class BankServiceTest {

    @Mock
    private RabbitTemplate rabbitTemplate;

    private BankService bankService;

    @BeforeEach
    void setUp() {
        bankService = new BankService(rabbitTemplate);
    }

    @Test
    void handleCreditRequest_withinLimit_approves() {
        CreditRequest request = new CreditRequest();
        request.setCorrelationId("corr-1");
        request.setUsername("elie");
        request.setAmount(new BigDecimal("5000"));
        request.setOperationType("BUY");

        bankService.handleCreditRequest(request);

        ArgumentCaptor<CreditResponse> captor = ArgumentCaptor.forClass(CreditResponse.class);
        verify(rabbitTemplate).convertAndSend(eq("credit.response.queue"), captor.capture());

        CreditResponse response = captor.getValue();
        assertTrue(response.isApproved());
        assertEquals("corr-1", response.getCorrelationId());
        assertEquals("elie", response.getUsername());
    }

    @Test
    void handleCreditRequest_exceedsLimit_rejects() {
        CreditRequest request = new CreditRequest();
        request.setCorrelationId("corr-2");
        request.setUsername("john");
        request.setAmount(new BigDecimal("15000"));
        request.setOperationType("BUY");

        bankService.handleCreditRequest(request);

        ArgumentCaptor<CreditResponse> captor = ArgumentCaptor.forClass(CreditResponse.class);
        verify(rabbitTemplate).convertAndSend(eq("credit.response.queue"), captor.capture());

        CreditResponse response = captor.getValue();
        assertFalse(response.isApproved());
        assertTrue(response.getMessage().contains("Insufficient credit"));
    }

    @Test
    void handleCreditRequest_multipleRequests_tracksCumulativeCredit() {
        // First request: 6000 (approved, 4000 remaining)
        CreditRequest req1 = new CreditRequest();
        req1.setCorrelationId("corr-1");
        req1.setUsername("elie");
        req1.setAmount(new BigDecimal("6000"));
        req1.setOperationType("BUY");
        bankService.handleCreditRequest(req1);

        assertEquals(new BigDecimal("6000"), bankService.getUsedCredit("elie"));
        assertEquals(new BigDecimal("4000"), bankService.getRemainingCredit("elie"));

        // Second request: 5000 (rejected, only 4000 remaining)
        CreditRequest req2 = new CreditRequest();
        req2.setCorrelationId("corr-2");
        req2.setUsername("elie");
        req2.setAmount(new BigDecimal("5000"));
        req2.setOperationType("RENT");
        bankService.handleCreditRequest(req2);

        // Credit should still be 6000 used (second request rejected)
        assertEquals(new BigDecimal("6000"), bankService.getUsedCredit("elie"));
    }

    @Test
    void handleCreditRequest_exactLimit_approves() {
        CreditRequest request = new CreditRequest();
        request.setCorrelationId("corr-1");
        request.setUsername("max");
        request.setAmount(new BigDecimal("10000"));
        request.setOperationType("BUY");

        bankService.handleCreditRequest(request);

        ArgumentCaptor<CreditResponse> captor = ArgumentCaptor.forClass(CreditResponse.class);
        verify(rabbitTemplate).convertAndSend(eq("credit.response.queue"), captor.capture());

        assertTrue(captor.getValue().isApproved());
        assertEquals(new BigDecimal("10000"), bankService.getUsedCredit("max"));
    }

    @Test
    void getRemainingCredit_newUser_returnsFullLimit() {
        assertEquals(new BigDecimal("10000"), bankService.getRemainingCredit("newuser"));
    }

    @Test
    void getUsedCredit_newUser_returnsZero() {
        assertEquals(BigDecimal.ZERO, bankService.getUsedCredit("newuser"));
    }

    @Test
    void getCreditLimit_returnsDefault() {
        assertEquals(new BigDecimal("10000"), bankService.getCreditLimit());
    }

    @Test
    void getAllUsedCredits_tracksMultipleUsers() {
        CreditRequest req1 = new CreditRequest();
        req1.setCorrelationId("c1");
        req1.setUsername("user1");
        req1.setAmount(new BigDecimal("1000"));
        req1.setOperationType("RENT");
        bankService.handleCreditRequest(req1);

        CreditRequest req2 = new CreditRequest();
        req2.setCorrelationId("c2");
        req2.setUsername("user2");
        req2.setAmount(new BigDecimal("2000"));
        req2.setOperationType("BUY");
        bankService.handleCreditRequest(req2);

        var allCredits = bankService.getAllUsedCredits();
        assertEquals(2, allCredits.size());
        assertEquals(new BigDecimal("1000"), allCredits.get("user1"));
        assertEquals(new BigDecimal("2000"), allCredits.get("user2"));
    }
}
