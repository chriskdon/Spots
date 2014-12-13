package ca.brocku.dotscanvas.app;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageButton;

public class MainMenuActivity extends ActionBarActivity {
  FrameLayout dim_overlay;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main_menu);

    ImageButton lifeButton = (ImageButton) findViewById(R.id.btn_StartLifeGame);
    dim_overlay = (FrameLayout) findViewById(R.id.dim_overlay);

    lifeButton.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        MainMenuActivity.this.startActivity(new Intent(MainMenuActivity.this, MainActivity.class));
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
