package com.codechamp.loanApp.service;

import com.codechamp.loanApp.model.Loan;
import com.codechamp.loanApp.model.Repayment;
import com.codechamp.loanApp.model.Subscriber;
import com.codechamp.loanApp.repository.LoanRepo;
import com.codechamp.loanApp.repository.RepaymentRepo;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.core.env.Environment;
import org.springframework.expression.common.LiteralExpression;
import org.springframework.integration.sftp.gateway.SftpOutboundGateway;
import org.springframework.integration.sftp.session.DefaultSftpSessionFactory;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@Slf4j
class LoanServiceTest {
    @Mock
    private LoanRepo loanRepo;

    @Mock
    private RepaymentRepo repaymentRepo;

    @Mock
    private Subscriber subscriber;

    private LoanService loanService;

    @Mock
    private final Environment environment;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        loanService = new LoanService(loanRepo, repaymentRepo, 3);
    }

    @Test
    void makeRepayment_ValidLoanIdAndAmount_RepaymentMade() {
        Long loanId = 1L;
        BigDecimal loanAmount = new BigDecimal("1000.00");
        BigDecimal repaymentAmount = new BigDecimal("500.00");

        Loan loan = new Loan();
        loan.setId(loanId);
        loan.setAmount(loanAmount);
        loan.setSubscriber(subscriber);

        Repayment repayment = new Repayment();
        repayment.setLoan(loan);

        when(loanRepo.findById(loanId)).thenReturn(Optional.of(loan));
        when(repaymentRepo.save(any(Repayment.class))).thenReturn(repayment);

        loanService.makeRepayment(loanId, repaymentAmount);

        verify(repaymentRepo, times(1)).save(any(Repayment.class));
        verify(loanService, times(1)).sendSmsNotification(subscriber, "Repayment made: " + repaymentAmount);
    }

    @Test
    void makeRepayment_InvalidLoanId_NoRepaymentMade() {
        Long loanId = 1L;
        BigDecimal repaymentAmount = new BigDecimal("500.00");

        when(loanRepo.findById(loanId)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> loanService.makeRepayment(loanId, repaymentAmount));
        verify(repaymentRepo, never()).save(any(Repayment.class));
        verify(loanService, never()).sendSmsNotification(any(Subscriber.class), anyString());
    }

    @Test
    void getTotalRepaymentAmount_ExistingLoan_CorrectTotalAmount() {
        Long loanId = 1L;
        BigDecimal repaymentAmount1 = new BigDecimal("300.00");
        BigDecimal repaymentAmount2 = new BigDecimal("200.00");

        Repayment repayment1 = new Repayment();
        repayment1.setAmount(repaymentAmount1);

        Repayment repayment2 = new Repayment();
        repayment2.setAmount(repaymentAmount2);

        List<Repayment> repayments = new ArrayList<>();
        repayments.add(repayment1);
        repayments.add(repayment2);

        when(repaymentRepo.findByLoanId(loanId)).thenReturn(repayments);

        BigDecimal totalRepaymentAmount = loanService.getTotalRepaymentAmount(new Loan(loanId));

        assertEquals(new BigDecimal("500.00"), totalRepaymentAmount);
    }

    @Test
    void getTotalRepaymentAmount_NoRepayments_ZeroTotalAmount() {
        // Arrange
        Long loanId = 1L;

        when(repaymentRepo.findByLoanId(loanId)).thenReturn(new ArrayList<>());

        // Act
        BigDecimal totalRepaymentAmount = loanService.getTotalRepaymentAmount(new Loan(loanId));

        // Assert
        assertEquals(BigDecimal.ZERO, totalRepaymentAmount);
    }

    @Test
    void sweepDefaultedLoans_DefaultedLoansExist_LoansDeleted() {
        // Arrange
        LocalDateTime thresholdDate = LocalDateTime.now().minusMonths(3);
        Loan defaultedLoan1 = new Loan();
        defaultedLoan1.setCreatedAt(thresholdDate.minusDays(1));
        Loan defaultedLoan2 = new Loan();
        defaultedLoan2.setCreatedAt(thresholdDate.minusDays(2));
        List<Loan> defaultedLoans = List.of(defaultedLoan1, defaultedLoan2);

        when(loanRepo.findByCreatedAtBeforeAndRepaymentsEmpty(thresholdDate)).thenReturn(defaultedLoans);

        // Act
        loanService.sweepDefaultedLoans();

        // Assert
        verify(loanRepo, times(2)).delete(any(Loan.class));
    }

    @Test
    void uploadDatabaseDumpToSftpServer_ValidDumpFilePath_FileUploadedSuccessfully() {

        String dumpFilePath = "/api/database/loanApplication";


        when(environment.getProperty("sftp.host")).thenReturn("sftp.example.com");
        when(environment.getProperty("sftp.port", Integer.class)).thenReturn(22);
        when(environment.getProperty("sftp.username")).thenReturn("username");
        when(environment.getProperty("sftp.password")).thenReturn("password");
        when(environment.getProperty("sftp.remoteDirectory")).thenReturn("/remote/directory");

        // Mocking session factory and outbound gateway
        DefaultSftpSessionFactory sftpSessionFactory = mock(DefaultSftpSessionFactory.class);
        SftpOutboundGateway sftpGateway = mock(SftpOutboundGateway.class);

        when(loanService.createSftpSessionFactory(anyString(), anyInt(), anyString(), anyString())).thenReturn(sftpSessionFactory);
        when(loanService.createSftpOutboundGateway(eq(sftpSessionFactory), anyString())).thenReturn(sftpGateway);
        loanService.uploadDatabaseDumpToSftpServer(dumpFilePath);

        // Assert
        verify(sftpSessionFactory, times(1)).setHost("sftp.example.com");
        verify(sftpSessionFactory, times(1)).setPort(22);
        verify(sftpSessionFactory, times(1)).setUser("username");
        verify(sftpSessionFactory, times(1)).setPassword("password");

        verify(sftpGateway, times(1)).setRemoteDirectoryExpression(any(LiteralExpression.class));
        verify(sftpGateway, times(1)).setAutoCreateDirectory(true);

        verify(sftpGateway, times(1)).handleRequestMessage(any(Message.class));

        log.info("Database dump uploaded successfully to the SFTP server.");
    }
}

