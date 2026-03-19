package com.yourname.connectfour;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.animation.DecelerateInterpolator;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        ImageView icon  = findViewById(R.id.splash_icon);
        TextView  title = findViewById(R.id.splash_title);

        icon.setAlpha(0f);
        icon.setScaleX(0.4f);
        icon.setScaleY(0.4f);
        title.setAlpha(0f);
        title.setTranslationY(30f);

        ObjectAnimator iconAlpha  = ObjectAnimator.ofFloat(icon,  "alpha",  0f, 1f);
        ObjectAnimator iconScaleX = ObjectAnimator.ofFloat(icon,  "scaleX", 0.4f, 1f);
        ObjectAnimator iconScaleY = ObjectAnimator.ofFloat(icon,  "scaleY", 0.4f, 1f);
        AnimatorSet iconAnim = new AnimatorSet();
        iconAnim.playTogether(iconAlpha, iconScaleX, iconScaleY);
        iconAnim.setDuration(600);
        iconAnim.setInterpolator(new DecelerateInterpolator());

        ObjectAnimator titleAlpha = ObjectAnimator.ofFloat(title, "alpha",        0f, 1f);
        ObjectAnimator titleY     = ObjectAnimator.ofFloat(title, "translationY", 30f, 0f);
        AnimatorSet titleAnim = new AnimatorSet();
        titleAnim.playTogether(titleAlpha, titleY);
        titleAnim.setDuration(500);
        titleAnim.setStartDelay(350);
        titleAnim.setInterpolator(new DecelerateInterpolator());

        iconAnim.start();
        titleAnim.start();

        new Handler().postDelayed(() -> {
            startActivity(new Intent(SplashActivity.this, MainActivity.class));
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            finish();
        }, 2200);
    }
}
