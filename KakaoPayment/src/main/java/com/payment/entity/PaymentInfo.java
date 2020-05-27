package com.payment.entity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Entity
@Getter @Setter @AllArgsConstructor @ToString
@Builder @NoArgsConstructor @EqualsAndHashCode(of="mgntId")
public class PaymentInfo {
	
	@Id
	@GeneratedValue
	private Integer Id;
	
	@Column(nullable = false, unique = true)
	private String mgntId;
	
	private String cardNum;
	
	private String validTerm;
	
	private String cvc;
	
	@Column(nullable = false)
	private String insMonth;
	
	@Column(nullable = false)
	private String paymentMoney;
	
	@Column(nullable = false)
	private String paymentStatus;
	
	@Column(nullable = true)
	private String addValueTax;
	
	@Column(nullable = true)
	private String orgMgntId;
	
}
