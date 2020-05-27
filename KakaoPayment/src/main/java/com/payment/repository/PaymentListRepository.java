package com.payment.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.payment.entity.PaymentList;

@Repository
public interface PaymentListRepository extends JpaRepository<PaymentList, Integer> {

	PaymentList findByMgntId(String mgntId);

}
