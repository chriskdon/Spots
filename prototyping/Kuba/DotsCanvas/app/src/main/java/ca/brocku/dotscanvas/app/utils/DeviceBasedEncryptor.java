package ca.brocku.dotscanvas.app.utils;

import android.content.Context;
import android.provider.Settings;
import android.util.Base64;
import android.util.Log;

import java.security.Key;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

public class DeviceBasedEncryptor {
  private static final String ALGO = "AES";
  private final Cipher encryptor, decryptor;

  public DeviceBasedEncryptor(Context context) {
    String encryptionKey = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);

    try {
      Key key = new SecretKeySpec(encryptionKey.getBytes(), ALGO);

      this.encryptor = Cipher.getInstance(ALGO);
      this.encryptor.init(Cipher.ENCRYPT_MODE, key);

      this.decryptor = Cipher.getInstance(ALGO);
      this.decryptor.init(Cipher.DECRYPT_MODE, key);
    } catch (Exception ex) {
      throw new RuntimeException("Invalid Cipher: " + ALGO);
    }
  }

  public String encrypt(String value) {
    try {
      return Base64.encodeToString(encryptor.doFinal(value.getBytes()), Base64.DEFAULT);
    } catch (Exception ex) {
      throw new RuntimeException(ex);
    }
  }

  public String decrypt(String value) {
    try {
      return new String(decryptor.doFinal(Base64.decode(value, Base64.DEFAULT)));
    } catch (Exception ex) {
      Log.e("B64", value);
      throw new RuntimeException(ex);
    }
  }
}
