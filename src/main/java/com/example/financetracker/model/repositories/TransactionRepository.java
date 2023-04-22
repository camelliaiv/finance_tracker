package com.example.financetracker.model.repositories;

import com.example.financetracker.model.entities.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface TransactionRepository extends JpaRepository<Transaction, Integer> {

    Page<Transaction> findAllByAccount_Owner(User user, Pageable pageable);

    Page<Transaction> findAllByAccount(Account account, Pageable pageable);

    Page<Transaction> findByDateBetweenAndCategoryAndAccount(LocalDateTime startDate, LocalDateTime endDate,
                                                             Category category, Account account, Pageable pageable);

    List<Transaction> findAllByPlannedPayment(PlannedPayment plannedPayment);

    List<Transaction> findByAccountAndDateBetween(Account account, LocalDateTime startDate, LocalDateTime endDate);
}
