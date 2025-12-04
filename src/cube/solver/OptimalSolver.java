package cube.solver;

import cube.model.face.FaceCube;
import cube.model.cubie.CubieCube;
import cube.model.coord.CoordCube;
import cube.moves.MoveTables;
import cube.symmetry.SymmetryTables;
import cube.pruning.PruningTables;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static cube.model.cubie.Defs.*;

/**
 * Optimal Solver - finds the shortest possible solution using IDA* with 3-axis pruning.
 * Extends AbstractSolver and implements the Solver interface.
 * Demonstrates inheritance and method overriding (getSolutionSuffix returns "f*").
 */
public class OptimalSolver extends AbstractSolver {

    private static final AtomicBoolean solFound = new AtomicBoolean(false);
    private static final AtomicLong nodeCount = new AtomicLong(0);
    private static volatile List<Integer> solutionMoves = new ArrayList<>();
    private static final Object solutionLock = new Object();

    // ========== Solver interface implementation ==========

    @Override
    public String getName() {
        return "Optimal Solver";
    }

    @Override
    public String getDescription() {
        return "Finds shortest solution (slow, not recommended for casual use)";
    }

    @Override
    protected int getDefaultMaxLength() {
        return 100;
    }

    @Override
    protected double getDefaultTimeout() {
        return 600.0;  // 10 minutes default
    }

    /**
     * Override getSolutionSuffix to return "f*" indicating optimal solution.
     * This demonstrates polymorphism - same method name, different behavior.
     */
    @Override
    protected String getSolutionSuffix() {
        return "f*";
    }

    @Override
    public SolveResult solve(String cubeString, int maxLength, double timeout) {
        FaceCube.Result validation = validateCube(cubeString);
        if (!validation.isSuccess()) {
            return new SolveResult(false, validation.getMessage());
        }

        FaceCube fc = parseFaceCube(cubeString);
        CubieCube cc = fc.toCubieCube();
        CoordCube coc = new CoordCube(cc);

        int udBig = coc.getUbigDepth(0);
        int rlBig = coc.getUbigDepth(1);
        int fbBig = coc.getUbigDepth(2);

        int togo = Math.max(Math.max(Math.max(coc.getUdPhasex24Depth(), coc.getRlPhasex24Depth()), 
                           Math.max(coc.getFbPhasex24Depth(), udBig)),
                           Math.max(rlBig, fbBig));

        solFound.set(false);
        solutionMoves = new ArrayList<>();
        long startTime = System.currentTimeMillis();
        long totalNodes = 0;
        nodeCount.set(0);

        int numThreads = Runtime.getRuntime().availableProcessors();
        if (numThreads == 0) numThreads = 1;
        final int splitDepth = 4;

        while (!solFound.get() && togo < maxLength) {
            long sTime = System.currentTimeMillis();
            totalNodes += nodeCount.get();
            nodeCount.set(0);

            if (togo <= splitDepth) {
                List<Integer> path = new ArrayList<>();
                search(coc.getUdFlip(), coc.getRlFlip(), coc.getFbFlip(),
                       coc.getUdTwist(), coc.getRlTwist(), coc.getFbTwist(),
                       coc.getUdSliceSorted(), coc.getRlSliceSorted(), coc.getFbSliceSorted(),
                       coc.getCorners(),
                       coc.getUdPhasex24Depth(), coc.getRlPhasex24Depth(), coc.getFbPhasex24Depth(),
                       udBig, rlBig, fbBig,
                       coc.getUdTetra(), coc.getRlTetra(), coc.getFbTetra(),
                       togo,
                       path, 0, null);
            } else {
                List<SearchJob> jobs = new ArrayList<>();
                List<Integer> path0 = new ArrayList<>();

                search(coc.getUdFlip(), coc.getRlFlip(), coc.getFbFlip(),
                       coc.getUdTwist(), coc.getRlTwist(), coc.getFbTwist(),
                       coc.getUdSliceSorted(), coc.getRlSliceSorted(), coc.getFbSliceSorted(),
                       coc.getCorners(),
                       coc.getUdPhasex24Depth(), coc.getRlPhasex24Depth(), coc.getFbPhasex24Depth(),
                       udBig, rlBig, fbBig,
                       coc.getUdTetra(), coc.getRlTetra(), coc.getFbTetra(),
                       togo,
                       path0, splitDepth, jobs);

                if (!solFound.get() && !jobs.isEmpty()) {
                    int threadCount = Math.min(numThreads, jobs.size());
                    AtomicInteger jobIndex = new AtomicInteger(0);
                    Thread[] workers = new Thread[threadCount];

                    for (int t = 0; t < threadCount; t++) {
                        workers[t] = new Thread(() -> {
                            List<Integer> localPath = new ArrayList<>();
                            while (!solFound.get()) {
                                int i = jobIndex.getAndIncrement();
                                if (i >= jobs.size()) break;
                                SearchJob job = jobs.get(i);
                                localPath.clear();
                                localPath.addAll(job.path);
                                search(job.udFlip, job.rlFlip, job.fbFlip,
                                       job.udTwist, job.rlTwist, job.fbTwist,
                                       job.udSliceSorted, job.rlSliceSorted, job.fbSliceSorted,
                                       job.corners,
                                       job.udDist, job.rlDist, job.fbDist,
                                       job.udBig, job.rlBig, job.fbBig,
                                       job.udTetra, job.rlTetra, job.fbTetra,
                                       job.togo,
                                       localPath, 0, null);
                            }
                        });
                        workers[t].start();
                    }

                    for (Thread worker : workers) {
                        try { worker.join(); } catch (InterruptedException e) { }
                    }
                }
            }

            if (togo > 13) {
                double elapsed = (System.currentTimeMillis() - sTime) / 1000.0 + 0.0001;
                long n = nodeCount.get();
                System.out.println("depth " + togo + " done in " + String.format("%.2f", elapsed) + " s, " +
                                   n + " nodes generated, about " + (long)(n / elapsed) + " nodes/s");
            }
            togo++;
        }

        double totalTime = (System.currentTimeMillis() - startTime) / 1000.0;
        System.out.println("total time: " + String.format("%.2f", totalTime) + " s, nodes generated: " + 
                           (totalNodes + nodeCount.get()));

        if (togo == maxLength && !solFound.get()) {
            return new SolveResult(false, "Proved Optimal (no solution within " + (maxLength - 1) + " moves)");
        }

        List<Integer> solution;
        synchronized (solutionLock) {
            solution = new ArrayList<>(solutionMoves);
        }

        String solutionStr = renderSolution(solution);
        return new SolveResult(true, solutionStr, solution);
    }

