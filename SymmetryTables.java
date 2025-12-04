package cube.symmetry;

import cube.model.face.*;
import cube.model.cubie.CubieCube;
import cube.model.cubie.Defs;
import java.io.*;
import java.util.*;
import java.util.concurrent.*;

import static cube.model.cubie.Defs.*;

// Symmetry related functions. Symmetry considerations increase the performance of the solver.
public class SymmetryTables {

    // Basic symmetry permutations
    // 120° clockwise rotation around the long diagonal URF-DBL
    private static final int[] cpROT_URF3 = {Corner.URF.ordinal(), Corner.DFR.ordinal(), Corner.DLF.ordinal(), Corner.UFL.ordinal(), Corner.UBR.ordinal(), Corner.DRB.ordinal(), Corner.DBL.ordinal(), Corner.ULB.ordinal()};
    private static final int[] coROT_URF3 = {1, 2, 1, 2, 2, 1, 2, 1};
    private static final int[] epROT_URF3 = {Edge.UF.ordinal(), Edge.FR.ordinal(), Edge.DF.ordinal(), Edge.FL.ordinal(), Edge.UB.ordinal(), Edge.BR.ordinal(), Edge.DB.ordinal(), Edge.BL.ordinal(), Edge.UR.ordinal(), Edge.DR.ordinal(), Edge.DL.ordinal(), Edge.UL.ordinal()};
    private static final int[] eoROT_URF3 = {1, 0, 1, 0, 1, 0, 1, 0, 1, 1, 1, 1};

    // 180° rotation around the axis through the F and B centers
    private static final int[] cpROT_F2 = {Corner.DLF.ordinal(), Corner.DFR.ordinal(), Corner.DRB.ordinal(), Corner.DBL.ordinal(), Corner.UFL.ordinal(), Corner.URF.ordinal(), Corner.UBR.ordinal(), Corner.ULB.ordinal()};
    private static final int[] coROT_F2 = {0, 0, 0, 0, 0, 0, 0, 0};
    private static final int[] epROT_F2 = {Edge.DL.ordinal(), Edge.DF.ordinal(), Edge.DR.ordinal(), Edge.DB.ordinal(), Edge.UL.ordinal(), Edge.UF.ordinal(), Edge.UR.ordinal(), Edge.UB.ordinal(), Edge.FL.ordinal(), Edge.FR.ordinal(), Edge.BR.ordinal(), Edge.BL.ordinal()};
    private static final int[] eoROT_F2 = {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};

    // 90° clockwise rotation around the axis through the U and D centers
    private static final int[] cpROT_U4 = {Corner.UBR.ordinal(), Corner.URF.ordinal(), Corner.UFL.ordinal(), Corner.ULB.ordinal(), Corner.DRB.ordinal(), Corner.DFR.ordinal(), Corner.DLF.ordinal(), Corner.DBL.ordinal()};
    private static final int[] coROT_U4 = {0, 0, 0, 0, 0, 0, 0, 0};
    private static final int[] epROT_U4 = {Edge.UB.ordinal(), Edge.UR.ordinal(), Edge.UF.ordinal(), Edge.UL.ordinal(), Edge.DB.ordinal(), Edge.DR.ordinal(), Edge.DF.ordinal(), Edge.DL.ordinal(), Edge.BR.ordinal(), Edge.FR.ordinal(), Edge.FL.ordinal(), Edge.BL.ordinal()};
    private static final int[] eoROT_U4 = {0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 1};

    // Reflection at the plane through the U, D, F, B centers
    private static final int[] cpMIRR_LR2 = {Corner.UFL.ordinal(), Corner.URF.ordinal(), Corner.UBR.ordinal(), Corner.ULB.ordinal(), Corner.DLF.ordinal(), Corner.DFR.ordinal(), Corner.DRB.ordinal(), Corner.DBL.ordinal()};
    private static final int[] coMIRR_LR2 = {3, 3, 3, 3, 3, 3, 3, 3};
    private static final int[] epMIRR_LR2 = {Edge.UL.ordinal(), Edge.UF.ordinal(), Edge.UR.ordinal(), Edge.UB.ordinal(), Edge.DL.ordinal(), Edge.DF.ordinal(), Edge.DR.ordinal(), Edge.DB.ordinal(), Edge.FL.ordinal(), Edge.FR.ordinal(), Edge.BR.ordinal(), Edge.BL.ordinal()};
    private static final int[] eoMIRR_LR2 = {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};

    public static CubieCube[] basicSymCube = new CubieCube[4];
    public static CubieCube[] symCube = new CubieCube[N_SYM];
    public static int[] invIdx = new int[N_SYM];

