package cube.app;

import cube.model.face.*;
import cube.model.cubie.*;
import cube.moves.MoveTables;
import cube.symmetry.SymmetryTables;
import cube.pruning.PruningTables;
import cube.solver.Solver;
import cube.solver.SolveResult;
import cube.solver.OptimalSolver;
import cube.solver.TwoPhaseSolver;

import java.util.Scanner;

/**
 * Main entry point for the Java Rubik's Cube Solver.
 * Implements an interactive super-loop matching the C++ OptimalSolver.cpp structure.
 * Demonstrates polymorphism by using the Solver interface.
 */
public class Main {

    private static final Scanner scanner = new Scanner(System.in);

    private static final String[] MOVE_NAMES = {
        "U", "U2", "U'", "R", "R2", "R'", "F", "F2", "F'",
        "D", "D2", "D'", "L", "L2", "L'", "B", "B2", "B'"
    };

    public static void main(String[] args) {
        System.out.println("=== Java Rubik's Cube Solver ===\n");

        // Initialize all tables
        long start = System.currentTimeMillis();
        initAllTables();
        double elapsed = (System.currentTimeMillis() - start) / 1000.0;
        System.out.println("Time taken for initialization: " + elapsed + "s\n");

        // Super loop - runs forever until user exits
        while (true) {
            int inputMode = readInputMode();
            String cubeString;
            if (inputMode == 0) {
                cubeString = readFromFacelet();
            } else {
                cubeString = readFromManeuver();
            }

            int solveMode = readSolvingMode();

            start = System.currentTimeMillis();
            if (solveMode == 0) {
                solveFast(cubeString);
            } else if (solveMode == 1) {
                solveOptimal(cubeString);
            } else {
                solveSmartOptimal(cubeString);
            }
            elapsed = (System.currentTimeMillis() - start) / 1000.0;
            System.out.println("Total time taken: " + elapsed + "s\n");
        }
    }

    /**
     * Initialize all lookup tables (move tables, symmetry tables, pruning tables).
     */
    private static void initAllTables() {
        System.out.println("Initializing tables...");

        long start = System.currentTimeMillis();
        MoveTables.init();
        long moveTime = System.currentTimeMillis() - start;

        start = System.currentTimeMillis();
        SymmetryTables.init();
        long symTime = System.currentTimeMillis() - start;

        start = System.currentTimeMillis();
        PruningTables.init();
        long prunTime = System.currentTimeMillis() - start;

        System.out.println("Tables initialized: Move=" + moveTime + "ms, Symmetry=" + symTime + 
                           "ms, Pruning=" + prunTime + "ms");
    }

    /**
     * Read the input mode from user.
     * @return 0 for facelet input, 1 for maneuver input
     */
    private static int readInputMode() {
        System.out.print("Enter the way of defining the cube (0 for Facelet, 1 for a set of moves) : ");
        System.out.flush();
        while (true) {
            if (scanner.hasNextInt()) {
                int mode = scanner.nextInt();
                if (mode == 0 || mode == 1) return mode;
            } else {
                scanner.next(); // consume invalid input
            }
            System.out.print("Invalid Choice, Choose again : ");
            System.out.flush();
        }
    }

    /**
     * Read a cube definition from facelet string.
     * @return Valid facelet string
     */
    private static String readFromFacelet() {
        while (true) {
            System.out.print("Enter the Facelet : ");
            System.out.flush();
            String s = scanner.next();
            FaceCube fc = new FaceCube();
            FaceCube.Result res = fc.fromString(s);
            if (!res.isSuccess()) {
                System.out.println(res.getMessage());
            } else {
                return fc.toString();
            }
        }
    }

    /**
     * Read a cube definition from a sequence of moves.
     * @return Facelet string corresponding to the scrambled cube
     */
    private static String readFromManeuver() {
        System.out.println("Enter the set of moves applied to shuffle the cube");
        scanner.nextLine(); // consume any leftover newline
        while (true) {
            String line = scanner.nextLine();
            String facelet = maneuverToFacelet(line);
            if (facelet.equals("Invalid Move")) {
                System.out.println("Invalid Move, Enter Again");
            } else {
                return facelet;
            }
        }
    }

    /**
     * Convert a sequence of moves to a facelet string.
     * @param line Space-separated moves like "R U R' U'"
     * @return Facelet string or "Invalid Move" if parsing fails
     */
    private static String maneuverToFacelet(String line) {
        CubieCube c = new CubieCube();
        String[] parts = line.trim().split("\\s+");
        for (String s : parts) {
            if (s.isEmpty()) continue;
            
            // Normalize: "R1" -> "R", "R3" -> "R'"
            if (s.endsWith("1")) {
                s = s.substring(0, s.length() - 1);
            } else if (s.endsWith("3")) {
                s = s.substring(0, s.length() - 1) + "'";
            }
            
            int m = parseMoveString(s);
            if (m == -1) {
                return "Invalid Move";
            }
            c.move(m);
        }
        return c.toFaceletCube().toString();
    }

