package com.codechamp.loanApp.controller;

import com.codechamp.loanApp.model.Loan;
import com.codechamp.loanApp.model.Subscriber;
import com.codechamp.loanApp.repository.LoanRepo;
import com.codechamp.loanApp.repository.RepaymentRepo;
import com.codechamp.loanApp.repository.SubscriberRepo;
import com.codechamp.loanApp.service.LoanService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/loans")
public class LoanController {
    private final SubscriberRepo subscriberRepo;
    private final LoanService loanService;
    private final LoanRepo loanRepository;
    private final RepaymentRepo repaymentRepo;

    @PostMapping("/{msisdn}")
    public ResponseEntity<String> requestLoan(
            @PathVariable("msisdn") String msisdn,
            @RequestParam("amount") BigDecimal amount) {
        Optional<Subscriber> optionalSubscriber = subscriberRepo.findByMsisdn(msisdn);
        if (optionalSubscriber.isPresent()) {
            Subscriber subscriber = optionalSubscriber.get();
            Loan loan = new Loan();
            loan.setAmount(amount);
            loan.setSubscriber(subscriber);
            loan.setCreatedAt(LocalDateTime.now());
            loanRepository.save(loan);
            // Send SMS notification
            loanService.sendSmsNotification(subscriber, "Loan requested: " + amount);
            return ResponseEntity.ok("Loan requested successfully.");
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/{loanId}/repay")
    public ResponseEntity<String> makeRepayment(@PathVariable("loanId") Long loanId,
                                                @RequestParam("amount") BigDecimal amount) {
        loanService.makeRepayment(loanId, amount);
        return ResponseEntity.ok("Repayment made successfully.");
    }
}
