package ca.brocku.dotscanvas.app.models;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import java.util.Arrays;

import ca.brocku.dotscanvas.app.HighscoresActivity;
import ca.brocku.dotscanvas.app.utils.DeviceBasedEncryptor;

public class HighscoreManager {
  private static final int MAX_SCORES = HighscoresActivity.MAX_SCORES;
  private static final String E_CHECK = "DECRYPTED";
  private static final String PREF_KEY = "SPOTS_SCORES";

  private final DeviceBasedEncryptor encryptor;
  private final SharedPreferences preferences;

  private int[] scores;

  public HighscoreManager(Context context) {
    this.encryptor = new DeviceBasedEncryptor(context);
    this.preferences = context.getSharedPreferences(PREF_KEY, Context.MODE_PRIVATE);

    if (!isKeyValid()) {
      initPreferences();
    }

    scores = loadScores();
  }

  public void reset() {
    for(int i = 0; i < MAX_SCORES; i++) {
      setScore(i, 0);
    }
  }

  public void updateScore(int score) {
    for (int i = 0; i < scores.length; i++) {
      if (score > scores[i]) {
        int[] tempScores = Arrays.copyOf(scores, scores.length);
        for(int x = i + 1; x < scores.length; x++) {
          setScore(x, tempScores[x - 1]);
        }

        setScore(i, score);
        return;
      }
    }
  }

  private void setValue(String key, String value) {
    SharedPreferences.Editor e = preferences.edit();
    e.putString(key, encryptor.encrypt(value));
    e.commit();
  }

  private String getValue(String key, String defaultValue) {
    String score = preferences.getString(key, defaultValue);
    if (score.equals(defaultValue)) {
      return defaultValue;
    }
    return encryptor.decrypt(score);
  }

  private void setScore(int rank, int value) {
    if (rank >= MAX_SCORES) {
      throw new RuntimeException("Can only have " + MAX_SCORES + " scores. 0 based indexing.");
    }

    scores[rank] = value;
    setValue("RANK_" + rank, Integer.toString(value));
  }

  public int getScore(int rank) {
    if (rank >= MAX_SCORES) {
      throw new RuntimeException("Can only have " + MAX_SCORES + " scores. 0 based indexing.");
    }

    return Integer.parseInt(getValue("RANK_" + rank, "0"));
  }

  private void initPreferences() {
    setValue("E_CHECK", E_CHECK);

    scores = new int[MAX_SCORES];

    for (int i = 0; i < MAX_SCORES; i++) {
      setScore(i, 0);
    }
  }

  private int[] loadScores() {
    int[] scores = new int[MAX_SCORES];
    for (int i = 0; i < MAX_SCORES; i++) {
      scores[i] = getScore(i);
    }
    return scores;
  }

  private boolean isKeyValid() {
    if (preferences.contains("E_CHECK")) {
      return getValue("E_CHECK", "INVALID").equals(E_CHECK);
    }

    return false;
  }
}
