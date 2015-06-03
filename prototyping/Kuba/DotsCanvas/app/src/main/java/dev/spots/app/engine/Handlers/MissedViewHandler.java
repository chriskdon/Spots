package dev.spots.app.engine.Handlers;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.widget.TextView;

public class MissedViewHandler extends Handler {
  private static final String KEY_GAME_MISSED = "GAME_MISSED";

  private TextView textView;

  public MissedViewHandler(TextView textView) {
    if (textView == null) {
      throw new IllegalArgumentException("[textView] can't be null.");
    }

    this.textView = textView;
  }


  public void updateMissedCount(int count) {
    Message message = obtainMessage();
    Bundle bundle = new Bundle();
    bundle.putInt(KEY_GAME_MISSED, count);
    message.setData(bundle);
    sendMessage(message);
  }

  @Override
  public void handleMessage(Message msg) {
    String missed = String.valueOf(msg.getData().getInt(KEY_GAME_MISSED));
    textView.setText(missed);
  }
}