package com.dws.challenge;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import java.math.BigDecimal;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import com.dws.challenge.domain.Account;
import com.dws.challenge.domain.TransferRequest;
import com.dws.challenge.exception.DuplicateAccountIdException;
import com.dws.challenge.exception.TransferValidationException;
import com.dws.challenge.service.AccountsService;

@ExtendWith(SpringExtension.class)
@SpringBootTest
class AccountsServiceTest {

	@Autowired
	private AccountsService accountsService;

	@BeforeEach
	void setUp() {
		MockitoAnnotations.openMocks(this);
	}

	@Test
	void addAccount() {
		Account account = new Account("Id-123");
		account.setBalance(new BigDecimal(1000));
		this.accountsService.createAccount(account);

		assertThat(this.accountsService.getAccount("Id-123")).isEqualTo(account);
	}

	@Test
	void addAccount_failsOnDuplicateId() {
		String uniqueId = "Id-" + System.currentTimeMillis();
		Account account = new Account(uniqueId);
		this.accountsService.createAccount(account);

		try {
			this.accountsService.createAccount(account);
			fail("Should have failed when adding duplicate account");
		} catch (DuplicateAccountIdException ex) {
			assertThat(ex.getMessage()).isEqualTo("Account id " + uniqueId + " already exists!");
		}
	}

	@Test
	void transfer_verifySuccessful() {
		Account fromAccount = new Account("Id-100");
		fromAccount.setBalance(new BigDecimal(1000));
		this.accountsService.createAccount(fromAccount);
		Account toAccount = new Account("Id-200");
		toAccount.setBalance(new BigDecimal(1000));
		this.accountsService.createAccount(toAccount);
		TransferRequest transferReq = new TransferRequest(fromAccount.getAccountId(), toAccount.getAccountId(),
				new BigDecimal(500));

		this.accountsService.transferAmount(transferReq);
		assertEquals(new BigDecimal(500), fromAccount.getBalance());
		assertEquals(new BigDecimal(1500), toAccount.getBalance());
	}

	@Test
	void transferTest_InsufficientBalance() {
		Account fromAccount = new Account("Id-300");
		fromAccount.setBalance(new BigDecimal(100));
		this.accountsService.createAccount(fromAccount);

		Account toAccount = new Account("Id-400");
		toAccount.setBalance(new BigDecimal(1000));
		this.accountsService.createAccount(toAccount);
		TransferRequest transferReq = new TransferRequest(fromAccount.getAccountId(), toAccount.getAccountId(),
				new BigDecimal(400));
		try {
			this.accountsService.transferAmount(transferReq);
		} catch (TransferValidationException te) {
			assertThat(te.getMessage()).isEqualTo("Given Account id: " + fromAccount.getAccountId()
					+ "does not have sufficient funds to initiate transfer.");
		}
	}

	@Test
	void testTransferRequest_InvalidFromAccountId() throws Exception {
		Account fromAccount = new Account("Id-500", new BigDecimal("999.01"));
		this.accountsService.createAccount(fromAccount);
		Account toAccount = new Account("Id-600", new BigDecimal("100"));
		this.accountsService.createAccount(toAccount);
		TransferRequest transferReq = new TransferRequest("A1", toAccount.getAccountId(), new BigDecimal(500));
		try {
			this.accountsService.transferAmount(transferReq);
		} catch (TransferValidationException te) {
			assertThat(te.getMessage()).isEqualTo("No account found for given fromAccountId.");
		}
	}

	@Test
	void testTransferRequest_InvalidToAccountId() throws Exception {
		Account fromAccount = new Account("Id-700", new BigDecimal("999.01"));
		this.accountsService.createAccount(fromAccount);
		Account toAccount = new Account("Id-800", new BigDecimal("100"));
		this.accountsService.createAccount(toAccount);
		TransferRequest transferReq = new TransferRequest(fromAccount.getAccountId(), "A2", new BigDecimal(500));
		try {
			this.accountsService.transferAmount(transferReq);
		} catch (TransferValidationException te) {
			assertThat(te.getMessage()).isEqualTo("No account found for given toAccountId.");
		}
	}
}
