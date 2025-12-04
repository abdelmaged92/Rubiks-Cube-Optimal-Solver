package cube.model.cubie;

import cube.model.face.*;
import cube.symmetry.SymmetryTables;
import java.util.ArrayList;
import java.util.Random;

import static cube.model.cubie.Defs.*;

/**
 * The cube on the cubie level is described by the permutation and orientations of corners and edges.
 * Demonstrates encapsulation with private fields and public accessors.
 */
public class CubieCube {

    // Private fields - encapsulation
    private int[] cp = new int[8];   // corner permutation
    private int[] co = new int[8];   // corner orientation (0..2, or >=3 for mirrored internal states)
    private int[] ep = new int[12];  // edge permutation
    private int[] eo = new int[12];  // edge orientation (0..1)

    // The basic six cube moves described by permutations and changes in orientation
    // U-move
    private static final int[] cpU = {Corner.UBR.ordinal(), Corner.URF.ordinal(), Corner.UFL.ordinal(), Corner.ULB.ordinal(), Corner.DFR.ordinal(), Corner.DLF.ordinal(), Corner.DBL.ordinal(), Corner.DRB.ordinal()};
    private static final int[] coU = {0, 0, 0, 0, 0, 0, 0, 0};
    private static final int[] epU = {Edge.UB.ordinal(), Edge.UR.ordinal(), Edge.UF.ordinal(), Edge.UL.ordinal(), Edge.DR.ordinal(), Edge.DF.ordinal(), Edge.DL.ordinal(), Edge.DB.ordinal(), Edge.FR.ordinal(), Edge.FL.ordinal(), Edge.BL.ordinal(), Edge.BR.ordinal()};
    private static final int[] eoU = {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};

    // R-move
    private static final int[] cpR = {Corner.DFR.ordinal(), Corner.UFL.ordinal(), Corner.ULB.ordinal(), Corner.URF.ordinal(), Corner.DRB.ordinal(), Corner.DLF.ordinal(), Corner.DBL.ordinal(), Corner.UBR.ordinal()};
    private static final int[] coR = {2, 0, 0, 1, 1, 0, 0, 2};
    private static final int[] epR = {Edge.FR.ordinal(), Edge.UF.ordinal(), Edge.UL.ordinal(), Edge.UB.ordinal(), Edge.BR.ordinal(), Edge.DF.ordinal(), Edge.DL.ordinal(), Edge.DB.ordinal(), Edge.DR.ordinal(), Edge.FL.ordinal(), Edge.BL.ordinal(), Edge.UR.ordinal()};
    private static final int[] eoR = {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};

    // F-move
    private static final int[] cpF = {Corner.UFL.ordinal(), Corner.DLF.ordinal(), Corner.ULB.ordinal(), Corner.UBR.ordinal(), Corner.URF.ordinal(), Corner.DFR.ordinal(), Corner.DBL.ordinal(), Corner.DRB.ordinal()};
    private static final int[] coF = {1, 2, 0, 0, 2, 1, 0, 0};
    private static final int[] epF = {Edge.UR.ordinal(), Edge.FL.ordinal(), Edge.UL.ordinal(), Edge.UB.ordinal(), Edge.DR.ordinal(), Edge.FR.ordinal(), Edge.DL.ordinal(), Edge.DB.ordinal(), Edge.UF.ordinal(), Edge.DF.ordinal(), Edge.BL.ordinal(), Edge.BR.ordinal()};
    private static final int[] eoF = {0, 1, 0, 0, 0, 1, 0, 0, 1, 1, 0, 0};

    // D-move
    private static final int[] cpD = {Corner.URF.ordinal(), Corner.UFL.ordinal(), Corner.ULB.ordinal(), Corner.UBR.ordinal(), Corner.DLF.ordinal(), Corner.DBL.ordinal(), Corner.DRB.ordinal(), Corner.DFR.ordinal()};
    private static final int[] coD = {0, 0, 0, 0, 0, 0, 0, 0};
    private static final int[] epD = {Edge.UR.ordinal(), Edge.UF.ordinal(), Edge.UL.ordinal(), Edge.UB.ordinal(), Edge.DF.ordinal(), Edge.DL.ordinal(), Edge.DB.ordinal(), Edge.DR.ordinal(), Edge.FR.ordinal(), Edge.FL.ordinal(), Edge.BL.ordinal(), Edge.BR.ordinal()};
    private static final int[] eoD = {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};

