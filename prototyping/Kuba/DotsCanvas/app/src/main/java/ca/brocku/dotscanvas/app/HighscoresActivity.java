package ca.brocku.dotscanvas.app;

import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.View;
import android.widget.TextView;

/**
 * Author: Chris Kellendonk
 * Student #: 4810800
 * Date: 2014-12-11
 */
public class HighscoresActivity extends ActionBarActivity {
  private TextView[] lbl_Highscores;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.highscores);

    this.lbl_Highscores = new TextView[]{
        (TextView) findViewById(R.id.lbl_Highscore1),
        (TextView) findViewById(R.id.lbl_Highscore2),
        (TextView) findViewById(R.id.lbl_Highscore3),
        (TextView) findViewById(R.id.lbl_Highscore4),
        (TextView) findViewById(R.id.lbl_Highscore5),
    };

  }

  /**
   * Set the highscore for a specific rank.
   *
   * @param rank  The rank of the score.
   * @param score The score to set it to.
   */
  private void setHighscore(int rank, int score) {
    if (rank >= lbl_Highscores.length) {
      throw new IllegalArgumentException("Rank must be less than: " + lbl_Highscores.length);
    }

    lbl_Highscores[rank].setText(Integer.toString(score));
    lbl_Highscores[rank].setVisibility(View.VISIBLE);
  }
}
