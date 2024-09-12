package com.dws.challenge.domain;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class TransferRequest implements Serializable {

	private static final long serialVersionUID = 6157421243178030064L;

	@NotNull
	@NotEmpty(message = "From Account Id should not be empty.")
	private final String fromAccountId;

	@NotNull
	@NotEmpty(message = "To Account Id should not be empty.")
	private String toAccountId;

	@NotNull
	@Min(value = 0, message = "Transfer Amount should be greater than 0.")
	private BigDecimal amount;

	@JsonCreator
	public TransferRequest(@JsonProperty("fromAccountId") String fromAccountId,
			@JsonProperty("toAccountId") String toAccountId, @JsonProperty("amount") BigDecimal amount) {
		this.fromAccountId = fromAccountId;
		this.toAccountId = toAccountId;
		this.amount = amount;
	}

	@Override
	public int hashCode() {
		return Objects.hash(amount, fromAccountId, toAccountId);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		TransferRequest other = (TransferRequest) obj;
		return Objects.equals(amount, other.amount) && Objects.equals(fromAccountId, other.fromAccountId)
				&& Objects.equals(toAccountId, other.toAccountId);
	}

}