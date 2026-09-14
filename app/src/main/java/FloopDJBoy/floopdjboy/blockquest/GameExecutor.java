package FloopDJBoy.floopdjboy.blockquest;

import android.os.Handler;
import android.os.Looper;

import FloopDJBoy.floopdjboy.blockquest.BlockAST.Block;

/**
 * GameExecutor manages the game loop on a background thread.
 * It runs the world tick at a fixed rate and notifies listeners on the main thread.
 */
public class GameExecutor {

    private World world;
    private final Handler mainHandler;
    private Thread gameThread;
    private volatile boolean isRunning = false;
    private volatile boolean isPaused = false;

    /**
     * Milliseconds between ticks (default 500ms = 2 ticks per second)
     */
    private int tickDelayMs = 500;

    /**
     * Listener for game events (called on main thread)
     */
    private GameListener listener;

    public GameExecutor(World world) {
        this.world = world;
        world.setBlockExecutionListener(
                this::notifyBlockExecuted
        );
        world.setLossListener(
                this::notifyLoss
        );
        this.mainHandler = new Handler(Looper.getMainLooper());
    }

    /**
     * Start the game loop
     */
    public void start() {
        if (isRunning) {
            return;
        }

        isRunning = true;
        isPaused = false;

        // Execute ON_START scripts
        world.start();
        notifyGameStarted();

        // Start game thread
        gameThread = new Thread(new GameLoop(), "GameExecutor-Thread");
        gameThread.start();
    }

    /**
     * Stop the game loop
     */
    public void stop() {
        isRunning = false;
        if (gameThread != null) {
            try {
                gameThread.join(1000); // one second to wrap all up running presses
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        notifyGameStopped();
    }

    /**
     * Pause the game (stops ticking but doesn't reset)
     */
    public void pause() {
        isPaused = true;
        notifyGamePaused();
    }

    /**
     * Resume the game
     */
    public void resume() {
        isPaused = false;
        notifyGameResumed();
    }

    /**
     * Set the tick rate (milliseconds between ticks)
     */
    public void setTickDelay(int milliseconds) {
        this.tickDelayMs = Math.max(50, milliseconds); // Minimum 50ms
    }

    /**
     * Set the game listener
     */
    public void setListener(GameListener listener) {
        this.listener = listener;
    }

    /**
     * Check if game is running
     */
    public boolean isRunning() {
        return isRunning;
    }

    /**
     * Check if game is paused
     */
    public boolean isPaused() {
        return isPaused;
    }

    public void setWorld(World world) {
        this.world = world;
        world.setBlockExecutionListener(
                this::notifyBlockExecuted
        );
        world.setLossListener(
                this::notifyLoss
        );
        stop();
    }

    // ========== Game Loop ==========

    private class GameLoop implements Runnable {
        @Override
        public void run() {
            long lastTickTime = System.currentTimeMillis();

            while (isRunning) {
                // Handle pause
                if (isPaused) {
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                    continue;
                }

                long currentTime = System.currentTimeMillis();
                long elapsed = currentTime - lastTickTime;

                if (elapsed >= tickDelayMs) {
                    // Execute one tickD
                    try {
                        world.tick();
                        notifyTick(world.getTickCount());
                        lastTickTime = currentTime;
                    } catch (Exception e) {
                        notifyError(e);
                        isRunning = false;
                        break;
                    }
                }

                // Sleep a bit to avoid busy waiting
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
    }

    // ========== Main Thread Notifications ==========
    private void notifyLoss() {
        if (listener != null) {
            mainHandler.post(() -> listener.onLoss());
        }
    }
    private void notifyBlockExecuted(Block block, int tick) {
        if (listener != null) {
            mainHandler.post(() -> listener.onBlockExecuted(block,tick));
        }
    }
    private void notifyGameStarted() {
        if (listener != null) {
            mainHandler.post(() -> listener.onGameStarted());
        }
    }

    private void notifyGameStopped() {
        if (listener != null) {
            mainHandler.post(() -> listener.onGameStopped());
        }
    }

    private void notifyGamePaused() {
        if (listener != null) {
            mainHandler.post(() -> listener.onGamePaused());
        }
    }

    private void notifyGameResumed() {
        if (listener != null) {
            mainHandler.post(() -> listener.onGameResumed());
        }
    }

    private void notifyTick(int tickCount) {
        if (listener != null) {
            mainHandler.post(() -> listener.onTick(tickCount));
        }
    }
    private void notifyError(Exception e) {
        if (listener != null) {
            mainHandler.post(() -> listener.onError(e));
        }
    }


    // ========== Listener Interface ==========

    /**
     * Listener for game events (all methods called on main thread)
     */
    public interface GameListener {
        /**
         * called on loss
         */
        void onLoss();
        /**
         * Called when the game starts
         */
        void onGameStarted();

        /**
         * Called when the game stops
         */
        void onGameStopped();

        /**
         * Called when the game is paused
         */
        void onGamePaused();

        /**
         * Called when the game is resumed
         */
        void onGameResumed();

        /**
         * Called after each tick
         * @param tickCount The current tick number
         */
        void onTick(int tickCount);

        /**
         * Called when a block is executed
         *
         * @param block the block that was executed
         * @param tick  the tick the block was executed on the script
         */
        void onBlockExecuted(Block block, int tick);

        /**
         * Called if an error occurs during execution
         * @param e The exception that occurred
         */
        void onError(Exception e);
    }
}