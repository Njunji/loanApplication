package com.codechamp.loanApp.repository;

import com.codechamp.loanApp.model.Repayment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RepaymentRepo extends JpaRepository<Repayment, Long> {
    List<Repayment> findByLoanId(Long loanId);

}
