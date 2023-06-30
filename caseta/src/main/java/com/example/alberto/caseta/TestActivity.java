package com.example.alberto.caseta;

import android.animation.AnimatorInflater;
import android.animation.ValueAnimator;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Switch;

import java.io.BufferedInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;


public class TestActivity extends AppCompatActivity implements View.OnClickListener, MediaPlayer.OnCompletionListener {

    private static final int TSTATE = 3500000;
    private static final int FS = 44100;

    private static MediaPlayer mediaPlayer;
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
    private CustomSeekBar seekbar;
    private int lastPosition = 0;

    private String cdtfilename;
    private boolean playpressed;
    private boolean releactive;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        Bundle b = getIntent().getExtras();
        cdtfilename = b.getString("filename");

        playButton = findViewById(R.id.play);
        pauseButton = findViewById(R.id.pause);
        seekbar = findViewById(R.id.seekBar);
        spindle1 = findViewById(R.id.spin1);
        spindle2 = findViewById(R.id.spin2);
        reel1 = findViewById(R.id.reel1);
        reel2 = findViewById(R.id.reel2);
        cassette = findViewById(R.id.cassette);
        releSwitch = findViewById(R.id.releSwitch);

        // Correct layout
        seekbar.setPadding(8,0,8,0);

        animator = (ValueAnimator) AnimatorInflater.loadAnimator(getApplicationContext(), R.animator.rotate_animator);
        animator2 = (ValueAnimator) AnimatorInflater.loadAnimator(getApplicationContext(), R.animator.rotate_animator);
        animator.setTarget(spindle1);
        animator2.setTarget(spindle2);

