package ca.brocku.dotscanvas.app.engine.Handlers;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.widget.TextView;

/**
 * Author: Chris Kellendonk
 * Student #: 4810800
 * Date: 2014-06-17
 */
public class ScoreViewHandler extends Handler {
    private static final String KEY_GAME_SCORE = "GAME_SCORE";

    private TextView textView;

    public ScoreViewHandler(TextView textView) {
        if(textView == null) { throw new IllegalArgumentException("[textView] can't be null."); }

        this.textView = textView;
    }

    public void updateScore(int score) {
        // Update score and send message to myself
        Message message = obtainMessage();
        Bundle bundle = new Bundle();
        bundle.putInt(KEY_GAME_SCORE, score);
        message.setData(bundle);
        sendMessage(message);
    }

    @Override
    public void handleMessage(Message msg) {
        String score = String.valueOf(msg.getData().getInt(KEY_GAME_SCORE));
        textView.setText(score);
    }
}