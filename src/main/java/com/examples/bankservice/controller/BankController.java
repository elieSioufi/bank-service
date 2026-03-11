package com.examples.bankservice.controller;

import com.examples.bankservice.service.BankService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/bank")
public class BankController {

    private final BankService bankService;

    public BankController(BankService bankService) {
        this.bankService = bankService;
    }

    @GetMapping("/credit/{username}")
    public ResponseEntity<?> getCreditInfo(@PathVariable String username) {
        return ResponseEntity.ok(Map.of(
                "username", username,
                "creditLimit", bankService.getCreditLimit(),
                "usedCredit", bankService.getUsedCredit(username),
                "remainingCredit", bankService.getRemainingCredit(username)
        ));
    }

    @GetMapping("/credits")
    public ResponseEntity<?> getAllCredits() {
        return ResponseEntity.ok(Map.of(
                "creditLimit", bankService.getCreditLimit(),
                "usedCredits", bankService.getAllUsedCredits()
        ));
    }
}
