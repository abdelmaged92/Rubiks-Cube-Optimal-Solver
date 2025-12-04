package cube.solver;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents the result of a cube solving operation.
 * Used by all solver implementations.
 */
public class SolveResult {
    
    private final boolean success;
    private final String message;
    private final List<Integer> moves;
    private final int moveCount;

    public SolveResult(boolean success, String message) {
        this.success = success;
        this.message = message;
        this.moves = new ArrayList<>();
        this.moveCount = 0;
    }

    public SolveResult(boolean success, String message, List<Integer> moves) {
        this.success = success;
        this.message = message;
        this.moves = new ArrayList<>(moves);
        this.moveCount = moves.size();
    }

    public boolean isSuccess() {
        return success;
    }

    public String getMessage() {
        return message;
    }

    public List<Integer> getMoves() {
        return new ArrayList<>(moves);
    }

    public int getMoveCount() {
        return moveCount;
    }
}

