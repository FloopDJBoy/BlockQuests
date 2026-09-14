package FloopDJBoy.floopdjboy.blockquest.Profiler;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public final class BlockProfilerExporter implements Closeable {

    private final File sessionFile;
    private final BufferedWriter writer;
    private final ScheduledExecutorService flushExecutor;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final Object writeLock = new Object();

    private final String sessionId;
    private final long startedAtMs;

    private BlockProfilerExporter(Context context) throws IOException, JSONException {
        this.startedAtMs = System.currentTimeMillis();
        this.sessionId = buildSessionId(startedAtMs);

        File dir = new File(context.getFilesDir(), "blockprofiler");
        if (!dir.exists() && !dir.mkdirs()) {
            throw new IOException("Could not create profiler directory: " + dir.getAbsolutePath());
        }

        this.sessionFile = new File(dir, "session_" + sessionId + ".jsonl");
        this.writer = new BufferedWriter(
                new OutputStreamWriter(new FileOutputStream(sessionFile, false), StandardCharsets.UTF_8)
        );

        this.flushExecutor = Executors.newSingleThreadScheduledExecutor();

        writeSessionStart();
    }

    public static BlockProfilerExporter start(Context context) throws IOException, JSONException {
        BlockProfilerExporter exporter = new BlockProfilerExporter(context.getApplicationContext());
        exporter.running.set(true);
        return exporter;
    }

    public File getSessionFile() {
        return sessionFile;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void startAutoFlush(long intervalMs) {
        if (!running.get()) return;

        flushExecutor.scheduleWithFixedDelay(() -> {
            if (!running.get()) return;
            try {
                flushNow();
            } catch (IOException ignored) {
                // keep session alive; caller can inspect file later
            }
        }, intervalMs, intervalMs, TimeUnit.MILLISECONDS);
    }

    public void flushNow() throws IOException {
        List<BlockProfiler.FrameDump> frames = BlockProfiler.drainFrames();
        if (frames.isEmpty()) return;

        synchronized (writeLock) {
            for (BlockProfiler.FrameDump dump : frames) {
                writer.write(frameDumpToJson(dump).toString());
                writer.newLine();
            }
            writer.flush();
        }
    }

    public File stop() throws IOException, JSONException {
        if (!running.compareAndSet(true, false)) {
            return sessionFile;
        }

        flushExecutor.shutdownNow();

        flushNow();

        synchronized (writeLock) {
            writeSessionEnd();
            writer.flush();
            writer.close();
        }

        return sessionFile;
    }

    @Override
    public void close() throws IOException {
        try {
            stop();
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    private void writeSessionStart() throws IOException, JSONException {
        synchronized (writeLock) {
            JSONObject obj = new JSONObject();
            obj.put("type", "session_start");
            obj.put("sessionId", sessionId);
            obj.put("startedAtMs", startedAtMs);
            writer.write(obj.toString());
            writer.newLine();
            writer.flush();
        }
    }

    private void writeSessionEnd() throws IOException, JSONException {
        JSONObject obj = new JSONObject();
        obj.put("type", "session_end");
        obj.put("sessionId", sessionId);
        obj.put("endedAtMs", System.currentTimeMillis());
        writer.write(obj.toString());
        writer.newLine();
    }

    private static JSONObject frameDumpToJson(BlockProfiler.FrameDump dump) {
        JSONObject obj = new JSONObject();
        try {
            obj.put("type", "frame");
            obj.put("frameId", dump.frameId);
            obj.put("frameStartNs", dump.frameStartNs);

            JSONArray slices = new JSONArray();
            if (dump.slices != null) {
                for (BlockProfiler.FrameSlice slice : dump.slices) {
                    JSONObject s = new JSONObject();
                    s.put("frameId", slice.frameId);
                    s.put("threadId", slice.threadId);
                    s.put("name", slice.name);
                    s.put("depth", slice.depth);
                    s.put("startOffsetNs", slice.startOffsetNs);
                    s.put("durationNs", slice.durationNs);
                    slices.put(s);
                }
            }

            obj.put("slices", slices);
        } catch (Exception ignored) {
        }
        return obj;
    }

    private static String buildSessionId(long startedAtMs) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US);
        sdf.setTimeZone(TimeZone.getDefault());
        return sdf.format(new Date(startedAtMs));
    }
}
