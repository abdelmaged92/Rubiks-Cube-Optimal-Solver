package cube.model.face;

// The moves in the faceturn metric. Not to be confused with the names of the facelet positions in class Facelet.
public enum Move {
    U1, U2, U3,
    R1, R2, R3,
    F1, F2, F3,
    D1, D2, D3,
    L1, L2, L3,
    B1, B2, B3;

    public static final int COUNT = 18;

    private static final String[] NAMES = {
        "U", "U2", "U'",
        "R", "R2", "R'",
        "F", "F2", "F'",
        "D", "D2", "D'",
        "L", "L2", "L'",
        "B", "B2", "B'"
    };

    public static Move fromIndex(int index) {
        return values()[index];
    }

    public int getFace() {
        return ordinal() / 3;
    }

    public int getTurnType() {
        return ordinal() % 3;
    }

    public Move inverse() {
        int face = getFace();
        int turn = getTurnType();
        int invTurn = (2 - turn);
        return fromIndex(face * 3 + invTurn);
    }

    public String toNotation() {
        return NAMES[ordinal()];
    }

    public static Move parse(String s) {
        if (s == null || s.isEmpty()) return null;
        
        s = s.trim();
        char face = s.charAt(0);
        int faceIdx;
        switch (face) {
            case 'U': faceIdx = 0; break;
            case 'R': faceIdx = 1; break;
            case 'F': faceIdx = 2; break;
            case 'D': faceIdx = 3; break;
            case 'L': faceIdx = 4; break;
            case 'B': faceIdx = 5; break;
            default: return null;
        }

        int turn = 0;
        if (s.length() == 1) {
            turn = 0;
        } else if (s.length() == 2) {
            char modifier = s.charAt(1);
            if (modifier == '1') turn = 0;
            else if (modifier == '2') turn = 1;
            else if (modifier == '3' || modifier == '\'') turn = 2;
            else return null;
        } else {
            return null;
        }

        return fromIndex(faceIdx * 3 + turn);
    }
}
