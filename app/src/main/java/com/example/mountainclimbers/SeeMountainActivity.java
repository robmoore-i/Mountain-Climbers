package com.example.mountainclimbers;

import android.content.Intent;
import android.content.SharedPreferences;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class SeeMountainActivity extends AppCompatActivity {

    private MountainView mountainView;
    public static int[] colorIDs = new int[] {R.color.climberGreen, R.color.climberPurple, R.color.climberOrange};
    private Button buttonBack, buttonNextLevel;
    private ImageView buttonReset;
    private TextView goButton, levelNumberText;
    private int levelID;
    private int levelPos;
    private DataBaseHandler db;
    private int speed;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_see_mountain);

        SharedPreferences preferences = getSharedPreferences(SettingsActivity.PREFERENCES, MODE_PRIVATE);
        speed = preferences.getInt(SettingsActivity.SPEED, 0);

        mountainView = findViewById(R.id.mountainView);

        Intent caller = getIntent();
        final int packPos = caller.getIntExtra(Levels.PACK_POS, -1);
        levelPos = caller.getIntExtra(Levels.LEVEL_POS, -1);
        Levels.Pack pack = Levels.packs[packPos];
        final Integer[] levelIDs = pack.getLevelIDs();
        levelID = levelIDs[levelPos];

        levelNumberText = findViewById(R.id.mountainLevelNumber);

        goButton = findViewById(R.id.mountainGoButton);
        goButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SeeMountainActivity.this.mountainView.go();
            }
        });

        buttonBack = findViewById(R.id.mountainBackButton);
        buttonBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        buttonReset = findViewById(R.id.mountainResetButton);
        buttonReset.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                loadLevel();
            }
        });

        buttonNextLevel = findViewById(R.id.mountainNextLevelButton);
        buttonNextLevel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                levelPos++;
                levelID = levelIDs[levelPos];
                loadLevel();
            }
        });

        mountainView.setOnVictoryListener(new MountainView.OnVictoryListener() {
            @Override
            public void onVictory() {
                goButton.setVisibility(View.INVISIBLE);

                db = new DataBaseHandler(SeeMountainActivity.this);
                db.markCompleted(levelID);

                if (levelPos < levelIDs.length - 1){
                    db.unlock(levelIDs[levelPos + 1]);
                    buttonNextLevel.setVisibility(View.VISIBLE);
                }
                db.close();
                buttonBack.setVisibility(View.VISIBLE);
            }
        });

        loadLevel();
    }

    private void loadLevel(){
        levelNumberText.setText(Integer.toString(levelPos + 1));

        buttonBack.setVisibility(View.INVISIBLE);
        buttonNextLevel.setVisibility(View.INVISIBLE);
        goButton.setVisibility(View.VISIBLE);
        try {
            InputStream stream = getResources().openRawResource(levelID);

            BufferedReader br = new BufferedReader(new InputStreamReader(stream));
            String[] heightStrings = br.readLine().split(" ");
            String[] climberString = br.readLine().split(" ");

            int[] heights = new int[heightStrings.length];
            for (int i = 0; i < heightStrings.length; i++) {
                heights[i] = Integer.parseInt(heightStrings[i]) / (1 + speed);
            }

            Mountain mountain = new Mountain(heights);
            mountainView.setMountain(mountain);

            for (int i = 0; i < climberString.length; i++) {
                MountainClimber climber = new MountainClimber();
                climber.setPosition(Integer.parseInt(climberString[i]) / (1 + speed));
                mountainView.addClimber(climber, colorIDs[i]);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onDestroy(){
        if (db != null){
            db.close();
        }
        super.onDestroy();
    }
}
