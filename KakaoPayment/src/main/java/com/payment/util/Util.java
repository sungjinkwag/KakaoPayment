package com.payment.util;

import java.nio.ByteBuffer;
import java.security.AlgorithmParameters;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

import com.payment.entity.PaymentInfo;
import com.payment.entity.PaymentList;

public class Util {
	
	private static Logger logger = LoggerFactory.getLogger(Util.class);
	
	final static String SEPERATE = ",";										// 카드정보 합칠때 쓰는 구분자
	final static String MY_KEY = "KingOfDavid";							// 카드정보 암호화에 필요한 개인키
	final static String ENC_SOURCE_PREFIX = "yehuruaa";		// 카드정보 합칠때 쓰는 접두어
	public final static String APPROVAL = "PAYMENT";							// 결재
	public final static String CANCEL = "CANCEL";								// 취소
	final static String RIGHT = "R";											// 패딩 구분값 
	final static String LEFT = "L";												// 패딩 구분값
	final static int MSG_SIZE = 450;											// 전문 최대 길이
	
	final static String REG_01 = "^[0-9]{10,16}$";	// 카드번호 검증 정규표현식
	final static String REG_02 = "^[0-9]{4}$";			// 유효기간 검증 정규표현식
	final static String REG_03 = "^[0-9]{3}$";			// CVC 검증 정규표현식
	final static String REG_04 = "^[0-9]{2}$";			// 할부개월수 검증 정규표현식
	final static String REG_05 = "^[0-9]{3,10}$";		// 결제금액 검증 정규표현식

	/**
	 * 카드정보를 암호화하기전 평문 만들기
	 * @param paymentInfoVO
	 * @return 
	 */
	public static String makeSource(PaymentInfo paymentInfoVO) {
		String source = ENC_SOURCE_PREFIX+paymentInfoVO.getCardNum()+SEPERATE
				+paymentInfoVO.getValidTerm()+SEPERATE
				+paymentInfoVO.getCvc();
		logger.debug("source : " + source);
		return source;
	}
	
	/**
	 * 유니크한 관리번호 만들기
	 * @return
	 */
	public static String makeUniqueString() {
		String id = UUID.randomUUID().toString().replace("-", "").substring(0, 20);
		logger.debug("id : " + id);
		return id;
	}
	
	/**
	 * AES256 암호화
	 * @param msg
	 * @return
	 * @throws Exception
	 */
	public static String encryptAES256(String msg) throws Exception {
	    SecureRandom random = new SecureRandom();
	    byte bytes[] = new byte[20];
	    random.nextBytes(bytes);
	    byte[] saltBytes = bytes;

	    SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
	    PBEKeySpec spec = new PBEKeySpec(MY_KEY.toCharArray(), saltBytes, 70000, 128);

	    SecretKey secretKey = factory.generateSecret(spec);
	    SecretKeySpec secret = new SecretKeySpec(secretKey.getEncoded(), "AES");

	    Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
	    cipher.init(Cipher.ENCRYPT_MODE, secret);
	    AlgorithmParameters params = cipher.getParameters();

	    byte[] ivBytes = params.getParameterSpec(IvParameterSpec.class).getIV();
	    byte[] encryptedTextBytes = cipher.doFinal(msg.getBytes("UTF-8"));
	    byte[] buffer = new byte[saltBytes.length + ivBytes.length + encryptedTextBytes.length];

	    System.arraycopy(saltBytes, 0, buffer, 0, saltBytes.length);
	    System.arraycopy(ivBytes, 0, buffer, saltBytes.length, ivBytes.length);
	    System.arraycopy(encryptedTextBytes, 0, buffer, saltBytes.length + ivBytes.length, encryptedTextBytes.length);

	    return Base64.getEncoder().encodeToString(buffer);
	}
	
	/**
	 * AES256 복호화
	 * @param msg
	 * @return
	 * @throws Exception
	 */
	public static String decryptAES256(String msg) throws Exception {

	    Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
	    ByteBuffer buffer = ByteBuffer.wrap(Base64.getDecoder().decode(msg));

	    byte[] saltBytes = new byte[20];
	    buffer.get(saltBytes, 0, saltBytes.length);
	    byte[] ivBytes = new byte[cipher.getBlockSize()];
	    buffer.get(ivBytes, 0, ivBytes.length);
	    byte[] encryoptedTextBytes = new byte[buffer.capacity() - saltBytes.length - ivBytes.length];
	    buffer.get(encryoptedTextBytes);

	    SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
	    PBEKeySpec spec = new PBEKeySpec(MY_KEY.toCharArray(), saltBytes, 70000, 128);

	    SecretKey secretKey = factory.generateSecret(spec);
	    SecretKeySpec secret = new SecretKeySpec(secretKey.getEncoded(), "AES");

	    cipher.init(Cipher.DECRYPT_MODE, secret, new IvParameterSpec(ivBytes));

	    byte[] decryptedTextBytes = cipher.doFinal(encryoptedTextBytes);
	    return new String(decryptedTextBytes);

	}

