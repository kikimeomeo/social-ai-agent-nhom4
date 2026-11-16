package com.tuankiet.myapplication.util;

import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;
import android.util.Log;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

/**
 * Tiện ích mã hóa & giải mã chuẩn AES-256-GCM bảo vệ bởi phần cứng Android KeyStore.
 * Đáp ứng yêu cầu bảo mật cao nhất cho API Keys và OAuth Access Tokens.
 */
public class AesEncryptionUtil {

    private static final String TAG = "AesEncryptionUtil";
    private static final String ANDROID_KEYSTORE = "AndroidKeyStore";
    private static final String KEY_ALIAS = "social_ai_aes_256_key";
    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int GCM_TAG_LENGTH = 128; // 128 bit GCM Tag

    private static SecretKey getOrCreateSecretKey() {
        try {
            KeyStore keyStore = KeyStore.getInstance(ANDROID_KEYSTORE);
            keyStore.load(null);

            if (!keyStore.containsAlias(KEY_ALIAS)) {
                KeyGenerator keyGenerator = KeyGenerator.getInstance(
                        KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE);

                KeyGenParameterSpec.Builder builder = new KeyGenParameterSpec.Builder(
                        KEY_ALIAS,
                        KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                        .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                        .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                        .setKeySize(256);

                keyGenerator.init(builder.build());
                return keyGenerator.generateKey();
            } else {
                KeyStore.SecretKeyEntry keyEntry = (KeyStore.SecretKeyEntry) keyStore.getEntry(KEY_ALIAS, null);
                return keyEntry.getSecretKey();
            }
        } catch (Exception e) {
            Log.e(TAG, "Lỗi tạo hoặc lấy khóa SecretKey từ Android KeyStore", e);
            return null;
        }
    }

    /**
     * Mã hóa chuỗi văn bản thuần bằng AES-256-GCM
     *
     * @param plainText Chuỗi cần mã hóa (API Key, OAuth Token)
     * @return Chuỗi mã hóa dạng Base64 chứa cả IV + CipherText
     */
    public static String encrypt(String plainText) {
        if (plainText == null || plainText.isEmpty()) {
            return plainText;
        }

        try {
            SecretKey secretKey = getOrCreateSecretKey();
            if (secretKey == null) {
                return plainText;
            }

            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey);

            byte[] iv = cipher.getIV();
            byte[] cipherText = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));

            // Đóng gói IV length (4 bytes) + IV bytes + CipherText bytes
            ByteBuffer byteBuffer = ByteBuffer.allocate(4 + iv.length + cipherText.length);
            byteBuffer.putInt(iv.length);
            byteBuffer.put(iv);
            byteBuffer.put(cipherText);

            return Base64.encodeToString(byteBuffer.array(), Base64.NO_WRAP);
        } catch (Exception e) {
            Log.e(TAG, "Lỗi trong quá trình mã hóa AES-256", e);
            return plainText;
        }
    }

    /**
     * Giải mã chuỗi Base64 về lại văn bản ban đầu
     *
     * @param encryptedBase64 Chuỗi Base64 đã mã hóa
     * @return Chuỗi văn bản gốc đã giải mã
     */
    public static String decrypt(String encryptedBase64) {
        if (encryptedBase64 == null || encryptedBase64.isEmpty()) {
            return encryptedBase64;
        }

        try {
            byte[] decoded = Base64.decode(encryptedBase64, Base64.NO_WRAP);
            if (decoded.length < 16) {
                // Không phải chuỗi mã hóa AES hợp lệ, trả về nguyên bản (hỗ trợ tương thích ngược)
                return encryptedBase64;
            }

            ByteBuffer byteBuffer = ByteBuffer.wrap(decoded);
            int ivLength = byteBuffer.getInt();

            if (ivLength < 8 || ivLength > 16 || decoded.length < 4 + ivLength) {
                return encryptedBase64;
            }

            byte[] iv = new byte[ivLength];
            byteBuffer.get(iv);

            byte[] cipherText = new byte[byteBuffer.remaining()];
            byteBuffer.get(cipherText);

            SecretKey secretKey = getOrCreateSecretKey();
            if (secretKey == null) {
                return encryptedBase64;
            }

            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, parameterSpec);

            byte[] plainTextBytes = cipher.doFinal(cipherText);
            return new String(plainTextBytes, StandardCharsets.UTF_8);

        } catch (Exception e) {
            Log.w(TAG, "Không thể giải mã AES (có thể là chuỗi văn bản cũ hoặc chưa mã hóa): " + e.getMessage());
            return encryptedBase64;
        }
    }
}
