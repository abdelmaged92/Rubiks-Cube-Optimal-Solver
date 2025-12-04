package cube.model.face;

// The names of the edge positions of the cube. Edge UR e.g. has an U(p) and R(ight) facelet.
public enum Edge {
    UR, UF, UL, UB, DR, DF, DL, DB, FR, FL, BL, BR;

    public static final int COUNT = 12;

    public static Edge fromIndex(int index) {
        return values()[index];
    }
}
