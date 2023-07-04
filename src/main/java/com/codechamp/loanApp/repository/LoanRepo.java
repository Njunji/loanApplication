package com.codechamp.loanApp.repository;

import com.codechamp.loanApp.model.Loan;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface LoanRepo extends JpaRepository<Loan, Long> {
    Optional<Loan> findById(Long Id);
    List<Loan> findByCreatedAtBeforeAndRepaymentsEmpty(LocalDateTime thresholdDate);
}