    // L-move
    private static final int[] cpL = {Corner.URF.ordinal(), Corner.ULB.ordinal(), Corner.DBL.ordinal(), Corner.UBR.ordinal(), Corner.DFR.ordinal(), Corner.UFL.ordinal(), Corner.DLF.ordinal(), Corner.DRB.ordinal()};
    private static final int[] coL = {0, 1, 2, 0, 0, 2, 1, 0};
    private static final int[] epL = {Edge.UR.ordinal(), Edge.UF.ordinal(), Edge.BL.ordinal(), Edge.UB.ordinal(), Edge.DR.ordinal(), Edge.DF.ordinal(), Edge.FL.ordinal(), Edge.DB.ordinal(), Edge.FR.ordinal(), Edge.UL.ordinal(), Edge.DL.ordinal(), Edge.BR.ordinal()};
    private static final int[] eoL = {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};

    // B-move
    private static final int[] cpB = {Corner.URF.ordinal(), Corner.UFL.ordinal(), Corner.UBR.ordinal(), Corner.DRB.ordinal(), Corner.DFR.ordinal(), Corner.DLF.ordinal(), Corner.ULB.ordinal(), Corner.DBL.ordinal()};
    private static final int[] coB = {0, 0, 1, 2, 0, 0, 2, 1};
    private static final int[] epB = {Edge.UR.ordinal(), Edge.UF.ordinal(), Edge.UL.ordinal(), Edge.BR.ordinal(), Edge.DR.ordinal(), Edge.DF.ordinal(), Edge.DL.ordinal(), Edge.BL.ordinal(), Edge.FR.ordinal(), Edge.FL.ordinal(), Edge.UB.ordinal(), Edge.DB.ordinal()};
    private static final int[] eoB = {0, 0, 0, 1, 0, 0, 0, 1, 0, 0, 1, 1};

    // These cubes represent the basic cube moves
    public static final CubieCube[] basicMoveCube = new CubieCube[6];

    // These cubes represent all 18 cube moves
    public static final CubieCube[] moveCube = new CubieCube[18];

    public static final boolean CUBE_OK = true;

    // Static initialization
    static {
        basicMoveCube[Color.U.ordinal()] = new CubieCube(cpU, coU, epU, eoU);
        basicMoveCube[Color.R.ordinal()] = new CubieCube(cpR, coR, epR, eoR);
        basicMoveCube[Color.F.ordinal()] = new CubieCube(cpF, coF, epF, eoF);
        basicMoveCube[Color.D.ordinal()] = new CubieCube(cpD, coD, epD, eoD);
        basicMoveCube[Color.L.ordinal()] = new CubieCube(cpL, coL, epL, eoL);
        basicMoveCube[Color.B.ordinal()] = new CubieCube(cpB, coB, epB, eoB);

        CubieCube cc;
        for (int c1 = 0; c1 < 6; ++c1) {
            cc = new CubieCube();
            for (int k1 = 0; k1 < 3; ++k1) {
                cc.multiply(basicMoveCube[c1]);
                moveCube[3 * c1 + k1] = new CubieCube(cc.cp, cc.co, cc.ep, cc.eo);
            }
        }
    }

    // ========== Constructors ==========

    // Identity cube constructor
    public CubieCube() {
        for (int i = 0; i < 8; ++i) { cp[i] = i; co[i] = 0; }
        for (int i = 0; i < 12; ++i) { ep[i] = i; eo[i] = 0; }
    }

