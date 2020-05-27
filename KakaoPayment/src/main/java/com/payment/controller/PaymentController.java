package com.payment.controller;

import java.util.HashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.payment.entity.PaymentInfo;
import com.payment.entity.PaymentList;
import com.payment.service.PaymentService;
import com.payment.util.PaymentValidator;
import com.payment.util.Util;

@RestController
public class PaymentController {
	
	private static Logger logger = LoggerFactory.getLogger(PaymentController.class);
	
    @Autowired
    private ObjectMapper objectMapper;
    
    @Autowired
    private PaymentService paymentService;
    
    /**
     * 결제 API
     * @param payInfo
     * @return
     * @throws Exception
     */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	@PostMapping(value="/payment-info", consumes="application/json;charset=UTF-8", produces="application/json;charset=UTF-8")
	public ResponseEntity addPaymentInfo(@RequestBody PaymentInfo payInfo) throws Exception {
		// 리턴 맵
		HashMap resMap = new HashMap();
		
		// 입력값 검증
		resMap = PaymentValidator.validate(payInfo);
		
		if(!"OK".equals((String)resMap.get("resultValid"))) {
			logger.error("입력값 유효성 검증 실패!");
			return setResponse(resMap, HttpStatus.INTERNAL_SERVER_ERROR);
		}
		
		// 입력값 제약조건 체크
		payInfo = checkInput(payInfo);

		// 결과 리턴
		String mgntId = paymentService.paymentSave(payInfo);
		resMap.put("mgntId", mgntId);
		return setResponse(resMap, HttpStatus.CREATED);
	}

	/**
	 * 결제 취소 API
	 * @param payInfo
	 * @return
	 * @throws Exception
	 */
	@SuppressWarnings({ "rawtypes", "unchecked", "null" })
	@PostMapping(value="/cancel-payment", consumes="application/json;charset=UTF-8", produces="application/json;charset=UTF-8")
	public ResponseEntity cancelPayment(@RequestBody PaymentInfo cancelInfo) throws Exception {
		logger.debug("취소하고자 하는 결제 건의 관리번호 : " + cancelInfo.getOrgMgntId());
		// 리턴 맵
		HashMap resMap = new HashMap();
		
		// 입력값 검증
		resMap = PaymentValidator.validate(cancelInfo);
		if(resMap.get("errCd") != null) {
			logger.error("입력값 유효성 검증 실패!");
			return setResponse(resMap, HttpStatus.INTERNAL_SERVER_ERROR);
		}
		
		// 기존 결제건 조회
		PaymentList orgPaymentList = null;				// 기존 결재건 전문
		PaymentInfo orgPaymentInfo = null;			// 기존 결재건 객체화
		try {
			orgPaymentList = paymentService.getPaymentInfo(cancelInfo.getOrgMgntId());
			if(orgPaymentList == null) {
				resMap.put("errCd", "BVAL0007");
				resMap.put("errMsg", "취소하고자 하는 결제건이 없습니다.");
				return setResponse(resMap, HttpStatus.INTERNAL_SERVER_ERROR);
			}
			orgPaymentInfo = Util.parseMessage(orgPaymentList);
			logger.debug("기존 결제 정보 : " + orgPaymentInfo.toString());
		} catch (Exception e) {
			e.printStackTrace();
			resMap.put("errCd", "BVAL0008");
			resMap.put("errMsg", "취소하고자 하는 결제건을 조회중 에러가 발생했습니다.");
			return setResponse(resMap, HttpStatus.INTERNAL_SERVER_ERROR);
		}
		
		// 취소 요청 금액이 원 결제금액보다 크면 에러 발생
		int cancelMoney = Integer.parseInt(cancelInfo.getPaymentMoney());
		int paymentMoney = Integer.parseInt(orgPaymentInfo.getPaymentMoney());
		if(cancelMoney > paymentMoney) {
			resMap.put("errCd", "BVAL0012");
			resMap.put("errMsg", "취소 금액이 결제금액보다 큽니다.");
			return setResponse(resMap, HttpStatus.INTERNAL_SERVER_ERROR);
		}
		
		// 취소 정보 재설정
		cancelInfo = setCancelInfo(orgPaymentInfo, cancelInfo);
		
		// 취소
		String mgntId = "";
		try {
			mgntId = paymentService.paymentSave(cancelInfo);
		} catch (Exception e) {
			e.printStackTrace();
			resMap.put("errCd", "BVAL0009");
			resMap.put("errMsg", "취소 하는 중 에러가 발생했습니다.");
			return setResponse(resMap, HttpStatus.INTERNAL_SERVER_ERROR);
		}
		logger.debug("취소 되어 생성된 관리번호 : " + mgntId);
		
		// 결과 리턴
		resMap.put("mgntId", mgntId);
		return setResponse(resMap, HttpStatus.CREATED);
	}
	
