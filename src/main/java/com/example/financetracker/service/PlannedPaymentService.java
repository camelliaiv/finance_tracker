package com.example.financetracker.service;

import com.example.financetracker.model.DTOs.PlannedPaymentDTOs.PlannedPaymentDTO;
import com.example.financetracker.model.DTOs.PlannedPaymentDTOs.PlannedPaymentRequestDTO;
import com.example.financetracker.model.DTOs.TransactionDTOs.TransactionDTOWithoutPlannedPayments;
import com.example.financetracker.model.DTOs.TransactionDTOs.TransactionRequestDTO;
import com.example.financetracker.model.entities.*;
import com.example.financetracker.model.exceptions.BadRequestException;
import com.example.financetracker.model.exceptions.NotFoundException;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

@EnableScheduling
@Service
public class PlannedPaymentService extends AbstractService {

    @Autowired
    private TransactionService transactionService;

    public PlannedPaymentDTO createPlannedPayment(PlannedPaymentRequestDTO plannedPaymentRequestDTO, int loggedUserId) {
        User user = getUserById(loggedUserId);
        Account account = getAccountById(plannedPaymentRequestDTO.getAccountId());
        checkUserAuthorization(account.getOwner().getId(), user.getId());
        Category category = getCategoryById(plannedPaymentRequestDTO.getCategoryId());
        checkSufficientFunds(account.getBalance(), plannedPaymentRequestDTO.getAmount());
        PlannedPayment plannedPayment = new PlannedPayment();
        plannedPayment.setAccount(account);
        plannedPayment.setCategory(category);
        plannedPayment.setDescription(plannedPaymentRequestDTO.getDescription());
        plannedPayment.setAmount(plannedPaymentRequestDTO.getAmount());
        plannedPayment.setDate(plannedPaymentRequestDTO.getDate());
        plannedPayment.setFrequency(getFrequencyById(plannedPaymentRequestDTO.getFrequencyId()));
        plannedPaymentRepository.save(plannedPayment);
        logger.info("Created planned payment: "+plannedPayment.getId()+"\n"+plannedPayment.toString());

        return mapper.map(plannedPayment, PlannedPaymentDTO.class);
    }

    @Transactional
    public PlannedPaymentDTO editPlannedPaymentById(int id, PlannedPaymentRequestDTO plannedPaymentRequestDTO, int loggedUserId) {
        User user = getUserById(loggedUserId);
        PlannedPayment plannedPayment = getPlannedPaymentById(id);
        checkUserAuthorization(plannedPayment.getAccount().getOwner().getId(), user.getId());
        Category category = getCategoryById(plannedPaymentRequestDTO.getCategoryId());
        checkSufficientFunds(plannedPayment.getAccount().getBalance(), plannedPaymentRequestDTO.getAmount());
        Account account = getAccountById(plannedPaymentRequestDTO.getAccountId());
        plannedPayment.setAccount(account);
        plannedPayment.setCategory(category);
        plannedPayment.setDescription(plannedPaymentRequestDTO.getDescription());
        plannedPayment.setAmount(plannedPaymentRequestDTO.getAmount());
        plannedPayment.setDate(plannedPaymentRequestDTO.getDate());
        plannedPayment.setFrequency(getFrequencyById(plannedPaymentRequestDTO.getFrequencyId()));
        plannedPaymentRepository.save(plannedPayment);
        logger.info("Updated planned payment: "+plannedPayment.getId()+"\n"+plannedPayment.toString());

        return mapper.map(plannedPayment, PlannedPaymentDTO.class);
    }

    public PlannedPaymentDTO deletePlannedPaymentById(int id, int loggedUserId) {
        User user = getUserById(loggedUserId);
        PlannedPayment plannedPayment = getPlannedPaymentById(id);
        checkUserAuthorization(plannedPayment.getAccount().getOwner().getId(), user.getId());
        List<Transaction> transactions = transactionRepository.findAllByPlannedPayment(plannedPayment);
        if (!transactions.isEmpty()) {
            throw new BadRequestException("Cannot delete planned payment that has related transactions.");
        }
        plannedPaymentRepository.delete(plannedPayment);
        logger.info("Deleted planned payment: "+plannedPayment.getId()+"\n"+plannedPayment.toString());

        return mapper.map(plannedPayment, PlannedPaymentDTO.class);
    }