    public CubieCube(int[] cp_, int[] co_, int[] ep_, int[] eo_) {
        for (int i = 0; i < 8; ++i) { cp[i] = cp_[i]; co[i] = co_[i]; }
        for (int i = 0; i < 12; ++i) { ep[i] = ep_[i]; eo[i] = eo_[i]; }
    }

    // ========== Encapsulation: Getters and Setters ==========

    public int getCp(int i) { return cp[i]; }
    public int getCo(int i) { return co[i]; }
    public int getEp(int i) { return ep[i]; }
    public int getEo(int i) { return eo[i]; }

    public void setCp(int i, int val) { cp[i] = val; }
    public void setCo(int i, int val) { co[i] = val; }
    public void setEp(int i, int val) { ep[i] = val; }
    public void setEo(int i, int val) { eo[i] = val; }

    // Array getters for bulk access (defensive copies)
    public int[] getCpArray() { return cp.clone(); }
    public int[] getCoArray() { return co.clone(); }
    public int[] getEpArray() { return ep.clone(); }
    public int[] getEoArray() { return eo.clone(); }

    // ========== Equality ==========

    public boolean equals(CubieCube other) {
        for (int i = 0; i < 8; ++i) if (cp[i] != other.cp[i] || co[i] != other.co[i]) return false;
        for (int i = 0; i < 12; ++i) if (ep[i] != other.ep[i] || eo[i] != other.eo[i]) return false;
        return true;
    }

    // ========== Conversion ==========

    // Return a facelet representation of the cube
    public FaceCube toFaceletCube() {
        FaceCube fc = new FaceCube();
        for (int i = 0; i < 8; i++) {
            int j = cp[i];
            int ori = co[i] % 3;
            for (int k = 0; k < 3; k++)
                fc.setFacelet(cornerFacelet[i][(k + ori) % 3], cornerColor[j][k]);
        }
        for (int i = 0; i < 12; i++) {
            int j = ep[i];
            int ori = (eo[i] & 1);
            for (int k = 0; k < 2; k++)
                fc.setFacelet(edgeFacelet[i][(k + ori) % 2], edgeColor[j][k]);
        }
        return fc;
    }

    // ========== Multiplication ==========

    // Multiply this cubie cube with another cubiecube b, restricted to the corners
    public void cornerMultiply(CubieCube b) {
        int[] cPerm = new int[8];
        int[] cOri = new int[8];
        for (int c = 0; c < 8; c++) {
            cPerm[c] = cp[b.cp[c]];
            int oriA = co[b.cp[c]];
            int oriB = b.co[c];
            int ori;
            if (oriA < 3 && oriB < 3) {
                ori = oriA + oriB;
                if (ori >= 3) ori -= 3;
            } else if (oriA < 3 && oriB >= 3) {
                ori = oriA + oriB;
                if (ori >= 6) ori -= 3;
            } else if (oriA >= 3 && oriB < 3) {
                ori = oriA - oriB;
                if (ori < 3) ori += 3;
            } else {
                ori = oriA - oriB;
                if (ori < 0) ori += 3;
            }
            cOri[c] = ori;
        }
        for (int c = 0; c < 8; ++c) {
            cp[c] = cPerm[c];
            co[c] = cOri[c];
        }
    }

    // Multiply this cubie cube with another cubiecube b, restricted to the edges
    public void edgeMultiply(CubieCube b) {
        int[] ePerm = new int[12];
        int[] eOri = new int[12];
        for (int e = 0; e < 12; ++e) {
            ePerm[e] = ep[b.ep[e]];
            eOri[e] = (b.eo[e] + eo[b.ep[e]]) % 2;
        }
        for (int e = 0; e < 12; ++e) {
            ep[e] = ePerm[e];
            eo[e] = eOri[e];
        }
    }

    public void multiply(CubieCube b) {
        cornerMultiply(b);
        edgeMultiply(b);
    }

    // Apply move m to CubieCube
    public void move(int m) {
        multiply(moveCube[m]);
    }