    // uint16_t in C++
    public static short[] conjMove = new short[N_MOVE * N_SYM];
    public static short[] twistConj = new short[N_TWIST * N_SYM_D4h];
    public static short[] tetraConj = new short[N_TETRA * N_SYM_D4h];
    public static short[] udEdgesConj = new short[N_UD_EDGES * N_SYM_D4h];

    // Symmetry-reduction for (flip, slice_sorted)
    // uint32_t classidx, uint8_t sym, uint32_t rep
    public static int[] flipslicesortedClassidx;
    public static byte[] flipslicesortedSym;
    public static int[] flipslicesortedRep;

    // Symmetry-reduction for (flip, slice)
    // uint16_t classidx, uint8_t sym, uint32_t rep
    public static short[] flipsliceClassidx;
    public static byte[] flipsliceSym;
    public static int[] flipsliceRep;

    // Symmetry-reduction for corners
    // uint16_t classidx, uint8_t sym, uint16_t rep
    public static short[] cornerClassidx;
    public static byte[] cornerSym;
    public static short[] cornerRep;

    private static final int INVALID32 = 0xffffffff;
    private static final short INVALID16 = (short) 0xffff;

    private static boolean initialized = false;
    private static final int NUM_THREADS = Runtime.getRuntime().availableProcessors();

    private static void generateSymCubes() {
        basicSymCube[0] = new CubieCube(cpROT_URF3, coROT_URF3, epROT_URF3, eoROT_URF3);
        basicSymCube[1] = new CubieCube(cpROT_F2, coROT_F2, epROT_F2, eoROT_F2);
        basicSymCube[2] = new CubieCube(cpROT_U4, coROT_U4, epROT_U4, eoROT_U4);
        basicSymCube[3] = new CubieCube(cpMIRR_LR2, coMIRR_LR2, epMIRR_LR2, eoMIRR_LR2);

        CubieCube cc = new CubieCube();
        int idx = 0;
        for (int urf3 = 0; urf3 < 3; ++urf3) {
            for (int f2 = 0; f2 < 2; ++f2) {
                for (int u4 = 0; u4 < 4; ++u4) {
                    for (int lr2 = 0; lr2 < 2; ++lr2) {
                        symCube[idx++] = new CubieCube(cc.getCpArray(), cc.getCoArray(), cc.getEpArray(), cc.getEoArray());
                        cc.multiply(basicSymCube[BasicSymmetry.MIRR_LR2.ordinal()]);
                    }
                    cc.multiply(basicSymCube[BasicSymmetry.ROT_U4.ordinal()]);
                }
                cc.multiply(basicSymCube[BasicSymmetry.ROT_F2.ordinal()]);
            }
            cc.multiply(basicSymCube[BasicSymmetry.ROT_URF3.ordinal()]);
        }
    }

    private static void generateInverseIndices() {
        for (int j = 0; j < N_SYM; ++j) {
            for (int i = 0; i < N_SYM; ++i) {
                CubieCube cc = new CubieCube(symCube[j].getCpArray(), symCube[j].getCoArray(), symCube[j].getEpArray(), symCube[j].getEoArray());
                cc.cornerMultiply(symCube[i]);
                if (cc.getCp(Corner.URF.ordinal()) == Corner.URF.ordinal() &&
                    cc.getCp(Corner.UFL.ordinal()) == Corner.UFL.ordinal() &&
                    cc.getCp(Corner.ULB.ordinal()) == Corner.ULB.ordinal()) {
                    invIdx[j] = i;
                    break;
                }
            }
        }
    }

    private static void generateConjMove() {
        for (int s = 0; s < N_SYM; ++s) {
            for (int m = 0; m < N_MOVE; m++) {
                CubieCube c = new CubieCube(symCube[s].getCpArray(), symCube[s].getCoArray(), symCube[s].getEpArray(), symCube[s].getEoArray());
                c.multiply(CubieCube.moveCube[m]);
                c.multiply(symCube[invIdx[s]]);
                for (int m2 = 0; m2 < N_MOVE; m2++) {
                    if (c.equals(CubieCube.moveCube[m2])) {
                        conjMove[N_MOVE * s + m] = (short) m2;
                        break;
                    }
                }
            }
        }
    }

