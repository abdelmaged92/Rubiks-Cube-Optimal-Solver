package cube.pruning;

import cube.model.cubie.CubieCube;
import cube.model.face.Edge;
import cube.model.face.Move;
import cube.symmetry.SymmetryTables;
import cube.moves.MoveTables;
import java.io.*;
import java.util.*;

import static cube.model.cubie.Defs.*;

// The pruning tables cut the search tree during the search
// The pruning values are stored modulo 3 which saves a lot of memory
public class PruningTables {

    // Two phase solver pruning tables
    public static int[] flipsliceTwistDepth3;       // packed 2-bit entries
    public static int[] cornersUdEdgesDepth3;       // packed 2-bit entries
    public static byte[] cornsliceDepth;
    
    // Optimal solver pruning tables
    public static int[] flipslicesortedTwistDepth3; // packed 2-bit entries
    public static byte[] cornerDepth;
    public static byte[] ubigPF;                    // Ultra-big pruning table (packed 5 trits/byte)

    // Distance lookup table
    public static byte[] dist = new byte[60];

    // Phase 2 edge merge table (uint16_t in C++)
    public static short[] uEdgesPlusDEdgesToUdEdges;

    // Lookup table for unpacking 5 trits from a byte
    private static byte[][] GETPACKED = new byte[243][5];

    private static boolean initialized = false;

    // Constants for ubigPF
    private static final long ENTRIES_PER_TETRA = (long) N_FLIPSLICE_CLASS * N_TWIST;
    private static final int TRITS_PER_BYTE = 5;
    private static final long BYTES_PER_TETRA = ENTRIES_PER_TETRA / TRITS_PER_BYTE;
    private static final long WORDS_PER_TETRA_2BIT = (ENTRIES_PER_TETRA + 15) / 16;
    private static final long G_SPLIT = (long) (N_FLIPSLICE_CLASS / 5) * N_TWIST;

    // ========================= Packed 2-bit accessors =========================
    
    public static int getFlipsliceTwistDepth3(int idx) {
        int y = flipsliceTwistDepth3[idx >> 4];
        y >>= (idx & 15) * 2;
        return y & 3;
    }

    public static void setFlipsliceTwistDepth3(int idx, int value) {
        int shift = (idx & 15) * 2;
        flipsliceTwistDepth3[idx >> 4] &= ~(3 << shift);
        flipsliceTwistDepth3[idx >> 4] |= (value & 3) << shift;
    }

    public static int getCornersUdEdgesDepth3(int idx) {
        int y = cornersUdEdgesDepth3[idx >> 4];
        y >>= (idx & 15) * 2;
        return y & 3;
    }

    public static void setCornersUdEdgesDepth3(int idx, int value) {
        int shift = (idx & 15) * 2;
        cornersUdEdgesDepth3[idx >> 4] &= ~(3 << shift);
        cornersUdEdgesDepth3[idx >> 4] |= (value & 3) << shift;
    }

    public static int getFlipslicesortedTwistDepth3(long idx) {
        int y = flipslicesortedTwistDepth3[(int)(idx >> 4)];
        y >>= (int)(idx & 15) * 2;
        return y & 3;
    }

    public static void setFlipslicesortedTwistDepth3(long idx, int value) {
        int shift = (int)(idx & 15) * 2;
        flipslicesortedTwistDepth3[(int)(idx >> 4)] &= ~(3 << shift);
        flipslicesortedTwistDepth3[(int)(idx >> 4)] |= (value & 3) << shift;
    }

    // ========================= UbigPF accessors =========================
    
    public static int getUbigMod3(int tetra, int idx) {
        if (ubigPF == null) return 0;
        long offset = (long) tetra * BYTES_PER_TETRA;
        long g4 = 4 * G_SPLIT;
        if (idx < g4) {
            int base = idx >> 2;
            int off = idx & 3;
            int b = ubigPF[(int)(offset + base)] & 0xFF;
            return GETPACKED[b][off];
        } else {
            int base = (int)(idx - g4);
            int b = ubigPF[(int)(offset + base)] & 0xFF;
            return GETPACKED[b][4];
        }
    }

    private static byte pack5(int a, int b, int c, int d, int e) {
        return (byte)(a + b * 3 + c * 9 + d * 27 + e * 81);
    }

    private static void initGetpacked() {
        for (int b = 0; b < 243; b++) {
            int v = b;
            for (int slot = 0; slot < 5; slot++) {
                GETPACKED[b][slot] = (byte)(v % 3);
                v /= 3;
            }
        }
    }

    // ========================= Distance table =========================
    