    // Store the inverse of this cubie cube in d
    public void invCubieCube(CubieCube d) {
        for (int e = 0; e < 12; ++e) d.ep[ep[e]] = e;
        for (int e = 0; e < 12; ++e) d.eo[e] = eo[d.ep[e]];
        for (int c = 0; c < 8; ++c) d.cp[cp[c]] = c;
        for (int c = 0; c < 8; ++c) {
            int ori = co[d.cp[c]];
            if (ori >= 3) d.co[c] = ori;
            else d.co[c] = (3 - ori) % 3;
        }
    }

    // ========== Parity ==========

    // Give the parity of the corner permutation
    public int cornerParity() {
        int s = 0;
        for (int i = 7; i > 0; --i) {
            for (int j = i - 1; j >= 0; --j) {
                if (cp[j] > cp[i]) s ^= 1;
            }
        }
        return s;
    }

    // Give the parity of the edge permutation
    public int edgeParity() {
        int s = 0;
        for (int i = 11; i > 0; --i) {
            for (int j = i - 1; j >= 0; --j) {
                if (ep[j] > ep[i]) s ^= 1;
            }
        }
        return s;
    }

    // ========== Coordinate getters/setters ==========

    // Get the twist of the 8 corners. 0 <= twist < 2187
    public int getTwist() {
        int ret = 0;
        for (int i = 0; i < 7; ++i) ret = 3 * ret + co[i];
        return ret;
    }

    public void setTwist(int twist) {
        int twistParity = 0;
        for (int i = 6; i >= 0; --i) {
            co[i] = twist % 3;
            twistParity += co[i];
            twist /= 3;
        }
        co[7] = (3 - twistParity % 3) % 3;
    }

    // Get the flip of the 12 edges. 0 <= flip < 2048
    public int getFlip() {
        int ret = 0;
        for (int i = 0; i < 11; ++i) ret = 2 * ret + eo[i];
        return ret;
    }

    public void setFlip(int flip) {
        eo[11] = 0;
        for (int i = 10; i >= 0; --i) {
            eo[i] = flip & 1;
            eo[11] ^= eo[i];
            flip >>= 1;
        }
    }

    // Get the location of the UD-slice edges FR,FL,BL and BR ignoring their permutation. 0 <= slice < 495
    public int getSlice() {
        int a = 0, x = 0;
        for (int j = 11; j >= 0; --j) {
            if (Edge.FR.ordinal() <= ep[j] && ep[j] <= Edge.BR.ordinal()) {
                a += nCr(11 - j, x + 1);
                ++x;
            }
        }
        return a;
    }

    public void setSlice(int idx) {
        int x = 4;
        int[] sliceEdge = {Edge.FR.ordinal(), Edge.FL.ordinal(), Edge.BL.ordinal(), Edge.BR.ordinal()};
        int[] otherEdge = {Edge.UR.ordinal(), Edge.UF.ordinal(), Edge.UL.ordinal(), Edge.UB.ordinal(), Edge.DR.ordinal(), Edge.DF.ordinal(), Edge.DL.ordinal(), Edge.DB.ordinal()};
        for (int j = 0; j < 12; ++j) ep[j] = -1;
        for (int j = 0; j < 12; ++j) {
            if (idx - nCr(11 - j, x) >= 0) {
                ep[j] = sliceEdge[4 - x];
                idx -= nCr(11 - j, x);
                --x;
            }
        }
        int k = 0;
        for (int j = 0; j < 12; ++j) {
            if (ep[j] == -1) ep[j] = otherEdge[k++];
        }
    }

    // Get the permutation and location of the UD-slice edges FR,FL,BL and BR. 0 <= slice_sorted < 11880
    public int getSliceSorted() {
        int a = 0, x = 0;
        int[] edge4 = new int[4];
        for (int j = 11; j >= 0; --j) {
            if (Edge.FR.ordinal() <= ep[j] && ep[j] <= Edge.BR.ordinal()) {
                a += nCr(11 - j, x + 1);
                edge4[3 - x] = ep[j];
                ++x;
            }
        }
        int b = 0;
        for (int j = 3; j >= 1; --j) {
            int k = 0;
            while (edge4[j] != j + 8) {
                rotateLeft(edge4, 0, j);
                ++k;
            }
            b = (j + 1) * b + k;
        }
        return 24 * a + b;
    }

