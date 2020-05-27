package com.payment;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import com.payment.entity.PaymentInfo;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class KakaoPaymentApplicationTests {

	private static Logger logger = LoggerFactory.getLogger(KakaoPaymentApplicationTests.class);
	
	@Autowired
    private TestRestTemplate restTemplate;
	
    /**
     * 결제 API 테스트
     * <<< 테스트 시나리오 >>>
     * 1. 결제 신청 정보 유효성 검증한다.
     * 2. 카드정보를 제외한 결제 신청 정보를 DB에 Insert한다.
     * 3. 암호화한 결제 전문 카드사에 전송한다. (DB Insert)
     * @throws Exception
     */
	@SuppressWarnings("rawtypes")
	@Test
	void creatPayment() throws Exception {
		PaymentInfo paymentInfo = PaymentInfo.builder()
				.cardNum("12345678901")
				.validTerm("1222")
				.cvc("313")
				.insMonth("12")
				.paymentMoney("1000")
				.paymentStatus("PAYMENT")
				.addValueTax("")
				.orgMgntId("")
				.build();
		
		HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<PaymentInfo> requestEntity = new HttpEntity<PaymentInfo>(paymentInfo, headers);
        
        ResponseEntity<HashMap> responseEntity = restTemplate.postForEntity("/payment-info", requestEntity, HashMap.class);
        
        logger.debug("mgntId : " + responseEntity.getBody().get("mgntId"));
        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        
	}
	
	/**
	 * 데이터 조회 API 테스트
	 * <<< 테스트 시나리오 >>>
     * 1. 조회 전 결제 요청건을 등록한다.
     * 2. 1번의 결제 요청건을 DB 조회한다.
     * 3. 조회한 데이터에서 암호화된 카드정보를 복호화한다.
     * 4. 복호환 카드정보 중 카드번호는 마스킹 처리하여 최종적으로 리턴한다.
	 * @throws Exception
	 */
	@SuppressWarnings("rawtypes")
	@Test
	void selectPaymentInfo() throws Exception {
		// 조회 전 등록 처리
		PaymentInfo paymentInfo = PaymentInfo.builder()
				.cardNum("12345678901")
				.validTerm("1222")
				.cvc("313")
				.insMonth("12")
				.paymentMoney("1000")
				.paymentStatus("PAYMENT")
				.addValueTax("")
				.orgMgntId("")
				.build();
		
		HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<PaymentInfo> payReqEntity = new HttpEntity<PaymentInfo>(paymentInfo, headers);
        
        ResponseEntity<HashMap> responseEntity = restTemplate.postForEntity("/payment-info", payReqEntity, HashMap.class);
        String payMgntId = (String) responseEntity.getBody().get("mgntId");
        logger.debug("payMgntId : " + payMgntId);
        
        // 조회
        HttpEntity<String> selectReqEntity = new HttpEntity<String>(payMgntId, headers);
        ResponseEntity<PaymentInfo> response = restTemplate.exchange(
        		"/payment-info/{mgntId}"
        		, HttpMethod.GET
        		, selectReqEntity
        		, PaymentInfo.class
        		, payMgntId);
        
        logger.debug("response cardNum : " + response.getBody().getCardNum());
        logger.debug("response validTerm : " + response.getBody().getValidTerm());
        logger.debug("response cvc : " + response.getBody().getCvc());
        logger.debug("response paymentStatus : " + response.getBody().getPaymentStatus());
        logger.debug("response paymentMoney : " + response.getBody().getPaymentMoney());
        logger.debug("response addValueTax : " + response.getBody().getAddValueTax());
        
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        
	}
	
	/**
	 * 결제 취소 API 테스트(전체 취소 버전)
	 * <<< 테스트 시나리오 >>>
     * 1. 취소 전 결제 요청건을 등록한다.
     * 2. 1번의 결제 요청건의 관리번호로 기존 취소건이 있는지 DB 조회한다.
     * 3. 기존 취소건이 있고 취소 금액이 결제 요청건의 금액과 동일 -> 에러처리(전체 취소는 1번만 가능)
     * 4. 취소 건이 없으면, 취소 요청건을 DB Insert 하고 취소 요청건의 관리번호를 리턴한다.
	 * @throws Exception
	 */
	@SuppressWarnings("rawtypes")
	@Test
	void cancelPayment() throws Exception {
		// 취소 전 등록 처리
		PaymentInfo paymentInfo = PaymentInfo.builder()
				.cardNum("12345678901")
				.validTerm("1222")
				.cvc("313")
				.insMonth("12")
				.paymentMoney("1000")
				.paymentStatus("PAYMENT")
				.addValueTax("")
				.orgMgntId("")
				.build();
		
		HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<PaymentInfo> payReqEntity = new HttpEntity<PaymentInfo>(paymentInfo, headers);
        
        ResponseEntity<HashMap> responseEntity = restTemplate.postForEntity("/payment-info", payReqEntity, HashMap.class);
        if(!responseEntity.getStatusCode().is2xxSuccessful()) {
        	logger.error("에러 코드 : " + responseEntity.getBody().get("Error Code"));
        	logger.error("에러 메세지 : " + responseEntity.getBody().get("Error Message"));
        }
        String payMgntId = (String) responseEntity.getBody().get("mgntId");
        logger.debug("결제 건의 관리번호 : " + payMgntId);
        
        // 취소건
        PaymentInfo cancelPaymentInfo = PaymentInfo.builder()
				.paymentMoney("1000")
				.paymentStatus("CANCEL")
				.addValueTax("")
				.orgMgntId(payMgntId)
				.build();
        
        HttpEntity<PaymentInfo> cancelReqEntity = new HttpEntity<PaymentInfo>(cancelPaymentInfo, headers);
        ResponseEntity<HashMap> cancelResEntity = restTemplate.postForEntity("/cancel-payment", cancelReqEntity, HashMap.class);
        if(!cancelResEntity.getStatusCode().is2xxSuccessful()) {
        	logger.error("에러 코드 : " + cancelResEntity.getBody().get("Error Code"));
        	logger.error("에러 메세지 : " + cancelResEntity.getBody().get("Error Message"));
        }
        logger.debug("생성된 취소 결제건의 관리번호 : " + (String) cancelResEntity.getBody().get("mgntId"));
        
        assertThat(cancelResEntity.getStatusCode()).isEqualTo(HttpStatus.CREATED);
	}
	
	// 부가가치세가 없는 경우, 원 결제건의 부가세로 취소

}