    private static void buildDistance() {
        for (int i = 0; i < 60; i++) dist[i] = 0;
        for (int i = 0; i < 20; i++) {
            for (int j = 0; j < 3; j++) {
                int v = (i / 3) * 3 + j;
                if (i % 3 == 2 && j == 0) v += 3;
                else if (i % 3 == 0 && j == 2) v -= 3;
                dist[3 * i + j] = (byte) v;
            }
        }
    }

    // ========================= Corner pruning table =========================
    
    private static void createCornerPrunTable() {
        String fname = "cornerprun";
        File f = new File(fname);

        if (f.exists()) {
            System.out.println("loading " + fname + " table...");
            cornerDepth = new byte[N_CORNERS];
            loadByteArray(fname, cornerDepth);
        } else {
            System.out.println("creating " + fname + " table...");
            cornerDepth = new byte[N_CORNERS];
            Arrays.fill(cornerDepth, (byte) -1);
            cornerDepth[0] = 0;
            int done = 1;
            int depth = 0;

            while (done < N_CORNERS) {
                for (int corners = 0; corners < N_CORNERS; corners++) {
                    if (cornerDepth[corners] == depth) {
                        for (int m = 0; m < N_MOVE; m++) {
                            int corners1 = MoveTables.cornersMove[N_MOVE * corners + m] & 0xFFFF;
                            if (cornerDepth[corners1] == -1) {
                                cornerDepth[corners1] = (byte) (depth + 1);
                                done++;
                                if (done % 2000 == 0) System.out.print(".");
                            }
                        }
                    }
                }
                depth++;
            }
            System.out.println();
            saveByteArray(fname, cornerDepth);
        }
    }

    // ========================= Phase 1 pruning table (two-phase solver) =========================
    
    private static void createPhase1PrunTable() {
        String fname = "phase1_prun";
        File f = new File(fname);
        int total = N_FLIPSLICE_CLASS * N_TWIST;

        if (f.exists()) {
            System.out.println("loading phase1_prun table...");
            flipsliceTwistDepth3 = new int[total / 16 + 1];
            loadIntArray(fname, flipsliceTwistDepth3);
        } else {
            System.out.println("creating phase1_prun table...");
            System.out.println("This may take some time depending on the hardware.");
            flipsliceTwistDepth3 = new int[total / 16 + 1];
            Arrays.fill(flipsliceTwistDepth3, 0xffffffff);

            // Create table with the symmetries of the flipslice classes
            CubieCube c = new CubieCube();
            int[] fsSym = new int[N_FLIPSLICE_CLASS];
            for (int i = 0; i < N_FLIPSLICE_CLASS; i++) {
                if ((i + 1) % 1000 == 0) System.out.print(".");
                int rep = SymmetryTables.flipsliceRep[i];
                c.setSlice(rep / N_FLIP);
                c.setFlip(rep % N_FLIP);
                for (int s = 0; s < N_SYM_D4h; s++) {
                    CubieCube ss = new CubieCube(SymmetryTables.symCube[s].getCpArray(), SymmetryTables.symCube[s].getCoArray(),
                                                  SymmetryTables.symCube[s].getEpArray(), SymmetryTables.symCube[s].getEoArray());
                    ss.edgeMultiply(c);
                    ss.edgeMultiply(SymmetryTables.symCube[SymmetryTables.invIdx[s]]);
                    if (ss.getSlice() == rep / N_FLIP && ss.getFlip() == rep % N_FLIP)
                        fsSym[i] |= (1 << s);
                }
            }
            System.out.println();

            int fsClassidx = 0;
            int twist = 0;
            setFlipsliceTwistDepth3(N_TWIST * fsClassidx + twist, 0);
            int done = 1;
            int depth = 0;
            boolean backsearch = false;
            System.out.println("depth: " + depth + " done: " + done + "/" + total);

            while (done != total) {
                int depth3 = depth % 3;
                if (depth == 9) {
                    System.out.println("flipping to backwards search...");
                    backsearch = true;
                }
                int mult = (depth < 8 ? 5 : 1);
                int idx = 0;
                for (fsClassidx = 0; fsClassidx < N_FLIPSLICE_CLASS; fsClassidx++) {
                    if ((fsClassidx + 1) % (200 * mult) == 0) System.out.print(".");
                    if ((fsClassidx + 1) % (16000 * mult) == 0) System.out.println();
                    twist = 0;
                    while (twist < N_TWIST) {
                        if (!backsearch && (idx & 15) == 0 && flipsliceTwistDepth3[idx >> 4] == 0xffffffff && twist < N_TWIST - 16) {
                            twist += 16;
                            idx += 16;
                            continue;
                        }
                        boolean match = backsearch ? (getFlipsliceTwistDepth3(idx) == 3) : (getFlipsliceTwistDepth3(idx) == depth3);
                        if (match) {
                            int flipslice = SymmetryTables.flipsliceRep[fsClassidx];
                            int flip = flipslice % N_FLIP;
                            int slice = flipslice / N_FLIP;
                            for (int m = 0; m < N_MOVE; m++) {
                                int twist1 = MoveTables.twistMove[N_MOVE * twist + m] & 0xFFFF;
                                int flip1 = MoveTables.flipMove[N_MOVE * flip + m] & 0xFFFF;
                                int slice1 = (MoveTables.sliceSortedMove[(N_MOVE * N_PERM_4) * slice + m] & 0xFFFF) / N_PERM_4;
                                int flipslice1 = slice1 * N_FLIP + flip1;
                                int fs1Classidx = SymmetryTables.flipsliceClassidx[flipslice1] & 0xFFFF;
                                int fs1Sym = SymmetryTables.flipsliceSym[flipslice1] & 0xFF;
                                twist1 = SymmetryTables.twistConj[(twist1 << 4) + fs1Sym] & 0xFFFF;
                                int idx1 = N_TWIST * fs1Classidx + twist1;
                                if (!backsearch) {
                                    if (getFlipsliceTwistDepth3(idx1) == 3) {
                                        setFlipsliceTwistDepth3(idx1, (depth + 1) % 3);
                                        done++;
                                        int sym = fsSym[fs1Classidx];
                                        if (sym != 1) {
                                            for (int k = 1; k < 16; k++) {
                                                sym >>= 1;
                                                if ((sym & 1) != 0) {
                                                    int twist2 = SymmetryTables.twistConj[(twist1 << 4) + k] & 0xFFFF;
                                                    int idx2 = N_TWIST * fs1Classidx + twist2;
                                                    if (getFlipsliceTwistDepth3(idx2) == 3) {
                                                        setFlipsliceTwistDepth3(idx2, (depth + 1) % 3);
                                                        done++;
                                                    }
                                                }
                                            }
                                        }
                                    }
                                } else {
                                    if (getFlipsliceTwistDepth3(idx1) == depth3) {
                                        setFlipsliceTwistDepth3(idx, (depth + 1) % 3);
                                        done++;
                                        break;
                                    }
                                }
                            }
                        }
                        twist++;
                        idx++;
                    }
                }
                depth++;
                System.out.println();
                System.out.println("depth: " + depth + " done: " + done + "/" + total);
            }
            saveIntArray(fname, flipsliceTwistDepth3);
        }
    }

