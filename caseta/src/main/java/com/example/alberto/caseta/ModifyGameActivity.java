package com.example.alberto.caseta;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;

import java.io.File;

public class ModifyGameActivity extends Activity implements OnClickListener {

    private EditText nameText;
    private Button updateBtn, cancelBtn;
    private EditText descText;

    private String filename;

    private long _id;

    private DBManager dbManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setTitle("Detalles del juego");

        setContentView(R.layout.mod_game);

        dbManager = new DBManager(this);
        dbManager.open();

        nameText = findViewById(R.id.name_edittext);
        descText = (EditText) findViewById(R.id.description_edittext);

        updateBtn = (Button) findViewById(R.id.btn_update);
        cancelBtn = (Button) findViewById(R.id.btn_cancel);

        Intent intent = getIntent();
        filename = intent.getStringExtra("filename");
        String imagename = intent.getStringExtra("imagename");
        File imgFile  = new File(Environment.getExternalStorageDirectory().toString() + "/Caseta/" + imagename);
        if(imgFile.exists()){
            Bitmap myBitmap = BitmapFactory.decodeFile(imgFile.getAbsolutePath());
            ImageView imageView = findViewById(R.id.imgDetGam);
            imageView.setImageBitmap(myBitmap);
        }

        String[] nameDesc = dbManager.getNameDescbyFilename(filename);

        nameText.setText(nameDesc[0]);
        descText.setText(nameDesc[1]);

        updateBtn.setOnClickListener(this);
        cancelBtn.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_update:
                String title = nameText.getText().toString();
                String desc = descText.getText().toString();

                dbManager.update(filename,title,desc);
                this.returnHome();
                break;

            case R.id.btn_cancel:
                this.returnHome();
                break;
        }
    }

    public void returnHome() {
        Intent home_intent = new Intent(getApplicationContext(), CDTSActivity.class)
                .setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(home_intent);
    }
}