    public void setSliceSorted(int idx) {
        int[] sliceEdge = {Edge.FR.ordinal(), Edge.FL.ordinal(), Edge.BL.ordinal(), Edge.BR.ordinal()};
        int[] otherEdge = {Edge.UR.ordinal(), Edge.UF.ordinal(), Edge.UL.ordinal(), Edge.UB.ordinal(), Edge.DR.ordinal(), Edge.DF.ordinal(), Edge.DL.ordinal(), Edge.DB.ordinal()};
        int a = idx / 24, b = idx % 24;
        for (int e = 0; e < 12; e++) ep[e] = -1;
        int j = 1;
        while (j < 4) {
            int k = b % (j + 1);
            b /= (j + 1);
            while (k > 0) { rotateRight(sliceEdge, 0, j); k--; }
            j++;
        }
        int x = 4;
        for (j = 0; j < 12; j++) {
            if (a - nCr(11 - j, x) >= 0) {
                ep[j] = sliceEdge[4 - x];
                a -= nCr(11 - j, x);
                x--;
            }
        }
        int t = 0;
        for (j = 0; j < 12; j++) {
            if (ep[j] == -1) ep[j] = otherEdge[t++];
        }
    }

    // Get the permutation of the 8 corners. 0 <= corners < 40320
    public int getCorners() {
        int[] perm = new int[8];
        for (int i = 0; i < 8; ++i) perm[i] = cp[i];
        int b = 0;
        for (int j = 7; j > 0; --j) {
            int k = 0;
            while (perm[j] != j) {
                rotateLeft(perm, 0, j);
                ++k;
            }
            b = (j + 1) * b + k;
        }
        return b;
    }

    public void setCorners(int idx) {
        for (int i = 0; i < 8; ++i) cp[i] = i;
        for (int j = 0; j < 8; ++j) {
            int k = idx % (j + 1);
            idx /= (j + 1);
            while (k > 0) {
                rotateRight(cp, 0, j);
                --k;
            }
        }
    }

    // Get the permutation and location of edges UR, UF, UL and UB. 0 <= u_edges < 11880
    public int getUEdges() {
        int a = 0, x = 0;
        int[] epMod = new int[12];
        for (int i = 0; i < 12; i++) epMod[i] = ep[i];
        for (int t = 0; t < 4; t++) rotateRight(epMod, 0, 11);
        int[] edge4 = new int[4];
        for (int j = 11; j >= 0; j--) {
            if (epMod[j] >= Edge.UR.ordinal() && epMod[j] <= Edge.UB.ordinal()) {
                a += nCr(11 - j, x + 1);
                edge4[3 - x] = epMod[j];
                x++;
            }
        }
        int b = 0;
        for (int j = 3; j >= 1; j--) {
            int k = 0;
            while (edge4[j] != j) { rotateLeft(edge4, 0, j); k++; }
            b = (j + 1) * b + k;
        }
        return 24 * a + b;
    }

    public void setUEdges(int idx) {
        int[] sliceEdge = {Edge.UR.ordinal(), Edge.UF.ordinal(), Edge.UL.ordinal(), Edge.UB.ordinal()};
        int[] otherEdge = {Edge.DR.ordinal(), Edge.DF.ordinal(), Edge.DL.ordinal(), Edge.DB.ordinal(), Edge.FR.ordinal(), Edge.FL.ordinal(), Edge.BL.ordinal(), Edge.BR.ordinal()};
        int a = idx / 24, b = idx % 24;
        for (int e = 0; e < 12; e++) ep[e] = -1;
        int j = 1;
        while (j < 4) {
            int k = b % (j + 1); b /= (j + 1);
            while (k > 0) { rotateRight(sliceEdge, 0, j); k--; }
            j++;
        }
        int x = 4;
        for (j = 0; j < 12; j++) {
            if (a - nCr(11 - j, x) >= 0) {
                ep[j] = sliceEdge[4 - x];
                a -= nCr(11 - j, x);
                x--;
            }
        }
        int t = 0;
        for (j = 0; j < 12; j++) if (ep[j] == -1) ep[j] = otherEdge[t++];
        for (int i = 0; i < 4; i++) rotateLeft(ep, 0, 11);
    }

