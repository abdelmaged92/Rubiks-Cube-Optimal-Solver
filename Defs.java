package cube.model.cubie;

import cube.model.face.*;

// Some definitions and constants
public class Defs {

    // Map the corner positions to facelet positions.
    public static final int[][] cornerFacelet = {
        {Facelet.U9.ordinal(), Facelet.R1.ordinal(), Facelet.F3.ordinal()},
        {Facelet.U7.ordinal(), Facelet.F1.ordinal(), Facelet.L3.ordinal()},
        {Facelet.U1.ordinal(), Facelet.L1.ordinal(), Facelet.B3.ordinal()},
        {Facelet.U3.ordinal(), Facelet.B1.ordinal(), Facelet.R3.ordinal()},
        {Facelet.D3.ordinal(), Facelet.F9.ordinal(), Facelet.R7.ordinal()},
        {Facelet.D1.ordinal(), Facelet.L9.ordinal(), Facelet.F7.ordinal()},
        {Facelet.D7.ordinal(), Facelet.B9.ordinal(), Facelet.L7.ordinal()},
        {Facelet.D9.ordinal(), Facelet.R9.ordinal(), Facelet.B7.ordinal()}
    };

    // Map the edge positions to facelet positions.
    public static final int[][] edgeFacelet = {
        {Facelet.U6.ordinal(), Facelet.R2.ordinal()},
        {Facelet.U8.ordinal(), Facelet.F2.ordinal()},
        {Facelet.U4.ordinal(), Facelet.L2.ordinal()},
        {Facelet.U2.ordinal(), Facelet.B2.ordinal()},
        {Facelet.D6.ordinal(), Facelet.R8.ordinal()},
        {Facelet.D2.ordinal(), Facelet.F8.ordinal()},
        {Facelet.D4.ordinal(), Facelet.L8.ordinal()},
        {Facelet.D8.ordinal(), Facelet.B8.ordinal()},
        {Facelet.F6.ordinal(), Facelet.R4.ordinal()},
        {Facelet.F4.ordinal(), Facelet.L6.ordinal()},
        {Facelet.B6.ordinal(), Facelet.L4.ordinal()},
        {Facelet.B4.ordinal(), Facelet.R6.ordinal()}
    };

    // Map the corner positions to facelet colors.
    public static final int[][] cornerColor = {
        {Color.U.ordinal(), Color.R.ordinal(), Color.F.ordinal()},
        {Color.U.ordinal(), Color.F.ordinal(), Color.L.ordinal()},
        {Color.U.ordinal(), Color.L.ordinal(), Color.B.ordinal()},
        {Color.U.ordinal(), Color.B.ordinal(), Color.R.ordinal()},
        {Color.D.ordinal(), Color.F.ordinal(), Color.R.ordinal()},
        {Color.D.ordinal(), Color.L.ordinal(), Color.F.ordinal()},
        {Color.D.ordinal(), Color.B.ordinal(), Color.L.ordinal()},
        {Color.D.ordinal(), Color.R.ordinal(), Color.B.ordinal()}
    };

    // Map the edge positions to facelet colors.
    public static final int[][] edgeColor = {
        {Color.U.ordinal(), Color.R.ordinal()},
        {Color.U.ordinal(), Color.F.ordinal()},
        {Color.U.ordinal(), Color.L.ordinal()},
        {Color.U.ordinal(), Color.B.ordinal()},
        {Color.D.ordinal(), Color.R.ordinal()},
        {Color.D.ordinal(), Color.F.ordinal()},
        {Color.D.ordinal(), Color.L.ordinal()},
        {Color.D.ordinal(), Color.B.ordinal()},
        {Color.F.ordinal(), Color.R.ordinal()},
        {Color.F.ordinal(), Color.L.ordinal()},
        {Color.B.ordinal(), Color.L.ordinal()},
        {Color.B.ordinal(), Color.R.ordinal()}
    };

    // Some constants
    public static final int N_PERM_4 = 24;
    public static final int N_MOVE = 18;

    public static final int N_TWIST = 2187;  // 3^7 possible corner orientations
    public static final int N_FLIP = 2048;   // 2^11 possible edge orientations

    public static final int N_U_EDGES_PHASE2 = 1680;
    public static final int N_UD_EDGES = 40320;  // 8! permutations of UD edges in phase 2

    public static final int N_SLICE_SORTED = 11880;  // 12*11*10*9
    public static final int N_SLICE = N_SLICE_SORTED / N_PERM_4;  // ignore permutation of FR,FL,BL,BR
    public static final int N_FLIPSLICE_CLASS = 64430;
    public static final int N_FLIPSLICESORTED_CLASS = 1523864;

    public static final int N_CORNERS = 40320;  // 8! corner permutations
    public static final int N_CORNERS_CLASS = 2768;

    public static final int N_SYM = 48;       // number of cube symmetries of full group Oh
    public static final int N_SYM_D4h = 16;   // number of symmetries of subgroup D4h

    public static final int N_TETRA = 70;

    // Binomial coefficient [n choose r]
    public static long nCr(long n, long r) {
        if (n < r) return 0;
        if (r > n / 2) r = n - r;
        long res = 1;
        for (long i = 0; i < r; i++) res = res * (n - i) / (i + 1);
        return res;
    }

    // Rotate array right between l and r (inclusive)
    public static void rotateRight(int[] arr, int l, int r) {
        int temp = arr[r];
        for (int i = r; i > l; --i) arr[i] = arr[i - 1];
        arr[l] = temp;
    }

    // Rotate array left between l and r (inclusive)
    public static void rotateLeft(int[] arr, int l, int r) {
        int temp = arr[l];
        for (int i = l; i < r; ++i) arr[i] = arr[i + 1];
        arr[r] = temp;
    }
}

