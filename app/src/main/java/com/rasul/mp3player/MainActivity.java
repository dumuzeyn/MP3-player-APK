package com.rasul.mp3player;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.ArrayList;

public class MainActivity extends Activity {
    private static final int PICK_AUDIO = 2001;
    private final ArrayList<Track> tracks = new ArrayList<>();
    private LinearLayout list;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 33);
        }
        tracks.addAll(TrackStore.load(this));
        buildUi();
    }

    private void buildUi() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(18, 28, 18, 18);
        root.setBackgroundColor(Color.WHITE);

        TextView title = new TextView(this);
        title.setText("MP3 Player");
        title.setTextColor(Color.BLACK);
        title.setTextSize(18);
        title.setGravity(Gravity.CENTER_VERTICAL);
        title.setTypeface(null, 1);
        root.addView(title, new LinearLayout.LayoutParams(-1, dp(42)));

        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        actions.setGravity(Gravity.CENTER);
        actions.setPadding(0, 8, 0, 12);
        root.addView(actions, new LinearLayout.LayoutParams(-1, -2));

        Button add = button("+ Добавить");
        add.setOnClickListener(view -> openPicker());
        actions.addView(add, new LinearLayout.LayoutParams(0, dp(54), 1));

        Button playAll = button("▶ Все");
        playAll.setOnClickListener(view -> playIndex(0));
        actions.addView(playAll, new LinearLayout.LayoutParams(0, dp(54), 1));

        ScrollView scroll = new ScrollView(this);
        list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        scroll.addView(list, new ScrollView.LayoutParams(-1, -2));
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));

        setContentView(root);
        renderList();
    }

    private Button button(String text) {
        Button button = new Button(this);
        button.setText(text);
        button.setTextColor(Color.BLACK);
        button.setBackgroundColor(Color.WHITE);
        return button;
    }

    private void renderList() {
        list.removeAllViews();
        if (tracks.isEmpty()) {
            TextView empty = rowText("Добавьте MP3 или другой аудиофайл", "Музыка будет играть даже после свайпа приложения из списка задач.");
            list.addView(empty);
            return;
        }
        for (int index = 0; index < tracks.size(); index++) {
            final int trackIndex = index;
            Track track = tracks.get(index);
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(0, 10, 0, 10);

            TextView text = rowText(track.title, track.artist);
            row.addView(text, new LinearLayout.LayoutParams(0, dp(70), 1));

            Button play = button("▶");
            play.setOnClickListener(view -> playIndex(trackIndex));
            row.addView(play, new LinearLayout.LayoutParams(dp(58), dp(58)));

            list.addView(row, new LinearLayout.LayoutParams(-1, -2));
        }
    }

    private TextView rowText(String main, String sub) {
        TextView text = new TextView(this);
        text.setText(main + "\n" + sub);
        text.setTextColor(Color.BLACK);
        text.setTextSize(16);
        text.setGravity(Gravity.CENTER_VERTICAL);
        text.setTypeface(null, 1);
        return text;
    }

    private void openPicker() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("audio/*");
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        startActivityForResult(Intent.createChooser(intent, "Выберите музыку"), PICK_AUDIO);
    }

    private void playIndex(int index) {
        if (tracks.isEmpty()) return;
        Intent intent = new Intent(this, PlayerService.class);
        intent.setAction(PlayerService.ACTION_PLAY_INDEX);
        intent.putExtra(PlayerService.EXTRA_INDEX, Math.max(0, Math.min(index, tracks.size() - 1)));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) startForegroundService(intent);
        else startService(intent);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != PICK_AUDIO || resultCode != RESULT_OK || data == null) return;
        if (data.getClipData() != null) {
            for (int index = 0; index < data.getClipData().getItemCount(); index++) {
                addTrack(data.getClipData().getItemAt(index).getUri());
            }
        } else if (data.getData() != null) {
            addTrack(data.getData());
        }
        TrackStore.sort(tracks);
        TrackStore.save(this, tracks);
        renderList();
    }

    private void addTrack(Uri uri) {
        try {
            getContentResolver().takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
        } catch (Exception ignored) {}
        String value = uri.toString();
        for (Track track : tracks) {
            if (track.uri.equals(value)) return;
        }
        tracks.add(TrackStore.fromUri(this, uri));
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
