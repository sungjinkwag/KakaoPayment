package com.payment.util;

import java.util.HashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.payment.entity.PaymentInfo;

public class PaymentValidator {
	
	private static Logger logger = LoggerFactory.getLogger(PaymentValidator.class);
	
	/**
	 * 결제 신청 요청 입력값 검증
	 * @param paymentInfo
	 * @return
	 */
    @SuppressWarnings({ "rawtypes", "unchecked" })
	public static HashMap validate(PaymentInfo paymentInfo){
    	HashMap resultMap = new HashMap();
    	
    	// Null check
    	// 결제 요청인 경우
    	if(Util.APPROVAL.equals(paymentInfo.getPaymentStatus())) {
    		
    		// 카드번호 검증
    		if(!Util.validateItem(paymentInfo.getCardNum(), Util.REG_01)) {
    			logger.error("카드번호 유효성 검증에 실패하였습니다.");
    			resultMap.put("errCd", "BVAL0001");
    			resultMap.put("errMsg", "카드번호는 10~16자리 숫자입니다.");
    			return resultMap;
    		}
    		
    		// 유효기간 검증
    		if(!Util.validateItem(paymentInfo.getValidTerm(), Util.REG_02)) {
    			logger.error("유효기간 유효성 검증에 실패하였습니다.");
    			resultMap.put("errCd", "BVAL0002");
    			resultMap.put("errMsg", "유효기간은 mmyy 4자리 숫자입니다.");
    			return resultMap;
    		}
    		
    		// CVC 검증
    		if(!Util.validateItem(paymentInfo.getCvc(), Util.REG_03)) {
    			resultMap.put("errCd", "BVAL0003");
    			resultMap.put("errMsg", "CVC는 3자리 숫자입니다.");
    			return resultMap;
    		}
    		
    		// 할부개월수 검증
    		if(!Util.validateItem(paymentInfo.getInsMonth(), Util.REG_04)) {
    			logger.error("할부개월수 유효성 검증에 실패하였습니다.");
    			resultMap.put("errCd", "BVAL0004");
    			resultMap.put("errMsg", "할부개월수는 12이하 숫자입니다.");
    			return resultMap;
    		}
    	}
    	
    	// 결제금액 검증
    	if(!Util.validateItem(paymentInfo.getPaymentMoney(), Util.REG_05)) {
    		logger.error("결제금액 유효성 검증에 실패하였습니다.");
    		resultMap.put("errCd", "BVAL0005");
    		resultMap.put("errMsg", "결제금액은 100원이상 10억이하입니다.");
    		return resultMap;
    	}

    	// 최소/결제 상태 검증
    	if(!Util.CANCEL.equals(paymentInfo.getPaymentStatus()) &&
    			!Util.APPROVAL.equals(paymentInfo.getPaymentStatus())) {
    		logger.error("결제/취소 상태 유효성 검증에 실패하였습니다.");
    		resultMap.put("errCd", "BVAL0006");
    		resultMap.put("errMsg", "잘못된 요청입니다.");
    		return resultMap;
    	}
    	
    	// Null Pointer Exception 방어용
    	resultMap.put("resultValid", "OK");
    	logger.info("유효성 검증을 통과하였습니다.");
    	return resultMap;
   }

}
