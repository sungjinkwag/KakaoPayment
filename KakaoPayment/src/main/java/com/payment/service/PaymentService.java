package com.payment.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.payment.entity.PaymentInfo;
import com.payment.entity.PaymentList;
import com.payment.repository.PaymentInfoRepository;
import com.payment.repository.PaymentListRepository;
import com.payment.util.Util;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PaymentService {
	
	private static Logger logger = LoggerFactory.getLogger(PaymentService.class);
	
	private final PaymentListRepository paymentListRepository;
	private final PaymentInfoRepository paymentInfoRepository;
	
	/**
	 * 결재 정보를 저장합니다.
	 * @param paramInfo
	 * @return
	 * @throws Exception
	 */
    public String paymentSave(PaymentInfo paramInfo) throws Exception {
    	// 결제 정보 저장
    	PaymentInfo saveInfo = PaymentInfo.builder()
    			.mgntId(Util.makeUniqueString())
    			.insMonth(paramInfo.getInsMonth())
    			.paymentMoney(paramInfo.getPaymentMoney())
    			.paymentStatus(paramInfo.getPaymentStatus())
    			.addValueTax(paramInfo.getAddValueTax())
    			.orgMgntId(paramInfo.getOrgMgntId())
    			.build();
    	
    	PaymentInfo savedInfo = paymentInfoRepository.save(saveInfo);
    	if(Util.CANCEL.equals(paramInfo.getPaymentStatus())) {
    		logger.debug("저장된 취소 정보 : " + savedInfo.toString());
    	} else {
    		logger.debug("저장된 결재 정보 : " + savedInfo.toString());
    	}
    	
    	// 카드사에 결제 요청 송신 (DB저장)
    	String message = Util.makeSendMessage(saveInfo.getMgntId(), paramInfo);
    	if(Util.CANCEL.equals(paramInfo.getPaymentStatus())) {
    		logger.debug("카드사에 취소 요청 송신 전문 : " + message);
    	} else {
    		logger.debug("카드사에 결제 요청 송신 전문 : " + message);
    	}
    	
    	PaymentList paymentList = PaymentList.builder()
    			.mgntId(saveInfo.getMgntId())
    			.message(message)
    			.build();
    	
    	paymentListRepository.save(paymentList);
    	logger.debug("카드사에 송신 요청 성공!");
    	
        return saveInfo.getMgntId();
    }

    /**
     * 결제 정보를 조회합니다.
     * @param mgntId
     * @return
     * @throws Exception 
     */
	public PaymentList getPaymentInfo(String mgntId) throws Exception {
		return paymentListRepository.findByMgntId(mgntId);
	}

}
