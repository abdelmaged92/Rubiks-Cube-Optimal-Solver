package cube.solver;

import cube.model.face.FaceCube;
import cube.model.face.Move;
import cube.model.cubie.CubieCube;
import cube.model.coord.TwoPhaseCoordCube;
import cube.moves.MoveTables;
import cube.symmetry.SymmetryTables;
import cube.pruning.PruningTables;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static cube.model.cubie.Defs.*;

/**
 * Two-Phase Solver - fast solver that finds good (not necessarily optimal) solutions.
 * Extends AbstractSolver and implements the Solver interface.
 * Demonstrates inheritance and method overriding.
 */
public class TwoPhaseSolver extends AbstractSolver {

    // ========== Solver interface implementation ==========

    @Override
    public String getName() {
        return "Two-Phase Solver";
    }

    @Override
    public String getDescription() {
        return "Fast solver (recommended, finds good solution under 1s)";
    }

    @Override
    protected int getDefaultMaxLength() {
        return 20;
    }

    @Override
    protected double getDefaultTimeout() {
        return 3.0;
    }

    // Note: getSolutionSuffix() is NOT overridden, so it uses parent's "f"

    @Override
    public SolveResult solve(String cubeString, int maxLength, double timeout) {
        FaceCube.Result validation = validateCube(cubeString);
        if (!validation.isSuccess()) {
            return new SolveResult(false, validation.getMessage());
        }

        FaceCube fc = parseFaceCube(cubeString);
        CubieCube cb = fc.toCubieCube();

        SharedState S = new SharedState();
        long startTime = System.currentTimeMillis();
        List<Integer> syms = cb.symmetries();

        List<Integer> tr = new ArrayList<>();
        boolean hasRot = false;
        boolean hasAnti = false;
        for (int x : syms) {
            if (x == 16 || x == 20 || x == 24 || x == 28) hasRot = true;
            if (x >= 48 && x < 96) hasAnti = true;
        }

        if (hasRot) {
            tr.add(0); tr.add(3);
        } else {
            for (int i = 0; i < 6; i++) tr.add(i);
        }
        if (hasAnti) {
            List<Integer> tmp = new ArrayList<>();
            for (int x : tr) if (x < 3) tmp.add(x);
            tr = tmp;
        }

        List<Thread> workers = new ArrayList<>();
        for (int i : tr) {
            int rot = i % 3, inv = i / 3;
            SolverThread solver = new SolverThread(cb, rot, inv, maxLength, timeout, startTime, S);
            Thread t = new Thread(solver);
            workers.add(t);
            t.start();
        }

        for (Thread t : workers) {
            try { t.join(); } catch (InterruptedException e) { }
        }

        if (S.solutions.isEmpty()) {
            return new SolveResult(false, "No solution found");
        }

        List<Integer> solution = S.solutions.get(S.solutions.size() - 1);
        String solutionStr = renderSolution(solution);

        return new SolveResult(true, solutionStr, solution);
    }

    // ========== Static convenience methods for backward compatibility ==========

    public static SolveResult solveStatic(String cubeString) {
        return new TwoPhaseSolver().solve(cubeString);
    }

    public static SolveResult solveStatic(String cubeString, int maxLength, int timeout) {
        return new TwoPhaseSolver().solve(cubeString, maxLength, timeout);
    }

    // ========== Internal classes ==========

    private static class SharedState {
        List<List<Integer>> solutions = Collections.synchronizedList(new ArrayList<>());
        AtomicInteger shortestLen = new AtomicInteger(999);
        AtomicBoolean terminated = new AtomicBoolean(false);
    }

    private class SolverThread implements Runnable {
        CubieCube cbCube;
        TwoPhaseCoordCube coCube;
        int rot;
        int inv;
        int retLength;
        double timeout;
        int cornerSave = 0;
        List<Integer> sofarPhase1 = new ArrayList<>();
        List<Integer> sofarPhase2 = new ArrayList<>();
        boolean phase2Done = false;
        long startTime;
        SharedState S;

        SolverThread(CubieCube cb, int rot, int inv, int retLen, double timeout, long startTime, SharedState S) {
            this.cbCube = new CubieCube(cb.getCpArray(), cb.getCoArray(), cb.getEpArray(), cb.getEoArray());
            this.rot = rot;
            this.inv = inv;
            this.retLength = retLen;
            this.timeout = timeout;
            this.startTime = startTime;
            this.S = S;
        }

