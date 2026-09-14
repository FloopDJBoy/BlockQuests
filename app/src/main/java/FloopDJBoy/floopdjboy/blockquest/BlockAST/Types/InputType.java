package FloopDJBoy.floopdjboy.blockquest.BlockAST.Types;

public enum InputType {
    //get value e.g. Turn <DIR>
    EXPRESSION,
    //used to connect to a statement body. Used in control blocks. e.g. If, Repeat
    STATEMENT,
    // A dummy input.  Used to add field(s) with no input. E.g. MOVE aka the "move" word
    DUMMY,
    //used to tell the renderer that this is the end of the row
    //this input like dummy does not have any connections
    END_ROW
}
