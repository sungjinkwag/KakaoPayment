package com.payment.entity;

import javax.persistence.Column;
import javax.persistence.Entity;
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
public class PaymentList {
	@Id
	private String mgntId;				// 관리번호
	@Column(length=500)
	private String message;			// 전체 메세지
	
}
