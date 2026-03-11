package com.examples.bankservice.service;

import com.examples.bankservice.config.RabbitMQConfig;
import com.examples.bankservice.model.CreditRequest;
import com.examples.bankservice.model.CreditResponse;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class BankService {

    private static final BigDecimal DEFAULT_CREDIT_LIMIT = new BigDecimal("100000");

    // Tracks how much credit each user has already used
    private final Map<String, BigDecimal> usedCredit = new ConcurrentHashMap<>();

    private final RabbitTemplate rabbitTemplate;

    public BankService(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    @RabbitListener(queues = RabbitMQConfig.CREDIT_REQUEST_QUEUE)
    public void handleCreditRequest(CreditRequest request) {
        System.out.println("[Bank Service] Received credit request: user=" + request.getUsername()
                + " amount=" + request.getAmount()
                + " operation=" + request.getOperationType());

        BigDecimal alreadyUsed = usedCredit.getOrDefault(request.getUsername(), BigDecimal.ZERO);
        BigDecimal remaining = DEFAULT_CREDIT_LIMIT.subtract(alreadyUsed);

        CreditResponse response;

        if (request.getAmount().compareTo(remaining) <= 0) {
            // Approved: deduct from available credit
            usedCredit.put(request.getUsername(), alreadyUsed.add(request.getAmount()));
            response = new CreditResponse(
                    request.getCorrelationId(),
                    request.getUsername(),
                    true,
                    "Credit approved. Remaining credit: " + remaining.subtract(request.getAmount()) + " EUR"
            );
            System.out.println("[Bank Service] APPROVED for " + request.getUsername());
        } else {
            // Rejected: insufficient credit
            response = new CreditResponse(
                    request.getCorrelationId(),
                    request.getUsername(),
                    false,
                    "Insufficient credit. Available: " + remaining + " EUR, requested: " + request.getAmount() + " EUR"
            );
            System.out.println("[Bank Service] REJECTED for " + request.getUsername());
        }

        rabbitTemplate.convertAndSend(RabbitMQConfig.CREDIT_RESPONSE_QUEUE, response);
    }

    public BigDecimal getCreditLimit() {
        return DEFAULT_CREDIT_LIMIT;
    }

    public BigDecimal getUsedCredit(String username) {
        return usedCredit.getOrDefault(username, BigDecimal.ZERO);
    }

    public BigDecimal getRemainingCredit(String username) {
        return DEFAULT_CREDIT_LIMIT.subtract(getUsedCredit(username));
    }

    public Map<String, BigDecimal> getAllUsedCredits() {
        return Map.copyOf(usedCredit);
    }
}
