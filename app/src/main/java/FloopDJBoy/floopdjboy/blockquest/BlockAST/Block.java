package FloopDJBoy.floopdjboy.blockquest.BlockAST;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.Objects;

import FloopDJBoy.floopdjboy.blockquest.BlockAST.Expressions.Expression;
import FloopDJBoy.floopdjboy.blockquest.BlockAST.Inputs.Input;
import FloopDJBoy.floopdjboy.blockquest.BlockAST.Statements.Statement;
import FloopDJBoy.floopdjboy.blockquest.BlockAST.Types.ConnectionType;
import FloopDJBoy.floopdjboy.blockquest.BlockUi.Constants;


public abstract class Block {
    public enum Category {
        MOTION("Motion", Constants.CategoryColors.Motion),
        LOOKS("Looks", Constants.CategoryColors.Looks),
        SOUND("Sound", Constants.CategoryColors.Sound),
        EVENTS("Events", Constants.CategoryColors.Events),
        CONTROL("Control", Constants.CategoryColors.Control),
        SENSING("Sensing", Constants.CategoryColors.Sensing),
        OPERATORS("Operators", Constants.CategoryColors.Operators),
        VARIABLES("Variables", Constants.CategoryColors.Variables),
        LISTS("Lists", Constants.CategoryColors.Lists),
        MY_BLOCKS("My Blocks", Constants.CategoryColors.MyBlocks),
        EXTENSIONS("Extensions", Constants.CategoryColors.Extensions);

        private final String label;
        private final int color;


        Category(String label, int color) {
            this.label = label;
            this.color = color;
        }

        @NonNull
        @Override
        public String toString() {
            return label;
        }

        public int getColor() {
            return color;
        }
    }
    private final Category category;
    private boolean dirty = true;

    public Category getCategory() {
        return category;
    }

    public ArrayList<Input<?>> inputs = new ArrayList<>();
    //used to avoid double disposal
    private boolean disposed = false;

    public void dispose() {
        if (disposed) return;
        // Mark disposed first to break any recursive cycles
        disposed = true;

        // Unplug this block from its neighbours without disposing them —
        // we handle disposal of children explicitly below.
        unplug();

        // Remove this block from the Registry
        Registry.getInstance().remove(Block.class, id);

        // Dispose every input's connection and the child block connected into it
        for (Input<?> input : inputs) {
            // DummyInput and EndRowInput have null connections — skip them
            if (input.connection == null) continue;

            // Capture the child block before we dispose the connection
            Block child = input.connection.getTargetBlock(); // null if socket is empty

            // Dispose the connection object itself
            input.connection.dispose();

            // Recursively dispose the child block that was plugged into this socket
            if (child != null) {
                child.dispose();
            }
        }

        // Dispose Statement-specific connections
        if (this instanceof Statement s) {
            // Dispose the next-chain block first, then the connection
            if (s.nextConnection != null) {
                Block next = s.nextConnection.getTargetBlock();
                s.nextConnection.dispose();
                if (next != null) {
                    next.dispose();
                }
            }
            // previousConnection only links upward; just dispose the connection object
            if (s.previousConnection != null) {
                s.previousConnection.dispose();
            }
        }

        // Dispose Expression output connection
        if (this instanceof Expression<?> e) {
            if (e.outputConnection != null) {
                e.outputConnection.dispose();
            }
        }
    }

    //unplug all connections from the stack
    public void unplug() {
        if(this instanceof Statement s){
            unplugStatement();
        }else if(this instanceof Expression<?> e){
            unplugExpression();
        }
    }
    private void unplugExpression() {
        Expression<?> expression = (Expression<?>)this;
        if(expression.outputConnection!=null && expression.outputConnection.isConnected()){
            expression.outputConnection.disconnect();
        }
    }

    //unplug from the stack
    private void unplugStatement() {
        Statement statement = (Statement)this;
        if(statement.hasParent()){
            statement.previousConnection.disconnect();
        }
        if(statement.hasNext()){
            statement.nextConnection.disconnect();
        }
    }
    public void disconnect(ConnectionType type) {
        Connection c = getConnection(type);
        if (c != null && c.isConnected()) {
            Block target = c.getTargetBlock();
            c.disconnect();
            if (target != null) target.setParent(null);
        }
    }
    public void makeDirty() {
        this.dirty = true;
    }

    public boolean isDirty() {
        return dirty;
    }
    public void makeClean(){
        this.dirty = false;
    }
    //unique id
    private final String id;
    //tag markup like if, move, turn ,ect
    private final String name;

    public Block getParent() {
        return parent;
    }

    public void setParent(Block parent) {
        this.parent = parent;
    }

    protected Block parent= null;

    public final String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public Block(String name,Category category) {
        this.name = name;
        this.category = category;
        id = Registry.getInstance().register(Block.class,this);
    }
    public Connection makeConnection(ConnectionType type) {
        return new Connection(this, type);
    }
    public void connect(Block other, ConnectionType thisType) {
        Objects.requireNonNull(other);
        Objects.requireNonNull(thisType);

        Connection a = getOrCreateConnection(thisType);
        Connection b = other.getCompatibleConnection(thisType);

        if (a == null || b == null) {
            throw new IllegalStateException("Invalid connection types");
        }

        if (!a.canConnect(a, b)) {
            throw new IllegalArgumentException("Incompatible connections");
        }

        // clean disconnects
        if (a.isConnected()) a.disconnect();
        if (b.isConnected()) b.disconnect();

        a.connect(b);

        // parent ownership
        if (this instanceof Statement && other instanceof Statement) {
            other.setParent(this);
        } else if (this instanceof Statement && other instanceof Expression) {
            other.setParent(this);
        }
    }
    private Connection getConnection(ConnectionType type) {
        if (this instanceof Statement s) {
            return switch (type) {
                case NEXT_STATEMENT -> s.nextConnection;
                case PREVIOUS_STATEMENT -> s.previousConnection;
                default -> null;
            };
        }
        if (this instanceof Expression<?> e) {
            return type == ConnectionType.OUTPUT_VALUE ? e.outputConnection : null;
        }
        return null;
    }

    private Connection getOrCreateConnection(ConnectionType type) {
        Connection c = getConnection(type);
        if (c != null) return c;

        if (this instanceof Statement s) {
            if (type == ConnectionType.NEXT_STATEMENT) {
                return s.nextConnection = makeConnection(type);
            }
            if (type == ConnectionType.PREVIOUS_STATEMENT) {
                return s.previousConnection = makeConnection(type);
            }
        }

        if (this instanceof Expression<?> e && type == ConnectionType.OUTPUT_VALUE) {
            return e.outputConnection = makeConnection(type);
        }

        return null;
    }

    private Connection getCompatibleConnection(ConnectionType otherType) {
        return switch (otherType) {
            case NEXT_STATEMENT, STATEMENT_INPUT -> getOrCreateConnection(ConnectionType.PREVIOUS_STATEMENT);
            case PREVIOUS_STATEMENT -> getOrCreateConnection(ConnectionType.NEXT_STATEMENT);
            case INPUT_VALUE -> getOrCreateConnection(ConnectionType.OUTPUT_VALUE);
            case OUTPUT_VALUE -> getOrCreateConnection(ConnectionType.INPUT_VALUE);
        };
    }
    public boolean isStatement(){return this instanceof Statement;}
    public boolean isExpression(){return this instanceof Expression;}

}