    private static void buildOrLoadConjTwist() {
        String fname = "conj_twist";
        File f = new File(fname);
        if (f.exists()) {
            loadShortArray(fname, twistConj);
        } else {
            System.out.println("creating conj_twist table...");
            for (int t = 0; t < N_TWIST; t++) {
                CubieCube c = new CubieCube();
                c.setTwist(t);
                for (int s = 0; s < N_SYM_D4h; s++) {
                    CubieCube ss = new CubieCube(symCube[s].getCpArray(), symCube[s].getCoArray(), symCube[s].getEpArray(), symCube[s].getEoArray());
                    ss.cornerMultiply(c);
                    ss.cornerMultiply(symCube[invIdx[s]]);
                    twistConj[N_SYM_D4h * t + s] = (short) ss.getTwist();
                }
            }
            saveShortArray(fname, twistConj);
        }
    }

    private static void buildConjTetra() {
        System.out.println("creating tetra_conj table...");
        CubieCube c = new CubieCube();
        for (int i = 0; i < N_TETRA; i++) {
            c.setTetra(i);
            for (int s = 0; s < N_SYM_D4h; s++) {
                CubieCube ss = new CubieCube(symCube[s].getCpArray(), symCube[s].getCoArray(), symCube[s].getEpArray(), symCube[s].getEoArray());
                ss.cornerMultiply(c);
                ss.cornerMultiply(symCube[invIdx[s]]);
                tetraConj[N_SYM_D4h * i + s] = (short) ss.getTetra();
            }
        }
    }

    private static void buildOrLoadConjUDEdges() {
        String fname = "conj_ud_edges";
        File f = new File(fname);
        if (f.exists()) {
            System.out.println("loading " + fname + " table...");
            loadShortArray(fname, udEdgesConj);
        } else {
            System.out.println("creating " + fname + " table (" + NUM_THREADS + " threads)...");
            ExecutorService exec = Executors.newFixedThreadPool(NUM_THREADS);
            int chunkSize = N_UD_EDGES / NUM_THREADS;
            
            for (int th = 0; th < NUM_THREADS; th++) {
                final int start = th * chunkSize;
                final int end = (th == NUM_THREADS - 1) ? N_UD_EDGES : start + chunkSize;
                exec.submit(() -> {
                    CubieCube c = new CubieCube();
                    for (int t = start; t < end; t++) {
                        c.setUdEdges(t);
                        for (int s = 0; s < N_SYM_D4h; s++) {
                            CubieCube ss = new CubieCube(symCube[s].getCpArray(), symCube[s].getCoArray(), symCube[s].getEpArray(), symCube[s].getEoArray());
                            ss.edgeMultiply(c);
                            ss.edgeMultiply(symCube[invIdx[s]]);
                            udEdgesConj[N_SYM_D4h * t + s] = (short) ss.getUdEdges();
                        }
                    }
                });
            }
            exec.shutdown();
            try { exec.awaitTermination(10, TimeUnit.MINUTES); } catch (InterruptedException e) {}
            System.out.println("done.");
            saveShortArray(fname, udEdgesConj);
        }
    }

    private static void buildOrLoadFlipSliceSortedSymTables() {
        String fname1 = "fs24_classidx";
        String fname2 = "fs24_sym";
        String fname3 = "fs24_rep";
        File f1 = new File(fname1), f2 = new File(fname2), f3 = new File(fname3);

        flipslicesortedClassidx = new int[N_FLIP * N_SLICE_SORTED];
        flipslicesortedSym = new byte[N_FLIP * N_SLICE_SORTED];
        flipslicesortedRep = new int[N_FLIPSLICESORTED_CLASS];

        if (f1.exists() && f2.exists() && f3.exists()) {
            System.out.println("loading flipslicesorted sym-tables...");
            loadIntArray(fname1, flipslicesortedClassidx);
            loadByteArray(fname2, flipslicesortedSym);
            loadIntArray(fname3, flipslicesortedRep);
        } else {
            System.out.println("creating flipslicesorted sym-tables...");
            Arrays.fill(flipslicesortedClassidx, INVALID32);

            int classidx = 0;
            CubieCube cc = new CubieCube();

            for (int slc = 0; slc < N_SLICE_SORTED; ++slc) {
                cc.setSliceSorted(slc);
                for (int flip = 0; flip < N_FLIP; ++flip) {
                    cc.setFlip(flip);
                    int idx = N_FLIP * slc + flip;
                    if ((idx + 1) % 400000 == 0) System.out.print(".");
                    if (flipslicesortedClassidx[idx] == INVALID32) {
                        flipslicesortedClassidx[idx] = classidx;
                        flipslicesortedSym[idx] = 0;
                        flipslicesortedRep[classidx] = idx;
                    } else continue;
                    for (int s = 0; s < N_SYM_D4h; ++s) {
                        CubieCube ss = new CubieCube(symCube[invIdx[s]].getCpArray(), symCube[invIdx[s]].getCoArray(), symCube[invIdx[s]].getEpArray(), symCube[invIdx[s]].getEoArray());
                        ss.edgeMultiply(cc);
                        ss.edgeMultiply(symCube[s]);
                        int idxNew = N_FLIP * ss.getSliceSorted() + ss.getFlip();
                        if (flipslicesortedClassidx[idxNew] == INVALID32) {
                            flipslicesortedClassidx[idxNew] = classidx;
                            flipslicesortedSym[idxNew] = (byte) s;
                        }
                    }
                    ++classidx;
                }
            }
            System.out.println();
            saveIntArray(fname1, flipslicesortedClassidx);
            saveByteArray(fname2, flipslicesortedSym);
            saveIntArray(fname3, flipslicesortedRep);
        }
    }