    public PlannedPaymentDTO getPlannedPaymentById(int id, int loggedUserId) {
        User user = getUserById(loggedUserId);
        PlannedPayment plannedPayment = getPlannedPaymentById(id);
        checkUserAuthorization(plannedPayment.getAccount().getOwner().getId(), user.getId());

        return mapper.map(plannedPayment, PlannedPaymentDTO.class);
    }

    public Page<PlannedPaymentDTO> getAllPlannedPaymentsForAccount(int accountId, int loggedUserId, Pageable pageable) {
        User user = getUserById(loggedUserId);
        Account account = getAccountById(accountId);
        checkUserAuthorization(account.getOwner().getId(), user.getId());
        Page<PlannedPayment> plannedPayments = plannedPaymentRepository.findAllByAccount(account, pageable);
        checkIfPlannedPaymentsExist(plannedPayments);

        return plannedPayments.map(plannedPayment -> mapper.map(plannedPayment, PlannedPaymentDTO.class));
    }

    public Page<TransactionDTOWithoutPlannedPayments> getAllTransactionsForPlannedPayment(int plannedPaymentId, int loggedUserId, Pageable pageable) {
        User user = getUserById(loggedUserId);
        PlannedPayment plannedPayment = getPlannedPaymentById(plannedPaymentId);
        checkUserAuthorization(plannedPayment.getAccount().getOwner().getId(), user.getId());
        Page<Transaction> transactions = transactionRepository.findAllByPlannedPayment(plannedPayment, pageable);
        checkIfTransactionsExist(transactions);

        return transactions.map(transaction -> mapper.map(transaction, TransactionDTOWithoutPlannedPayments.class));
    }

    @Transactional
    //@Scheduled(fixedDelay = 1000) // test - every 2 seconds
    @Scheduled(fixedDelay = 24 * 60 * 60 * 1000) // every 24 hours
    public void processPlannedPayments() {
        List<PlannedPayment> plannedPayments = plannedPaymentRepository.findAllByDate(LocalDateTime.now().truncatedTo(ChronoUnit.DAYS));
        for (PlannedPayment plannedPayment : plannedPayments) {
            TransactionRequestDTO transactionRequestDTO = new TransactionRequestDTO();
            transactionRequestDTO.setAmount(plannedPayment.getAmount());
            transactionRequestDTO.setDate(plannedPayment.getDate());
            transactionRequestDTO.setAccountId(plannedPayment.getAccount().getId());
            transactionRequestDTO.setDescription(plannedPayment.getDescription());
            transactionRequestDTO.setCategoryId(plannedPayment.getCategory().getId());
            transactionRequestDTO.setPlannedPaymentId(plannedPayment.getId());
            transactionRequestDTO.setCurrencyId(plannedPayment.getAccount().getCurrency().getId());
            transactionService.createTransaction(transactionRequestDTO, plannedPayment.getAccount().getOwner().getId());

            Frequency frequency = plannedPayment.getFrequency();
            LocalDateTime nextPaymentDate = plannedPayment.getDate().plusDays(1);
            switch (frequency.getFrequencyType()) {
                case DAILY:
                    nextPaymentDate = plannedPayment.getDate().plusDays(1);
                    break;
                case WEEKLY:
                    nextPaymentDate = plannedPayment.getDate().plusWeeks(1);
                    break;
                case MONTHLY:
                    nextPaymentDate = plannedPayment.getDate().plusMonths(1);
                    break;
                case YEARLY:
                    nextPaymentDate = plannedPayment.getDate().plusYears(1);
                    break;
            }
            plannedPayment.setDate(nextPaymentDate);
        }

        plannedPaymentRepository.saveAll(plannedPayments);

    }

    private void checkIfPlannedPaymentsExist(Page<PlannedPayment> plannedPayments){
        if (plannedPayments.isEmpty()) {
            throw new NotFoundException("Planned payments not found");
        }
    }
}