        static int invertMove(int m) {
            int f = m / 3, t = m % 3;
            return 3 * f + (2 - t);
        }

        static boolean isPhase2ForbiddenQuarter(int m) {
            return (m == 3 || m == 5 || m == 6 || m == 8 || m == 12 || m == 14 || m == 15 || m == 17);
        }

        static boolean badSuccessive(int a, int b) {
            int da = a / 3, db = b / 3, diff = da - db;
            return diff == 0 || diff == 3;
        }

        void storeSolutionIfBetter() {
            List<Integer> curSol = new ArrayList<>(sofarPhase1);
            curSol.addAll(sofarPhase2);
            if (inv == 1) {
                Collections.reverse(curSol);
                for (int i = 0; i < curSol.size(); i++) {
                    curSol.set(i, invertMove(curSol.get(i)));
                }
            }
            for (int i = 0; i < curSol.size(); i++) {
                int m = curSol.get(i);
                curSol.set(i, SymmetryTables.conjMove[N_MOVE * (16 * rot) + m] & 0xFFFF);
            }
            synchronized (S) {
                if (curSol.size() < S.shortestLen.get()) {
                    S.solutions.add(new ArrayList<>(curSol));
                    S.shortestLen.set(curSol.size());
                    if (S.shortestLen.get() <= retLength) {
                        S.terminated.set(true);
                    }
                }
            }
        }

        void searchPhase2(int corners, int udEdges, int sliceSorted, int disti, int togoPhase2) {
            if (S.terminated.get() || phase2Done) return;
            if (togoPhase2 == 0 && sliceSorted == 0) {
                storeSolutionIfBetter();
                phase2Done = true;
            } else {
                for (int m = 0; m < N_MOVE; m++) {
                    if (isPhase2ForbiddenQuarter(m)) continue;
                    if (!sofarPhase2.isEmpty()) {
                        if (badSuccessive(sofarPhase2.get(sofarPhase2.size() - 1), m)) continue;
                    } else if (!sofarPhase1.isEmpty()) {
                        if (badSuccessive(sofarPhase1.get(sofarPhase1.size() - 1), m)) continue;
                    }

                    int cornersNew = MoveTables.cornersMove[18 * corners + m] & 0xFFFF;
                    int udEdgesNew = MoveTables.udEdgesMove[18 * udEdges + m] & 0xFFFF;
                    int sliceSortedNew = MoveTables.sliceSortedMove[18 * sliceSorted + m] & 0xFFFF;
                    int classidx = SymmetryTables.cornerClassidx[cornersNew] & 0xFFFF;
                    int sym = SymmetryTables.cornerSym[cornersNew] & 0xFF;
                    int distNewMod3 = PruningTables.getCornersUdEdgesDepth3(
                        N_UD_EDGES * classidx + (SymmetryTables.udEdgesConj[(udEdgesNew << 4) + sym] & 0xFFFF));
                    int distNew = PruningTables.dist[3 * disti + distNewMod3] & 0xFF;
                    int cornslice = PruningTables.cornsliceDepth[24 * cornersNew + sliceSortedNew] & 0xFF;
                    if (Math.max(distNew, cornslice) >= togoPhase2) continue;

                    sofarPhase2.add(m);
                    searchPhase2(cornersNew, udEdgesNew, sliceSortedNew, distNew, togoPhase2 - 1);
                    sofarPhase2.remove(sofarPhase2.size() - 1);
                    if (S.terminated.get() || phase2Done) return;
                }
            }
        }