    // ========================= Phase 1x24 pruning table (optimal solver) =========================
    
    private static void createPhase1x24PrunTable() {
        String fname = "phase1x24_prun";
        File f = new File(fname);
        long total = (long) N_FLIPSLICESORTED_CLASS * N_TWIST;

        if (f.exists()) {
            System.out.println("loading " + fname + " table...");
            flipslicesortedTwistDepth3 = new int[(int)(total / 16 + 1)];
            loadIntArray(fname, flipslicesortedTwistDepth3);
        } else {
            System.out.println("creating " + fname + " table...");
            System.out.println("This may take some time depending on the hardware.");
            flipslicesortedTwistDepth3 = new int[(int)(total / 16 + 1)];
            Arrays.fill(flipslicesortedTwistDepth3, 0xffffffff);

            // Create table with the symmetries of the flipslicesorted classes
            CubieCube cc = new CubieCube();
            int[] fsSym = new int[N_FLIPSLICESORTED_CLASS];
            for (int i = 0; i < N_FLIPSLICESORTED_CLASS; i++) {
                if ((i + 1) % 48000 == 0) System.out.print(".");
                int rep = SymmetryTables.flipslicesortedRep[i];
                cc.setSliceSorted(rep / N_FLIP);
                cc.setFlip(rep % N_FLIP);
                for (int s = 0; s < N_SYM_D4h; s++) {
                    CubieCube ss = new CubieCube(SymmetryTables.symCube[s].getCpArray(), SymmetryTables.symCube[s].getCoArray(),
                                                  SymmetryTables.symCube[s].getEpArray(), SymmetryTables.symCube[s].getEoArray());
                    ss.edgeMultiply(cc);
                    ss.edgeMultiply(SymmetryTables.symCube[SymmetryTables.invIdx[s]]);
                    if (ss.getSliceSorted() == rep / N_FLIP && ss.getFlip() == rep % N_FLIP)
                        fsSym[i] |= (1 << s);
                }
            }
            System.out.println();

            setFlipslicesortedTwistDepth3(0, 0);
            long done = 1;
            int depth = 0;
            boolean backsearch = false;
            System.out.println("depth: " + depth + " done: " + done + "/" + total);

            while (done != total) {
                int depth3 = depth % 3;
                if (depth == 10) {
                    System.out.println("flipping to backwards search...");
                    backsearch = true;
                }
                long idx = 0;
                for (int fsClassidx = 0; fsClassidx < N_FLIPSLICESORTED_CLASS; fsClassidx++) {
                    if ((fsClassidx + 1) % 20000 == 0) System.out.print(".");
                    if ((fsClassidx + 1) % 1600000 == 0) System.out.println();

                    int twist = 0;
                    while (twist < N_TWIST) {
                        if (!backsearch && idx % 16 == 0 && flipslicesortedTwistDepth3[(int)(idx / 16)] == 0xffffffff && twist < N_TWIST - 16) {
                            twist += 16;
                            idx += 16;
                            continue;
                        }
                        boolean match = backsearch ? (getFlipslicesortedTwistDepth3(idx) == 3) : (getFlipslicesortedTwistDepth3(idx) == depth3);
                        if (match) {
                            int flipslicesorted = SymmetryTables.flipslicesortedRep[fsClassidx];
                            int flip = flipslicesorted % N_FLIP;
                            int slicesorted = flipslicesorted / N_FLIP;
                            for (int m = 0; m < N_MOVE; m++) {
                                int twist1 = MoveTables.twistMove[N_MOVE * twist + m] & 0xFFFF;
                                int flip1 = MoveTables.flipMove[N_MOVE * flip + m] & 0xFFFF;
                                int slicesorted1 = MoveTables.sliceSortedMove[N_MOVE * slicesorted + m] & 0xFFFF;

                                int flipslicesorted1 = (slicesorted1 << 11) + flip1;
                                int fs1Classidx = SymmetryTables.flipslicesortedClassidx[flipslicesorted1];
                                int fs1Sym = SymmetryTables.flipslicesortedSym[flipslicesorted1] & 0xFF;
                                twist1 = SymmetryTables.twistConj[(twist1 << 4) + fs1Sym] & 0xFFFF;
                                long idx1 = (long) N_TWIST * fs1Classidx + twist1;

                                if (!backsearch) {
                                    if (getFlipslicesortedTwistDepth3(idx1) == 3) {
                                        setFlipslicesortedTwistDepth3(idx1, (depth + 1) % 3);
                                        done++;
                                        int sym = fsSym[fs1Classidx];
                                        if (sym != 1) {
                                            for (int k = 1; k < 16; k++) {
                                                sym >>= 1;
                                                if ((sym & 1) != 0) {
                                                    int twist2 = SymmetryTables.twistConj[(twist1 << 4) + k] & 0xFFFF;
                                                    long idx2 = (long) N_TWIST * fs1Classidx + twist2;
                                                    if (getFlipslicesortedTwistDepth3(idx2) == 3) {
                                                        setFlipslicesortedTwistDepth3(idx2, (depth + 1) % 3);
                                                        done++;
                                                    }
                                                }
                                            }
                                        }
                                    }
                                } else {
                                    if (getFlipslicesortedTwistDepth3(idx1) == depth3) {
                                        setFlipslicesortedTwistDepth3(idx, (depth + 1) % 3);
                                        done++;
                                        break;
                                    }
                                }
                            }
                        }
                        twist++;
                        idx++;
                    }
                }
                depth++;
                System.out.println();
                System.out.println("depth: " + depth + " done: " + done + "/" + total);
            }
            saveIntArray(fname, flipslicesortedTwistDepth3);
        }
    }