    /**
     * Parse a move string to move index.
     * @param s Move string like "R", "R2", "R'"
     * @return Move index (0-17) or -1 if invalid
     */
    private static int parseMoveString(String s) {
        for (int m = 0; m < 18; m++) {
            if (cmpIgnoreCase(MOVE_NAMES[m], s)) {
                return m;
            }
        }
        return -1;
    }

    /**
     * Case-insensitive string comparison.
     */
    private static boolean cmpIgnoreCase(String s1, String s2) {
        if (s1.length() != s2.length()) return false;
        for (int i = 0; i < s1.length(); i++) {
            if (Character.toLowerCase(s1.charAt(i)) != Character.toLowerCase(s2.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    /**
     * Read the solving mode from user.
     * @return 0 for fast, 1 for optimal, 2 for smart optimal
     */
    private static int readSolvingMode() {
        System.out.println("Enter the Solving mode (0/1/2):");
        System.out.println("0 : Fast Mode (most recommended, gives a very good solution under 1s)");
        System.out.println("1 : Optimal Mode (not recommended, very slow)");
        System.out.println("2 : Smart Optimal Mode (10X faster than Optimal in >90% of cases)");
        System.out.print("Enter the mode : ");
        System.out.flush();
        while (true) {
            if (scanner.hasNextInt()) {
                int mode = scanner.nextInt();
                if (0 <= mode && mode <= 2) return mode;
            } else {
                scanner.next(); // consume invalid input
            }
            System.out.print("Invalid Choice, Choose again : ");
            System.out.flush();
        }
    }

    /**
     * Solve using the fast two-phase solver.
     * Demonstrates polymorphism: Solver interface, TwoPhaseSolver implementation.
     */
    private static void solveFast(String cubeString) {
        System.out.println("Enter two space separated values (target solving length) (timelimit in seconds)");
        int len = scanner.nextInt();
        double timelimit = scanner.nextDouble();

        // Polymorphism: using Solver interface
        Solver solver = new TwoPhaseSolver();
        System.out.println("Using: " + solver.getName() + " - " + solver.getDescription());
        
        SolveResult result = solver.solve(cubeString, len, timelimit);
        System.out.println(result.getMessage());
    }

    /**
     * Solve using the optimal solver.
     * Demonstrates polymorphism: Solver interface, OptimalSolver implementation.
     */
    private static void solveOptimal(String cubeString) {
        // Polymorphism: using Solver interface
        Solver solver = new OptimalSolver();
        System.out.println("Using: " + solver.getName() + " - " + solver.getDescription());
        
        SolveResult result = solver.solve(cubeString);
        System.out.println(result.getMessage());
    }

    /**
     * Solve using the smart optimal approach.
     * First finds a good solution quickly, then tries to improve it.
     */
    private static void solveSmartOptimal(String cubeString) {
        int limit = 18;
        
        // First, find an initial solution using the fast solver
        long start = System.currentTimeMillis();
        Solver fastSolver = new TwoPhaseSolver();
        SolveResult result = fastSolver.solve(cubeString, limit, 10);
        double t = (System.currentTimeMillis() - start) / 1000.0;
        
        int solutionLength = result.getMoveCount();
        System.out.println("Found Initial Solution in " + t + "s : " + result.getMessage());

        // Try to optimize while solution is long and time is short
        while (solutionLength > 17 && t < 1) {
            System.out.print("Optimizing the Solution...");
            System.out.flush();
            limit = solutionLength - 1;
            
            start = System.currentTimeMillis();
            result = fastSolver.solve(cubeString, limit, 10);
            t = (System.currentTimeMillis() - start) / 1000.0;
            
            if (result.getMoveCount() == limit) {
                System.out.println("\nOptimization Successful in " + t + "s : " + result.getMessage());
                solutionLength = result.getMoveCount();
            } else {
                System.out.println("Optimization Failed in " + t + "s");
                break;
            }
        }

        // Try harder with increasing timeouts
        double ti = 10;
        while (solutionLength > 20 && t <= 100) {
            ti *= 2;
            start = System.currentTimeMillis();
            result = fastSolver.solve(cubeString, 20, Math.min(ti, 100.0));
            t = (System.currentTimeMillis() - start) / 1000.0;
            
            if (result.getMoveCount() <= 20) {
                System.out.println("Optimization Successful in " + t + "s : " + result.getMessage());
                solutionLength = result.getMoveCount();
            } else {
                System.out.println("Optimization Failed in " + t + "s");
            }
        }

        // Finally, try to find optimal or prove optimality
        System.out.println("Finding Better Solution / Proving Optimality ...");
        Solver optimalSolver = new OptimalSolver();
        result = optimalSolver.solve(cubeString, solutionLength, 600);
        
        if (result.isSuccess()) {
            System.out.println("Optimal Solution Found : " + result.getMessage());
        } else {
            System.out.println(result.getMessage());
        }
    }

    /**
     * Render a solution as a human-readable string.
     * @param moves Array of move indices
     * @return Formatted solution string
     */
    private static String renderSolution(int[] moves) {
        StringBuilder sb = new StringBuilder();
        for (int m : moves) {
            sb.append(MOVE_NAMES[m]).append(" ");
        }
        sb.append("(").append(moves.length).append("f)");
        return sb.toString();
    }
}
