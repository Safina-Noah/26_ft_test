package team.ft.reponote.auth.service;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import team.ft.reponote.common.error.Problem;

@Component
public class TokenCipher {
  private final String configuredKey;

  public TokenCipher(@Value("${app.token-key}") String key) {
    this.configuredKey = key;
  }

  private SecretKeySpec key() {
    try {
      byte[] bytes = Base64.getDecoder().decode(configuredKey);
      if (bytes.length != 32) throw new IllegalArgumentException();
      return new SecretKeySpec(bytes, "AES");
    } catch (IllegalArgumentException e) {
      throw Problem.invalid("TOKEN_ENCRYPTION_KEY에 Base64 인코딩한 32바이트 키가 필요합니다.");
    }
  }

  public String encrypt(String text) {
    try {
      byte[] nonce = new byte[12];
      new SecureRandom().nextBytes(nonce);
      Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
      cipher.init(Cipher.ENCRYPT_MODE, key(), new GCMParameterSpec(128, nonce));
      byte[] encrypted = cipher.doFinal(text.getBytes(StandardCharsets.UTF_8));
      return Base64.getEncoder().encodeToString(nonce)
          + "."
          + Base64.getEncoder().encodeToString(encrypted);
    } catch (Problem e) {
      throw e;
    } catch (Exception e) {
      throw new IllegalStateException("토큰 암호화 실패", e);
    }
  }

  public String decrypt(String text) {
    try {
      String[] parts = text.split("\\.");
      Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
      cipher.init(
          Cipher.DECRYPT_MODE,
          key(),
          new GCMParameterSpec(128, Base64.getDecoder().decode(parts[0])));
      return new String(
          cipher.doFinal(Base64.getDecoder().decode(parts[1])), StandardCharsets.UTF_8);
    } catch (Exception e) {
      throw new IllegalStateException("저장된 연결 정보를 확인해주세요.", e);
    }
  }
}