    // Get the permutation and location of the edges DR, DF, DL and DB. d_edges = 0 for solved cube.
    public int getDEdges() {
        int a = 0, x = 0;
        int[] epMod = new int[12];
        for (int i = 0; i < 12; i++) epMod[i] = ep[i];
        for (int t = 0; t < 4; t++) rotateRight(epMod, 0, 11);
        int[] edge4 = new int[4];
        for (int j = 11; j >= 0; j--) {
            if (epMod[j] >= Edge.DR.ordinal() && epMod[j] <= Edge.DB.ordinal()) {
                a += nCr(11 - j, x + 1);
                edge4[3 - x] = epMod[j];
                ++x;
            }
        }
        int b = 0;
        for (int j = 3; j >= 1; j--) {
            int k = 0;
            while (edge4[j] != j + 4) { rotateLeft(edge4, 0, j); k++; }
            b = (j + 1) * b + k;
        }
        return 24 * a + b;
    }

    public void setDEdges(int idx) {
        int[] sliceEdge = {Edge.DR.ordinal(), Edge.DF.ordinal(), Edge.DL.ordinal(), Edge.DB.ordinal()};
        int[] otherEdge = {Edge.FR.ordinal(), Edge.FL.ordinal(), Edge.BL.ordinal(), Edge.BR.ordinal(), Edge.UR.ordinal(), Edge.UF.ordinal(), Edge.UL.ordinal(), Edge.UB.ordinal()};
        int a = idx / 24, b = idx % 24;
        for (int e = 0; e < 12; e++) ep[e] = -1;
        int j = 1;
        while (j < 4) {
            int k = b % (j + 1); b /= (j + 1);
            while (k > 0) { rotateRight(sliceEdge, 0, j); k--; }
            j++;
        }
        int x = 4;
        for (j = 0; j < 12; j++) {
            if (a - nCr(11 - j, x) >= 0) {
                ep[j] = sliceEdge[4 - x];
                a -= nCr(11 - j, x);
                x--;
            }
        }
        int t = 0;
        for (j = 0; j < 12; j++) if (ep[j] == -1) ep[j] = otherEdge[t++];
        for (int i = 0; i < 4; i++) rotateLeft(ep, 0, 11);
    }

    // Get the permutation of the 8 U and D edges. 0 <= ud_edges < 40320
    public int getUdEdges() {
        int[] perm = new int[8];
        for (int i = 0; i < 8; i++) perm[i] = ep[i];
        int b = 0;
        for (int j = 7; j > 0; j--) {
            int k = 0;
            while (perm[j] != j) { rotateLeft(perm, 0, j); k++; }
            b = (j + 1) * b + k;
        }
        return b;
    }

    public void setUdEdges(int idx) {
        for (int i = 0; i < 8; i++) ep[i] = i;
        for (int j = 0; j < 8; j++) {
            int k = idx % (j + 1);
            idx /= (j + 1);
            while (k > 0) { rotateRight(ep, 0, j); k--; }
        }
    }

    // Tetra coordinate. 0 <= tetra < 70
    public int getTetra() {
        int n = 7, k = 3, s = 0;
        while (k >= 0) {
            if (cp[n] >= Corner.DFR.ordinal()) k--;
            else s += nCr(n, k);
            n--;
        }
        return s;
    }

