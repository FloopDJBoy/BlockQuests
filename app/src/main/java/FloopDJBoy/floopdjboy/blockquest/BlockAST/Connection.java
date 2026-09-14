package FloopDJBoy.floopdjboy.blockquest.BlockAST;

import FloopDJBoy.floopdjboy.blockquest.BlockAST.Types.ConnectionType;

public class Connection {
    private final Block sourceBlock;
    private Connection targetConnection;
    private final ConnectionType type;
    private boolean disposed = false;
    private final String id;

    public Connection(Block sourceBlock, ConnectionType type) {
        this.sourceBlock = sourceBlock;
        id = Registry.getInstance().register(Connection.class,this);
        this.type = type;
    }

    public Block getSourceBlock() {
        return sourceBlock;
    }

    public Block getTargetBlock() {
        return targetConnection != null ? targetConnection.sourceBlock : null;
    }

    public boolean isConnected() {
        return targetConnection != null;
    }

    public ConnectionType getType() {
        return type;
    }

    public void connect(Connection other) {
        if (other == null) throw new IllegalArgumentException();
        if (!canConnect(this, other)) {
            throw new IllegalArgumentException("Incompatible connection types");
        }

        // clean slate
        disconnect();
        other.disconnect();

        this.targetConnection = other;
        other.targetConnection = this;
    }

    public void disconnect() {
        if (!isConnected()) return;
        Connection other = targetConnection;
        targetConnection = null;
        other.targetConnection = null;
    }
    public void dispose() {
        if (disposed) return;

        disconnect();

        Registry.getInstance().remove(Connection.class, id);
        disposed = true;
    }
    public boolean canConnect(Connection a, Connection b) {
        return (a.type == ConnectionType.INPUT_VALUE && b.type == ConnectionType.OUTPUT_VALUE)
                || (a.type == ConnectionType.OUTPUT_VALUE && b.type == ConnectionType.INPUT_VALUE)
                || (a.type == ConnectionType.NEXT_STATEMENT && b.type == ConnectionType.PREVIOUS_STATEMENT)
                || (a.type == ConnectionType.PREVIOUS_STATEMENT && b.type == ConnectionType.NEXT_STATEMENT)
                || (a.type == ConnectionType.STATEMENT_INPUT && b.type == ConnectionType.PREVIOUS_STATEMENT)
                || (a.type == ConnectionType.PREVIOUS_STATEMENT && b.type == ConnectionType.STATEMENT_INPUT);
    }
}
