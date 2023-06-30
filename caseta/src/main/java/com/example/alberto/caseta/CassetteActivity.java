package com.example.alberto.caseta;

import android.animation.AnimatorInflater;
import android.animation.ValueAnimator;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.support.constraint.ConstraintLayout;
import android.support.constraint.Group;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Switch;
import android.widget.TextView;

import java.io.BufferedInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.ref.WeakReference;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.Random;

public class CassetteActivity extends AppCompatActivity implements View.OnClickListener, MediaPlayer.OnCompletionListener {

    private static final int TSTATE = 3500000;
    private static final int FS = 44100;

    private static MediaPlayer mediaPlayer;
    private AsyncTask<String,Integer,Void> cdtParsing;
    private AsyncTask<Void,Integer,Void> trackAudio;
    private PowerManager.WakeLock wakeLock;
    private boolean completed;
    private ValueAnimator animator;
    private ValueAnimator animator2;

    private ImageButton playButton;
    private ImageButton pauseButton;
    private Switch releSwitch;
    private ImageView spindle1;
    private ImageView spindle2;
    private ImageView reel1;
    private ImageView reel2;
    private ImageView cassette;
    private View telon;
    private CustomSeekBar seekbar;
    private TextView caratulaText;
    private int lastPosition = 0;

    private String cdtfilename;
    private boolean playpressed;
    private boolean releactive;

    private int[] blockStarts;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Bundle b = getIntent().getExtras();
        cdtfilename = b.getString("filename");

        caratulaText = findViewById(R.id.caratulaText);
        playButton = findViewById(R.id.play);
        pauseButton = findViewById(R.id.pause);
        ImageButton ffwButton = findViewById(R.id.ffw);
        ImageButton rewButton = findViewById(R.id.rew);
        seekbar = findViewById(R.id.seekBar);
        spindle1 = findViewById(R.id.spin1);
        spindle2 = findViewById(R.id.spin2);
        reel1 = findViewById(R.id.reel1);
        reel2 = findViewById(R.id.reel2);
        cassette = findViewById(R.id.cassette);
        releSwitch = findViewById(R.id.releSwitch);
        telon = findViewById(R.id.telon);
        //toolbarcassete = (Toolbar) findViewById(R.id.toolbarcassete);
        //setSupportActionBar(toolbarcassete);
        getSupportActionBar().setTitle(b.getString("name"));
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        caratulaText.setText(b.getString("name"));

        // Correct layout
        seekbar.setPadding(8,0,8,0);

        animator = (ValueAnimator) AnimatorInflater.loadAnimator(getApplicationContext(), R.animator.rotate_animator);
        animator2 = (ValueAnimator) AnimatorInflater.loadAnimator(getApplicationContext(), R.animator.rotate_animator);
        animator.setTarget(spindle1);
        animator2.setTarget(spindle2);

        playButton.setOnClickListener(this);
        pauseButton.setOnClickListener(this);
        ffwButton.setOnClickListener(this);
        rewButton.setOnClickListener(this);

        cdtParsing = new CDT2WAV();
        cdtParsing.execute(cdtfilename);

        if (trackAudio != null) trackAudio.cancel(true);
        trackAudio = new TrackAudio(this);

        PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
                "MyApp::MyWakelockTag");
        wakeLock.acquire();

    }

    @Override
    protected void onDestroy() {
        trackAudio.cancel(true);
        wakeLock.release();
        super.onDestroy();
    }

    @Override
    protected void onResume() {
        super.onResume();

        IntentFilter filter = new IntentFilter(Intent.ACTION_HEADSET_PLUG);
        getApplicationContext().registerReceiver(mReceiver, filter);
    }

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            Log.d("Mio", "New intent: " + action);
            if (Intent.ACTION_HEADSET_PLUG.equals(action)) {
                Log.d("Mio", "state: " + intent.getIntExtra("state", -1));
                Log.d("Mio", "microphone: " + intent.getIntExtra("microphone", -1));
            }
        }
    };

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        super.onKeyDown(keyCode, event);
        if(event.getRepeatCount() == 0 && keyCode == KeyEvent.KEYCODE_HEADSETHOOK){
            releSwitch.setChecked(true);
            releactive = true;
            if (playpressed) {
                trackAudio.execute();
                mediaPlayer.start(); // no need to call prepare(); create() does that for you
                animator.start();
                animator2.start();
            }
            return true;
        }

        if (keyCode == KeyEvent.KEYCODE_BACK) {
            finish();
        }
        return false;
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if(keyCode == KeyEvent.KEYCODE_HEADSETHOOK){
            releSwitch.setChecked(false);
            releactive = false;
            mediaPlayer.pause();
            animator.cancel();
            animator2.cancel();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.play:
                completed = false;
                playButton.setEnabled(false);
                pauseButton.setEnabled(true);
                playpressed = true;
                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
                if (releactive || !prefs.getBoolean("remcontrol_enabled", false)) {
                    trackAudio.execute();
                    mediaPlayer.start(); // no need to call prepare(); create() does that for you
                    animator.start();
                    animator2.start();
                }
                break;

            case R.id.pause:
                playpressed = false;
                playButton.setEnabled(true);
                pauseButton.setEnabled(false);
                mediaPlayer.pause();
                animator.cancel();
                animator2.cancel();
                break;

            case R.id.ffw:
                playpressed = false;
                playButton.setEnabled(true);
                pauseButton.setEnabled(false);
                mediaPlayer.pause();
                animator.cancel();
                animator2.cancel();
                {
                    int posNow = mediaPlayer.getCurrentPosition();
                    //Log.i("Mio", "Voy a " + blockStarts[2]);
                    for (int i = 0; i < blockStarts.length; i++) {
                        if (posNow < blockStarts[i]) {
                            lastPosition = blockStarts[i];
                            break;
                        }
                    }
                    Log.i("Mio", "Voy a " + lastPosition);
                }
                mediaPlayer.seekTo(lastPosition);
                seekbar.setProgress(lastPosition);
                break;

            case R.id.rew:
                playpressed = false;
                playButton.setEnabled(true);
                pauseButton.setEnabled(false);
                mediaPlayer.pause();
                animator.cancel();
                animator2.cancel();
                {
                    int posNow = mediaPlayer.getCurrentPosition();
                    for (int i = 0; i < blockStarts.length; i++) {
                        if (posNow <= blockStarts[i]) {
                            lastPosition = blockStarts[i-1];
                            break;
                        }
                    }
                    Log.i("Mio", "Voy a " + lastPosition);
                }
                mediaPlayer.seekTo(lastPosition);
                seekbar.setProgress(lastPosition);
                break;
        }
    }

    private void initDataToSeekbar(ArrayList<Integer> durations, ArrayList<Integer> types, int totalduration) {

        ArrayList<ProgressItem> progressItemList = new ArrayList<>();
        int[] colorList = {android.R.color.holo_blue_light,android.R.color.holo_red_light,android.R.color.holo_green_light,android.R.color.holo_orange_light};

        for (int i = 0; i < durations.size(); i++) {
            ProgressItem mProgressItem = new ProgressItem();
            mProgressItem.progressItemPercentage = durations.get(i)*100F/totalduration;
            //Log.i("Mio", "Duracion " + durations.get(i)*100F/totalduration);

            mProgressItem.color = colorList[types.get(i)];
            progressItemList.add(mProgressItem);
        }

        seekbar.initData(progressItemList);
        seekbar.invalidate();
        //seekbar.setPadding(0, 0, 0, 0);
        seekbar.setVisibility(View.VISIBLE);
        telon.setVisibility(View.INVISIBLE);
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        lastPosition = 0;
        completed = true;
        playpressed = false;
        seekbar.setProgress(0);
    }

    private class CDT2WAV extends AsyncTask<String,Integer,Void> {

        private ArrayList<Integer> blockDurations = new ArrayList<>();
        private ArrayList<Integer> blockTypes = new ArrayList<>();
        private int lenaudio;
        private TextView cargandoTexto;

        private String wavfilename;
        private boolean wavalreadycreated = false;

        @Override
        protected  void onPreExecute(){
            Log.i("Mio", "PreEx");

            cargandoTexto = findViewById(R.id.cargandoTexto);

            // Remove and check all files
            wavfilename = cdtfilename.substring(0, cdtfilename.length()-4) + ".wav";
            for(File tempFile : getApplicationContext().getFilesDir().listFiles()) {
                if (!tempFile.getName().equals(wavfilename)) {
                    tempFile.delete();
                } else {
                    wavalreadycreated = true;
                }
            }
            super.onPreExecute();
        }

        @Override
        protected Void doInBackground(String... filenames) {

            Log.i("Mio", "Corriendo bg con " + filenames[0]);

            // Salir si el wav ya está
            if (wavalreadycreated) {
                Log.i("Mio", "Ya hecho, saliendo");
                return null;
            }

            File cdtfile  = new File(Environment.getExternalStorageDirectory().toString() + "/Caseta/" + filenames[0]);
            File rawFile = new File(getApplicationContext().getFilesDir(), "temp.raw");
            BufferedInputStream buf = null;
            FileOutputStream fos = null;

            try {
                buf = new BufferedInputStream(new FileInputStream(cdtfile));
                fos = new FileOutputStream(rawFile);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }


            int pilotpulse;
            int pilottone;
            int synpulse;
            int zeropulse;
            int onepulse;
            int datalen;

            int lastDuration = 0;

            try {
                Log.d("Mio", "Buffer de " + buf.available() + " bytes.");

                {

                    byte[] signature;
                    byte[] majRev;
                    byte[] minRev;

                    // Leer cabecera
                    signature = new byte[7];
                    buf.read(signature, 0, 7);
                    String cabeza = new String(signature, "UTF-8");

                    // Leer version
                    buf.skip(1);
                    majRev = new byte[1];
                    buf.read(majRev, 0, 1);
                    minRev = new byte[1];
                    buf.read(minRev, 0, 1);
                    Log.d("Mio", cabeza + " v" + ((int) majRev[0]) + "." + ((int) minRev[0]));
                }

                // Leer bloques
                bloqueloop: while (buf.available() > 0) {

                    byte[] blockType = new byte[1];
                    buf.read(blockType, 0, 1);
                    Log.d("Mio", "Bloque: " + blockType[0]);

                    switch (blockType[0]) {

                        // Standard block
                        case 0x10: {
                            ArrayList<Byte> dataChunk = new ArrayList<>();
                            Log.i("Mio", "Standard");

                            // 0x00-0x01: Pause after this block (ms.) {1000}
                            byte[] pauAfter = new byte[2];
                            buf.read(pauAfter, 0, 2);
                            short pauseafter = getShortFromLEBytes(pauAfter);
                            Log.v("Mio", "Pause after: " + pauseafter);

                            // 0x02-0x03: Length of data that follow
                            byte[] lenData = new byte[2];
                            buf.read(lenData, 0, 2);
                            datalen = getShortFromLEBytes(lenData);
                            Log.v("Mio", "Data length: " + datalen);

                            // 0x04-END: Data as in .TAP files
                            byte[] databytes = new byte[datalen];
                            buf.read(databytes, 0, datalen);

                            // Generar senal piloto
                            if (databytes[0] == 0x0) {
                                appendPilot(2168,8064,dataChunk);
                            } else {
                                appendPilot(2168,3220,dataChunk);
                            }

                            // Generar senal syn
                            appendSyn(667,735,dataChunk);

                            // Generar datos
                            appendData(855,1710,(byte) 8,databytes,dataChunk);

                            // Anadir duracion de bloques de datos
                            blockDurations.add(dataChunk.size());
                            if (databytes[0] == 0x2C) {
                                String chunkName = "";
                                for (int i = 1; i < 17; i++) {
                                    if (databytes[i] != 0) {
                                        chunkName += (char) databytes[i];
                                    }
                                }
                                Log.v("Mio", "This is a header block: " + chunkName);
                                blockTypes.add(1);
                            } else {
                                blockTypes.add(2); // TODO: crear constantes
                            }

                            // Generar pausa
                            int lenpauseafter = (int) Math.round(pauseafter * 44.1);
                            for (int i = 0; i < lenpauseafter; i++) {
                                dataChunk.add((byte) 128);
                            }

                            // Anadir duracion de pausa
                            blockDurations.add(lenpauseafter);
                            blockTypes.add(0);  //TODO: crear constantes

                            //Log.v("Mio", "Todavian quedan " + buf.available() + " bytes.");

                            lenaudio += dataChunk.size();

                            byte[] byteDataChunk;
                            byteDataChunk = toByteArray(dataChunk);

                            fos.write(byteDataChunk);
                        }
                        break;

                        // Turbo block
                        case 0x11: {
                            ArrayList<Byte> dataChunk = new ArrayList<>();
                            Log.i("Mio", "Turbo");

                            // 0x00-0x01: Length of PILOT pulse {2168}
                            byte[] lenPilot = new byte[2];
                            buf.read(lenPilot, 0, 2);
                            pilotpulse = getShortFromLEBytes(lenPilot);
                            Log.v("Mio", "Pilot length: " + pilotpulse);

                            // 0x02-0x03: Length of SYNC first pulse {667}
                            byte[] lenSyn1p = new byte[2];
                            buf.read(lenSyn1p, 0, 2);
                            synpulse = getShortFromLEBytes(lenSyn1p);
                            Log.v("Mio", "Syn1 length: " + synpulse);

                            // 0x04-0x05: Length of SYNC second pulse {735}
                            byte[] lenSyn2p = new byte[2];
                            buf.read(lenSyn2p, 0, 2);
                            short syn2len = getShortFromLEBytes(lenSyn2p);
                            Log.v("Mio", "Syn2 length: " + syn2len);

                            // 0x06-0x07: Length of ZERO bit pulse {855}
                            byte[] lenZero = new byte[2];
                            buf.read(lenZero, 0, 2);
                            zeropulse = getShortFromLEBytes(lenZero);
                            Log.v("Mio", "Zero length: " + zeropulse);

                            // 0x08-0x09: Length of ONE bit pulse {1710}
                            byte[] lenOne = new byte[2];
                            buf.read(lenOne, 0, 2);
                            onepulse = getShortFromLEBytes(lenOne);
                            Log.v("Mio", "One length: " + onepulse);

                            // 0x0A-0x0B: Length of PILOT tone (number of pulses) {8063 header (flag<128), 3223 data (flag>=128)}
                            byte[] lenPilotTone = new byte[2];
                            buf.read(lenPilotTone, 0, 2);
                            pilottone = getShortFromLEBytes(lenPilotTone);
                            Log.v("Mio", "Pilot tone length: " + pilottone);

                            // 0x0C: Used bits in the last byte (other bits should be 0) {8}
                            byte[] usedLast = new byte[1];
                            buf.read(usedLast,0,1); //TODO: implement last used bits (correctly ?)

                            // 0x0D-0x0E: Pause after this block (ms.) {1000}
                            byte[] pauAfter = new byte[2];
                            buf.read(pauAfter, 0, 2);
                            short pauseafter = getShortFromLEBytes(pauAfter);
                            Log.v("Mio", "Pause after: " + pauseafter);

                            // 0x0F-0x11: Length of data that follow
                            byte[] lenData = new byte[4];
                            buf.read(lenData, 0, 3);
                            datalen = getIntFromLEBytes(lenData);
                            Log.v("Mio", "Data length: " + datalen);

                            // 0x12-END: Data as in .TAP files
                            byte[] databytes = new byte[datalen];
                            buf.read(databytes, 0, datalen);

                            // Generar senal piloto
                            appendPilot(pilotpulse,pilottone,dataChunk);

                            // Generar senal syn
                            appendSyn(synpulse,syn2len,dataChunk);

                            // Generar datos
                            appendData(zeropulse,onepulse,usedLast[0],databytes,dataChunk);

                            // Anadir duracion de bloques de datos
                            blockDurations.add(dataChunk.size());
                            if (databytes[0] == 0x2C) {
                                String chunkName = "";
                                for (int i = 1; i < 17; i++) {
                                    if (databytes[i] != 0) {
                                        chunkName += (char) databytes[i];
                                    }
                                }
                                Log.v("Mio", "This is a header block: " + chunkName);
                                blockTypes.add(1);
                            } else {
                                blockTypes.add(2); // TODO: crear constantes
                            }

                            // Generar pausa
                            int lenpauseafter = (int) Math.round(pauseafter * 44.1);
                            for (int i = 0; i < lenpauseafter; i++) {
                                dataChunk.add((byte) 128);
                            }

                            // Anadir duracion de pausa
                            blockDurations.add(lenpauseafter);
                            blockTypes.add(0);  //TODO: crear constantes

                            //Log.v("Mio", "Todavian quedan " + buf.available() + " bytes.");

                            lenaudio += dataChunk.size();

                            byte[] byteDataChunk;
                            byteDataChunk = toByteArray(dataChunk);

                            fos.write(byteDataChunk);
                        }
                            break;

                        // Pure tone
                        case 0x12: {
                            ArrayList<Byte> dataChunk = new ArrayList<>();
                            Log.i("Mio", "Tone");

                            // 0x00-0x01: 	Length of one pulse in T-states
                            byte[] lenPilot = new byte[2];
                            buf.read(lenPilot, 0, 2);
                            pilotpulse = getShortFromLEBytes(lenPilot);
                            Log.v("Mio", "Pilot length: " + pilotpulse);

                            // 0x02-0x03: Number of pulses
                            byte[] lenPilotTone = new byte[2];
                            buf.read(lenPilotTone, 0, 2);
                            pilottone = getShortFromLEBytes(lenPilotTone);
                            Log.v("Mio", "Pilot tone length: " + pilottone);

                            // Generar senal piloto
                            appendPilot(pilotpulse,pilottone,dataChunk);

                            // Anadir duracion de bloques de datos
                            blockDurations.add(dataChunk.size());
                            blockTypes.add(1); // TODO: Crear const para tono

                            lenaudio += dataChunk.size();

                            byte[] byteDataChunk;
                            byteDataChunk = toByteArray(dataChunk);

                            fos.write(byteDataChunk);
                        }
                        break;

                        // Pulse sequence
                        case 0x13: {
                            ArrayList<Byte> dataChunk = new ArrayList<>();
                            Log.i("Mio", "Pulses");

                            // 0x00-0x01: 	Number of pulses
                            byte[] npulses = new byte[1];
                            buf.read(npulses, 0, 1);
                            int inpulses = npulses[0] & 0xFF;
                            Log.i("Mio", "N pul: " + inpulses);

                            // 0x02-END: 	Pulses' lengths
                            for (int i = 0; i < inpulses; i++) {
                                byte[] wordlen = new byte[2];
                                buf.read(wordlen, 0, 2);
                                int plen = Math.round(((float) getShortFromLEBytes(wordlen)) * FS / TSTATE);
                                Log.i("Mio", "Pulao: " + plen);

                                // TODO: Do I need to inverse polarity w.r.t. previous blocks?
                                byte invPolarity = -1;
                                if(dataChunk.size() > 0 && dataChunk.get(dataChunk.size()-1) == -1) invPolarity = 0; // To invert polarity if needed
                                for (short j = 0; j < plen; j++) {
                                    dataChunk.add(invPolarity);
                                }
                            }

                            // Anadir duracion de bloques de datos
                            blockDurations.add(dataChunk.size());
                            blockTypes.add(1); // TODO: Crear const para tono

                            lenaudio += dataChunk.size();

                            byte[] byteDataChunk;
                            byteDataChunk = toByteArray(dataChunk);

                            fos.write(byteDataChunk);

                        }
                        break;

                        // Pure data block
                        case 0x14: {
                            ArrayList<Byte> dataChunk = new ArrayList<>();
                            Log.i("Mio", "Pure data");

                            // 0x00-0x01: Length of ZERO bit pulse
                            byte[] lenZero = new byte[2];
                            buf.read(lenZero, 0, 2);
                            zeropulse = getShortFromLEBytes(lenZero);
                            Log.v("Mio", "Zero length: " + zeropulse);

                            // 0x02-0x03: Length of ONE bit pulse
                            byte[] lenOne = new byte[2];
                            buf.read(lenOne, 0, 2);
                            onepulse = getShortFromLEBytes(lenOne);
                            Log.v("Mio", "One length: " + onepulse);

                            // 0x04: Used bits in the last byte (other bits should be 0) {8}
                            byte[] usedLast = new byte[1];
                            buf.read(usedLast,0,1); //TODO: implement last used bits

                            // 0x05-0x06: Pause after this block (ms.) {1000}
                            byte[] pauAfter = new byte[2];
                            buf.read(pauAfter, 0, 2);
                            short pauseafter = getShortFromLEBytes(pauAfter);
                            Log.v("Mio", "Pause after: " + pauseafter);

                            // 0x07-0x09: Length of data that follow
                            byte[] lenData = new byte[4];
                            buf.read(lenData, 0, 3);
                            datalen = getIntFromLEBytes(lenData);
                            Log.v("Mio", "Data length: " + datalen);

                            // 0x0A-END: Data as in .TAP files
                            byte[] databytes = new byte[datalen];
                            buf.read(databytes, 0, datalen);

                            // Generar datos
                            appendData(zeropulse,onepulse,usedLast[0],databytes,dataChunk);

                            // Anadir duracion de bloques de datos //TODO: Pensar en como hacer esto
                            blockDurations.add(dataChunk.size());
                            if (databytes[0] == 0x2C) {
                                String chunkName = "";
                                for (int i = 1; i < 17; i++) {
                                    if (databytes[i] != 0) {
                                        chunkName += (char) databytes[i];
                                    }
                                }
                                Log.v("Mio", "This is a header block: " + chunkName);
                                blockTypes.add(1);
                            } else {
                                blockTypes.add(2); // TODO: crear constantes
                            }

                            // Generar pausa
                            int lenpauseafter = (int) Math.round(pauseafter * 44.1);
                            for (int i = 0; i < lenpauseafter; i++) {
                                dataChunk.add((byte) 128);
                            }

                            // Anadir duracion de pausa
                            blockDurations.add(lenpauseafter);
                            blockTypes.add(0);  //TODO: crear constantes

                            //Log.v("Mio", "Todavian quedan " + buf.available() + " bytes.");

                            lenaudio += dataChunk.size();

                            byte[] byteDataChunk;
                            byteDataChunk = toByteArray(dataChunk);

                            fos.write(byteDataChunk);
                        }
                        break;

                        // Pausa
                        case 0x20: {
                            Log.i("Mio", "Bloque de pausa.");
                            byte[] pauBlock = new byte[2];
                            buf.read(pauBlock, 0, 2);
                            short pausebefore = getShortFromLEBytes(pauBlock);
                            Log.v("Mio", "Pause before: " + pausebefore);
                            // Generar pausa
                            int pausalen = (int) Math.round(pausebefore * 44.1);
                            byte[] byPause = new byte[pausalen];
                            Arrays.fill(byPause, (byte) 128);
                            fos.write(byPause);

                            // Anadir duracion de pausa
                            blockDurations.add(pausalen);
                            Log.v("Mio", "Anadida pausa de: " + lastDuration);
                            lastDuration = pausalen;
                            lenaudio += pausalen;
                            blockTypes.add(0);  //TODO: crear constantes
                        }
                            break;

                        // Group start
                        case 0x21: {
                            // 0x00: Length of the group name string
                            byte[] grnamlen = new byte[1];
                            buf.read(grnamlen, 0, 1);

                            // 0x01-END: Group name in ASCII format (please keep it under 30 characters long)
                            byte[] grnam = new byte[(int) grnamlen[0]];
                            buf.read(grnam, 0, (int) grnamlen[0]);

                            String groupName = new String(grnam);
                            Log.i("Mio", "Group start: " + groupName);
                        }
                        break;

                        // Group end
                        case 0x22:
                            Log.i("Mio", "Group end");
                            // TODO: Para que vale esto?
                            break;

                        // Descripcion
                        case 0x30:
                            byte[] desclen = new byte[1];
                            buf.read(desclen, 0, 1);
                            Log.i("Mio", "Descripcion len: " + (int) desclen[0] + " o " + desclen[0]);
                            byte[] desc = new byte[(int) desclen[0]];
                            buf.read(desc,0,(int) desclen[0]);
                            String descstr = new String(desc);
                            Log.i("Mio", "Descripcion: " + descstr);
                            break;

                        default:
                            Log.e("Mio" , "Bloque no manejado");
                            break bloqueloop;


                    }

                    publishProgress(blockDurations.size());

                }

                // Anadir pausa de 1s al final
                // Generar pausa
                byte[] finalPauseDataChunk = new byte[44100];
                for (int i = 0; i < finalPauseDataChunk.length; i++) {
                    finalPauseDataChunk[i] = (byte) 128;
                }

                // Anadir duracion de pausa
                blockDurations.add(44100);
                blockTypes.add(0);  //TODO: crear constantes

                //Log.v("Mio", "Todavian quedan " + buf.available() + " bytes.");

                lenaudio += 44100;
                fos.write(finalPauseDataChunk);

                fos.close();
                File wavfile = new File(getApplicationContext().getFilesDir(), wavfilename);
                //rawToWave(bigArray,wavfile);
                writeWAVheader(rawFile,wavfile);
                rawFile.delete();

                // Cerrar buffer
                Log.d("Mio", "No hay mas bloques");
                buf.close();

            } catch (UnsupportedEncodingException e1) {
                e1.printStackTrace();
            } catch (IOException e1) {
                e1.printStackTrace();
            }

            // Calcular inicio de los bloques
            blockStarts = new int[blockDurations.size()];

            for (int i = 1; i < blockStarts.length; i++) {
                blockStarts[i] = blockStarts[i-1] + Math.round(blockDurations.get(i-1)/44.1F);
            }

            return null;
        }

        private void appendPilot(int lenPulse, int lenTones, ArrayList<Byte> dataArray) {
            int lenpiloto = Math.round(((float) lenPulse) * FS / TSTATE);
            for (int i = 0; i < (lenpiloto * lenTones); i++) {
                if ((i % (lenpiloto * 2)) < lenpiloto) {
                    dataArray.add((byte) -1);
                } else {
                    dataArray.add((byte) 0);
                }
            }
        }

        private void appendSyn(int lenPulse1, int lenPulse2, ArrayList<Byte> dataArray) {
            int lensyn1 = Math.round(((float) lenPulse1) * FS / TSTATE);
            byte invPolarity = 0;
            if(dataArray.size() > 0 && dataArray.get(dataArray.size()-1) == -1) invPolarity = (byte) -1; // To invert polarity if needed
            for (int i = 0; i < lensyn1; i++) {
                dataArray.add((byte) (-1 ^ invPolarity));
            }
            int lensyn2 = Math.round(((float) lenPulse2) * FS / TSTATE);
            for (int i = 0; i < lensyn2; i++) {
                dataArray.add(invPolarity);
            }
        }

        private void appendData(int lenZeroPulse, int lenOnePulse, byte usedLast, byte[] data, ArrayList<Byte> dataArray) {
            //TODO: Optimize code
            byte invPolarity = 0;
            if(dataArray.size() > 0 && dataArray.get(dataArray.size()-1) == -1) invPolarity = (byte) 0xFF; // To invert polarity if needed
            // Generar senal zero
            int lenzero = Math.round(((float) lenZeroPulse) * FS / TSTATE);
            byte[] zero = new byte[2 * lenzero];
            for (int i = 0; i < 2 * lenzero; i++) {
                if ((i % (lenzero * 2)) < lenzero) {
                    zero[i] = (byte) (-1 ^ invPolarity); // This xor operation inverses the byte if needed (not to be the same as the previous)
                } else {
                    zero[i] = invPolarity;
                }
            }

            // Generar senal one
            int lenone = Math.round(((float) lenOnePulse) * FS / TSTATE);
            byte[] one = new byte[2 * lenone];
            for (int i = 0; i < 2 * lenone; i++) {
                if ((i % (lenone * 2)) < lenone) {
                    one[i] = (byte) (-1 ^ invPolarity);
                } else {
                    one[i] = invPolarity;
                }
            }

            // Generar senal datos
            for (int j = 0; j < data.length-1; j++) {
                for (int i = 7; i >= 0; i--) {
                    int bit = ((data[j] >> i) & 1);
                    if (bit == 0) {
                        //at.write(zero,0,zero.length);
                        for (byte b : zero) {
                            dataArray.add(b);
                        }
                    } else {
                        for (byte b : one) {
                            dataArray.add(b);
                        }
                    }
                }
            }

            // Ultimo byte
            for (int i = 7; i >= 8-usedLast; i--) {
                int bit = ((data[data.length-1] >> i) & 1);
                if (bit == 0) {
                    //at.write(zero,0,zero.length);
                    for (byte b : zero) {
                        dataArray.add(b);
                    }
                } else {
                    for (byte b : one) {
                        dataArray.add(b);
                    }
                }
            }

            // Acabar en 0
            if (invPolarity != 0) {
                for (int i = 0; i < 44; i++) {
                    dataArray.add((byte) 0);
                }
            }


        }

        private void appendPause(int lenms, ArrayList<Byte> dataArray) {
            int lenpauseafter = (int) Math.round(lenms * 44.1);
            for (int i = 0; i < lenpauseafter; i++) {
                dataArray.add((byte) 128);
            }
        }

        private void writeWAVheader(final File rawFile, final File waveFile) throws IOException {

            int lenRaw = (int) rawFile.length();
            byte[] rawDataBytes = new byte[lenRaw];

            BufferedInputStream bufis = null;
            DataOutputStream output = null;
            try {
                bufis = new BufferedInputStream(new FileInputStream(rawFile));
                bufis.read(rawDataBytes, 0, lenRaw);
                bufis.close();
                output = new DataOutputStream(new FileOutputStream(waveFile));
                // WAVE header
                // see http://ccrma.stanford.edu/courses/422/projects/WaveFormat/
                writeString(output, "RIFF"); // chunk id
                writeInt(output, 36 + lenRaw); // chunk size
                writeString(output, "WAVE"); // format
                writeString(output, "fmt "); // subchunk 1 id
                writeInt(output, 16); // subchunk 1 size
                writeShort(output, (short) 1); // audio format (1 = PCM)
                writeShort(output, (short) 1); // number of channels
                writeInt(output, 44100); // sample rate
                writeInt(output, 44100); // byte rate
                writeShort(output, (short) 1); // block align
                writeShort(output, (short) 8); // bits per sample
                writeString(output, "data"); // subchunk 2 id
                writeInt(output, lenRaw); // subchunk 2 size

                output.write(rawDataBytes);
            } finally {
                if (output != null) {
                    output.close();
                }
            }
        }

        private byte[] toByteArray(List<Byte> list){
            byte[] ret = new byte[list.size()];
            for(int i = 0;i < ret.length;i++)
                ret[i] = list.get(i);
            return ret;
        }

        private void writeInt(final DataOutputStream output, final int value) throws IOException {
            output.write(value);
            output.write(value >> 8);
            output.write(value >> 16);
            output.write(value >> 24);
        }

        private void writeShort(final DataOutputStream output, final short value) throws IOException {
            output.write(value);
            output.write(value >> 8);
        }

        private void writeString(final DataOutputStream output, final String value) throws IOException {
            for (int i = 0; i < value.length(); i++) {
                output.write(value.charAt(i));
            }
        }

        private int getIntFromLEBytes(byte[] inData) {
            ByteBuffer buffer = ByteBuffer.wrap(inData);
            buffer.order(ByteOrder.LITTLE_ENDIAN);
            return buffer.getInt();
        }

        private short getShortFromLEBytes(byte[] inData) {
            ByteBuffer buffer = ByteBuffer.wrap(inData);
            buffer.order(ByteOrder.LITTLE_ENDIAN);
            return buffer.getShort();
        }

        @Override
        protected void onProgressUpdate(Integer... progreso){
            cargandoTexto.setText("Procesado bloque " + progreso[0]);

        }

        @Override
        protected void onPostExecute(Void result) {
            mediaPlayer = MediaPlayer.create(getApplicationContext(),
                    Uri.fromFile(new File(getApplicationContext().getFilesDir().toString() + "/" + wavfilename)));

            float offset = 0.3288f*cassette.getHeight();
            spindle1.setX(spindle1.getX() - offset);
            spindle2.setX(spindle2.getX() + offset);
            reel1.setX(reel1.getX() - offset);
            reel2.setX(reel2.getX() + offset);

            ProgressBar cargando = findViewById(R.id.cargandoCassete);
            cargando.setVisibility(View.GONE);

            //final Drawable drawable = ResourcesCompat.getDrawable(getResources(), R.drawable.cassette_oscuro, wrapper.getTheme());
            final int[] cassetes_op = {R.drawable.cassette_claro, R.drawable.cassette_oscuro};
            Random r = new Random();

            cassette.setImageResource(cassetes_op[r.nextInt(cassetes_op.length)]);
            cargandoTexto.setVisibility(View.GONE);
            /*spindle1.setVisibility(View.VISIBLE);
            spindle2.setVisibility(View.VISIBLE);
            reel1.setVisibility(View.VISIBLE);
            reel2.setVisibility(View.VISIBLE);
            cassette.setVisibility(View.VISIBLE);
            caratulaText.setVisibility(View.VISIBLE);*/
            /*Group cassete_group = findViewById(R.id.cassette_group);
            cassete_group.setVisibility(View.VISIBLE);*/

            // Colocar etiquetas de side y tape
            TextView side_text = findViewById(R.id.side_text);
            TextView tape_text = findViewById(R.id.tape_text);
            offset = 0.376f*cassette.getWidth();
            side_text.setX(side_text.getX() + offset);
            tape_text.setX(tape_text.getX() - offset);
            //side_text.setVisibility(View.VISIBLE);
            //tape_text.setVisibility(View.VISIBLE);
            //telon.setVisibility(View.INVISIBLE);


            playButton.setEnabled(true);
            //mediaPlayer.setOnCompletionListener(CassetteActivity);
            seekbar.setMax(mediaPlayer.getDuration());
            initDataToSeekbar(blockDurations, blockTypes, lenaudio);

        }

    }

    private static class TrackAudio extends AsyncTask<Void,Integer,Void> {

        private WeakReference<CassetteActivity> activityReference;
        private float total;
        private long tiempo;

        TrackAudio(CassetteActivity context) {
            activityReference = new WeakReference<>(context);
        }

        @Override
        protected  void onPreExecute(){
            super.onPreExecute();
            total = mediaPlayer.getDuration();
        }

        @Override
        protected Void doInBackground(Void... voids) {

            CassetteActivity activity = activityReference.get();

            while (!mediaPlayer.isPlaying()) {} //Wait until it is indeed playing

            long tiempo_last = Calendar.getInstance().getTimeInMillis(); // Get time of playing start

            while (mediaPlayer.isPlaying()) {

                try {

                    if (this.isCancelled()) return null;
                    Thread.sleep(50); // Wait 50 ms (25 fps)

                    tiempo = Calendar.getInstance().getTimeInMillis();
                    Log.i("Mio", "Abur: " + tiempo);

                    // Set position to lastPosition + elapsed time
                    publishProgress(Math.round((activity.lastPosition + (tiempo - tiempo_last)) ));

                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            return null;
        }

        @Override
        protected void onProgressUpdate(Integer... progreso){
            CassetteActivity CActivity = activityReference.get();
            CActivity.seekbar.setProgress(progreso[0]);

            // Change reel sizes
            CActivity.reel1.setScaleX(1.2f-(progreso[0]/total)*0.2f);
            CActivity.reel1.setScaleX(1.2f-(progreso[0]/total)*0.2f);
            CActivity.reel2.setScaleX(1f+(progreso[0]/total)*0.2f);
            CActivity.reel2.setScaleX(1f+(progreso[0]/total)*0.2f);
        }

        @Override
        protected void onPostExecute(Void result) {

            CassetteActivity CActivity = activityReference.get();

            if (!CActivity.completed) {
                // If not completed get corrected lastPosition
                CActivity.lastPosition = mediaPlayer.getCurrentPosition();
                CActivity. seekbar.setProgress(CActivity.lastPosition);
            } else {
                CActivity.lastPosition = 0;
            }

            Log.i("Mio", "Se A Cabo ");
            if (!CActivity.playpressed) {
                CActivity.playButton.setEnabled(true);
                CActivity.pauseButton.setEnabled(false);
            }
            CActivity.animator.cancel();
            CActivity.animator2.cancel();

        }

    }


}