    // ========================= Phase 2 pruning table =========================
    
    private static void createPhase2PrunTable() {
        String fname = "phase2_prun";
        File f = new File(fname);
        int total = N_CORNERS_CLASS * N_UD_EDGES;

        if (f.exists()) {
            System.out.println("loading phase2_prun table...");
            cornersUdEdgesDepth3 = new int[total / 16];
            loadIntArray(fname, cornersUdEdgesDepth3);
        } else {
            System.out.println("creating phase2_prun table...");
            cornersUdEdgesDepth3 = new int[total / 16];
            Arrays.fill(cornersUdEdgesDepth3, 0xffffffff);

            // Create table with the symmetries of the corner classes
            CubieCube c = new CubieCube();
            int[] cSym = new int[N_CORNERS_CLASS];
            for (int i = 0; i < N_CORNERS_CLASS; i++) {
                if ((i + 1) % 1000 == 0) System.out.print(".");
                int rep = SymmetryTables.cornerRep[i] & 0xFFFF;
                c.setCorners(rep);
                for (int s = 0; s < N_SYM_D4h; s++) {
                    CubieCube ss = new CubieCube(SymmetryTables.symCube[s].getCpArray(), SymmetryTables.symCube[s].getCoArray(),
                                                  SymmetryTables.symCube[s].getEpArray(), SymmetryTables.symCube[s].getEoArray());
                    ss.cornerMultiply(c);
                    ss.cornerMultiply(SymmetryTables.symCube[SymmetryTables.invIdx[s]]);
                    if (ss.getCorners() == rep) cSym[i] |= (1 << s);
                }
            }
            System.out.println();

            int cClassidx = 0;
            int udEdge = 0;
            setCornersUdEdgesDepth3(N_UD_EDGES * cClassidx + udEdge, 0);
            int done = 1;
            int depth = 0;
            System.out.println("depth: " + depth + " done: " + done + "/" + total);

            int[] phase2Moves = {
                Move.U1.ordinal(), Move.U2.ordinal(), Move.U3.ordinal(),
                Move.R2.ordinal(), Move.F2.ordinal(),
                Move.D1.ordinal(), Move.D2.ordinal(), Move.D3.ordinal(),
                Move.L2.ordinal(), Move.B2.ordinal()
            };

            while (depth < 10) {
                int depth3 = depth % 3;
                int idx = 0;
                int mult = (depth > 9 ? 1 : 2);
                for (cClassidx = 0; cClassidx < N_CORNERS_CLASS; cClassidx++) {
                    if ((cClassidx + 1) % (20 * mult) == 0) System.out.print(".");
                    if ((cClassidx + 1) % (1600 * mult) == 0) System.out.println();
                    udEdge = 0;
                    while (udEdge < N_UD_EDGES) {
                        if (((idx & 15) == 0) && cornersUdEdgesDepth3[idx >> 4] == 0xffffffff && udEdge < N_UD_EDGES - 16) {
                            udEdge += 16;
                            idx += 16;
                            continue;
                        }
                        if (getCornersUdEdgesDepth3(idx) == depth3) {
                            int corner = SymmetryTables.cornerRep[cClassidx] & 0xFFFF;
                            for (int m : phase2Moves) {
                                int udEdge1 = MoveTables.udEdgesMove[N_MOVE * udEdge + m] & 0xFFFF;
                                int corner1 = MoveTables.cornersMove[N_MOVE * corner + m] & 0xFFFF;
                                int c1Classidx = SymmetryTables.cornerClassidx[corner1] & 0xFFFF;
                                int c1Sym = SymmetryTables.cornerSym[corner1] & 0xFF;
                                udEdge1 = SymmetryTables.udEdgesConj[(udEdge1 << 4) + c1Sym] & 0xFFFF;
                                int idx1 = N_UD_EDGES * c1Classidx + udEdge1;
                                if (getCornersUdEdgesDepth3(idx1) == 3) {
                                    setCornersUdEdgesDepth3(idx1, (depth + 1) % 3);
                                    done++;
                                    int sym = cSym[c1Classidx];
                                    if (sym != 1) {
                                        for (int k = 1; k < 16; k++) {
                                            sym >>= 1;
                                            if ((sym & 1) != 0) {
                                                int udEdge2 = SymmetryTables.udEdgesConj[(udEdge1 << 4) + k] & 0xFFFF;
                                                int idx2 = N_UD_EDGES * c1Classidx + udEdge2;
                                                if (getCornersUdEdgesDepth3(idx2) == 3) {
                                                    setCornersUdEdgesDepth3(idx2, (depth + 1) % 3);
                                                    done++;
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        udEdge++;
                        idx++;
                    }
                }
                depth++;
                System.out.println();
                System.out.println("depth: " + depth + " done: " + done + "/" + total);
            }
            System.out.println("remaining unfilled entries have depth >= 11");
            saveIntArray(fname, cornersUdEdgesDepth3);
        }
    }

    // ========================= Phase 2 cornslice pruning table =========================
    
    private static void createPhase2CornslicePrunTable() {
        String fname = "phase2_cornsliceprun";
        File f = new File(fname);

        if (f.exists()) {
            System.out.println("loading " + fname + " table...");
            cornsliceDepth = new byte[N_CORNERS * N_PERM_4];
            loadByteArray(fname, cornsliceDepth);
        } else {
            System.out.println("creating " + fname + " table...");
            cornsliceDepth = new byte[N_CORNERS * N_PERM_4];
            Arrays.fill(cornsliceDepth, (byte) -1);
            cornsliceDepth[0] = 0;
            int done = 1;
            int depth = 0;

            int[] phase2Moves = {
                Move.U1.ordinal(), Move.U2.ordinal(), Move.U3.ordinal(),
                Move.R2.ordinal(), Move.F2.ordinal(),
                Move.D1.ordinal(), Move.D2.ordinal(), Move.D3.ordinal(),
                Move.L2.ordinal(), Move.B2.ordinal()
            };

            while (done != N_CORNERS * N_PERM_4) {
                for (int corners = 0; corners < N_CORNERS; corners++) {
                    for (int slice = 0; slice < N_PERM_4; slice++) {
                        if (cornsliceDepth[N_PERM_4 * corners + slice] == depth) {
                            for (int m : phase2Moves) {
                                int corners1 = MoveTables.cornersMove[N_MOVE * corners + m] & 0xFFFF;
                                int slice1 = MoveTables.sliceSortedMove[N_MOVE * slice + m] & 0xFFFF;
                                int idx1 = N_PERM_4 * corners1 + slice1;
                                if (cornsliceDepth[idx1] == -1) {
                                    cornsliceDepth[idx1] = (byte) (depth + 1);
                                    done++;
                                    if (done % 20000 == 0) System.out.print(".");
                                }
                            }
                        }
                    }
                }
                depth++;
            }
            System.out.println();
            saveByteArray(fname, cornsliceDepth);
        }
    }

    // ========================= Phase 2 edge merge table =========================
    
    private static void initPhase2EdgeMergeTable() {
        String fname = "phase2_edgemerge";
        File f = new File(fname);

        if (f.exists()) {
            System.out.println("loading " + fname + " table...");
            uEdgesPlusDEdgesToUdEdges = new short[N_U_EDGES_PHASE2 * N_PERM_4];
            loadShortArray(fname, uEdgesPlusDEdgesToUdEdges);
        } else {
            System.out.println("creating " + fname + " table...");
            uEdgesPlusDEdgesToUdEdges = new short[N_U_EDGES_PHASE2 * N_PERM_4];

            CubieCube cU = new CubieCube();
            CubieCube cD = new CubieCube();
            CubieCube cUd = new CubieCube();
            
            int cnt = 0;
            for (int i = 0; i < N_U_EDGES_PHASE2; i++) {
                cU.setUEdges(i);
                for (int j = 0; j < 70; j++) { // 8C4 = 70
                    cD.setDEdges(j * N_PERM_4);
                    boolean invalid = false;
                    
                    // Check whether this U+D combination is compatible
                    for (int e = Edge.UR.ordinal(); e <= Edge.DB.ordinal(); e++) {
                        cUd.setEp(e, -1);
                        int cu = cU.getEp(e);
                        int cd = cD.getEp(e);
                        if (cu >= Edge.UR.ordinal() && cu <= Edge.UB.ordinal()) cUd.setEp(e, cu);
                        if (cd >= Edge.DR.ordinal() && cd <= Edge.DB.ordinal()) cUd.setEp(e, cd);
                        if (cUd.getEp(e) == -1) {
                            invalid = true;
                            break;
                        }
                    }
                    
                    if (!invalid) {
                        // Fill all 24 permutations of D edges
                        for (int k = 0; k < N_PERM_4; k++) {
                            cD.setDEdges(j * N_PERM_4 + k);
                            for (int e = Edge.UR.ordinal(); e <= Edge.DB.ordinal(); e++) {
                                int cu = cU.getEp(e);
                                int cd = cD.getEp(e);
                                if (cu >= Edge.UR.ordinal() && cu <= Edge.UB.ordinal()) cUd.setEp(e, cu);
                                if (cd >= Edge.DR.ordinal() && cd <= Edge.DB.ordinal()) cUd.setEp(e, cd);
                            }
                            uEdgesPlusDEdgesToUdEdges[N_PERM_4 * i + k] = (short) cUd.getUdEdges();
                            cnt++;
                            if (cnt % 8000 == 0) System.out.print(".");
                        }
                    }
                }
            }
            System.out.println();
            saveShortArray(fname, uEdgesPlusDEdgesToUdEdges);
        }
    }

    // ========================= UbigPF table (optimal solver) =========================
    
    private static void createUbigPFTable() {
        String fname = "ubigPF";
        File f = new File(fname);
        long expectedSize = (long) N_TETRA * BYTES_PER_TETRA;

        if (f.exists() && f.length() == expectedSize) {
            System.out.println("loading " + fname + " table...");
            ubigPF = new byte[(int) expectedSize];
            loadByteArray(fname, ubigPF);
        } else {
            System.out.println("creating ubigPF table...");
            System.out.println("This may take some time depending on the hardware.");
            buildUbigPFInMemory();
            // File already saved by buildUbigPFInMemory, no need to save again
        }
    }

    private static int get2(int[] A, long idx) {
        int base = (int)(idx >> 4);
        int off = (int)(idx & 15);
        int w = A[base];
        return (w >> (off * 2)) & 3;
    }

    private static void set2(int[] A, long idx, int v) {
        int base = (int)(idx >> 4);
        int off = (int)(idx & 15);
        int mask = ~(3 << (off * 2));
        A[base] = (A[base] & mask) | (v << (off * 2));
    }

    // Check if there are any entries with value 3 in a packed word
    private static int posOf3s(int x) {
        return x & (x >>> 1) & 0x55555555;  // >>> for unsigned/logical shift
    }

    // Check if there are any entries matching mask (0,1,2) in a packed word
    private static int posOfMsk(int x, int mask) {
        int f = x ^ (0xFFFFFFFF + (mask & 1));
        int s = x ^ (0xFFFFFFFF + (mask & 2));
        return f & (s >>> 1) & 0x55555555;  // >>> for unsigned/logical shift
    }

    private static void buildUbigPFInMemory() {
        long totalEntries = (long) N_TETRA * ENTRIES_PER_TETRA;
        
        // 2-bit temporary storage
        int[][] tmp = new int[N_TETRA][(int) WORDS_PER_TETRA_2BIT];
        for (int t = 0; t < N_TETRA; t++) {
            Arrays.fill(tmp[t], 0xffffffff);
        }

        // Build symmetry masks for flipslice classes
        int[] fsSym = new int[N_FLIPSLICE_CLASS];
        System.out.print("Building flipslice symmetry table");
        CubieCube c = new CubieCube();
        for (int i = 0; i < N_FLIPSLICE_CLASS; i++) {
            if ((i + 1) % 1000 == 0) System.out.print(".");
            int rep = SymmetryTables.flipsliceRep[i];
            int slice = rep / N_FLIP;
            int flip = rep % N_FLIP;
            c.setSlice(slice);
            c.setFlip(flip);
            for (int s = 0; s < N_SYM_D4h; s++) {
                CubieCube ss = new CubieCube(SymmetryTables.symCube[s].getCpArray(), SymmetryTables.symCube[s].getCoArray(),
                                              SymmetryTables.symCube[s].getEpArray(), SymmetryTables.symCube[s].getEoArray());
                ss.edgeMultiply(c);
                ss.edgeMultiply(SymmetryTables.symCube[SymmetryTables.invIdx[s]]);
                if (ss.getSlice() == slice && ss.getFlip() == flip) fsSym[i] |= (1 << s);
            }
        }
        System.out.println();

        long done = 0;
        // Solved state: tetra=0, flipslice_classidx=0, twist=0
        set2(tmp[0], 0, 0);
        done = 1;

        int depth = 0;
        boolean backsearch = false;

        while (done < totalEntries) {
            System.out.println("ubigPF depth: " + depth + " done: " + done + "/" + totalEntries);
            if (depth == 10) {
                System.out.println("flipping to backwards search...");
                backsearch = true;
            }
            int depth3 = depth % 3;

            for (int tetra = 0; tetra < N_TETRA; tetra++) {
                int[] layer = tmp[tetra];
                long idx = 0;
                for (int fsClassidx = 0; fsClassidx < N_FLIPSLICE_CLASS; fsClassidx++) {
                    int rep = SymmetryTables.flipsliceRep[fsClassidx];
                    int slice = rep / N_FLIP;
                    int flip = rep % N_FLIP;
                    for (int twist = 0; twist < N_TWIST; twist++, idx++) {
                        // Skip optimization: skip 16 entries if none match what we're looking for
                        if (!backsearch && (idx & 15) == 0 && posOfMsk(layer[(int)(idx >> 4)], depth3) == 0 && twist < N_TWIST - 16) {
                            twist += 15;
                            idx += 15;
                            continue;
                        }
                        if (backsearch && (idx & 15) == 0 && posOf3s(layer[(int)(idx >> 4)]) == 0 && twist < N_TWIST - 16) {
                            twist += 15;
                            idx += 15;
                            continue;
                        }

                        boolean match = backsearch ? (get2(layer, idx) == 3) : (get2(layer, idx) == depth3);
                        if (!match) continue;

                        for (int m = 0; m < N_MOVE; m++) {
                            int twist1 = MoveTables.twistMove[N_MOVE * twist + m] & 0xFFFF;
                            int flip1 = MoveTables.flipMove[N_MOVE * flip + m] & 0xFFFF;
                            int slice1 = (MoveTables.sliceSortedMove[(N_MOVE * N_PERM_4) * slice + m] & 0xFFFF) / N_PERM_4;
                            int tetra1 = MoveTables.tetraMove[N_MOVE * tetra + m] & 0xFFFF;

                            int flipslice1 = N_FLIP * slice1 + flip1;
                            int fs1Classidx = SymmetryTables.flipsliceClassidx[flipslice1] & 0xFFFF;
                            int fs1Sym = SymmetryTables.flipsliceSym[flipslice1] & 0xFF;

                            int twist1C = SymmetryTables.twistConj[(twist1 << 4) + fs1Sym] & 0xFFFF;
                            int tetra1C = SymmetryTables.tetraConj[N_SYM_D4h * tetra1 + fs1Sym] & 0xFFFF;

                            long idx1 = (long) N_TWIST * fs1Classidx + twist1C;
                            int[] layer1 = tmp[tetra1C];

                            if (!backsearch) {
                                if (get2(layer1, idx1) == 3) {
                                    int val = (depth + 1) % 3;
                                    set2(layer1, idx1, val);
                                    done++;

                                    // Fill symmetric variants
                                    int mask = fsSym[fs1Classidx];
                                    if (mask != 1) {
                                        for (int k = 1; k < N_SYM_D4h; k++) {
                                            mask >>= 1;
                                            if ((mask & 1) != 0) {
                                                int twist2 = SymmetryTables.twistConj[(twist1C << 4) + k] & 0xFFFF;
                                                int tetra2 = SymmetryTables.tetraConj[N_SYM_D4h * tetra1C + k] & 0xFFFF;
                                                long idx2 = (long) N_TWIST * fs1Classidx + twist2;
                                                int[] layer2 = tmp[tetra2];
                                                if (get2(layer2, idx2) == 3) {
                                                    set2(layer2, idx2, val);
                                                    done++;
                                                }
                                            }
                                        }
                                    }
                                }
                            } else {
                                if (get2(layer1, idx1) == depth3) {
                                    int val = (depth + 1) % 3;
                                    set2(layer, idx, val);
                                    done++;

                                    // Fill symmetric variants
                                    int mask = fsSym[fsClassidx];
                                    if (mask != 1) {
                                        for (int k = 1; k < N_SYM_D4h; k++) {
                                            mask >>= 1;
                                            if ((mask & 1) != 0) {
                                                int twist2 = SymmetryTables.twistConj[(twist << 4) + k] & 0xFFFF;
                                                int tetra2 = SymmetryTables.tetraConj[N_SYM_D4h * tetra + k] & 0xFFFF;
                                                long idx2 = (long) N_TWIST * fsClassidx + twist2;
                                                int[] layer2 = tmp[tetra2];
                                                if (get2(layer2, idx2) == 3) {
                                                    set2(layer2, idx2, val);
                                                    done++;
                                                }
                                            }
                                        }
                                    }
                                    break;
                                }
                            }
                        }
                    }
                }
            }
            depth++;
        }
        System.out.println("ubigPF depth: " + depth + " done: " + done + "/" + totalEntries);

        // Pack tmp (2-bit) and write directly to file to avoid memory issues
        System.out.println("Packing and saving ubigPF...");
        try (FileOutputStream fos = new FileOutputStream("ubigPF");
             BufferedOutputStream bos = new BufferedOutputStream(fos, 1 << 20)) {
            byte[] tetraBuffer = new byte[(int) BYTES_PER_TETRA];
            for (int tetra = 0; tetra < N_TETRA; tetra++) {
                int[] layer = tmp[tetra];
                for (int i = 0; i < G_SPLIT; i++) {
                    int v0 = get2(layer, 4L * i + 0);
                    int v1 = get2(layer, 4L * i + 1);
                    int v2 = get2(layer, 4L * i + 2);
                    int v3 = get2(layer, 4L * i + 3);
                    int v4 = get2(layer, 4L * G_SPLIT + i);
                    tetraBuffer[i] = pack5(v0, v1, v2, v3, v4);
                }
                bos.write(tetraBuffer);
                tmp[tetra] = null; // Free this layer's memory
            }
        } catch (IOException e) {
            System.err.println("Error writing ubigPF: " + e.getMessage());
        }
        tmp = null; // Help GC
        System.gc(); // Request garbage collection before loading
        
        // Now load the file into ubigPF array
        System.out.println("Loading ubigPF into memory...");
        ubigPF = new byte[(int)((long) N_TETRA * BYTES_PER_TETRA)];
        loadByteArray("ubigPF", ubigPF);
        System.out.println("ubigPF build complete.");
    }

    // ========================= Main init =========================
    
    public static void init() {
        if (initialized) return;
        System.out.println("Initializing pruning tables...");

        buildDistance();
        initGetpacked();

        createCornerPrunTable();
        createPhase1PrunTable();
        createPhase1x24PrunTable();
        createPhase2PrunTable();
        createPhase2CornslicePrunTable();
        initPhase2EdgeMergeTable();
        createUbigPFTable();

        initialized = true;
        System.out.println("Pruning tables initialized.");
    }

    // ========================= I/O helpers =========================
    
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
