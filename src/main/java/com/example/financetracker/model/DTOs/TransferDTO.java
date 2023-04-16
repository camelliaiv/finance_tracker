package com.example.financetracker.model.DTOs;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class TransferDTO {

    private int id;
    private LocalDateTime date;
    private String description;
    private BigDecimal amount;
    private AccountWithoutOwnerDTO accountSender;
    private AccountWithoutOwnerDTO accountReceiver;

}