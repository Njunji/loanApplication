package com.codechamp.loanApp.service;

import com.codechamp.loanApp.model.Loan;
import com.codechamp.loanApp.model.Repayment;
import com.codechamp.loanApp.model.Subscriber;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.env.Environment;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.expression.common.LiteralExpression;
import org.springframework.integration.sftp.gateway.SftpOutboundGateway;
import org.springframework.integration.sftp.session.DefaultSftpSessionFactory;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;

import com.codechamp.loanApp.repository.*;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;

import java.io.File;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@AllArgsConstructor
@Slf4j
public class LoanService {
    private final Environment environment;
    private final LoanRepo loanRepository;
    private final RepaymentRepo repaymentRepository;
    @Value("${loan.age.threshold.months}")
    private int loanAgeThresholdMonths;

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

    public void sendSmsNotification(Subscriber subscriber, String message) {
        // Send SMS notification logic
    }
    @Scheduled(cron = "0 0 0 * * *")
    public void sweepDefaultedLoans() {
        LocalDateTime thresholdDate = LocalDateTime.now().minusMonths(loanAgeThresholdMonths);
        List<Loan> defaultedLoans = loanRepository.findByCreatedAtBeforeAndRepaymentsEmpty(thresholdDate);
        for (Loan loan : defaultedLoans) {
            // Perform necessary logic to clear the defaulted loan
            loanRepository.delete(loan);
        }
    }

    public void uploadDatabaseDumpToSftpServer(String dumpFilePath) {
        try {
            StandardEnvironment environment = new StandardEnvironment();
            String sftpHost = environment.getProperty("sftp.host");
            int sftpPort = environment.getProperty("sftp.port", Integer.class);
            String sftpUsername = environment.getProperty("sftp.username");
            String sftpPassword = environment.getProperty("sftp.password");
            String sftpRemoteDirectory = environment.getProperty("sftp.remoteDirectory");

            SessionFactory<ChannelSftp.LsEntry> sftpSessionFactory = createSftpSessionFactory(sftpHost, sftpPort, sftpUsername, sftpPassword);
            SftpOutboundGateway sftpGateway = createSftpOutboundGateway(sftpSessionFactory, sftpRemoteDirectory);

            File dumpFile = new File(dumpFilePath);
            Message<File> fileMessage = MessageBuilder.withPayload(dumpFile).build();
            sftpGateway.handleRequestMessage(fileMessage);

            log.info("Database dump uploaded successfully to the SFTP server.");
        } catch (Exception e) {
            log.error("Error uploading the database dump to the SFTP server: " + e.getMessage());
        }
    }

    private SessionFactory<ChannelSftp.LsEntry> createSftpSessionFactory(String sftpHost, int sftpPort, String sftpUsername, String sftpPassword) {
        DefaultSftpSessionFactory sftpSessionFactory = new DefaultSftpSessionFactory();
        sftpSessionFactory.setHost(sftpHost);
        sftpSessionFactory.setPort(sftpPort);
        sftpSessionFactory.setUser(sftpUsername);
        sftpSessionFactory.setPassword(sftpPassword);
        return sftpSessionFactory;
    }

    private SftpOutboundGateway createSftpOutboundGateway(SessionFactory<ChannelSftp.LsEntry> sftpSessionFactory, String sftpRemoteDirectory) {
        SftpOutboundGateway sftpGateway = new SftpOutboundGateway(sftpSessionFactory, "put", "payload");
        sftpGateway.setRemoteDirectoryExpression(new LiteralExpression(sftpRemoteDirectory));
        sftpGateway.setAutoCreateDirectory(true);
        return sftpGateway;
    }
}
