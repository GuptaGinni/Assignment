package com.dws.challenge;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

import java.math.BigDecimal;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.context.WebApplicationContext;

import com.dws.challenge.domain.Account;
import com.dws.challenge.domain.TransferRequest;
import com.dws.challenge.service.AccountsService;
import com.fasterxml.jackson.databind.ObjectMapper;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@WebAppConfiguration
class AccountsControllerTest {

	private MockMvc mockMvc;
	private ObjectMapper objectMapper;

	@Autowired
	private AccountsService accountsService;

	@Autowired
	private WebApplicationContext webApplicationContext;

	@BeforeEach
	void prepareMockMvc() {
		this.mockMvc = webAppContextSetup(this.webApplicationContext).build();

		// Reset the existing accounts before each test.
		accountsService.getAccountsRepository().clearAccounts();
		objectMapper = new ObjectMapper();
	}

	@Test
	void createAccount() throws Exception {
		this.mockMvc.perform(post("/v1/accounts").contentType(MediaType.APPLICATION_JSON)
				.content("{\"accountId\":\"Id-123\",\"balance\":1000}")).andExpect(status().isCreated());

		Account account = accountsService.getAccount("Id-123");
		assertThat(account.getAccountId()).isEqualTo("Id-123");
		assertThat(account.getBalance()).isEqualByComparingTo("1000");
	}

	@Test
	void createDuplicateAccount() throws Exception {
		this.mockMvc.perform(post("/v1/accounts").contentType(MediaType.APPLICATION_JSON)
				.content("{\"accountId\":\"Id-123\",\"balance\":1000}")).andExpect(status().isCreated());

		this.mockMvc.perform(post("/v1/accounts").contentType(MediaType.APPLICATION_JSON)
				.content("{\"accountId\":\"Id-123\",\"balance\":1000}")).andExpect(status().isBadRequest());
	}

	@Test
	void createAccountNoAccountId() throws Exception {
		this.mockMvc.perform(post("/v1/accounts").contentType(MediaType.APPLICATION_JSON).content("{\"balance\":1000}"))
				.andExpect(status().isBadRequest());
	}

	@Test
	void createAccountNoBalance() throws Exception {
		this.mockMvc.perform(
				post("/v1/accounts").contentType(MediaType.APPLICATION_JSON).content("{\"accountId\":\"Id-123\"}"))
				.andExpect(status().isBadRequest());
	}

	@Test
	void createAccountNoBody() throws Exception {
		this.mockMvc.perform(post("/v1/accounts").contentType(MediaType.APPLICATION_JSON))
				.andExpect(status().isBadRequest());
	}

	@Test
	void createAccountNegativeBalance() throws Exception {
		this.mockMvc.perform(post("/v1/accounts").contentType(MediaType.APPLICATION_JSON)
				.content("{\"accountId\":\"Id-123\",\"balance\":-1000}")).andExpect(status().isBadRequest());
	}

	@Test
	void createAccountEmptyAccountId() throws Exception {
		this.mockMvc.perform(post("/v1/accounts").contentType(MediaType.APPLICATION_JSON)
				.content("{\"accountId\":\"\",\"balance\":1000}")).andExpect(status().isBadRequest());
	}

	@Test
	void getAccount() throws Exception {
		String uniqueAccountId = "Id-" + System.currentTimeMillis();
		Account account = new Account(uniqueAccountId, new BigDecimal("123.45"));
		this.accountsService.createAccount(account);
		this.mockMvc.perform(get("/v1/accounts/" + uniqueAccountId)).andExpect(status().isOk())
				.andExpect(content().string("{\"accountId\":\"" + uniqueAccountId + "\",\"balance\":123.45}"));
	}

	@Test
	void testTransferRequest_Success() throws Exception {
		Account fromAccount = new Account("Id-123", new BigDecimal("799.01"));
		this.accountsService.createAccount(fromAccount);
		Account toAccount = new Account("Id-456", new BigDecimal("100"));
		this.accountsService.createAccount(toAccount);
		TransferRequest transferReq = new TransferRequest(fromAccount.getAccountId(), toAccount.getAccountId(),
				new BigDecimal(500));
		this.mockMvc
				.perform(post("/v1/accounts/transferAmount").contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(transferReq)))
				.andExpect(status().isOk()).andExpect(content().string("Transfer Successful."));
	}

	@Test
	void testTransferRequest_InsufficientFunds() throws Exception {
		Account fromAccount = new Account("Id-123", new BigDecimal("99.01"));
		this.accountsService.createAccount(fromAccount);
		Account toAccount = new Account("Id-456", new BigDecimal("100"));
		this.accountsService.createAccount(toAccount);
		TransferRequest transferReq = new TransferRequest(fromAccount.getAccountId(), toAccount.getAccountId(),
				new BigDecimal(500));
		this.mockMvc
				.perform(post("/v1/accounts/transferAmount").contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(transferReq)))
				.andExpect(status().isBadRequest()).andExpect(content().string("Given Account id: "
						+ fromAccount.getAccountId() + "does not have sufficient funds to initiate transfer."));
	}

	@Test
	void testTransferRequest_SameAccountIds() throws Exception {
		Account fromAccount = new Account("Id-123", new BigDecimal("999.01"));
		this.accountsService.createAccount(fromAccount);
		TransferRequest transferReq = new TransferRequest(fromAccount.getAccountId(), fromAccount.getAccountId(),
				new BigDecimal(500));
		this.mockMvc
				.perform(post("/v1/accounts/transferAmount").contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(transferReq)))
				.andExpect(status().isBadRequest())
				.andExpect(content().string("fromAccountId and toAccountId cannot be same."));
	}

	@Test
	void testTransferRequest_InvalidFromAccountId() throws Exception {
		Account fromAccount = new Account("Id-123", new BigDecimal("999.01"));
		this.accountsService.createAccount(fromAccount);
		Account toAccount = new Account("Id-456", new BigDecimal("100"));
		this.accountsService.createAccount(toAccount);
		TransferRequest transferReq = new TransferRequest("A1", toAccount.getAccountId(), new BigDecimal(500));
		this.mockMvc
				.perform(post("/v1/accounts/transferAmount").contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(transferReq)))
				.andExpect(status().isBadRequest())
				.andExpect(content().string("No account found for given fromAccountId."));
	}

	@Test
	void testTransferRequest_InvalidToAccountId() throws Exception {
		Account fromAccount = new Account("Id-123", new BigDecimal("999.01"));
		this.accountsService.createAccount(fromAccount);
		Account toAccount = new Account("Id-456", new BigDecimal("100"));
		this.accountsService.createAccount(toAccount);
		TransferRequest transferReq = new TransferRequest(fromAccount.getAccountId(), "A2", new BigDecimal(500));
		this.mockMvc
				.perform(post("/v1/accounts/transferAmount").contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(transferReq)))
				.andExpect(status().isBadRequest())
				.andExpect(content().string("No account found for given toAccountId."));
	}
}
