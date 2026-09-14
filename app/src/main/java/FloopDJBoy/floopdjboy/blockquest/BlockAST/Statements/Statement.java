package FloopDJBoy.floopdjboy.blockquest.BlockAST.Statements;

import FloopDJBoy.floopdjboy.blockquest.BlockAST.Block;
import FloopDJBoy.floopdjboy.blockquest.BlockAST.Connection;
import FloopDJBoy.floopdjboy.blockquest.BlockAST.ScriptContext;
import FloopDJBoy.floopdjboy.blockquest.BlockAST.Types.ConnectionType;
import FloopDJBoy.floopdjboy.blockquest.World;

public abstract class Statement extends Block {
    public enum ExecResult {
        DONE,RUNNING,ERROR
    }
    public Connection nextConnection=null;
    public Connection previousConnection=null;
    @Override
    public Connection makeConnection(ConnectionType type) {
        assert type != ConnectionType.OUTPUT_VALUE;
        // STATEMENT_INPUT is allowed: used by StatementInput on flow-control blocks (e.g. IF body)
        return super.makeConnection(type);
    }
    public boolean hasNext(){
        return nextConnection!=null && nextConnection.isConnected();
    }
    public boolean hasParent(){
        return previousConnection!=null && previousConnection.isConnected();
    }
    public Statement(String name,Category category) {
        super(name,category);
    }
    /**
     * Executes this statement in the given context and world.
     *
     * @param ctx   the {@link ScriptContext} where the statement is running
     * @param world the {@link World} where the statement is running
     *
     * @return the {@link ExecResult} indicating the execution outcome:<br>
     * <table border="1"  cellpadding="4" cellspacing="0">
     *   <tr><th>Value</th><th>Description</th></tr>
     *   <tr><td>{@code DONE}</td><td>The statement finished execution successfully; move to the next statement.</td></tr>
     *   <tr><td>{@code RUNNING}</td><td>The statement is still executing (usually for flow-control blocks); wait for more ticks.</td></tr>
     *   <tr><td>{@code ERROR}</td><td>An error occurred; the block should be skipped.</td></tr>
     * </table>
     */
    public abstract ExecResult execute(final ScriptContext ctx,final World world);
}
