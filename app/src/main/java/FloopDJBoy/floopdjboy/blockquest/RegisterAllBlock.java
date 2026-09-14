package FloopDJBoy.floopdjboy.blockquest;

import android.content.Context;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import dalvik.system.DexFile;
import FloopDJBoy.floopdjboy.blockquest.BlockAST.Block;
import FloopDJBoy.floopdjboy.blockquest.BlockAST.VariableBlock;
import FloopDJBoy.floopdjboy.blockquest.BlockSerialization.DataTypes;
import FloopDJBoy.floopdjboy.blockquest.BlockUi.PaletteAdapter;

public class RegisterAllBlock {
    private static final List<Runnable> listeners = new ArrayList<>();
    private static final Map<Block.Category, PaletteAdapter.PaletteCategory> map =
            new EnumMap<>(Block.Category.class);
    private static volatile boolean ready = false;
    private static volatile boolean loading = false;
    private static final String CACHE_FILE = "blockRegisterCache.json";
    private static final boolean reCACHE = false;

    private static final Gson gson = new Gson();


    private static List<PaletteAdapter.PaletteCategory> cached = new ArrayList<>();

    private static final Executor executor =
            Executors.newSingleThreadExecutor();

    private RegisterAllBlock() {}

    // ---------------------------
    // Public API
    // ---------------------------

    public static void loadBlocks(Context ctx) {
        synchronized (RegisterAllBlock.class) {
            if (ready || loading) return;
            loading = true;
        }

        executor.execute(() -> {
            try {
                List<PaletteAdapter.PaletteCategory> result = loadCache(ctx);
                if (result == null || reCACHE) {
                    result = getBlocksInternal(ctx);
                }

                synchronized (RegisterAllBlock.class) {
                    cached = result;
                    ready = true;
                    loading = false;
                    for (Runnable r : listeners) {
                        r.run();
                    }
                    listeners.clear();
                }

            } catch (Exception e) {
                synchronized (RegisterAllBlock.class) {
                    loading = false;
                }
                Log.e("BLOCK_SCAN", "Failed to load blocks", e);
            }
        });
    }

    public static boolean isReady() {
        return ready;
    }

    public static List<PaletteAdapter.PaletteCategory> getCached() {
        return cached;
    }

    // ---------------------------
    // Core scanning logic
    // ---------------------------

    private static List<PaletteAdapter.PaletteCategory> getBlocksInternal(Context context) {
        map.clear();
        List<DataTypes.CachedCategory> cacheData = new ArrayList<>();
        for (Block.Category cat : Block.Category.values()) {
            map.put(cat, new PaletteAdapter.PaletteCategory(cat));

            DataTypes.CachedCategory cached = new DataTypes.CachedCategory();
            cached.category = cat.ordinal();

            cacheData.add(cached);
        }

        try {
            DexFile dex = new DexFile(context.getPackageCodePath());
            Enumeration<String> entries = dex.entries();

            while (entries.hasMoreElements()) {
                String className = entries.nextElement();

                try {
                    Class<?> cls = Class.forName(className, false, context.getClassLoader());

                    if (!Block.class.isAssignableFrom(cls)) continue;
                    if (Modifier.isAbstract(cls.getModifiers())) continue;
                    if(VariableBlock.class.isAssignableFrom(cls)) continue;

                    Constructor<?> ctor = cls.getDeclaredConstructor();
                    ctor.setAccessible(true);

                    // instantiate once only to read category
                    // very efficient
                    Block instance = (Block) ctor.newInstance();

                    PaletteAdapter.PaletteCategory cat =
                            map.get(instance.getCategory());
                    cacheData.get(cat.category.ordinal()).blocks.add(className);


                    if (cat != null) {
                        @SuppressWarnings("unchecked")
                        Constructor<? extends Block> typedCtor =
                                (Constructor<? extends Block>) ctor;

                        cat.blocks.add(() -> {
                            try {
                                return typedCtor.newInstance();
                            } catch (Exception e) {
                                throw new RuntimeException(e);
                            }
                        });
                    }

                } catch (Exception e) {
                    Log.e("BLOCK_SCAN", "Failed class: " + className, e);
                }
            }

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        saveCache(context, cacheData);
        return new ArrayList<>(map.values());
    }
    public static void onReady(Runnable callback) {
        synchronized (RegisterAllBlock.class) {
            if (ready) {
                callback.run();
                return;
            }
            listeners.add(callback);
        }
    }
    private static void saveCache(Context context, List<DataTypes.CachedCategory> data) {
        try {
            String json = gson.toJson(data);

            try (FileOutputStream out =
                         context.openFileOutput(CACHE_FILE, Context.MODE_PRIVATE)) {
                out.write(json.getBytes());
            }

        } catch (Exception e) {
            Log.e("BLOCK_CACHE", "save failed", e);
        }
    }
    private static List<PaletteAdapter.PaletteCategory> loadCache(Context context) {
        try {
            StringBuilder json = new StringBuilder();

            try (
                    InputStream in = context.getAssets().open("blockRegisterCache.json");
                    BufferedReader reader = new BufferedReader(
                            new InputStreamReader(in)
                    )
            ) {
                String line;
                while ((line = reader.readLine()) != null) {
                    json.append(line);
                }
            }

            Type type = new TypeToken<List<DataTypes.CachedCategory>>() {}.getType();
            List<DataTypes.CachedCategory> cached =
                    gson.fromJson(json.toString(), type);

            List<PaletteAdapter.PaletteCategory> result =
                    new ArrayList<>();

            for (DataTypes.CachedCategory c : cached) {
                Block.Category category = Block.Category.values()[c.category];

                PaletteAdapter.PaletteCategory palette =
                        new PaletteAdapter.PaletteCategory(category);

                for (String className : c.blocks) {
                    palette.blocks.add(() -> {
                        try {
                            Class<?> cls = Class.forName(className);
                            Constructor<?> ctor =
                                    cls.getDeclaredConstructor();

                            ctor.setAccessible(true);

                            return (Block) ctor.newInstance();

                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                    });
                }

                result.add(palette);
            }

            return result;

        } catch (Exception e) {
            return null;
        }
    }
    public static void clearCache(Context context) {
        context.deleteFile(CACHE_FILE);
    }
}