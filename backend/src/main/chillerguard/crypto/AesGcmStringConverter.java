package chillerguard.crypto;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

//aes-gcm jpa converter for sensitive cols
@Converter
public class AesGcmStringConverter implements AttributeConverter<String, String> {
    private static final int GCM_TAG_BITS = 128;
    private static final int IV_BYTES = 12;
    private final SecureRandom secureRandom = new SecureRandom();

    @Override
    public String convertToDatabaseColumn(String attribute) {
        if (attribute == null || attribute.isBlank()) {
            return attribute;
        }
        try {
            byte[] iv = new byte[IV_BYTES];
            secureRandom.nextBytes(iv);

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, secretKey(), new GCMParameterSpec(GCM_TAG_BITS, iv));
            byte[] encrypted = cipher.doFinal(attribute.getBytes(StandardCharsets.UTF_8));

            ByteBuffer buffer = ByteBuffer.allocate(iv.length + encrypted.length);
            buffer.put(iv);
            buffer.put(encrypted);
            return Base64.getEncoder().encodeToString(buffer.array());
        } catch (Exception e) {
            throw new IllegalStateException("Failed to encrypt value", e);
        }
    }

    @Override
    public String convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank()) {
            return dbData;
        }
        try {
            byte[] raw = Base64.getDecoder().decode(dbData);
            ByteBuffer buffer = ByteBuffer.wrap(raw);
            byte[] iv = new byte[IV_BYTES];
            buffer.get(iv);
            byte[] encrypted = new byte[buffer.remaining()];
            buffer.get(encrypted);

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, secretKey(), new GCMParameterSpec(GCM_TAG_BITS, iv));
            byte[] decrypted = cipher.doFinal(encrypted);
            return new String(decrypted, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to decrypt value", e);
        }
    }

    private SecretKeySpec secretKey() {
        String rawKey = System.getenv().getOrDefault(
                "APP_SECURITY_ENCRYPTION_KEY",
                System.getProperty("app.security.encryption-key", "dev-32-byte-key-change-me-123456")
        );
        byte[] key = rawKey.getBytes(StandardCharsets.UTF_8);
        if (key.length < 32) {
            byte[] padded = new byte[32];
            System.arraycopy(key, 0, padded, 0, key.length);
            key = padded;
        }
        if (key.length > 32) {
            byte[] trimmed = new byte[32];
            System.arraycopy(key, 0, trimmed, 0, 32);
            key = trimmed;
        }
        return new SecretKeySpec(key, "AES");
    }
}
