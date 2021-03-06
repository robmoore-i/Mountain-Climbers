package com.gmail.mountainapp.scrane.mountainclimbers;

import android.content.Intent;

import androidx.annotation.NonNull;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.common.images.ImageManager;
import com.google.android.gms.games.Games;
import com.google.android.gms.games.Player;
import com.google.android.gms.games.PlayersClient;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;

import java.util.Date;

public class MainActivity extends DriveActivity {

    private Button playButton, levelSelectButton, timedButton, puzzleButton, dailyButton;
    private ImageView settingsButton;
    private TextView userNameText, streakText;
    private ImageView userProfilePicture, streakBlob;
    private SharedPreferences preferences;
    private SharedPreferences.Editor editor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        preferences = getSharedPreferences(getString(R.string.PREFERENCES), MODE_PRIVATE);
        editor = preferences.edit();

        playButton = findViewById(R.id.mainPlayButton);
        playButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                editor.putInt(getString(R.string.MODE), Common.MODE_DEFAULT);

                Intent selectPack = new Intent();
                selectPack.setClass(MainActivity.this, PackSelectActivity.class);

                Intent selectLevel = new Intent();
                selectLevel.setClass(MainActivity.this, LevelSelectActivity.class);
                Intent playGame = new Intent();

                int packPos = 0;
                int levelPos = 0;
                Levels.Pack pack;

                DataBaseHandler db = new DataBaseHandler(MainActivity.this);
                boolean goToTutorial = true;
                boolean stop = false;
                while (!stop) {
                    pack = Levels.packs[packPos];
                    if (goToTutorial && pack.getNumTutorials() > 0){
                        levelPos = 0;
                        while (db.isCompletedTutorial(db.getId(packPos, levelPos)) && levelPos < pack.getNumTutorials()){
                            levelPos++;
                        }
                        if (levelPos == pack.getNumTutorials()){
                            goToTutorial = false;
                            levelPos = 0;
                        } else {
                            stop = true;
                        }
                    } else if (goToTutorial && pack.getNumTutorials() == 0) {
                        goToTutorial = false;
                    } else {
                        levelPos = 0;
                        while(db.isCompleted(db.getId(packPos, levelPos)) && levelPos < pack.getLength()){
                            levelPos++;
                        }
                        if (levelPos == pack.getLength()){
                            goToTutorial = true;
                            levelPos = 0;
                            packPos++;
                        } else {
                            stop = true;
                        }
                    }
                    if (packPos == Levels.packs.length){
                        stop = true;
                        levelPos = 0;
                        packPos = 0;
                    }
                }
                editor.putInt(getString(R.string.LEVELPOS), levelPos);
                editor.putInt(getString(R.string.PACKPOS), packPos);
                startActivity(selectPack);
                startActivity(selectLevel);
                if (goToTutorial){
                    playGame.setClass(MainActivity.this, TutorialActivity.class);
                } else {
                    playGame.setClass(MainActivity.this, PlayGameActivity.class);
                }
                editor.apply();
                startActivity(playGame);
            }
        });

        levelSelectButton = findViewById(R.id.mainLevelSelectButton);
        levelSelectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent selectPack = new Intent();
                selectPack.setClass(MainActivity.this, PackSelectActivity.class);
                editor.putInt(getString(R.string.MODE), Common.MODE_DEFAULT);
                editor.apply();
                startActivity(selectPack);
            }
        });

        timedButton = findViewById(R.id.mainTimedModeButton);
        timedButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent selectPack = new Intent();
                selectPack.setClass(MainActivity.this, PackSelectActivity.class);
                editor.putInt(getString(R.string.MODE), Common.MODE_TIMED);
                editor.apply();
                startActivity(selectPack);
            }
        });

        puzzleButton = findViewById(R.id.mainPuzzleModeButton);
        puzzleButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent selectPack = new Intent();
                selectPack.setClass(MainActivity.this, PackSelectActivity.class);
                editor.putInt(getString(R.string.MODE), Common.MODE_PUZZLE);
                editor.apply();
                startActivity(selectPack);
            }
        });

        settingsButton = findViewById(R.id.mainSettingsButton);
        settingsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent settings = new Intent();
                settings.setClass(MainActivity.this, SettingsActivity.class);
                startActivity(settings);
            }
        });

        dailyButton = findViewById(R.id.mainDailyPuzzle);
        dailyButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent playDaily = new Intent();
                playDaily.setClass(MainActivity.this, PlayDailyLevel.class);
                startActivity(playDaily);
            }
        });

        userNameText = findViewById(R.id.userName);
        userProfilePicture = findViewById(R.id.userProfilePicture);

        userProfilePicture.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent viewProfile = new Intent();
                viewProfile.setClass(MainActivity.this, ActivityViewProfile.class);
                startActivity(viewProfile);
            }
        });

        streakBlob = findViewById(R.id.streakBlob);
        streakText = findViewById(R.id.streakText);
    }

    @Override
    protected void onSignIn(GoogleSignInAccount account){
        super.onSignIn(account);
        Log.d("MAIN", "Signed in");
        if (account == null || !signedIn){
            userNameText.setText(getString(R.string.sign_in));
            userProfilePicture.setImageDrawable(getDrawable(R.drawable.nobody));
            return;
        }
        userNameText.setText(account.getGivenName());
        PlayersClient playersClient = Games.getPlayersClient(this, account);
        playersClient.getCurrentPlayer().addOnCompleteListener(new OnCompleteListener<Player>() {
            @Override
            public void onComplete(@NonNull Task<Player> task) {
                if (!task.isSuccessful()){
                    return;
                }
                Player player = task.getResult();
                if (player == null){
                    return;
                }
                if (player.getIconImageUri() == null){
                    userProfilePicture.setImageDrawable(getDrawable(R.drawable.nobody));
                } else {
                    ImageManager.create(MainActivity.this).loadImage(userProfilePicture, player.getIconImageUri());
                }
            }
        });
        gamesClient = Games.getGamesClient(this, account);
        gamesClient.setViewForPopups(findViewById(R.id.container_pop_up));
    }

    @Override
    protected void onResume(){
        super.onResume();
        DataBaseHandler db = new DataBaseHandler(this);
        Date date = new Date();
        int days = (int) (date.getTime() / (1000 * 24 * 60 * 60));
        int streak = db.getDailyStreak(days);
        streakText.setText(Integer.toString(streak));
        if (streak == 0){
            streakBlob.setColorFilter(getColor(R.color.streak0));
        } else if (streak < 10){
            streakBlob.setColorFilter(getColor(R.color.streak1_9));
        } else {
            streakBlob.setColorFilter(getColor(R.color.streak10_plus));
        }
        if (db.howManyCompletedOnDay(days) > 0){
            dailyButton.setClickable(false);
            dailyButton.setTextColor(getColor(R.color.darkTextBlue));
        }
        db.close();
    }
}
