package cube.solver;

/**
 * Interface defining the contract for all Rubik's Cube solvers.
 * Demonstrates the use of interfaces for abstraction.
 */
public interface Solver {
    
    /**
     * Solve the cube using default parameters.
     * @param cubeString The 54-character facelet string representing the cube
     * @return SolveResult containing success status and solution
     */
    SolveResult solve(String cubeString);
    
    /**
     * Solve the cube with specified parameters.
     * @param cubeString The 54-character facelet string
     * @param maxLength Maximum solution length to search for
     * @param timeout Timeout in seconds
     * @return SolveResult containing success status and solution
     */
    SolveResult solve(String cubeString, int maxLength, double timeout);
    
    /**
     * Get the name of this solver.
     * @return Human-readable solver name
     */
    String getName();
    
    /**
     * Get a description of this solver's characteristics.
     * @return Description string
     */
    String getDescription();
}

