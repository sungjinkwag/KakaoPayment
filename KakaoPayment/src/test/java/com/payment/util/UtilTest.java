package com.payment.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.ByteBuffer;
import java.security.AlgorithmParameters;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.util.Base64;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

import com.payment.entity.PaymentInfo;
import com.payment.entity.PaymentList;

public class UtilTest {
	
	private static Logger logger = LoggerFactory.getLogger(UtilTest.class);
	
	final static String SEPERATE = ",";										// 카드정보 합칠때 쓰는 구분자
	final static String MY_KEY = "KingOfDavid";							// 카드정보 암호화에 필요한 개인키
	final static String ENC_SOURCE_PREFIX = "yehuruaa";		// 카드정보 합칠때 쓰는 접두어
	final static String APPROVAL = "PAYMENT";							// 결재
	final static String CANCEL = "CANCEL";								// 취소
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
	 */
	@Test
	public void makeSource() {
		
		PaymentInfo paymentInfoVO = PaymentInfo.builder()
					.cardNum("12345678901")
					.validTerm("1222")
					.cvc("313")
					.insMonth("12")
					.paymentMoney("1000")
					.paymentStatus("PAYMENT")
					.addValueTax("")
					.orgMgntId("")
					.build();
		
		String source = ENC_SOURCE_PREFIX+paymentInfoVO.getCardNum()+SEPERATE
				+paymentInfoVO.getValidTerm()+SEPERATE
				+paymentInfoVO.getCvc();
		logger.debug("source : " + source);
		assertThat(source).isNotNull();
	}
	
	/**
	 * 유니크한 관리번호 만들기 
	 */
	@Test
	public void makeUniqueString() {
		String id = UUID.randomUUID().toString().replace("-", "").substring(0, 20);
		logger.debug("id : " + id);
		assertThat(id).isNotNull();
	}
	
	/**
	 * AES256 암호화
	 * @throws Exception
	 */
	@Test
	public void encryptAES256() throws Exception {
		String msg = "yehuruaa12345678901,1222,313";
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

	    String encryptAES256 = Base64.getEncoder().encodeToString(buffer);
	    logger.debug("encryptAES256 : " + encryptAES256);
	    
	    assertThat(encryptAES256).isNotNull();
	}
	
	/**
	 * AES256 복호화
	 * @throws Exception
	 */
	@Test
	public void decryptAES256() throws Exception {
		String msg = "BbLqy+Ck/pCMA6Wta88WQPZzei3UNvDwE9cTnP9QV2tYk3FvFpcw9Sb08SatwWOBkfL6edfEJTm9QNFDzu+wMQhaWU0lrJIolYylmkIQCH4Hfp2D";
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
	    
	    String decryptAES256 = new String(decryptedTextBytes);
	    logger.debug("decryptAES256 : " + decryptAES256);
	    
	    assertThat(decryptAES256).isNotNull();

	}

