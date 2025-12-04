package cube.model.face;

// The possible colors of the cube facelets. Color U refers to the color of the U(p)-face etc.
// Also used to name the faces itself.
public enum Color {
    U, R, F, D, L, B;

    public static final int COUNT = 6;

    public static Color fromIndex(int index) {
        return values()[index];
    }

    public static Color fromChar(char c) {
        switch (c) {
            case 'U': return U;
            case 'R': return R;
            case 'F': return F;
            case 'D': return D;
            case 'L': return L;
            case 'B': return B;
            default: return null;
        }
    }

    public char toChar() {
        return name().charAt(0);
    }
}