	/**
	 * 카드사에 전송할 전문 만들기
	 * @param mgntNo
	 * @param paymentInfo
	 * @return
	 * @throws Exception
	 */
	public static String makeSendMessage(String mgntNo, PaymentInfo paymentInfo) throws Exception {
		String encSource = encryptAES256(makeSource(paymentInfo));
		logger.debug("encSource size : " + encSource.length());
		String sendEncSource = encSource.substring(0,100);
		String restEncSource = encSource.substring(100);

		// 카드사에 보낼 메세지
		StringBuffer message = new StringBuffer();
		// 데이터길이를 뺀 공통헤더부문 세팅
		message.append(makePaddingMsg(paymentInfo.getPaymentStatus(),10,"R","_"))		// (문자 10) 기능 구분값
				.append(makePaddingMsg(mgntNo,20,"R","_"))												// (문자 20) 관리번호
				//데이터부문
				.append(makePaddingMsg(paymentInfo.getCardNum(),20,"R","_"))					// (숫자L 20) 카드번호
				.append(makePaddingMsg(paymentInfo.getInsMonth(),2,"L","0"))						// (숫자0 2) 할부개월수
				.append(makePaddingMsg(paymentInfo.getValidTerm(),4,"R","_"))					// (숫자L 4) 카드유효기간
				.append(makePaddingMsg(paymentInfo.getCvc(),3,"R","_"))								// (숫자L 3) cvc
				.append(makePaddingMsg(paymentInfo.getPaymentMoney(),10,"L","_"))			// (숫자 10) 거래금액
				.append(makePaddingMsg(paymentInfo.getAddValueTax(),10,"L","0"))				// (숫자0 10) 부가가치세
				.append(makePaddingMsg(makeOrgMgntNo(paymentInfo),20,"R","_"))				// (문자 20) 원거래 관리번호 
				.append(makePaddingMsg(sendEncSource,300,"R","_"))									// (문자 300) 암호화된 카드정보 
				.append(makePaddingMsg(restEncSource,47,"R","_"))										// (문자 47) 예비필드 
				;
		
		if(message.length()+4 == MSG_SIZE) {
			String dataLength = makePaddingMsg(Integer.toString(message.length()),4,"L","_");
//			logger.debug("카드사에 보낼 전문 : " + dataLength.concat(message.toString()));
			return dataLength.concat(message.toString());
		} else {
			throw new Exception();
		}
	}

	/**
	 * 카드사에 보낼 전문 만들때 항목 패딩화 하기
	 * @param str
	 * @param size
	 * @param LorR
	 * @param type
	 * @return
	 */
	public static String makePaddingMsg(String str, int size, String LorR, String type) {
		int underSize = size - str.length();
		StringBuffer sb = new StringBuffer();
		for (int i = 0; i < underSize; i++) {
			sb.append(type);
		}
		if(LorR.equals(RIGHT)) {
			return str.concat(sb.toString());
		} else if(LorR.equals(LEFT)) {
			return (sb.toString()).concat(str);
		}
		return null;
	}
	
	/**
	 * 취소 결재인경우 결재 관리번호 세팅하기
	 * @param paymentInfoVO
	 * @return
	 */
	public static String makeOrgMgntNo(PaymentInfo paymentInfoVO) {
		if(CANCEL.equals(paymentInfoVO.getPaymentStatus())) {
			return  paymentInfoVO.getOrgMgntId();
		}
		return "";
	}
	
	/**
	 * 부가가치세 제약조건 검증
	 * @param paymentInfoVO
	 * @return
	 */
	public static String checkAddValue(PaymentInfo paymentInfoVO) {
		float fltPayMoney = Float.parseFloat(paymentInfoVO.getPaymentMoney());
		float fltAddValue;
		if(StringUtils.isEmpty(paymentInfoVO.getAddValueTax())) {
			fltAddValue = 0;
		} else {
			fltAddValue = Float.parseFloat(paymentInfoVO.getAddValueTax());
		}
		
		int intAddValue = 0;
		
		logger.debug("fltPayMoney : " + fltPayMoney);
		logger.debug("fltAddValue01 : " + fltAddValue);
		logger.debug("intAddValue01 : " + intAddValue);
		
		if(fltAddValue == 0) {
			fltAddValue = fltPayMoney / 11;
			intAddValue = Math.round(fltAddValue);
			logger.debug("intAddValue02 : " + intAddValue);
		}
		
		if(intAddValue < Integer.parseInt(paymentInfoVO.getPaymentMoney())) {
			logger.debug("intAddValue03 : " + intAddValue);
			return Integer.toString(intAddValue);
		}
		
		return null;
	}

