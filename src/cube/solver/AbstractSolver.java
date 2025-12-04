package cube.solver;

import cube.model.face.FaceCube;
import cube.model.cubie.CubieCube;

import java.util.List;

/**
 * Abstract base class for all Rubik's Cube solvers.
 * Provides shared functionality and defines template methods for subclasses.
 * Demonstrates inheritance and abstraction.
 */
public abstract class AbstractSolver implements Solver {
    
    // Shared move name constants
    protected static final String[] MOVE_NAMES = {
        "U", "U2", "U'", "R", "R2", "R'", "F", "F2", "F'",
        "D", "D2", "D'", "L", "L2", "L'", "B", "B2", "B'"
    };

    /**
     * Template method - solve with default parameters.
     * Calls the abstract methods to get defaults, then delegates to full solve.
     */
    @Override
    public SolveResult solve(String cubeString) {
        return solve(cubeString, getDefaultMaxLength(), getDefaultTimeout());
    }

    /**
     * Render a solution as a human-readable string.
     * Uses getSolutionSuffix() which can be overridden by subclasses.
     * @param moves List of move indices
     * @return Formatted solution string
     */
    protected String renderSolution(List<Integer> moves) {
        StringBuilder sb = new StringBuilder();
        for (int m : moves) {
            sb.append(MOVE_NAMES[m]).append(" ");
        }
        sb.append("(").append(moves.size()).append(getSolutionSuffix()).append(")");
        return sb.toString();
    }

    /**
     * Get the name of a single move.
     * @param m Move index (0-17)
     * @return Move name string
     */
    protected String getMoveName(int m) {
        return MOVE_NAMES[m];
    }

    /**
     * Validate a cube string and convert to CubieCube.
     * @param cubeString The 54-character facelet string
     * @return FaceCube.Result indicating success or failure with message
     */
    protected FaceCube.Result validateCube(String cubeString) {
        FaceCube fc = new FaceCube();
        FaceCube.Result parseResult = fc.fromString(cubeString);
        if (!parseResult.isSuccess()) {
            return parseResult;
        }
        CubieCube cb = fc.toCubieCube();
        return cb.verify();
    }

    /**
     * Parse a FaceCube from string.
     * @param cubeString The 54-character facelet string
     * @return FaceCube object or null if invalid
     */
    protected FaceCube parseFaceCube(String cubeString) {
        FaceCube fc = new FaceCube();
        FaceCube.Result result = fc.fromString(cubeString);
        return result.isSuccess() ? fc : null;
    }

    // ========== Abstract methods - subclasses MUST implement ==========

    /**
     * Get the default maximum solution length for this solver.
     * @return Default max length
     */
    protected abstract int getDefaultMaxLength();

    /**
     * Get the default timeout in seconds for this solver.
     * @return Default timeout
     */
    protected abstract double getDefaultTimeout();

    // ========== Hook methods - subclasses CAN override ==========

    /**
     * Get the suffix for solution length display.
     * Default is "f" for moves. OptimalSolver overrides to "f*".
     * @return Solution suffix string
     */
    protected String getSolutionSuffix() {
        return "f";
    }
}

