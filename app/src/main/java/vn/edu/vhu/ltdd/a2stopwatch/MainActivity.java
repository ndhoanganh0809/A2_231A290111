package vn.edu.vhu.ltdd.a2stopwatch;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.util.Log;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.util.ArrayList;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "A2_231A290111";

    // Khóa lưu trạng thái vào Bundle
    private static final String KEY_RUNNING = "running";
    private static final String KEY_ACCUMULATED = "accumulated";
    private static final String KEY_START = "start";
    private static final String KEY_RECREATE = "recreate";
    // Khóa cho 2 bài nâng cao
    private static final String KEY_LAPS = "laps";
    private static final String KEY_PAUSE_BG = "pause_bg";

    private TextView tvTime, tvStatus, tvRecreate, tvLaps;
    private Button btnStartPause, btnReset, btnLap;
    private CheckBox cbPauseOnBg;

    // Trạng thái của đồng hồ
    private boolean running = false;
    private long accumulated = 0L;
    private long startTime = 0L;
    private int recreateCount = 0;

    // NC1: Danh sách lưu các mốc thời gian Vòng (Lap)
    private ArrayList<String> laps = new ArrayList<>();

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable ticker = new Runnable() {
        @Override
        public void run() {
            updateTimeText();
            handler.postDelayed(this, 100); // cập nhật 10 lần/giây
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return insets;
        });

        tvTime = findViewById(R.id.tvTime);
        tvStatus = findViewById(R.id.tvStatus);
        tvRecreate = findViewById(R.id.tvRecreate);
        tvLaps = findViewById(R.id.tvLaps);
        btnStartPause = findViewById(R.id.btnStartPause);
        btnReset = findViewById(R.id.btnReset);
        btnLap = findViewById(R.id.btnLap);
        cbPauseOnBg = findViewById(R.id.cbPauseOnBg);

        if (savedInstanceState != null) {
            running = savedInstanceState.getBoolean(KEY_RUNNING);
            accumulated = savedInstanceState.getLong(KEY_ACCUMULATED);
            startTime = savedInstanceState.getLong(KEY_START);
            recreateCount = savedInstanceState.getInt(KEY_RECREATE) + 1;

            // Khôi phục dữ liệu NC1 & NC2
            ArrayList<String> savedLaps = savedInstanceState.getStringArrayList(KEY_LAPS);
            if (savedLaps != null) {
                laps = savedLaps;
            }
            cbPauseOnBg.setChecked(savedInstanceState.getBoolean(KEY_PAUSE_BG, false));

            Log.d(TAG, "onCreate: KHÔI PHỤC trạng thái, running=" + running
                    + ", accumulated=" + accumulated + "ms, laps=" + laps.size());
        } else {
            Log.d(TAG, "onCreate: khởi tạo mới (savedInstanceState == null)");
        }

        btnStartPause.setOnClickListener(v -> {
            if (running) {
                pauseStopwatch();
            } else {
                startStopwatch();
            }
        });

        btnReset.setOnClickListener(v -> resetStopwatch());

        // NC1: Sự kiện bấm nút Vòng (Lap)
        btnLap.setOnClickListener(v -> recordLap());

        updateUi();
    }

    // --- Logic đồng hồ ---
    private long elapsed() {
        return running ? accumulated + (SystemClock.elapsedRealtime() - startTime) : accumulated;
    }

    private String formatTime(long ms) {
        long giay = (ms % 60000) / 1000;
        long phut = ms / 60000;
        long phanMuoi = (ms % 1000) / 100;
        return String.format(Locale.getDefault(), "%02d:%02d.%d", phut, giay, phanMuoi);
    }

    private void startStopwatch() {
        running = true;
        startTime = SystemClock.elapsedRealtime();
        startTicking();
        updateUi();
        Log.i(TAG, "BẮT ĐẦU đếm giờ");
    }

    private void pauseStopwatch() {
        accumulated += SystemClock.elapsedRealtime() - startTime;
        running = false;
        stopTicking();
        updateUi();
        Log.i(TAG, "TẠM DỪNG tại " + accumulated + "ms");
    }

    private void resetStopwatch() {
        running = false;
        accumulated = 0L;
        startTime = 0L;
        laps.clear(); // Xóa danh sách vòng khi Đặt lại
        stopTicking();
        updateUi();
        Log.i(TAG, "ĐẶT LẠI về 00:00.0");
    }

    // NC1: Hàm ghi nhận mốc thời gian Vòng (Lap)
    private void recordLap() {
        if (!running && accumulated == 0L) return; // Không bấm vòng khi chưa chạy
        String currentFormatted = formatTime(elapsed());
        String lapEntry = "Vòng " + (laps.size() + 1) + ":   " + currentFormatted;
        laps.add(lapEntry);
        updateLapsText();
        Log.i(TAG, "Ghi nhận " + lapEntry);
    }

    private void startTicking() {
        handler.removeCallbacks(ticker);
        handler.post(ticker);
    }

    private void stopTicking() {
        handler.removeCallbacks(ticker);
    }

    // --- Cập nhật giao diện ---
    private void updateTimeText() {
        tvTime.setText(formatTime(elapsed()));
    }

    private void updateLapsText() {
        StringBuilder sb = new StringBuilder();
        for (int i = laps.size() - 1; i >= 0; i--) {
            sb.append(laps.get(i)).append("\n");
        }
        tvLaps.setText(sb.toString());
    }

    private void updateUi() {
        updateTimeText();
        updateLapsText();
        btnStartPause.setText(running ? R.string.pause : R.string.start);
        tvStatus.setText(running ? R.string.status_running : R.string.status_paused);
        tvRecreate.setText(getString(R.string.recreate_count, recreateCount));
    }

    // --- Vòng đời ---
    @Override
    protected void onStart() {
        super.onStart();
        Log.d(TAG, "onStart");
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume - bật lại việc cập nhật giao diện nếu đồng hồ đang chạy");
        if (running) {
            startTicking();
            updateUi();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopTicking();
        Log.d(TAG, "onPause - tạm dừng cập nhật giao diện");
    }

    @Override
    protected void onStop() {
        super.onStop();
        // NC2: Nếu CheckBox được tick và đồng hồ đang chạy thì tự động tạm dừng
        if (cbPauseOnBg.isChecked() && running) {
            pauseStopwatch();
            Log.d(TAG, "onStop - đã tự động tạm dừng đồng hồ do bật 'Dừng khi ra nền'");
        } else {
            Log.d(TAG, "onStop");
        }
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        Log.d(TAG, "onRestart");
    }

    @Override
    protected void onDestroy() {
        stopTicking();
        Log.d(TAG, "onDestroy");
        super.onDestroy();
    }

    // --- Lưu & khôi phục trạng thái ---
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(KEY_RUNNING, running);
        outState.putLong(KEY_ACCUMULATED, accumulated);
        outState.putLong(KEY_START, startTime);
        outState.putInt(KEY_RECREATE, recreateCount);
        // Lưu trạng thái NC1 & NC2
        outState.putStringArrayList(KEY_LAPS, laps);
        outState.putBoolean(KEY_PAUSE_BG, cbPauseOnBg.isChecked());

        Log.d(TAG, "onSaveInstanceState: đã lưu " + elapsed() + "ms và " + laps.size() + " vòng vào Bundle");
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        Log.d(TAG, "onRestoreInstanceState - được gọi sau onStart()");
    }
}