    private static void buildOrLoadFlipSliceSymTables() {
        String fname1 = "fs_classidx";
        String fname2 = "fs_sym";
        String fname3 = "fs_rep";
        File f1 = new File(fname1), f2 = new File(fname2), f3 = new File(fname3);

        flipsliceClassidx = new short[N_FLIP * N_SLICE];
        flipsliceSym = new byte[N_FLIP * N_SLICE];
        flipsliceRep = new int[N_FLIPSLICE_CLASS];

        if (f1.exists() && f2.exists() && f3.exists()) {
            System.out.println("loading flipslice sym-tables...");
            loadShortArray(fname1, flipsliceClassidx);
            loadByteArray(fname2, flipsliceSym);
            loadIntArray(fname3, flipsliceRep);
        } else {
            System.out.println("creating flipslice sym-tables...");
            Arrays.fill(flipsliceClassidx, INVALID16);

            int classidx = 0;
            CubieCube c = new CubieCube();

            for (int slc = 0; slc < N_SLICE; slc++) {
                c.setSlice(slc);
                for (int flip = 0; flip < N_FLIP; flip++) {
                    c.setFlip(flip);
                    int idx = N_FLIP * slc + flip;
                    if ((idx + 1) % 4000 == 0) System.out.print(".");
                    if ((idx + 1) % 320000 == 0) System.out.println();
                    if (flipsliceClassidx[idx] == INVALID16) {
                        flipsliceClassidx[idx] = (short) classidx;
                        flipsliceSym[idx] = 0;
                        flipsliceRep[classidx] = idx;
                    } else continue;
                    for (int s = 0; s < N_SYM_D4h; s++) {
                        CubieCube ss = new CubieCube(symCube[invIdx[s]].getCpArray(), symCube[invIdx[s]].getCoArray(), symCube[invIdx[s]].getEpArray(), symCube[invIdx[s]].getEoArray());
                        ss.edgeMultiply(c);
                        ss.edgeMultiply(symCube[s]);
                        int idxNew = N_FLIP * ss.getSlice() + ss.getFlip();
                        if (flipsliceClassidx[idxNew] == INVALID16) {
                            flipsliceClassidx[idxNew] = (short) classidx;
                            flipsliceSym[idxNew] = (byte) s;
                        }
                    }
                    classidx++;
                }
            }
            System.out.println();
            saveShortArray(fname1, flipsliceClassidx);
            saveByteArray(fname2, flipsliceSym);
            saveIntArray(fname3, flipsliceRep);
        }
    }

    private static void buildOrLoadCornerSymTables() {
        String fname1 = "co_classidx";
        String fname2 = "co_sym";
        String fname3 = "co_rep";
        File f1 = new File(fname1), f2 = new File(fname2), f3 = new File(fname3);

        cornerClassidx = new short[N_CORNERS];
        cornerSym = new byte[N_CORNERS];
        cornerRep = new short[N_CORNERS_CLASS];

        if (f1.exists() && f2.exists() && f3.exists()) {
            System.out.println("loading corner sym-tables...");
            loadShortArray(fname1, cornerClassidx);
            loadByteArray(fname2, cornerSym);
            loadShortArray(fname3, cornerRep);
        } else {
            System.out.println("creating corner sym-tables...");
            Arrays.fill(cornerClassidx, INVALID16);

            int classidx = 0;
            CubieCube c = new CubieCube();

            for (int cp = 0; cp < N_CORNERS; cp++) {
                c.setCorners(cp);
                if ((cp + 1) % 8000 == 0) System.out.print(".");
                if (cornerClassidx[cp] == INVALID16) {
                    cornerClassidx[cp] = (short) classidx;
                    cornerSym[cp] = 0;
                    cornerRep[classidx] = (short) cp;
                } else continue;
                for (int s = 0; s < N_SYM_D4h; s++) {
                    CubieCube ss = new CubieCube(symCube[invIdx[s]].getCpArray(), symCube[invIdx[s]].getCoArray(), symCube[invIdx[s]].getEpArray(), symCube[invIdx[s]].getEoArray());
                    ss.cornerMultiply(c);
                    ss.cornerMultiply(symCube[s]);
                    int cpNew = ss.getCorners();
                    if (cornerClassidx[cpNew] == INVALID16) {
                        cornerClassidx[cpNew] = (short) classidx;
                        cornerSym[cpNew] = (byte) s;
                    }
                }
                classidx++;
            }
            System.out.println();
            saveShortArray(fname1, cornerClassidx);
            saveByteArray(fname2, cornerSym);
            saveShortArray(fname3, cornerRep);
        }
    }