	/**
	 * 카드사에 전송할 전문 만들기
	 * @throws Exception
	 */
	@Test
	public void makeSendMessage() throws Exception {
		String mgntNo = UUID.randomUUID().toString().replace("-", "").substring(0, 20); 
		
		PaymentInfo paymentInfo = PaymentInfo.builder()
					.cardNum("1234567890123456")
					.validTerm("1222")
					.cvc("313")
					.insMonth("12")
					.paymentMoney("1000")
					.paymentStatus("PAYMENT")
					.addValueTax("")
					.orgMgntId("")
					.build();
		
		String encSource = "bu0vUHyhAsZSsoHqQRslRhgk9pii57uRHoBt0Ajl7nXjVhBT6hS39B12345678XSvtX2kE1iTg0zzXklWrZpu+8NqFyY1EGT9/8=";
		logger.debug("encSource : " + encSource);
		logger.debug("encSource length : " + encSource.length());
		String sendEncSource = encSource.substring(0,100);
		String restEncSource = encSource.substring(100);

		// 카드사에 보낼 메세지
		StringBuffer message = new StringBuffer();
		// 데이터길이를 뺀 공통헤더부문 세팅
		message.append(makePaddingMsg(paymentInfo.getPaymentStatus(),10,"R","_"))		// (문자 10) 기능 구분값
				.append(makePaddingMsg(mgntNo,20,"R","_"))													// (문자 20) 관리번호
				//데이터부문
				.append(makePaddingMsg(paymentInfo.getCardNum(),20,"R","_"))					// (숫자L 20) 카드번호
				.append(makePaddingMsg(paymentInfo.getInsMonth(),2,"L","0"))					// (숫자0 2) 할부개월수
				.append(makePaddingMsg(paymentInfo.getValidTerm(),4,"R","_"))					// (숫자L 4) 카드유효기간
				.append(makePaddingMsg(paymentInfo.getCvc(),3,"R","_"))							// (숫자L 3) cvc
				.append(makePaddingMsg(paymentInfo.getPaymentMoney(),10,"L","_"))			// (숫자 10) 거래금액
				.append(makePaddingMsg(paymentInfo.getPaymentMoney(),10,"L","0"))			// (숫자0 10) 부가가치세
				.append(makePaddingMsg(makeOrgMgntNo(paymentInfo),20,"R","_"))				// (문자 20) 원거래 관리번호 
				.append(makePaddingMsg(sendEncSource,300,"R","_"))										// (문자 300) 암호화된 카드정보 
				.append(makePaddingMsg(restEncSource,47,"R","_"))											// (문자 47) 예비필드 
				;
		
		logger.debug("message 01 : " + message.toString());
		logger.debug("message length 01: " + message.length());
		// 헤더부의 데이터길이 세팅
		if(message.length()+4 == MSG_SIZE) {
			String dataLength = makePaddingMsg(Integer.toString(message.length()),4,"L","_");
			logger.debug("dataLength : " + dataLength);
			String result = dataLength.concat(message.toString());
			logger.debug("result : " + result);
			logger.debug("result length 01: " + result.length());
		}
		
	}
	
	/**
	 * 카드사에 보낼 전문 만들때 항목 패딩화 하기
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
	 * 취소 결재인경우 결재 관리번호 세팅하기
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
	 * 카드사 전문 조합용 항목별 패딩
	 */
	@Test
	public void makePaddingMsg() {
		
		String str = "abc";
		int size = 10;
		String LorR = "R";
		String type = "_";
		String result = null;
		
		int underSize = size - str.length();
		StringBuffer sb = new StringBuffer();
		for (int i = 0; i < underSize; i++) {
			sb.append(type);
		}
		if(LorR.equals(RIGHT)) {
			result = str.concat(sb.toString());
			logger.debug("R padding : " + result);
			assertThat(result).isNotNull();
		} else if(LorR.equals(LEFT)) {
			result = (sb.toString()).concat(str);
			logger.debug("L padding : " + result);
			assertThat(result).isNotNull();
		}
		assertThat(result).isNotNull();
	}
	
