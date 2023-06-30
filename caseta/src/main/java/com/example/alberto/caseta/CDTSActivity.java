package com.example.alberto.caseta;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Environment;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.GridView;

import java.io.File;

public class CDTSActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener, AdapterView.OnItemClickListener, AdapterView.OnItemLongClickListener {

    private Game[] games;
    private DBManager dbManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cdts);

        Log.i("Mio", "Creando CDTs act");

        // Check directory
        File f = new File(Environment.getExternalStorageDirectory(), "Caseta");
        if (!f.exists()) {
            f.mkdirs();
        }

        File[] files = f.listFiles();
        int gamesLength = 0;
        for (int i = 0; i < files.length; i++) {
            if (files[i].getName().endsWith(".cdt")) {
                gamesLength++;
            }
        }

        dbManager = new DBManager(this);
        dbManager.open();

        games = new Game[gamesLength];
        int j = 0;
        for (int i = 0; i < files.length; i++) {
            if (files[i].getName().endsWith(".cdt")) {
                String filename = files[i].getName();
                String purename = filename.substring(0, filename.length()-4);
                // Check if exists in database
                String name_db = dbManager.check(filename);
                if (name_db == null) {
                    //Log.i("Mio", "No existe " + filename + ". Creando...");
                    dbManager.insert(purename, filename, purename + ".jpg","");
                    games[j++] = new Game(filename,purename + ".jpg", purename);
                } else {
                    //Log.i("Mio", "Ya existe " + filename + ".");
                    games[j++] = new Game(filename,purename + ".jpg", name_db);
                };

            }
        }

        dbManager.close();


        GridView gamesview = findViewById(R.id.gameview);

        GamesAdapter gamesAdapter = new GamesAdapter(this,games);
        gamesview.setAdapter(gamesAdapter);
        gamesview.setOnItemClickListener(this);
        gamesview.setOnItemLongClickListener(this);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.cdt, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            Intent intent = new Intent(this, SettingsActivity.class);
            startActivity(intent);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }



    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_camera) {
            // Handle the camera action
        } else if (id == R.id.nav_gallery) {

        } else if (id == R.id.nav_slideshow) {

        } else if (id == R.id.nav_manage) {

        } else if (id == R.id.nav_share) {

        } else if (id == R.id.nav_send) {

        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        Intent intent = new Intent(this, CassetteActivity.class);
        Bundle b = new Bundle();
        b.putString("filename",games[position].getFilename());
        b.putString("name",games[position].getName());
        intent.putExtras(b); //Put your id to your next Intent
        startActivity(intent);
    }

    @Override
    public boolean onItemLongClick(AdapterView<?> adapterView, View view, int position, long id) {
        Intent intent = new Intent(this, ModifyGameActivity.class);
        Bundle b = new Bundle();
        b.putString("filename",games[position].getFilename());
        b.putString("imagename", games[position].getImagename());
        intent.putExtras(b); //Put your id to your next Intent
        startActivity(intent);
        return true;
    }
}
