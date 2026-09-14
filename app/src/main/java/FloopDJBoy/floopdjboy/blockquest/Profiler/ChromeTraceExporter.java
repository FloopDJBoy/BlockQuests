package FloopDJBoy.floopdjboy.blockquest.Profiler;

import android.content.Context;
import android.os.Process;

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
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TimeZone;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public final class ChromeTraceExporter implements Closeable {

    private static final int PID = Process.myPid();
    private static final String TRACE_CATEGORY = "blockprofiler";

    private  File file;
    private  BufferedWriter writer;
    private final ScheduledExecutorService executor =
            Executors.newSingleThreadScheduledExecutor();
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final AtomicBoolean autoFlushStarted = new AtomicBoolean(false);
    private final Object lock = new Object();
    private final Set<Long> emittedThreadNames = new HashSet<>();

    private boolean firstEvent = true;
    private boolean started = false;

    private long sessionStartNs;
    private  long startedAtMs;
    private  String sessionId;
    private static final boolean Enabled;
    static {
        Enabled = BlockProfiler.isEnabled();
    }

    private ChromeTraceExporter(Context context) throws IOException {
        if (!Enabled) return;
        this.startedAtMs = System.currentTimeMillis();
        this.sessionId = buildSessionId(startedAtMs);

        File dir = new File(context.getFilesDir(), "traces");
        if (!dir.exists() && !dir.mkdirs()) {
            throw new IOException("Could not create trace directory: " + dir.getAbsolutePath());
        }

        this.file = new File(dir, "trace_" + sessionId + ".json");
        this.writer = new BufferedWriter(
                new OutputStreamWriter(new FileOutputStream(file, false), StandardCharsets.UTF_8)
        );
    }

    public static ChromeTraceExporter start(Context context) throws IOException {
        if (!Enabled) return null;
        ChromeTraceExporter e = new ChromeTraceExporter(context.getApplicationContext());
        e.running.set(true);
        e.sessionStartNs = System.nanoTime();
        e.writeHeader();
        e.writeProcessMetadata();
        e.writeSessionStartEvent();
        return e;
    }

    public File getFile() {
        return file;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void startAutoFlush(long intervalMs) {
        if (!Enabled) return;
        if (!running.get() || intervalMs <= 0) return;
        if (!autoFlushStarted.compareAndSet(false, true)) return;

        executor.scheduleWithFixedDelay(() -> {
            if (!running.get()) return;
            try {
                flush();
            } catch (IOException ignored) {
                // keep trace alive; file can still be inspected later
            }
        }, intervalMs, intervalMs, TimeUnit.MILLISECONDS);
    }

    public void flush() throws IOException {
        if (!Enabled) return;
        List<BlockProfiler.FrameDump> frames = BlockProfiler.drainFrames();
        if (frames.isEmpty()) return;

        synchronized (lock) {
            for (BlockProfiler.FrameDump frame : frames) {
                writeFrame(frame);
            }
            writer.flush();
        }
    }

    public File stop() throws IOException {
        if (!Enabled) return null;
        if (!running.compareAndSet(true, false)) {
            return file;
        }

        executor.shutdownNow();

        flush();

        synchronized (lock) {
            writeSessionEndEvent();
            writeFooter();
            writer.flush();
            writer.close();
        }

        return file;
    }

    @Override
    public void close() throws IOException {
        stop();
    }

    private void writeHeader() throws IOException {
        synchronized (lock) {
            if (started) return;
            writer.write("{\"traceEvents\":[\n");
            started = true;
        }
    }

    private void writeFooter() throws IOException {
        writer.write("\n]}\n");
    }

    private void writeProcessMetadata() throws IOException {
        JSONObject obj = new JSONObject();
        put(obj, "name", "process_name");
        put(obj, "ph", "M");
        put(obj, "pid", PID);

        JSONObject args = new JSONObject();
        put(args, "name", "BlockProfiler");
        put(obj, "args", args);

        writeEvent(obj);
    }

    private void writeThreadMetadata(long threadId) throws IOException {
        if (emittedThreadNames.contains(threadId)) return;
        emittedThreadNames.add(threadId);

        JSONObject obj = new JSONObject();
        put(obj, "name", "thread_name");
        put(obj, "ph", "M");
        put(obj, "pid", PID);
        put(obj, "tid", threadId);

        JSONObject args = new JSONObject();
        put(args, "name", "thread_" + threadId);
        put(obj, "args", args);

        writeEvent(obj);
    }

    private void writeSessionStartEvent() throws IOException {
        JSONObject obj = new JSONObject();
        put(obj, "name", "session_start");
        put(obj, "cat", TRACE_CATEGORY);
        put(obj, "ph", "I");
        put(obj, "ts", 0L);
        put(obj, "pid", PID);
        put(obj, "tid", 0L);

        JSONObject args = new JSONObject();
        put(args, "sessionId", sessionId);
        put(args, "startedAtMs", startedAtMs);
        put(obj, "args", args);

        writeEvent(obj);
    }

    private void writeSessionEndEvent() throws IOException {
        JSONObject obj = new JSONObject();
        put(obj, "name", "session_end");
        put(obj, "cat", TRACE_CATEGORY);
        put(obj, "ph", "I");
        put(obj, "ts", toUs(System.nanoTime() - sessionStartNs));
        put(obj, "pid", PID);
        put(obj, "tid", 0L);

        JSONObject args = new JSONObject();
        put(args, "sessionId", sessionId);
        put(args, "endedAtMs", System.currentTimeMillis());
        put(obj, "args", args);

        writeEvent(obj);
    }

    private void writeFrame(BlockProfiler.FrameDump frame) throws IOException {
        long frameStartUs = toUs(frame.frameStartNs - sessionStartNs);

        JSONObject frameObj = new JSONObject();
        put(frameObj, "name", "frame");
        put(frameObj, "cat", TRACE_CATEGORY);
        put(frameObj, "ph", "I");
        put(frameObj, "ts", frameStartUs);
        put(frameObj, "pid", PID);
        put(frameObj, "tid", 0L);

        JSONObject frameArgs = new JSONObject();
        put(frameArgs, "frameId", frame.frameId);
        put(frameArgs, "frameStartNs", frame.frameStartNs);
        put(frameObj, "args", frameArgs);

        writeEvent(frameObj);

        if (frame.slices == null) return;

        for (BlockProfiler.FrameSlice s : frame.slices) {
            writeThreadMetadata(s.threadId);

            long tsUs = toUs(frame.frameStartNs + s.startOffsetNs - sessionStartNs);
            long durUs = Math.max(0L, s.durationNs / 1000L);

            JSONObject sliceObj = new JSONObject();
            put(sliceObj, "name", s.name);
            put(sliceObj, "cat", TRACE_CATEGORY);
            put(sliceObj, "ph", "X");
            put(sliceObj, "ts", tsUs);
            put(sliceObj, "dur", durUs);
            put(sliceObj, "pid", PID);
            put(sliceObj, "tid", s.threadId);

            JSONObject args = new JSONObject();
            put(args, "frameId", s.frameId);
            put(args, "depth", s.depth);
            put(args, "startOffsetNs", s.startOffsetNs);
            put(args, "durationNs", s.durationNs);
            put(args, "threadId", s.threadId);
            put(sliceObj, "args", args);

            writeEvent(sliceObj);
        }
    }

    private void writeEvent(JSONObject obj) throws IOException {
        synchronized (lock) {
            if (!started) {
                throw new IOException("Exporter has not been started");
            }

            if (!firstEvent) {
                writer.write(",\n");
            }
            writer.write(obj.toString());
            firstEvent = false;
        }
    }

    private static void put(JSONObject obj, String key, Object value) throws IOException {
        try {
            obj.put(key, value);
        } catch (JSONException e) {
            throw new IOException(e);
        }
    }

    private static long toUs(long nanos) {
        long us = nanos / 1000L;
        return Math.max(0L, us);
    }

    private static String buildSessionId(long startedAtMs) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US);
        sdf.setTimeZone(TimeZone.getDefault());
        return sdf.format(new Date(startedAtMs));
    }
}