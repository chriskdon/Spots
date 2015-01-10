package ca.brocku.dotscanvas.app;

import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.SoundPool;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;

public class MainMenuActivity extends ActionBarActivity {
  private static final float VOLUME = 0.15f;

  private FrameLayout dim_overlay;
  private MediaPlayer mButtonSoundPlayer;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main_menu);

    mButtonSoundPlayer = MediaPlayer.create(this, R.raw.button_press);
    mButtonSoundPlayer.setVolume(VOLUME, VOLUME);

    ImageButton lifeButton = (ImageButton) findViewById(R.id.btn_StartLifeGame);
    ImageButton highscoresButton = (ImageButton) findViewById(R.id.main_menu_highscores);
    ImageButton infoButton = (ImageButton) findViewById(R.id.btnInfoScreen);

    dim_overlay = (FrameLayout) findViewById(R.id.dim_overlay);

    // Start Game
    lifeButton.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        mButtonSoundPlayer.start();
        MainMenuActivity.this.startActivity(new Intent(MainMenuActivity.this, MainActivity.class));
      }
    });

    // Show High scores
    highscoresButton.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        mButtonSoundPlayer.start();
        MainMenuActivity.this.startActivity(new Intent(MainMenuActivity.this, HighscoresActivity.class));
      }
    });

    infoButton.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        MainMenuActivity.this.startActivity(new Intent(MainMenuActivity.this, InformationActivity.class));
      }
    });
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    // Inflate the menu; this adds items to the action bar if it is present.
    getMenuInflater().inflate(R.menu.main_menu, menu);
    return true;
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    // Handle action bar item clicks here. The action bar will
    // automatically handle clicks on the Home/Up button, so long
    // as you specify a parent activity in AndroidManifest.xml.
    int id = item.getItemId();
    if (id == R.id.action_settings) {
      return true;
    }
    return super.onOptionsItemSelected(item);
  }

  @Override
  protected void onResume() {
    dimActivity(false);
    super.onResume();
  }

  @Override
  public void startActivity(Intent intent) {
    dimActivity(true);
    super.startActivity(intent);
  }


  /**
   * Toggle the overlay for the activity.
   * <p/>
   * Used when starting/returning from other activities.
   */
  private void dimActivity(boolean shouldDim) {
    int visibility = shouldDim ? View.VISIBLE : View.INVISIBLE;
    dim_overlay.setVisibility(visibility);
  }
}