    // ========== Static convenience methods for backward compatibility ==========

    public static SolveResult solveStatic(String cubeString) {
        return new OptimalSolver().solve(cubeString);
    }

    public static SolveResult solveStatic(String cubeString, int maxLength) {
        return new OptimalSolver().solve(cubeString, maxLength, 600.0);
    }

    // ========== Internal classes ==========

    private static class SearchJob {
        int udFlip, rlFlip, fbFlip;
        int udTwist, rlTwist, fbTwist;
        int udSliceSorted, rlSliceSorted, fbSliceSorted;
        int corners;
        int udDist, rlDist, fbDist;
        int udBig, rlBig, fbBig;
        int udTetra, rlTetra, fbTetra;
        int togo;
        List<Integer> path;
    }

    private static void search(
            int udFlip, int rlFlip, int fbFlip,
            int udTwist, int rlTwist, int fbTwist,
            int udSliceSorted, int rlSliceSorted, int fbSliceSorted,
            int corners,
            int udDist, int rlDist, int fbDist,
            int udBig, int rlBig, int fbBig,
            int udTetra, int rlTetra, int fbTetra,
            int togo,
            List<Integer> path,
            int frontierDepthLeft,
            List<SearchJob> jobs) {

        if (solFound.get()) return;

        if (togo == 0) {
            if (corners == 0) {
                if (solFound.compareAndSet(false, true)) {
                    synchronized (solutionLock) {
                        solutionMoves = new ArrayList<>(path);
                    }
                }
            }
            return;
        } else if (jobs != null && frontierDepthLeft == 0) {
            SearchJob job = new SearchJob();
            job.udFlip = udFlip; job.rlFlip = rlFlip; job.fbFlip = fbFlip;
            job.udTwist = udTwist; job.rlTwist = rlTwist; job.fbTwist = fbTwist;
            job.udSliceSorted = udSliceSorted;
            job.rlSliceSorted = rlSliceSorted;
            job.fbSliceSorted = fbSliceSorted;
            job.corners = corners;
            job.udDist = udDist; job.rlDist = rlDist; job.fbDist = fbDist;
            job.udBig = udBig; job.rlBig = rlBig; job.fbBig = fbBig;
            job.udTetra = udTetra; job.rlTetra = rlTetra; job.fbTetra = fbTetra;
            job.togo = togo;
            job.path = new ArrayList<>(path);
            jobs.add(job);
            return;
        } else {
            for (int m = 0; m < N_MOVE; m++) {
                if (!path.isEmpty()) {
                    int diff = path.get(path.size() - 1) / 3 - m / 3;
                    if (diff == 0 || diff == 3) {
                        m += 2;
                        continue;
                    }
                }

                nodeCount.incrementAndGet();

                // Corner pruning
                int corners1 = MoveTables.cornersMove[N_MOVE * corners + m] & 0xFFFF;
                int coDist1 = PruningTables.cornerDepth[corners1] & 0xFF;
                if (coDist1 >= togo) continue;

                // UD axis
                int udTwist1 = MoveTables.twistMove[N_MOVE * udTwist + m] & 0xFFFF;
                int udFlip1 = MoveTables.flipMove[N_MOVE * udFlip + m] & 0xFFFF;
                int udSliceSorted1 = MoveTables.sliceSortedMove[N_MOVE * udSliceSorted + m] & 0xFFFF;

                int fs = N_FLIP * udSliceSorted1 + udFlip1;
                int fsIdx = SymmetryTables.flipslicesortedClassidx[fs];
                int fsSym = SymmetryTables.flipslicesortedSym[fs] & 0xFF;

                int udDist1Mod3 = PruningTables.getFlipslicesortedTwistDepth3(
                    (long) N_TWIST * fsIdx + (SymmetryTables.twistConj[(udTwist1 << 4) + fsSym] & 0xFFFF));
                int udDist1 = PruningTables.dist[3 * udDist + udDist1Mod3] & 0xFF;

                if (udDist1 >= togo) continue;

                // RL axis
                int mrl = SymmetryTables.conjMove[N_MOVE * 16 + m] & 0xFFFF;

                int rlTwist1 = MoveTables.twistMove[N_MOVE * rlTwist + mrl] & 0xFFFF;
                int rlFlip1 = MoveTables.flipMove[N_MOVE * rlFlip + mrl] & 0xFFFF;
                int rlSliceSorted1 = MoveTables.sliceSortedMove[N_MOVE * rlSliceSorted + mrl] & 0xFFFF;

                fs = N_FLIP * rlSliceSorted1 + rlFlip1;
                fsIdx = SymmetryTables.flipslicesortedClassidx[fs];
                fsSym = SymmetryTables.flipslicesortedSym[fs] & 0xFF;

                int rlDist1Mod3 = PruningTables.getFlipslicesortedTwistDepth3(
                    (long) N_TWIST * fsIdx + (SymmetryTables.twistConj[(rlTwist1 << 4) + fsSym] & 0xFFFF));
                int rlDist1 = PruningTables.dist[3 * rlDist + rlDist1Mod3] & 0xFF;

                if (rlDist1 >= togo) continue;

                // FB axis
                int mfb = SymmetryTables.conjMove[N_MOVE * 32 + m] & 0xFFFF;

                int fbTwist1 = MoveTables.twistMove[N_MOVE * fbTwist + mfb] & 0xFFFF;
                int fbFlip1 = MoveTables.flipMove[N_MOVE * fbFlip + mfb] & 0xFFFF;
                int fbSliceSorted1 = MoveTables.sliceSortedMove[N_MOVE * fbSliceSorted + mfb] & 0xFFFF;

                fs = N_FLIP * fbSliceSorted1 + fbFlip1;
                fsIdx = SymmetryTables.flipslicesortedClassidx[fs];
                fsSym = SymmetryTables.flipslicesortedSym[fs] & 0xFF;

                int fbDist1Mod3 = PruningTables.getFlipslicesortedTwistDepth3(
                    (long) N_TWIST * fsIdx + (SymmetryTables.twistConj[(fbTwist1 << 4) + fsSym] & 0xFFFF));
                int fbDist1 = PruningTables.dist[3 * fbDist + fbDist1Mod3] & 0xFF;

                if (fbDist1 >= togo) continue;

                // 3-axis pruning check
                if (udDist1 != 0 && udDist1 == rlDist1 && rlDist1 == fbDist1) {
                    if (udDist1 + 1 >= togo) continue;
                }

                // Tetra coordinates
                int udTetra1 = MoveTables.tetraMove[N_MOVE * udTetra + m] & 0xFFFF;
                int rlTetra1 = MoveTables.tetraMove[N_MOVE * rlTetra + mrl] & 0xFFFF;
                int fbTetra1 = MoveTables.tetraMove[N_MOVE * fbTetra + mfb] & 0xFFFF;

                // UD big pruning
                int sliceUd = udSliceSorted1 / N_PERM_4;
                int fsUd = sliceUd * N_FLIP + udFlip1;
                int fsUdCl = SymmetryTables.flipsliceClassidx[fsUd] & 0xFFFF;
                int fsUdSy = SymmetryTables.flipsliceSym[fsUd] & 0xFF;

                int twistUdC = SymmetryTables.twistConj[(udTwist1 << 4) + fsUdSy] & 0xFFFF;
                int tetraUdC = SymmetryTables.tetraConj[N_SYM_D4h * udTetra1 + fsUdSy] & 0xFFFF;
                int idxUd = N_TWIST * fsUdCl + twistUdC;

                int udMod3 = PruningTables.getUbigMod3(tetraUdC, idxUd);
                int udBig1 = PruningTables.dist[3 * udBig + udMod3] & 0xFF;
                if (udBig1 >= togo) continue;

                // RL big pruning
                int sliceRl = rlSliceSorted1 / N_PERM_4;
                int fsRl = sliceRl * N_FLIP + rlFlip1;
                int fsRlCl = SymmetryTables.flipsliceClassidx[fsRl] & 0xFFFF;
                int fsRlSy = SymmetryTables.flipsliceSym[fsRl] & 0xFF;

                int twistRlC = SymmetryTables.twistConj[(rlTwist1 << 4) + fsRlSy] & 0xFFFF;
                int tetraRlC = SymmetryTables.tetraConj[N_SYM_D4h * rlTetra1 + fsRlSy] & 0xFFFF;
                int idxRl = N_TWIST * fsRlCl + twistRlC;

                int rlMod3 = PruningTables.getUbigMod3(tetraRlC, idxRl);
                int rlBig1 = PruningTables.dist[3 * rlBig + rlMod3] & 0xFF;
                if (rlBig1 >= togo) continue;

                // FB big pruning
                int sliceFb = fbSliceSorted1 / N_PERM_4;
                int fsFb = sliceFb * N_FLIP + fbFlip1;
                int fsFbCl = SymmetryTables.flipsliceClassidx[fsFb] & 0xFFFF;
                int fsFbSy = SymmetryTables.flipsliceSym[fsFb] & 0xFF;

                int twistFbC = SymmetryTables.twistConj[(fbTwist1 << 4) + fsFbSy] & 0xFFFF;
                int tetraFbC = SymmetryTables.tetraConj[N_SYM_D4h * fbTetra1 + fsFbSy] & 0xFFFF;
                int idxFb = N_TWIST * fsFbCl + twistFbC;

                int fbMod3 = PruningTables.getUbigMod3(tetraFbC, idxFb);
                int fbBig1 = PruningTables.dist[3 * fbBig + fbMod3] & 0xFF;
                if (fbBig1 >= togo) continue;

                // 3-axis big pruning check
                if (udBig1 != 0 && udBig1 == rlBig1 && rlBig1 == fbBig1) {
                    if (udBig1 + 1 >= togo) continue;
                }

                // Recurse
                path.add(m);
                int childFrontier = jobs != null ? Math.max(frontierDepthLeft - 1, 0) : 0;
                search(udFlip1, rlFlip1, fbFlip1,
                       udTwist1, rlTwist1, fbTwist1,
                       udSliceSorted1, rlSliceSorted1, fbSliceSorted1,
                       corners1,
                       udDist1, rlDist1, fbDist1,
                       udBig1, rlBig1, fbBig1,
                       udTetra1, rlTetra1, fbTetra1,
                       togo - 1,
                       path,
                       childFrontier,
                       jobs);
                if (solFound.get()) return;
                path.remove(path.size() - 1);
            }
        }
    }
}
