package cube.model.face;

// The names of the corner positions of the cube. Corner URF e.g. has an U(p), a R(ight) and a F(ront) facelet.
public enum Corner {
    URF, UFL, ULB, UBR, DFR, DLF, DBL, DRB;

    public static final int COUNT = 8;

    public static Corner fromIndex(int index) {
        return values()[index];
    }
}