	/**
	 * 부가가치세 제약조건 검증
	 */
	@Test
	public void checkAddValue() {
		String strPayMoney = "1000";
		String strAddValue = "";
		float fltPayMoney = Float.parseFloat(strPayMoney);
		float fltAddValue;
		if(StringUtils.isEmpty(strAddValue)) {
			fltAddValue = 0;
		} else {
			fltAddValue = Float.parseFloat(strAddValue);
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
		
		if(intAddValue < Integer.parseInt(strPayMoney)) {
			logger.debug("intAddValue03 : " + intAddValue);
			assertThat(intAddValue).isNotNull();
		}
		
		assertThat(strAddValue).isNotNull();
	}

	/**
	 * 카드번호가 16자리 이하인경우 16자리로 맞추기
	 */
	@Test
	public void checkCardNum() {
		String cardNum = "12345601";
		if(cardNum.length() < 16) {
			logger.debug("result : "+makePaddingMsg(cardNum,16,"L","_"));
		}
	}
	/**
	 * 결제 요청시 유효성 검증
	 */
	@Test
	public void validateItem() {
		
		String source = "123456";
		String regExp = REG_01;
		
		Pattern p = Pattern.compile(regExp);
		Matcher m = p.matcher(source);
		
		if((regExp.equals(REG_02) || regExp.equals(REG_04)) && m.find()) {
			if(Integer.parseInt(source.substring(0,2)) > 12) {
				logger.debug("false");
			}
			logger.debug("true");
		}
		
		if(regExp.equals(REG_05) && m.find()) {
			if(Integer.parseInt(source) > 1000000000) {
				logger.debug("false");
			}
			logger.debug("true");
		}
		
		logger.debug("m.find() : "+m.find());
	}
	
	@Test
	public void parseMessage() throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeySpecException, InvalidKeyException, InvalidAlgorithmParameterException, IllegalBlockSizeException, BadPaddingException {
		String message = "_446PAYMENT___97e5c3ffd6414df18086_____12345678901____121222313______10000000000091____________________BbLqy+Ck/pCMA6Wta88WQPZzei3UNvDwE9cTnP9QV2tYk3FvFpcw9Sb08SatwWOBkfL6edfEJTm9QNFDzu+wMQhaWU0lrJIolYyl________________________________________________________________________________________________________________________________________________________________________________________________________mkIQCH4Hfp2D___________________________________";
		
//		logger.debug("카드관리번호(4) : " + message.substring(0,4));
//		logger.debug("결재상태(10) : " + message.substring(4,14));
//		logger.debug("관리번호(20) : " + message.substring(14,34));
//		logger.debug("카드번호(20) : " + message.substring(34,54));
//		String temp = message.substring(34,54);
//		String cardNum = temp.replace("_", "");
//		logger.debug("_제거된 카드번호 : " + cardNum);
//		StringBuffer sb = new StringBuffer();
//		sb.append(cardNum.substring(0,6));
//		for (int i = 0; i < cardNum.length()-9; i++) {
//			sb.append("*");
//		}
//		sb.append(cardNum.substring(cardNum.length()-3,cardNum.length()));
//		logger.debug("마스킹 카드번호 : " + sb.toString());
		
//		logger.debug("할부개월수(2) : " + message.substring(54,56));
		logger.debug("카드유효기간(4) : " + message.substring(56,60));
		logger.debug("CVC(3) : " + message.substring(60,63));
		logger.debug("결제/취소금액(10) : " + message.substring(63,73));
		logger.debug("결제/취소금액 부가세(10) : " + message.substring(73,83));
		logger.debug("취소시 결제 관리번호(20) : " + message.substring(83,103));
		
		logger.debug("암호화 카드정보(300) : " + message.substring(103,403));
		String temp01 = message.substring(103,403);
		String cardInfo01 = temp01.replace("_", "");
		logger.debug("_제거된 암호화 카드정보(300) : " + cardInfo01);
		
		logger.debug("예비(300) : " + message.substring(403,450));
		String temp02 = message.substring(403,450);
		String cardInfo02 = temp02.replace("_", "");
		logger.debug("_제거된 예비(300) : " + cardInfo02);
		
		String encCardInfo = cardInfo01.concat(cardInfo02);
		logger.debug("암호화된 카드정보 : " + encCardInfo);
		
	    Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
	    ByteBuffer buffer = ByteBuffer.wrap(Base64.getDecoder().decode(encCardInfo));

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
	    
	    String decryptAES256 = new String(decryptedTextBytes);
	    logger.debug("decryptAES256 : " + decryptAES256);
	    
	    decryptAES256 = decryptAES256.replace(ENC_SOURCE_PREFIX, "").replace("_", "");
	    logger.debug("decryptAES256 remove prefix : " + decryptAES256);
	    
	    String[] cardInfo = decryptAES256.split(SEPERATE);
	    
	    logger.debug("Card Num : " + cardInfo[0]);
		StringBuffer sb = new StringBuffer();
		String cardNum = cardInfo[0];
		sb.append(cardNum.substring(0,6));
		for (int i = 0; i < cardNum.length()-9; i++) {
			sb.append("*");
		}
		sb.append(cardNum.substring(cardNum.length()-3,cardNum.length()));
		logger.debug("마스킹 카드번호 : " + sb.toString());
		
	    logger.debug("Valid Term : " + cardInfo[1]);
	    logger.debug("CVC : " + cardInfo[2]);
		
	}
	

	/**
	 * 할부개월수가 1자리수일때, 0X로 변환
	 */
	@Test
	public void checkInsMonth() {
		String strInsMonth = "0";
		if(strInsMonth.length() == 1) {
			strInsMonth = "0"+strInsMonth;
		}
		logger.debug("strInsMonth : " + strInsMonth);
	}
}
