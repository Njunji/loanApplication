package com.codechamp.loanApp.service;

import com.codechamp.loanApp.model.Loan;
import com.codechamp.loanApp.model.Repayment;
import com.codechamp.loanApp.model.Subscriber;
import com.codechamp.loanApp.repository.LoanRepo;
import com.codechamp.loanApp.repository.RepaymentRepo;
import com.codechamp.loanApp.repository.SubscriberRepo;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@AllArgsConstructor
public class LoanService {
    private final SubscriberRepo subscriberRepository;
    private final LoanRepo loanRepository;
    private final RepaymentRepo repaymentRepository;

    public void makeRepayment(Long loanId, BigDecimal amount) {
        Optional<Loan> optionalLoan = loanRepository.findById(loanId);
        if (optionalLoan.isPresent()) {
            Loan loan = optionalLoan.get();
            BigDecimal remainingAmount = loan.getAmount().subtract(getTotalRepaymentAmount(loan));
            if (amount.compareTo(remainingAmount) >= 0) {
                amount = remainingAmount;
            }
            Repayment repayment = new Repayment();
            repayment.setAmount(amount);
            repayment.setLoan(loan);
            repayment.setCreatedAt(LocalDateTime.now());
            repaymentRepository.save(repayment);
            // Send SMS notification
            sendSmsNotification(loan.getSubscriber(), "Repayment made: " + amount);
        }
    }

    private BigDecimal getTotalRepaymentAmount(Loan loan) {
        List<Repayment> repayments = repaymentRepository.findByLoanId(loan.getId());
        return repayments.stream()
                .map(Repayment::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private void sendSmsNotification(Subscriber subscriber, String message) {
        // Send SMS notification logic
    }
}
