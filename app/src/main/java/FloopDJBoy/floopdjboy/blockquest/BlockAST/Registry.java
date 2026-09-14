package FloopDJBoy.floopdjboy.blockquest.BlockAST;

import androidx.annotation.CheckResult;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import FloopDJBoy.floopdjboy.blockquest.Actor;

//this class is a singleton that holds all the blocks and actors in the game
public class Registry {
    private static Registry instance;
    private final Map<Class<?>, Map<String, Object>> registries = new HashMap<>();
    @CheckResult
    public <T> String register(Class<T> type, T obj) {
        String id = UUID.randomUUID().toString();
        registries.computeIfAbsent(type, k -> new HashMap<>()).put(id, obj);
        return id;
    }
    public <T> String register(Class<T> type, T obj, String id) {
        registries.computeIfAbsent(type, k -> new HashMap<>()).put(id, obj);
        return id;
    }
    @SuppressWarnings("unchecked")
    public <T> T get(Class<T> type, String id) {
        return (T) registries.getOrDefault(type, Map.of()).get(id);
    }
    public <T> void remove(Class<T> type, String id) {
        if(!registries.containsKey(type))
            return;
        if(!registries.get(type).containsKey(id))
            return;
        registries.get(type).remove(id);
    }
    //this return a reference to the registry of the given type
    //the map returned is immutable
    @SuppressWarnings("unchecked")
    public <T> Map<String, T> getAll(Class<T> type) {
        Map<String, T> map =(Map<String, T>) registries.getOrDefault(type, Map.of());
        return Collections.unmodifiableMap(map);
    }
    public void clear() {
        for (Map<String, Object> map : registries.values()) {
            map.clear();
        }
    }
    private Registry() {
        registries.put(Block.class, new HashMap<>());
        registries.put(Actor.class, new HashMap<>());
    }
    public static Registry getInstance() {
        if(instance==null){
            instance = new Registry();
        }
        return instance;
    }
}
