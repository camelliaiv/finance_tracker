package com.example.financetracker.service;

import com.example.financetracker.AccountStatementExcelGenerator;
import com.example.financetracker.AccountStatementPdfGenerator;
import com.example.financetracker.model.DTOs.AccountDTOs.AccountWithOwnerDTO;
import com.example.financetracker.model.DTOs.AccountDTOs.AccountWithoutOwnerDTO;
import com.example.financetracker.model.DTOs.AccountDTOs.CreateAccountDTO;
import com.example.financetracker.model.DTOs.AccountDTOs.EditAccountDTO;
import com.example.financetracker.model.entities.Account;
import com.example.financetracker.model.entities.Currency;
import com.example.financetracker.model.entities.Transaction;
import com.example.financetracker.model.entities.User;
import com.example.financetracker.model.exceptions.BadRequestException;
import com.example.financetracker.model.exceptions.NotFoundException;
import com.example.financetracker.model.repositories.AccountRepository;
import com.example.financetracker.model.repositories.CurrencyRepository;
import com.fatboyindustrial.gsonjavatime.Converters;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class AccountService extends AbstractService {

    @Autowired
    private CurrencyRepository currencyRepository;
    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private AccountStatementPdfGenerator accountStatementPdfGenerator;

    @Autowired
    private AccountStatementExcelGenerator accountStatementExcelGenerator;

    public AccountWithOwnerDTO create(CreateAccountDTO dto, int userId) {
        Account account = new Account();
        account.setName(dto.getName());
        account.setBalance(dto.getBalance());
        User u = getUserById(userId);
        account.setOwner(u);
        Currency currency = getCurrencyById(dto.getCurrencyId());
        account.setCurrency(currency);
        validateAccountData(account);
        accountRepository.save(account);
        logger.info("Created account: "+account.getId()+"\n"+account.toString());
        return mapper.map(account, AccountWithOwnerDTO.class);
    }

    public AccountWithoutOwnerDTO edit(int id, EditAccountDTO dto, int userId) {
        Account account = accountRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Account not found"));
        if (account.getOwner().getId() != userId) {
            throw new BadRequestException("You can not change the owner of account!");
        }
        account.setName(dto.getName());
        account.setBalance(dto.getBalance());
        // set the currency using the currency_id in the DTO
        Currency currency = currencyRepository.findById(dto.getCurrencyId())
                .orElseThrow(() -> new NotFoundException("Currency not found"));
        account.setCurrency(currency);
        validateAccountData(account);
        accountRepository.save(account);
        logger.info("Updated account: "+account.getId()+"\n"+account.toString());

        return mapper.map(account, AccountWithoutOwnerDTO.class);
    }

    public AccountWithOwnerDTO getById(int id, int userId) {
        Optional<Account> account = accountRepository.findById(id);
        if (account.isPresent()) {
            AccountWithOwnerDTO account1 = mapper.map(account.get(), AccountWithOwnerDTO.class);
            if (account1.getOwner().getId() == userId) {
                return account1;
            }
            throw new BadRequestException("You can't view foreign account");
        }
        throw new NotFoundException("Account not found!");
    }

    private void validateAccountData(Account account) {
        if (account.getBalance().compareTo(BigDecimal.ZERO) < 0) {
            throw new BadRequestException("You can not update your account balance with a negative or zero number value.");
        }
        if (account.getCurrency() == null) {
            throw new BadRequestException("Please choose any currency for your account!");
        }
        if (account.getName() == null) {
            throw new BadRequestException("You have to write any account name.");
        }
    }

    public List<AccountWithoutOwnerDTO> getAllAccounts(int id) {
        return accountRepository.findAllByOwnerId(id)
                .stream()
                .map(account -> mapper.map(account, AccountWithoutOwnerDTO.class))
                .collect(Collectors.toList());
    }

    public AccountWithoutOwnerDTO deleteAccountById(int id, int userId) {
        Optional<Account> optionalAccount = accountRepository.findById(id);
        if (!optionalAccount.isPresent()) {
            throw new NotFoundException("Account not found.");
        }
        Account account = optionalAccount.get();
        if (account.getOwner().getId() == userId) {
            accountRepository.deleteById(id);
        }
        logger.info("Deleted account: "+account.getId()+"\n"+account.toString());

        return mapper.map(account, AccountWithoutOwnerDTO.class);
    }

    @SneakyThrows
    public ByteArrayOutputStream generateAccountStatementPdf(int id, LocalDateTime startDate, LocalDateTime endDate, int loggedUserId) {
        Account account = getAccountById(id);
        User user = getUserById(loggedUserId);
        checkUserAuthorization(account.getOwner().getId(), user.getId());

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        return outputStream = accountStatementPdfGenerator.generatePdf(account, startDate, endDate, outputStream);
    }

    @SneakyThrows
    public ByteArrayOutputStream generateAccountStatementExcel(int id, LocalDateTime startDate, LocalDateTime endDate, int loggedUserId) {
        Account account = getAccountById(id);
        User user = getUserById(loggedUserId);
        checkUserAuthorization(account.getOwner().getId(), user.getId());

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        return outputStream = accountStatementExcelGenerator.generateExcel(account, startDate, endDate, outputStream);
    }

    @SneakyThrows
    public ByteArrayOutputStream generateAccountStatementJson(int id, LocalDateTime startDate, LocalDateTime endDate, int loggedUserId) {
        Account account = getAccountById(id);
        User user = getUserById(loggedUserId);
        checkUserAuthorization(account.getOwner().getId(), user.getId());

        List<Transaction> transactions = transactionRepository.findAllByAccount_IdAndDateBetween(id, startDate, endDate);
        checkIfTransactionsExist(transactions);

        GsonBuilder gsonBuilder = new GsonBuilder();
        Converters.registerLocalDateTime(gsonBuilder); // Added the LocalDateTime converter
        gsonBuilder.excludeFieldsWithoutExposeAnnotation();
        Gson gson = gsonBuilder.create();
        String json = gson.toJson(transactions);

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        outputStream.write(json.getBytes());

        return outputStream;
    }
}
