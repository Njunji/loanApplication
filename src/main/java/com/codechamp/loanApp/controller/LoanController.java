package com.codechamp.loanApp.controller;

import com.codechamp.loanApp.repository.RepaymentRepo;
import com.codechamp.loanApp.repository.SubscriberRepo;
import com.codechamp.loanApp.service.LoanService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/loans")
public class LoanController {
    private final SubscriberRepo subscriberRepo;
    private final LoanService loanService;
    private final RepaymentRepo repaymentRepo;
}