        void search(int flip, int twist, int sliceSorted, int disti, int togoPhase1) {
            if (S.terminated.get()) return;
            if (togoPhase1 == 0) {
                long now = System.currentTimeMillis();
                if ((now - startTime) / 1000.0 > timeout && S.shortestLen.get() < 999) {
                    S.terminated.set(true);
                    return;
                }

                int m = sofarPhase1.isEmpty() ? Move.U1.ordinal() : sofarPhase1.get(sofarPhase1.size() - 1);

                int corners;
                if (m == Move.R3.ordinal() || m == Move.F3.ordinal() || m == Move.L3.ordinal() || m == Move.B3.ordinal()) {
                    corners = MoveTables.cornersMove[18 * cornerSave + (m - 1)] & 0xFFFF;
                } else {
                    corners = coCube.getCorners();
                    for (int mm : sofarPhase1) {
                        corners = MoveTables.cornersMove[18 * corners + mm] & 0xFFFF;
                    }
                    cornerSave = corners;
                }

                int togo2Limit = Math.min(S.shortestLen.get() - sofarPhase1.size(), 11);
                if ((PruningTables.cornsliceDepth[24 * corners + sliceSorted] & 0xFF) >= togo2Limit) return;

                int uEdges = coCube.getUEdges();
                int dEdges = coCube.getDEdges();
                for (int mm : sofarPhase1) {
                    uEdges = MoveTables.uEdgesMove[18 * uEdges + mm] & 0xFFFF;
                    dEdges = MoveTables.dEdgesMove[18 * dEdges + mm] & 0xFFFF;
                }
                int udEdges = PruningTables.uEdgesPlusDEdgesToUdEdges[24 * uEdges + (dEdges % 24)] & 0xFFFF;
                int dist2 = TwoPhaseCoordCube.getDepthPhase2(corners, udEdges);

                for (int togo2 = dist2; togo2 < togo2Limit; togo2++) {
                    sofarPhase2.clear();
                    phase2Done = false;
                    searchPhase2(corners, udEdges, sliceSorted, dist2, togo2);
                    if (phase2Done || S.terminated.get()) break;
                }
                return;
            } else {
                for (int m = 0; m < N_MOVE; m++) {
                    if (disti == 0 && togoPhase1 < 5 &&
                        (m == Move.U1.ordinal() || m == Move.U2.ordinal() || m == Move.U3.ordinal() ||
                         m == Move.R2.ordinal() || m == Move.F2.ordinal() ||
                         m == Move.D1.ordinal() || m == Move.D2.ordinal() || m == Move.D3.ordinal() ||
                         m == Move.L2.ordinal() || m == Move.B2.ordinal())) continue;

                    if (!sofarPhase1.isEmpty()) {
                        if (badSuccessive(sofarPhase1.get(sofarPhase1.size() - 1), m)) continue;
                    }

                    int flipNew = MoveTables.flipMove[18 * flip + m] & 0xFFFF;
                    int twistNew = MoveTables.twistMove[18 * twist + m] & 0xFFFF;
                    int sliceSortedNew = MoveTables.sliceSortedMove[18 * sliceSorted + m] & 0xFFFF;
                    int flipslice = N_FLIP * (sliceSortedNew / N_PERM_4) + flipNew;
                    int classidx = SymmetryTables.flipsliceClassidx[flipslice] & 0xFFFF;
                    int sym = SymmetryTables.flipsliceSym[flipslice] & 0xFF;
                    int distNewMod3 = PruningTables.getFlipsliceTwistDepth3(
                        N_TWIST * classidx + (SymmetryTables.twistConj[(twistNew << 4) + sym] & 0xFFFF));
                    int distNew = PruningTables.dist[3 * disti + distNewMod3] & 0xFF;
                    if (distNew >= togoPhase1) continue;

                    sofarPhase1.add(m);
                    search(flipNew, twistNew, sliceSortedNew, distNew, togoPhase1 - 1);
                    sofarPhase1.remove(sofarPhase1.size() - 1);
                }
            }
        }

        @Override
        public void run() {
            CubieCube cb;
            if (rot == 0) {
                cb = cbCube;
            } else if (rot == 1) {
                cb = new CubieCube(SymmetryTables.symCube[32].getCpArray(), SymmetryTables.symCube[32].getCoArray(),
                                   SymmetryTables.symCube[32].getEpArray(), SymmetryTables.symCube[32].getEoArray());
                cb.multiply(cbCube);
                cb.multiply(SymmetryTables.symCube[16]);
            } else {
                cb = new CubieCube(SymmetryTables.symCube[16].getCpArray(), SymmetryTables.symCube[16].getCoArray(),
                                   SymmetryTables.symCube[16].getEpArray(), SymmetryTables.symCube[16].getEoArray());
                cb.multiply(cbCube);
                cb.multiply(SymmetryTables.symCube[32]);
            }
            if (inv == 1) {
                CubieCube tmp = new CubieCube();
                cb.invCubieCube(tmp);
                cb = tmp;
            }
            cbCube = cb;
            coCube = new TwoPhaseCoordCube(cb);
            int dist = coCube.getDepthPhase1();
            for (int togo1 = dist; togo1 < 20; togo1++) {
                sofarPhase1.clear();
                search(coCube.getFlip(), coCube.getTwist(), coCube.getSliceSorted(), dist, togo1);
                if (S.terminated.get()) break;
            }
        }
    }
}