    public static void init() {
        if (initialized) return;
        System.out.println("Initializing symmetry tables...");
        generateSymCubes();
        generateInverseIndices();
        generateConjMove();
        buildOrLoadConjTwist();
        buildConjTetra();
        buildOrLoadFlipSliceSortedSymTables();
        buildOrLoadConjUDEdges();
        buildOrLoadFlipSliceSymTables();
        buildOrLoadCornerSymTables();
        initialized = true;
        System.out.println("Symmetry tables initialized.");
    }

    // I/O helpers for different types - all use LITTLE_ENDIAN to match C++
    private static void saveIntArray(String fname, int[] arr) {
        try (FileOutputStream fos = new FileOutputStream(fname);
             BufferedOutputStream bos = new BufferedOutputStream(fos, 65536)) {
            java.nio.ByteBuffer bb = java.nio.ByteBuffer.allocate(arr.length * 4);
            bb.order(java.nio.ByteOrder.LITTLE_ENDIAN);
            bb.asIntBuffer().put(arr);
            bos.write(bb.array());
        } catch (IOException e) {
            System.err.println("Error saving " + fname + ": " + e.getMessage());
        }
    }

    private static void loadIntArray(String fname, int[] arr) {
        try (FileInputStream fis = new FileInputStream(fname);
             BufferedInputStream bis = new BufferedInputStream(fis, 65536)) {
            byte[] bytes = bis.readAllBytes();
            java.nio.ByteBuffer bb = java.nio.ByteBuffer.wrap(bytes);
            bb.order(java.nio.ByteOrder.LITTLE_ENDIAN);
            bb.asIntBuffer().get(arr);
        } catch (IOException e) {
            System.err.println("Error loading " + fname + ": " + e.getMessage());
        }
    }

    private static void saveShortArray(String fname, short[] arr) {
        try (FileOutputStream fos = new FileOutputStream(fname);
             BufferedOutputStream bos = new BufferedOutputStream(fos, 65536)) {
            java.nio.ByteBuffer bb = java.nio.ByteBuffer.allocate(arr.length * 2);
            bb.order(java.nio.ByteOrder.LITTLE_ENDIAN);
            bb.asShortBuffer().put(arr);
            bos.write(bb.array());
        } catch (IOException e) {
            System.err.println("Error saving " + fname + ": " + e.getMessage());
        }
    }

    private static void loadShortArray(String fname, short[] arr) {
        try (FileInputStream fis = new FileInputStream(fname);
             BufferedInputStream bis = new BufferedInputStream(fis, 65536)) {
            byte[] bytes = bis.readAllBytes();
            java.nio.ByteBuffer bb = java.nio.ByteBuffer.wrap(bytes);
            bb.order(java.nio.ByteOrder.LITTLE_ENDIAN);
            bb.asShortBuffer().get(arr);
        } catch (IOException e) {
            System.err.println("Error loading " + fname + ": " + e.getMessage());
        }
    }

    private static void saveByteArray(String fname, byte[] arr) {
        try (FileOutputStream fos = new FileOutputStream(fname);
             BufferedOutputStream bos = new BufferedOutputStream(fos, 65536)) {
            bos.write(arr);
        } catch (IOException e) {
            System.err.println("Error saving " + fname + ": " + e.getMessage());
        }
    }

    private static void loadByteArray(String fname, byte[] arr) {
        try (FileInputStream fis = new FileInputStream(fname);
             BufferedInputStream bis = new BufferedInputStream(fis, 65536)) {
            bis.read(arr);
        } catch (IOException e) {
            System.err.println("Error loading " + fname + ": " + e.getMessage());
        }
    }
}
