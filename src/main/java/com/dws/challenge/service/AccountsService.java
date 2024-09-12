package com.dws.challenge.service;

import java.math.BigDecimal;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.dws.challenge.domain.Account;
import com.dws.challenge.domain.TransferRequest;
import com.dws.challenge.exception.TransferValidationException;
import com.dws.challenge.repository.AccountsRepository;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class AccountsService {

	@Getter
	private final AccountsRepository accountsRepository;

	@Autowired
	private NotificationService notificationService;

	@Autowired
	public AccountsService(AccountsRepository accountsRepository) {
		this.accountsRepository = accountsRepository;
	}

	public void createAccount(Account account) {
		this.accountsRepository.createAccount(account);
	}

	public Account getAccount(String accountId) {
		return this.accountsRepository.getAccount(accountId);
	}

	public void transferAmount(TransferRequest transferRequest) {
		String fromAccountId = transferRequest.getFromAccountId();
		String toAccountId = transferRequest.getToAccountId();

		// Check if both the account ids are different
		if (fromAccountId.equals(toAccountId)) {
			throw new TransferValidationException("fromAccountId and toAccountId cannot be same.");
		}

		Account fromAccount = getAccount(fromAccountId);
		Account toAccount = getAccount(toAccountId);
		if (fromAccount == null)
			throw new TransferValidationException("No account found for given fromAccountId.");
		if (toAccount == null)
			throw new TransferValidationException("No account found for given toAccountId.");
		
		// Create a lock on combination of both the Account Ids to handle concurrent request
		synchronized (getAccountLock(fromAccountId, toAccountId)) {
				// Check from account has sufficient balance
				if (fromAccount.getBalance().compareTo(transferRequest.getAmount()) < 0) {
					throw new TransferValidationException("Given Account id: " + fromAccountId
							+ "does not have sufficient funds to initiate transfer.");
				}

				// Update account balance in both accounts
				BigDecimal fromAccountBalance = fromAccount.getBalance().subtract(transferRequest.getAmount());
				fromAccount.setBalance(fromAccountBalance);
				accountsRepository.save(fromAccount);
				BigDecimal toAccountBalance = toAccount.getBalance().add(transferRequest.getAmount());
				toAccount.setBalance(toAccountBalance);
				accountsRepository.save(toAccount);

				log.info("Account Balance Updated");
				// notify both the accounts once transaction is successful
				notificationService.notifyAboutTransfer(toAccount,
						"Successfully received amount " + transferRequest.getAmount() + " from Account " + fromAccount);
				notificationService.notifyAboutTransfer(fromAccount,
						"Successfully transferred amount " + transferRequest.getAmount() + " to Account " + toAccount);
		}
	}

	private Object getAccountLock(String fromAccountId, String toAccountId) {
		return fromAccountId.compareTo(toAccountId) < 0 ? (fromAccountId + toAccountId).intern()
				: (toAccountId + fromAccountId).intern();
	}

}
