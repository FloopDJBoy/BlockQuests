package FloopDJBoy.floopdjboy.blockquest.Profiler;

public final class BlockProfiler {

    private static final Scope NO_OP_SCOPE = new Scope();
    private static volatile boolean enabled = false;

    private static final ThreadLocal<java.util.ArrayDeque<Active>> stack =
            ThreadLocal.withInitial(java.util.ArrayDeque::new);

    private static final java.util.concurrent.ConcurrentLinkedQueue<FrameSlice> currentFrameSlices =
            new java.util.concurrent.ConcurrentLinkedQueue<>();

    private static final java.util.concurrent.ConcurrentLinkedQueue<FrameDump> completedFrames =
            new java.util.concurrent.ConcurrentLinkedQueue<>();

    private static volatile long frameStartNs = 0;

    public static boolean isEnabled() {
        return enabled;
    }

    private static volatile int frameId = 0;

    private static final int MAX_FRAMES = 300;

    private BlockProfiler() {}

    public static void setEnabled(boolean value) {
        enabled = value;
    }

    public static void beginFrame(int id) {
        if (!enabled) return;

        frameId = id;
        frameStartNs = System.nanoTime();
        currentFrameSlices.clear();
    }

    public static void endFrame() {
        if (!enabled) return;

        FrameDump dump = new FrameDump();
        dump.frameId = frameId;
        dump.frameStartNs = frameStartNs;
        dump.slices = new java.util.ArrayList<>(currentFrameSlices);

        completedFrames.add(dump);

        while (completedFrames.size() > MAX_FRAMES) {
            completedFrames.poll();
        }
    }

    public static Scope scope(String name) {
        if (!enabled) return NO_OP_SCOPE;
        return new Scope(name);
    }

    public static final class Scope implements AutoCloseable {
        private final String name;
        private final long startNs;
        private final int depth;
        private boolean closed;

        Scope(String name) {
            this.name = name;
            this.startNs = System.nanoTime();

            java.util.ArrayDeque<Active> localStack = stack.get();
            this.depth = localStack.size();
            localStack.push(new Active(name, startNs));
        }
        Scope() {
            this.name = null;
            this.startNs = 0;
            this.depth = 0;
            this.closed = false;
        }



        @Override
        public void close() {
            if (closed || !enabled) return;
            closed = true;

            long endNs = System.nanoTime();
            java.util.ArrayDeque<Active> localStack = stack.get();
            Active active = localStack.isEmpty() ? null : localStack.pop();

            long start = active != null ? active.startNs : startNs;
            String sliceName = active != null ? active.name : name;

            FrameSlice slice = new FrameSlice();
            slice.frameId = frameId;
            slice.name = sliceName;
            slice.threadId = Thread.currentThread().getId();
            slice.depth = depth;
            slice.startOffsetNs = start - frameStartNs;
            slice.durationNs = endNs - start;

            currentFrameSlices.add(slice);
        }
    }

    private static final class Active {
        final String name;
        final long startNs;

        Active(String name, long startNs) {
            this.name = name;
            this.startNs = startNs;
        }
    }

    public static final class FrameSlice {
        public int frameId;
        public long threadId;
        public String name;
        public int depth;
        public long startOffsetNs;
        public long durationNs;
    }

    public static final class FrameDump {
        public int frameId;
        public long frameStartNs;
        public java.util.List<FrameSlice> slices;
    }

    public static java.util.List<FrameDump> drainFrames() {
        java.util.ArrayList<FrameDump> out = new java.util.ArrayList<>();
        FrameDump d;
        while ((d = completedFrames.poll()) != null) {
            out.add(d);
        }
        return out;
    }
}