	/**
	 * 데이터 조회 API
	 * @param mgntId
	 * @return
	 * @throws Exception
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	@GetMapping(value="/payment-info/{mgntId}", consumes="application/json;charset=UTF-8", produces="application/json;charset=UTF-8")
	public ResponseEntity<HashMap> getPaymentInfo(@PathVariable String mgntId) throws Exception {
		logger.debug("mgntId : " + mgntId);
		HashMap resMap = new HashMap();
		
		// 입력값 검증
		if(mgntId == null || mgntId == "") {
			logger.error("관리번호는 필수 입니다.");
			resMap.put("errCd", "BVAL0011");
			resMap.put("errMsg","관리번호는 필수 입니다.");
			return setResponse(resMap, HttpStatus.INTERNAL_SERVER_ERROR);
		}

		PaymentInfo returnObj = null;
		try {
			PaymentList resPaymentList = paymentService.getPaymentInfo(mgntId);
			if(resPaymentList == null) {
				resMap.put("errCd", "BVAL0013");
				resMap.put("errMsg", "조회하고자 하는 결제건이 없습니다.");
				return setResponse(resMap, HttpStatus.INTERNAL_SERVER_ERROR);
			}
			returnObj = Util.parseMessage(resPaymentList);
		} catch (Exception e) {
			e.printStackTrace();
			resMap.put("errCd", "BVAL0014");
			resMap.put("errMsg", "조회 거래 중 에러가 발생하였습니다.");
			return setResponse(resMap, HttpStatus.INTERNAL_SERVER_ERROR);
		}
		
		return setResponse(returnObj, HttpStatus.OK);
	}

	/**
	 * 공통 리턴 함수
	 * @param resObj
	 * @param status
	 * @return
	 * @throws JsonProcessingException
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	private ResponseEntity setResponse(Object resObj, HttpStatus status) throws JsonProcessingException {
		HttpHeaders responseHeaders = new HttpHeaders();
		responseHeaders.add("Content-Type", "application/json; charset=UTF-8");
		responseHeaders.add("Accept", "application/json; charset=UTF-8");
		return new ResponseEntity(objectMapper.writeValueAsString(resObj), responseHeaders, status);
	}
	
	/**
	 * 취소 정보 재설정
	 * @param orgPaymentInfo
	 * @param cancelInfo
	 * @return
	 */
	private PaymentInfo setCancelInfo(PaymentInfo orgPaymentInfo, PaymentInfo cancelInfo) {
		// 할부개월수 재설정
		cancelInfo.setInsMonth("00");
		
		// 부가세 재설정
		if(cancelInfo.getAddValueTax() == null || "".equals(cancelInfo.getAddValueTax())){
			cancelInfo.setAddValueTax(orgPaymentInfo.getAddValueTax());
		}
		
		// 카드정보 설정
		cancelInfo.setCardNum(Util.checkCardNum(orgPaymentInfo));
		cancelInfo.setValidTerm(orgPaymentInfo.getValidTerm());
		cancelInfo.setCvc(orgPaymentInfo.getCvc());
		
		return cancelInfo;
	}
	
	/**
	 * 결제 요청시 입력값 제약조건 체크
	 * @param payInfo
	 * @return
	 */
	private PaymentInfo checkInput(PaymentInfo payInfo) {
		
		// 부가가치세 없는 경우, 결제 신청 금액으로부터 계산
		payInfo.setAddValueTax(Util.checkAddValue(payInfo));
		
		// 카드번호가 16자리 이하일때 _ 패딩 처리 
		payInfo.setCardNum(Util.checkCardNum(payInfo));
		
		// 할부개월수가 1자리수일때, 0X로 변환
		payInfo.setInsMonth(Util.checkInsMonth(payInfo));
		
		return payInfo;
	}
}
