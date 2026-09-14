package FloopDJBoy.floopdjboy.blockquest.BlockAST;

public final class ScriptEvent {

    public static final ScriptEvent ON_TICK = new ScriptEvent("ON_TICK");
    public static final ScriptEvent ON_COLLISION = new ScriptEvent("ON_COLLISION");
    public static final ScriptEvent ON_START = new ScriptEvent("ON_START");
    public static final ScriptEvent WHILE_COLLIDING = new ScriptEvent("WHILE_COLLIDING");
    private final String name;

    public ScriptEvent(String name) {
        this.name = name;
    }

    public String getName() { return name; }

    @Override
    public boolean equals(Object o) {
        return o instanceof ScriptEvent e && name.equals(e.name);
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }
}

