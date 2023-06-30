package com.example.alberto.caseta;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Environment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.File;

public class GamesAdapter extends BaseAdapter {
    private final Context mContext;
    private final Game[] games;

    // 1
    public GamesAdapter(Context context, Game[] games) {
        this.mContext = context;
        this.games = games;
        //Log.i("Mio", "juegur0: " + games[0].toString());
    }

    // 2
    @Override
    public int getCount() {
        return games.length;
    }

    // 3
    @Override
    public long getItemId(int position) {
        return 0;
    }

    // 4
    @Override
    public Object getItem(int position) {
        return null;
    }

    // 5
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        // 1
        final Game game = games[position];
        //Log.i("Mio", "juegur: " + games[0].toString());

        // 2
        if (convertView == null) {
            final LayoutInflater layoutInflater = LayoutInflater.from(mContext);
            convertView = layoutInflater.inflate(R.layout.layout_game, null);
        }

        // 3
        final ImageView imageView = (ImageView)convertView.findViewById(R.id.iconogame);
        final TextView nameTextView = (TextView)convertView.findViewById(R.id.titulogame);

        // 4
        //imageView.setImageResource(game.getImageResource());
        nameTextView.setText(game.getName());

        File imgFile  = new File(Environment.getExternalStorageDirectory().toString() + "/Caseta/" + game.getImagename());

        if(imgFile.exists()){

            Bitmap myBitmap = BitmapFactory.decodeFile(imgFile.getAbsolutePath());
            imageView.setImageBitmap(myBitmap);

        }

        return convertView;
    }
}
