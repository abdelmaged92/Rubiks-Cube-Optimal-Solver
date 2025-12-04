package cube.model.face;

import cube.model.cubie.CubieCube;
import cube.model.cubie.Defs;

/**
 * The cube on the facelet level is described by positions of the colored stickers.
 * Demonstrates encapsulation with private fields and public accessors.
 */
public class FaceCube {

    // Private field - encapsulation
    private int[] f = new int[54];

    // ========== Constructors ==========

    public FaceCube() {
        for (int i = 0;  i < 9;  i++) f[i] = Color.U.ordinal();
        for (int i = 9;  i < 18; i++) f[i] = Color.R.ordinal();
        for (int i = 18; i < 27; i++) f[i] = Color.F.ordinal();
        for (int i = 27; i < 36; i++) f[i] = Color.D.ordinal();
        for (int i = 36; i < 45; i++) f[i] = Color.L.ordinal();
        for (int i = 45; i < 54; i++) f[i] = Color.B.ordinal();
    }

    // ========== Encapsulation: Getters and Setters ==========

    /**
     * Get the color at a specific facelet position.
     * @param i Facelet index (0-53)
     * @return Color ordinal value
     */
    public int getFacelet(int i) {
        return f[i];
    }

    /**
     * Set the color at a specific facelet position.
     * @param i Facelet index (0-53)
     * @param color Color ordinal value
     */
    public void setFacelet(int i, int color) {
        f[i] = color;
    }

    /**
     * Get a copy of all facelets.
     * Returns a defensive copy to maintain encapsulation.
     * @return Copy of the facelet array
     */
    public int[] getFacelets() {
        return f.clone();
    }

    // ========== Parsing and Conversion ==========

    /**
     * Parse a cube definition string.
     * @param s 54-character string representing the cube
     * @return Result with success status and error message
     */
    public Result fromString(String s) {
        if (s.length() < 54) {
            return new Result(false, "Error: Cube definition string contains less than 54 facelets.");
        }
        if (s.length() > 54) {
            return new Result(false, "Error: Cube definition string contains more than 54 facelets.");
        }
        int[] cnt = new int[6];
        for (int i = 0; i < 54; i++) {
            char c = s.charAt(i);
            if (c == 'U') cnt[f[i] = Color.U.ordinal()]++;
            else if (c == 'R') cnt[f[i] = Color.R.ordinal()]++;
            else if (c == 'F') cnt[f[i] = Color.F.ordinal()]++;
            else if (c == 'D') cnt[f[i] = Color.D.ordinal()]++;
            else if (c == 'L') cnt[f[i] = Color.L.ordinal()]++;
            else if (c == 'B') cnt[f[i] = Color.B.ordinal()]++;
            else return new Result(false, "Error: Cube definition string contains a character that is not from {'U','R','F','D','L','B'}");
        }
        for (int i = 0; i < 6; i++) {
            if (cnt[i] != 9) {
                return new Result(false, "Error: Cube definition string doesn't contain exactly 9 facelets of each color");
            }
        }
        return new Result(true, "");
    }

    @Override
    public String toString() {
        String str = "URFDLB";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 54; i++) sb.append(str.charAt(f[i]));
        return sb.toString();
    }

    public String to2DString() {
        String s = toString();
        StringBuilder res = new StringBuilder();
        res.append("   ").append(s.substring(0, 3)).append("\n");
        res.append("   ").append(s.substring(3, 6)).append("\n");
        res.append("   ").append(s.substring(6, 9)).append("\n");
        res.append(s.substring(36, 39)).append(s.substring(18, 21)).append(s.substring(9, 12)).append(s.substring(45, 48)).append("\n");
        res.append(s.substring(39, 42)).append(s.substring(21, 24)).append(s.substring(12, 15)).append(s.substring(48, 51)).append("\n");
        res.append(s.substring(42, 45)).append(s.substring(24, 27)).append(s.substring(15, 18)).append(s.substring(51, 54)).append("\n");
        res.append("   ").append(s.substring(27, 30)).append("\n");
        res.append("   ").append(s.substring(30, 33)).append("\n");
        res.append("   ").append(s.substring(33, 36)).append("\n");
        return res.toString();
    }

    /**
     * Return a cubie cube representation of the facelet cube.
     * @return CubieCube representation
     */
    public CubieCube toCubieCube() {
        CubieCube c = new CubieCube();
        for (int i = 0; i < 8; i++) c.setCp(i, -1);
        for (int i = 0; i < 12; i++) c.setEp(i, -1);

        for (int i = 0; i < 8; i++) {
            int[] face = Defs.cornerFacelet[i];
            int ori = 0;
            for (ori = 0; ori < 3; ori++) {
                int col = f[face[ori]];
                if (col == Color.U.ordinal() || col == Color.D.ordinal()) break;
            }
            int col1 = f[face[(ori + 1) % 3]];
            int col2 = f[face[(ori + 2) % 3]];
            for (int j = 0; j < 8; j++) {
                int[] col = Defs.cornerColor[j];
                if (col[1] == col1 && col[2] == col2) {
                    c.setCp(i, j);
                    c.setCo(i, ori);
                    break;
                }
            }
        }

        for (int i = 0; i < 12; i++) {
            for (int j = 0; j < 12; j++) {
                if (f[Defs.edgeFacelet[i][0]] == Defs.edgeColor[j][0] &&
                    f[Defs.edgeFacelet[i][1]] == Defs.edgeColor[j][1]) {
                    c.setEp(i, j);
                    c.setEo(i, 0);
                    break;
                }
                if (f[Defs.edgeFacelet[i][0]] == Defs.edgeColor[j][1] &&
                    f[Defs.edgeFacelet[i][1]] == Defs.edgeColor[j][0]) {
                    c.setEp(i, j);
                    c.setEo(i, 1);
                    break;
                }
            }
        }
        return c;
    }

    // ========== Helper class for result ==========

    /**
     * Result class for validation operations.
     * Encapsulates success status and message.
     */
    public static class Result {
        private final boolean success;
        private final String message;

        public Result(boolean success, String message) {
            this.success = success;
            this.message = message;
        }

        public boolean isSuccess() {
            return success;
        }

        public String getMessage() {
            return message;
        }

        // Keep public fields for backward compatibility
        public boolean getSuccess() { return success; }
    }
}