        playButton.setOnClickListener(this);
        pauseButton.setOnClickListener(this);

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
        Log.i("Mio", "Algo abajo");
        if(event.getRepeatCount() == 0 && keyCode == KeyEvent.KEYCODE_HEADSETHOOK){
            releSwitch.setChecked(true);
            releactive = true;
            if (playpressed) {
                new TestActivity.TrackAudio().execute();
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

    private byte[] toByteArray(List<Byte> list){
        byte[] ret = new byte[list.size()];
        for(int i = 0;i < ret.length;i++)
            ret[i] = list.get(i);
        return ret;
    }

    private void rawToWave(byte[] rawData, final File waveFile) throws IOException {

        DataOutputStream output = null;
        try {
            output = new DataOutputStream(new FileOutputStream(waveFile));
            // WAVE header
            // see http://ccrma.stanford.edu/courses/422/projects/WaveFormat/
            writeString(output, "RIFF"); // chunk id
            writeInt(output, 36 + rawData.length); // chunk size
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
            writeInt(output, rawData.length); // subchunk 2 size

            output.write(rawData);
        } finally {
            if (output != null) {
                output.close();
            }
        }
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

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.play:
                completed = false;
                playButton.setEnabled(false);
                pauseButton.setEnabled(true);
                playpressed = true;
                if (releactive) {
                    new TestActivity.TrackAudio().execute();
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
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            float offset = 0.3288f*cassette.getHeight();
            spindle1.setX(spindle1.getX() - offset);
            spindle2.setX(spindle2.getX() + offset);
            reel1.setX(reel1.getX() - offset);
            reel2.setX(reel2.getX() + offset);

            convertCDTtoWAV(cdtfilename);
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

    private void convertCDTtoWAV(String filename) {

        File cdtfile  = new File(Environment.getExternalStorageDirectory().toString() + "/Caseta/" + filename);
        BufferedInputStream buf = null;
        try {
            buf = new BufferedInputStream(new FileInputStream(cdtfile));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }


        int pilotpulse;
        int pilottone;
        int synpulse;
        int zeropulse;
        int onepulse;
        int datalen;

        ArrayList<Byte> bigData = new ArrayList<>();
        ArrayList<Integer> blockDurations = new ArrayList<>();
        ArrayList<Integer> blockTypes = new ArrayList<>();
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
            while (buf.available() > 0) {

                byte[] blockType = new byte[1];
                buf.read(blockType, 0, 1);
                Log.d("Mio", "Bloque: " + blockType[0]);

                switch (blockType[0]) {
                    // Pausa
                    case 0x20:
                        Log.d("Mio", "Bloque de pausa.");
                        byte[] pauBlock = new byte[2];
                        buf.read(pauBlock,0,2);
                        short pausebefore = getShortFromLEBytes(pauBlock);
                        Log.v("Mio", "Pause before: " + pausebefore);
                        // Generar pausa
                        for (int i = 0; i < Math.round(pausebefore * 44.1); i++) {
                            bigData.add((byte) 0);
                        }

                        // Anadir duracion de pausa
                        blockDurations.add(bigData.size() - lastDuration);
                        Log.v("Mio", "Anadida pausa de: " + lastDuration + " a " + bigData.size() + ". Duracion: " + blockDurations.get(blockDurations.size()-1));
                        lastDuration = bigData.size();
                        blockTypes.add(0);  //TODO: crear constantes
                        break;

                    // Turbo block
                    case 0x11:
                        Log.v("Mio", "Turbo");

                        byte[] lenPilot = new byte[2];
                        buf.read(lenPilot, 0, 2);
                        pilotpulse = getShortFromLEBytes(lenPilot);
                        Log.v("Mio", "Pilot length: " + pilotpulse);

                        byte[] lenSyn1p = new byte[2];
                        buf.read(lenSyn1p, 0, 2);
                        synpulse = getShortFromLEBytes(lenSyn1p);
                        Log.v("Mio", "Syn1 length: " + synpulse);

                        byte[] lenSyn2p = new byte[2];
                        buf.read(lenSyn2p, 0, 2);
                        short syn2len = getShortFromLEBytes(lenSyn2p);
                        Log.v("Mio", "Syn2 length: " + syn2len);

                        byte[] lenZero = new byte[2];
                        buf.read(lenZero, 0, 2);
                        zeropulse = getShortFromLEBytes(lenZero);
                        Log.v("Mio", "Zero length: " + zeropulse);

                        byte[] lenOne = new byte[2];
                        buf.read(lenOne, 0, 2);
                        onepulse = getShortFromLEBytes(lenOne);
                        Log.v("Mio", "One length: " + onepulse);

                        byte[] lenPilotTone = new byte[2];
                        buf.read(lenPilotTone, 0, 2);
                        pilottone = getShortFromLEBytes(lenPilotTone);
                        Log.v("Mio", "Pilot tone length: " + pilottone);

                        buf.skip(1); //TODO: implement last used bits

                        byte[] pauAfter = new byte[2];
                        buf.read(pauAfter, 0, 2);
                        short pauseafter = getShortFromLEBytes(pauAfter);
                        Log.v("Mio", "Pause after: " + pauseafter);

                        byte[] lenData = new byte[4];
                        buf.read(lenData, 0, 3);
                        datalen = getIntFromLEBytes(lenData);
                        Log.v("Mio", "Data length: " + datalen);

                        // Leer bytes de datos
                        byte[] databytes = new byte[datalen];
                        buf.read(databytes, 0, datalen);

                        // Generar senal piloto
                        int lenpiloto = Math.round(((float) pilotpulse) * FS / TSTATE);
                        for (int i = 0; i < lenpiloto * pilottone; i++) {
                            if ((i % (lenpiloto * 2)) < lenpiloto) {
                                bigData.add((byte) -1);
                            } else {
                                bigData.add((byte) 0);
                            }
                        }

                        // Generar senal syn
                        int lensyn = Math.round(((float) synpulse) * FS / TSTATE);
                        for (int i = 0; i < 2 * lensyn; i++) {
                            if ((i % (lensyn * 2)) < lensyn) {
                                bigData.add((byte) -1);
                            } else {
                                bigData.add((byte) 0);
                            }
                        }

                        // Anadir duracion de piloto+syn
                        blockDurations.add(bigData.size() - lastDuration);
                        lastDuration = bigData.size();
                        blockTypes.add(1); //TODO: crear constantes

                        // Generar senal zero
                        int lenzero = Math.round(((float) zeropulse) * FS / TSTATE);
                        byte[] zero = new byte[2 * lenzero];
                        for (int i = 0; i < 2 * lenzero; i++) {
                            if ((i % (lenzero * 2)) < lenzero) {
                                zero[i] = -1;
                            } else {
                                zero[i] = 0;
                            }
                        }

                        // Generar senal one
                        int lenone = Math.round(((float) onepulse) * FS / TSTATE);
                        byte[] one = new byte[2 * lenone];
                        for (int i = 0; i < 2 * lenone; i++) {
                            if ((i % (lenone * 2)) < lenone) {
                                one[i] = -1;
                            } else {
                                one[i] = 0;
                            }
                        }

                        // Generar senal datos
                        for (byte d : databytes) {
                            for (int i = 7; i >= 0; i--) {
                                int bit = ((d >> i) & 1);
                                if (bit == 0) {
                                    //at.write(zero,0,zero.length);
                                    for (byte b : zero) {
                                        bigData.add(b);
                                    }
                                } else {
                                    for (byte b : one) {
                                        bigData.add(b);
                                    }
                                }
                            }
                        }

                        // Anadir duracion de bloques de datos
                        blockDurations.add(bigData.size() - lastDuration);
                        Log.v("Mio", "Anadido bloque de: " + lastDuration + " a " + bigData.size() + ". Duracion: " + blockDurations.get(blockDurations.size()-1));
                        lastDuration = bigData.size();
                        blockTypes.add(2); // TODO: crear constantes
                        // TODO: Distinguir entre cabeceras y datos

                        // Generar pausa
                        for (int i = 0; i < Math.round(pauseafter * 44.1); i++) {
                            bigData.add((byte) 0);
                        }

                        // Anadir duracion de pausa
                        blockDurations.add(bigData.size() - lastDuration);
                        Log.v("Mio", "Anadida pausa de: " + lastDuration + " a " + bigData.size() + ". Duracion: " + blockDurations.get(blockDurations.size()-1));
                        lastDuration = bigData.size();
                        blockTypes.add(0);  //TODO: crear constantes

                        Log.v("Mio", "Todavian quedan " + buf.available() + " bytes.");

                    case 0x14:
                        break;


                }

                byte[] bigArray;
                bigArray = toByteArray(bigData);

                File wavfile = new File(getApplicationContext().getFilesDir(), "result.wav");
                rawToWave(bigArray,wavfile);
            }

            // Cerrar buffer
            Log.d("Mio", "No hay mas bloques");
            buf.close();

        } catch (UnsupportedEncodingException e1) {
            e1.printStackTrace();
        } catch (IOException e1) {
            e1.printStackTrace();
        }

        mediaPlayer = MediaPlayer.create(getApplicationContext(),
                Uri.fromFile(new File(getApplicationContext().getFilesDir().toString() + "/result.wav")));

        playButton.setEnabled(true);
        mediaPlayer.setOnCompletionListener(this);
        seekbar.setMax(mediaPlayer.getDuration());
        initDataToSeekbar(blockDurations, blockTypes, bigData.size());

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
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        lastPosition = 0;
        completed = true;
        playpressed = false;
        seekbar.setProgress(0);
    }

    private class TrackAudio extends AsyncTask<Void,Integer,Void> {

        private float total;
        private long tiempo;

        @Override
        protected  void onPreExecute(){
            super.onPreExecute();
            total = mediaPlayer.getDuration();
        }

        @Override
        protected Void doInBackground(Void... voids) {

            while (!mediaPlayer.isPlaying()) {} //Wait until it is indeed playing

            long tiempo_last = Calendar.getInstance().getTimeInMillis(); // Get time of playing start

            while (mediaPlayer.isPlaying()) {

                try {

                    Thread.sleep(50); // Wait 50 ms (25 fps)

                    tiempo = Calendar.getInstance().getTimeInMillis();

                    // Set position to lastPosition + elapsed time
                    publishProgress(Math.round((lastPosition + (tiempo - tiempo_last)) ));

                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            return null;
        }

        @Override
        protected void onProgressUpdate(Integer... progreso){
            seekbar.setProgress(progreso[0]);

            // Change reel sizes
            reel1.setScaleX(1.2f-(progreso[0]/total)*0.2f);
            reel1.setScaleX(1.2f-(progreso[0]/total)*0.2f);
            reel2.setScaleX(1f+(progreso[0]/total)*0.2f);
            reel2.setScaleX(1f+(progreso[0]/total)*0.2f);
        }

        @Override
        protected void onPostExecute(Void result) {

            if (!completed) {
                // If not completed get corrected lastPosition
                lastPosition = mediaPlayer.getCurrentPosition();
            } else {
                lastPosition = 0;
            }

            Log.i("Mio", "Se A Cabo ");
            if (!playpressed) {
                playButton.setEnabled(true);
                pauseButton.setEnabled(false);
            }
            animator.cancel();
            animator2.cancel();

        }

    }


}

