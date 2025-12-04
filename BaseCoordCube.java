package cube.model.coord;

import static cube.model.cubie.Defs.*;

/**
 * Abstract base class for coordinate-level cube representations.
 * Provides common fields and methods shared by CoordCube and TwoPhaseCoordCube.
 * Demonstrates abstraction and inheritance in the coordinate cube hierarchy.
 */
public abstract class BaseCoordCube {

    public static final int SOLVED = 0;

    // Common coordinates (protected for subclass access)
    protected int twist;
    protected int flip;
    protected int sliceSorted;
    protected int corners;

    // ========== Getters (encapsulation) ==========

    public int getTwist() {
        return twist;
    }

    public int getFlip() {
        return flip;
    }

    public int getSliceSorted() {
        return sliceSorted;
    }

    public int getCorners() {
        return corners;
    }

    // ========== Shared helper methods ==========

    /**
     * Get the slice coordinate (sliceSorted / 24).
     * @return The slice value
     */
    protected int getSlice() {
        return sliceSorted / N_PERM_4;
    }

    // ========== Abstract methods - subclasses MUST implement ==========

    /**
     * Check if this coordinate cube represents a solved state.
     * Each solver type has different criteria for "solved".
     * @return true if solved
     */
    public abstract boolean isSolved();

    /**
     * Get the pruning depth estimate for this cube state.
     * Different solvers use different pruning strategies.
     * @return Estimated depth to solution
     */
    public abstract int getDepth();
}