	/**
	 * 카드번호가 16자리 이하인경우 16자리로 맞추기
	 * @param payInfo
	 * @return
	 */
	public static String checkCardNum(PaymentInfo payInfo) {
		String cardNum = payInfo.getCardNum();
		if(cardNum.length() < 16) {
			return makePaddingMsg(cardNum,16,"L","_");
		}
		return null;
	}
	
	/**
	 * 결재 신청시 입력값 검증
	 * @param source
	 * @param regExp
	 * @return
	 */
	public static boolean validateItem(String source, String regExp) {
		// Null Check
		if(source == null || "".equals(source)) {
			return false;
		}
		
		// 유효성 Check
		Pattern p = Pattern.compile(regExp);
		Matcher m = p.matcher(source);
		
		if((regExp.equals(REG_02) || regExp.equals(REG_04)) && m.find()) {
			if(Integer.parseInt(source.substring(0,2)) > 12) {
				return false;
			}
			return true;
		}
		
		if(regExp.equals(REG_05) && m.find()) {
			if(Integer.parseInt(source) > 1000000000) {
				return false;
			}
			return true;
		}
		
		return m.find();
	}

	/**
	 * 결재 정보 조회시 전문 파싱하기
	 * @param paymentList
	 * @return
	 * @throws Exception 
	 */
	public static PaymentInfo parseMessage(PaymentList paymentList) throws Exception {
		String message = paymentList.getMessage();
		// 관리번호
		String mgntId = message.substring(14,34);
		
		// 암호화된 카드정보 조립
		String encCardInfo01 = message.substring(103,403);		// 카드정보 암호문 1
		encCardInfo01 = encCardInfo01.replace("_", "");
		String encCardInfo02 = message.substring(403,450);		// 카드정보 암호문 2
		encCardInfo02 = encCardInfo02.replace("_", "");
		String encCardInfo = encCardInfo01.concat(encCardInfo02);		// 카드정보 암호문 전체
		
		// 복호화
		String decCardInfo = decryptAES256(encCardInfo);
		decCardInfo = decCardInfo.replace(ENC_SOURCE_PREFIX, "").replace("_", "");
		
		String[] cardInfo = decCardInfo.split(SEPERATE);

		// 결제/취소 상태
		String status = message.substring(4,14);
		status = status.replace("_", "");
		
		// 결제 요청인 경우, 카드번호 마스킹
		String maskCardNum = "";
		if(Util.APPROVAL.equals(status)) {
			StringBuffer sb = new StringBuffer();
			String cardNum = cardInfo[0];
			sb.append(cardNum.substring(0,6));
			for (int i = 0; i < cardNum.length()-9; i++) {
				sb.append("*");
			}
			sb.append(cardNum.substring(cardNum.length()-3,cardNum.length()));
			maskCardNum = sb.toString();
		} else {
			maskCardNum = cardInfo[0]; 
		}
		
		// 유효기간
		String validTerm = cardInfo[1];
		
		// cvc
		String cvc  = cardInfo[2];
		
		// 결제/취소 금액
		String money = message.substring(63,73);
		money = money.replace("_", "");
		
		// 부가가치세
		String strAddVal = message.substring(73,83);
		strAddVal = Integer.toString(Integer.parseInt(strAddVal));
		
		// 결과값 세팅
		PaymentInfo returnObj = PaymentInfo.builder()
				.mgntId(mgntId)
				.cardNum(maskCardNum)
				.validTerm(validTerm)
				.cvc(cvc)
				.paymentMoney(money)
				.paymentStatus(status)
				.addValueTax(strAddVal)
				.build();
		// 취소시 결제 관리번호
		if(CANCEL.equals(status)) {
			returnObj.setOrgMgntId(message.substring(83,103));
		}
		
		return returnObj;
	}

	/**
	 * 할부개월수가 1자리수일때, 0X로 변환
	 * @param payInfo
	 * @return
	 */
	public static String checkInsMonth(PaymentInfo payInfo) {
		String strInsMonth = payInfo.getInsMonth();
		if(strInsMonth.length() == 1) {
			strInsMonth = "0"+strInsMonth;
		}
		return strInsMonth;
	}
	
	
}
