package com.dws.challenge.web;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.dws.challenge.domain.Account;
import com.dws.challenge.domain.TransferRequest;
import com.dws.challenge.exception.DuplicateAccountIdException;
import com.dws.challenge.exception.TransferValidationException;
import com.dws.challenge.service.AccountsService;

import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/v1/accounts")
@Slf4j
public class AccountsController {

	private final AccountsService accountsService;

	@Autowired
	public AccountsController(AccountsService accountsService) {
		this.accountsService = accountsService;
	}

	@PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Object> createAccount(@RequestBody @Valid Account account) {
		log.info("Creating account {}", account);

		try {
			this.accountsService.createAccount(account);
		} catch (DuplicateAccountIdException daie) {
			return new ResponseEntity<>(daie.getMessage(), HttpStatus.BAD_REQUEST);
		}

		return new ResponseEntity<>(HttpStatus.CREATED);
	}

	@GetMapping(path = "/{accountId}")
	public Account getAccount(@PathVariable("accountId") String accountId) {
		log.info("Retrieving account for id {}", accountId);
		return this.accountsService.getAccount(accountId);
	}

	@PostMapping(path = "/transferAmount")
	public ResponseEntity<Object> transferAmount(@Valid @RequestBody TransferRequest transferRequest) {
		log.info("Received request for Amount transfer.");
		log.debug("Transfer Request: {}", transferRequest);
		try {
			accountsService.transferAmount(transferRequest);
		} catch (TransferValidationException te) {
			log.error("Transfer failed due to validation failure: ",te.getMessage());
			return new ResponseEntity<>(te.getMessage(), HttpStatus.BAD_REQUEST);
		} catch (Exception e) {
			log.error("Transfer failed due to exception: ",e);
			return new ResponseEntity<>("Transfer Failed.",HttpStatus.INTERNAL_SERVER_ERROR);
		}
		return ResponseEntity.ok("Transfer Successful.");
	}

}