    public void setTetra(int idx) {
        boolean[] occupied = new boolean[8];
        int n = 7, k = 3;
        while (k >= 0) {
            long v = nCr(n, k);
            if (idx < v) {
                occupied[n] = true;
                k--;
            } else idx -= v;
            n--;
        }
        int ptr = Corner.DFR.ordinal();
        for (int c = 0; c < 8; c++) {
            if (!occupied[c]) continue;
            for (int i = 0; i < 8; i++) {
                if (cp[i] == ptr) {
                    cp[i] = cp[c];
                    break;
                }
            }
            cp[c] = ptr;
            if (ptr < Corner.DRB.ordinal()) ptr++;
        }
    }

    // ========== Random cube generation ==========

    public void randomize() {
        Random rng = new Random();

        // Randomize edges uniformly over 12!
        for (int i = 0; i < 12; i++) ep[i] = i;
        for (int j = 0; j < 12; j++) {
            int idx = rng.nextInt(479001600);  // 12!
            int k = idx % (j + 1);
            while (k > 0) { rotateRight(ep, 0, j); k--; }
        }
        int p = edgeParity();

        // Randomize corners uniformly over 8!, then fix parity to match edges
        setCorners(rng.nextInt(N_CORNERS));
        if (cornerParity() != p) {
            int temp = cp[Corner.URF.ordinal()];
            cp[Corner.URF.ordinal()] = cp[Corner.ULB.ordinal()];
            cp[Corner.ULB.ordinal()] = temp;
        }

        // Randomize orientations
        setFlip(rng.nextInt(N_FLIP));
        setTwist(rng.nextInt(N_TWIST));
    }

    // ========== Verification ==========

    // Check if cubiecube is valid. Returns {success, message}
    public FaceCube.Result verify() {
        int[] edgeCount = new int[12];
        for (int i = 0; i < 12; ++i) edgeCount[ep[i]]++;
        for (int i = 0; i < 12; ++i) if (edgeCount[i] != 1) return new FaceCube.Result(false, "Error: Some edges are undefined.");

        int s = 0;
        for (int i = 0; i < 12; ++i) s ^= eo[i];
        if (s != 0) return new FaceCube.Result(false, "Error: Total edge flip is wrong.");

        int[] cornerCount = new int[8];
        for (int i = 0; i < 8; ++i) cornerCount[cp[i]]++;
        for (int i = 0; i < 8; ++i) if (cornerCount[i] != 1) return new FaceCube.Result(false, "Error: Some corners are undefined.");

        s = 0;
        for (int i = 0; i < 8; ++i) s += co[i];
        if (s % 3 != 0) return new FaceCube.Result(false, "Error: Total corner twist is wrong.");

        if (edgeParity() != cornerParity()) return new FaceCube.Result(false, "Error: Wrong edge and corner parity.");

        return new FaceCube.Result(true, "OK");
    }

    // ========== Symmetries ==========

    // Generate a list of the symmetries and antisymmetries of the cubie cube
    public ArrayList<Integer> symmetries() {
        ArrayList<Integer> s = new ArrayList<>();
        CubieCube inv = new CubieCube();
        this.invCubieCube(inv);
        
        for (int j = 0; j < N_SYM; j++) {
            CubieCube c = new CubieCube(SymmetryTables.symCube[j].getCpArray(), SymmetryTables.symCube[j].getCoArray(), 
                                        SymmetryTables.symCube[j].getEpArray(), SymmetryTables.symCube[j].getEoArray());
            c.multiply(this);
            c.multiply(SymmetryTables.symCube[SymmetryTables.invIdx[j]]);
            if (this.equals(c)) {
                s.add(j);
            }
        }
        
        for (int j = 0; j < N_SYM; j++) {
            CubieCube c = new CubieCube(SymmetryTables.symCube[j].getCpArray(), SymmetryTables.symCube[j].getCoArray(), 
                                        SymmetryTables.symCube[j].getEpArray(), SymmetryTables.symCube[j].getEoArray());
            c.multiply(inv);
            c.multiply(SymmetryTables.symCube[SymmetryTables.invIdx[j]]);
            if (this.equals(c)) {
                s.add(j + N_SYM);
            }
        }
        return s;
    }
}
