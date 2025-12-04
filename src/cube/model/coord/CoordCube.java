package cube.model.coord;

import cube.model.cubie.CubieCube;
import cube.symmetry.SymmetryTables;
import cube.moves.MoveTables;
import cube.pruning.PruningTables;

import static cube.model.cubie.Defs.*;

/**
 * Coordinate-level cube representation for the optimal solver.
 * Extends BaseCoordCube and overrides isSolved() and getDepth().
 * 
 * Uses 3-axis coordinates (UD, RL, FB) for pruning.
 */
public class CoordCube extends BaseCoordCube {

    // UD-axis coordinates (inherited: twist->UD_twist, flip->UD_flip, sliceSorted->UD_slice_sorted)
    protected int UD_tetra;

    // RL-axis (after 120° URF-DBL rotation)
    protected int RL_twist;
    protected int RL_flip;
    protected int RL_slice_sorted;
    protected int RL_tetra;

    // FB-axis (after 240° URF-DBL rotation)
    protected int FB_twist;
    protected int FB_flip;
    protected int FB_slice_sorted;
    protected int FB_tetra;

    // Symmetry-reduced (flip, slice_sorted) triplets per axis
    protected int UD_flipslicesorted_clsidx, UD_flipslicesorted_sym;
    protected int UD_flipslicesorted_rep;

    protected int RL_flipslicesorted_clsidx, RL_flipslicesorted_sym;
    protected int RL_flipslicesorted_rep;

    protected int FB_flipslicesorted_clsidx, FB_flipslicesorted_sym;
    protected int FB_flipslicesorted_rep;

    // Cached phase-1×24 depths (absolute, reconstructed from mod-3)
    protected int UD_phasex24_depth, RL_phasex24_depth, FB_phasex24_depth;

    // Exact corner distance
    protected int corner_depth;

    // ========== Getters for UD-axis (using parent's protected fields) ==========

    public int getUdTwist() { return twist; }
    public int getUdFlip() { return flip; }
    public int getUdSliceSorted() { return sliceSorted; }
    public int getUdTetra() { return UD_tetra; }
    public int getUdPhasex24Depth() { return UD_phasex24_depth; }

    // ========== Getters for RL-axis ==========

    public int getRlTwist() { return RL_twist; }
    public int getRlFlip() { return RL_flip; }
    public int getRlSliceSorted() { return RL_slice_sorted; }
    public int getRlTetra() { return RL_tetra; }
    public int getRlPhasex24Depth() { return RL_phasex24_depth; }

    // ========== Getters for FB-axis ==========

    public int getFbTwist() { return FB_twist; }
    public int getFbFlip() { return FB_flip; }
    public int getFbSliceSorted() { return FB_slice_sorted; }
    public int getFbTetra() { return FB_tetra; }
    public int getFbPhasex24Depth() { return FB_phasex24_depth; }

    // ========== Other getters ==========

    public int getCornerDepth() { return corner_depth; }

    // ========== Constructors ==========

    // Solved coordinates
    public CoordCube() {
        corners = 0;
        twist = 0; flip = 0; sliceSorted = 0; UD_tetra = 0;
        RL_twist = 0; RL_flip = 0; RL_slice_sorted = 0; RL_tetra = 0;
        FB_twist = 0; FB_flip = 0; FB_slice_sorted = 0; FB_tetra = 0;
        UD_flipslicesorted_clsidx = 0; UD_flipslicesorted_sym = 0; UD_flipslicesorted_rep = 0;
        RL_flipslicesorted_clsidx = 0; RL_flipslicesorted_sym = 0; RL_flipslicesorted_rep = 0;
        FB_flipslicesorted_clsidx = 0; FB_flipslicesorted_sym = 0; FB_flipslicesorted_rep = 0;
        UD_phasex24_depth = 0; RL_phasex24_depth = 0; FB_phasex24_depth = 0;
        corner_depth = 0;
    }

    // Construct from a CubieCube
    public CoordCube(CubieCube cc) {
        // UD-axis (identity)
        corners = cc.getCorners();
        twist = cc.getTwist();
        flip = cc.getFlip();
        sliceSorted = cc.getSliceSorted();
        UD_tetra = cc.getTetra();

        // RL-axis: 120° rotation around URF-DBL, then conjugate cc
        {
            CubieCube ss = new CubieCube(SymmetryTables.symCube[16].getCpArray(), SymmetryTables.symCube[16].getCoArray(),
                                         SymmetryTables.symCube[16].getEpArray(), SymmetryTables.symCube[16].getEoArray());
            ss.multiply(cc);
            ss.multiply(SymmetryTables.symCube[32]);
            RL_twist = ss.getTwist();
            RL_flip = ss.getFlip();
            RL_slice_sorted = ss.getSliceSorted();
            RL_tetra = ss.getTetra();
        }

        // FB-axis: 240° rotation (inverse of above)
        {
            CubieCube ss = new CubieCube(SymmetryTables.symCube[32].getCpArray(), SymmetryTables.symCube[32].getCoArray(),
                                         SymmetryTables.symCube[32].getEpArray(), SymmetryTables.symCube[32].getEoArray());
            ss.multiply(cc);
            ss.multiply(SymmetryTables.symCube[16]);
            FB_twist = ss.getTwist();
            FB_flip = ss.getFlip();
            FB_slice_sorted = ss.getSliceSorted();
            FB_tetra = ss.getTetra();
        }

        // Symmetry-reduced (flip, slice_sorted) per axis
        int idx = N_FLIP * sliceSorted + flip;
        UD_flipslicesorted_clsidx = SymmetryTables.flipslicesortedClassidx[idx];
        UD_flipslicesorted_sym = SymmetryTables.flipslicesortedSym[idx] & 0xFF;
        UD_flipslicesorted_rep = SymmetryTables.flipslicesortedRep[UD_flipslicesorted_clsidx];

        idx = N_FLIP * RL_slice_sorted + RL_flip;
        RL_flipslicesorted_clsidx = SymmetryTables.flipslicesortedClassidx[idx];
        RL_flipslicesorted_sym = SymmetryTables.flipslicesortedSym[idx] & 0xFF;
        RL_flipslicesorted_rep = SymmetryTables.flipslicesortedRep[RL_flipslicesorted_clsidx];

        idx = N_FLIP * FB_slice_sorted + FB_flip;
        FB_flipslicesorted_clsidx = SymmetryTables.flipslicesortedClassidx[idx];
        FB_flipslicesorted_sym = SymmetryTables.flipslicesortedSym[idx] & 0xFF;
        FB_flipslicesorted_rep = SymmetryTables.flipslicesortedRep[FB_flipslicesorted_clsidx];

        // Cached depths reconstructed from mod-3 pruning
        UD_phasex24_depth = getPhasex24Depth(0);
        RL_phasex24_depth = getPhasex24Depth(1);
        FB_phasex24_depth = getPhasex24Depth(2);

        // Exact corner distance
        corner_depth = PruningTables.cornerDepth[corners] & 0xFF;
    }

    // ========== Abstract method implementations (polymorphism) ==========

    /**
     * Override from BaseCoordCube.
     * For optimal solver, solved means corners and all UD coordinates are zero.
     */
    @Override
    public boolean isSolved() {
        return corners == SOLVED && twist == SOLVED && flip == SOLVED && sliceSorted == SOLVED;
    }

    /**
     * Override from BaseCoordCube.
     * Returns the corner depth for pruning.
     */
    @Override
    public int getDepth() {
        return corner_depth;
    }

    // ========== Move method ==========

    // Apply a Move (0..17)
    public void move(int m) {
        // Apply on UD axis
        corners = MoveTables.cornersMove[N_MOVE * corners + m] & 0xFFFF;
        twist = MoveTables.twistMove[N_MOVE * twist + m] & 0xFFFF;
        flip = MoveTables.flipMove[N_MOVE * flip + m] & 0xFFFF;
        sliceSorted = MoveTables.sliceSortedMove[N_MOVE * sliceSorted + m] & 0xFFFF;
        UD_tetra = MoveTables.tetraMove[N_MOVE * UD_tetra + m] & 0xFFFF;

        int idx = N_FLIP * sliceSorted + flip;
        UD_flipslicesorted_clsidx = SymmetryTables.flipslicesortedClassidx[idx];
        UD_flipslicesorted_sym = SymmetryTables.flipslicesortedSym[idx] & 0xFF;
        UD_flipslicesorted_rep = SymmetryTables.flipslicesortedRep[UD_flipslicesorted_clsidx];

        // Conjugate move for RL axis (120° URF-DBL)
        m = SymmetryTables.conjMove[N_MOVE * 16 + m] & 0xFFFF;
        RL_twist = MoveTables.twistMove[N_MOVE * RL_twist + m] & 0xFFFF;
        RL_flip = MoveTables.flipMove[N_MOVE * RL_flip + m] & 0xFFFF;
        RL_slice_sorted = MoveTables.sliceSortedMove[N_MOVE * RL_slice_sorted + m] & 0xFFFF;
        RL_tetra = MoveTables.tetraMove[N_MOVE * RL_tetra + m] & 0xFFFF;

        idx = N_FLIP * RL_slice_sorted + RL_flip;
        RL_flipslicesorted_clsidx = SymmetryTables.flipslicesortedClassidx[idx];
        RL_flipslicesorted_sym = SymmetryTables.flipslicesortedSym[idx] & 0xFF;
        RL_flipslicesorted_rep = SymmetryTables.flipslicesortedRep[RL_flipslicesorted_clsidx];

        // Conjugate again for FB axis (240° total)
        m = SymmetryTables.conjMove[N_MOVE * 16 + m] & 0xFFFF;
        FB_twist = MoveTables.twistMove[N_MOVE * FB_twist + m] & 0xFFFF;
        FB_flip = MoveTables.flipMove[N_MOVE * FB_flip + m] & 0xFFFF;
        FB_slice_sorted = MoveTables.sliceSortedMove[N_MOVE * FB_slice_sorted + m] & 0xFFFF;
        FB_tetra = MoveTables.tetraMove[N_MOVE * FB_tetra + m] & 0xFFFF;

        idx = N_FLIP * FB_slice_sorted + FB_flip;
        FB_flipslicesorted_clsidx = SymmetryTables.flipslicesortedClassidx[idx];
        FB_flipslicesorted_sym = SymmetryTables.flipslicesortedSym[idx] & 0xFF;
        FB_flipslicesorted_rep = SymmetryTables.flipslicesortedRep[FB_flipslicesorted_clsidx];
    }

    // ========== Depth calculation methods ==========

    // Compute absolute phase-1×24 distance from the given axis position (reconstruct from mod-3)
    public int getPhasex24Depth(int position) {
        int slicesorted_, flip_, twist_;
        int clsidx, sym;

        if (position == 0) {
            slicesorted_ = sliceSorted; flip_ = flip; twist_ = twist;
            clsidx = UD_flipslicesorted_clsidx; sym = UD_flipslicesorted_sym;
        } else if (position == 1) {
            slicesorted_ = RL_slice_sorted; flip_ = RL_flip; twist_ = RL_twist;
            clsidx = RL_flipslicesorted_clsidx; sym = RL_flipslicesorted_sym;
        } else {
            slicesorted_ = FB_slice_sorted; flip_ = FB_flip; twist_ = FB_twist;
            clsidx = FB_flipslicesorted_clsidx; sym = FB_flipslicesorted_sym;
        }

        int depthMod3 = PruningTables.getFlipslicesortedTwistDepth3(
            (long) N_TWIST * clsidx + (SymmetryTables.twistConj[(twist_ << 4) + sym] & 0xFFFF));
        int depth = 0;

        while (flip_ != SOLVED || slicesorted_ != SOLVED || twist_ != SOLVED) {
            if (depthMod3 == 0) depthMod3 = 3;
            for (int m = 0; m < N_MOVE; ++m) {
                int twist1 = MoveTables.twistMove[N_MOVE * twist_ + m] & 0xFFFF;
                int flip1 = MoveTables.flipMove[N_MOVE * flip_ + m] & 0xFFFF;
                int slicesorted1 = MoveTables.sliceSortedMove[N_MOVE * slicesorted_ + m] & 0xFFFF;
                int fs1 = N_FLIP * slicesorted1 + flip1;
                int classidx1 = SymmetryTables.flipslicesortedClassidx[fs1];
                int sym1 = SymmetryTables.flipslicesortedSym[fs1] & 0xFF;
                if (PruningTables.getFlipslicesortedTwistDepth3(
                        (long) N_TWIST * classidx1 + (SymmetryTables.twistConj[(twist1 << 4) + sym1] & 0xFFFF)) == depthMod3 - 1) {
                    twist_ = twist1;
                    flip_ = flip1;
                    slicesorted_ = slicesorted1;
                    depth++;
                    depthMod3--;
                    break;
                }
            }
        }
        return depth;
    }

    // Get ubig depth for the given direction (0=UD, 1=RL, 2=FB)
    public int getUbigDepth(int direction) {
        int twist_, flip_, slicesorted_, tetra;
        if (direction == 0) {
            twist_ = twist; flip_ = flip; slicesorted_ = sliceSorted; tetra = UD_tetra;
        } else if (direction == 1) {
            twist_ = RL_twist; flip_ = RL_flip; slicesorted_ = RL_slice_sorted; tetra = RL_tetra;
        } else {
            twist_ = FB_twist; flip_ = FB_flip; slicesorted_ = FB_slice_sorted; tetra = FB_tetra;
        }

        int slice = slicesorted_ / N_PERM_4;
        int fs = N_FLIP * slice + flip_;
        int clsidx = SymmetryTables.flipsliceClassidx[fs] & 0xFFFF;
        int sym = SymmetryTables.flipsliceSym[fs] & 0xFF;

        int twistC = SymmetryTables.twistConj[(twist_ << 4) + sym] & 0xFFFF;
        int tetraC = SymmetryTables.tetraConj[N_SYM_D4h * tetra + sym] & 0xFFFF;

        int depth = 0;

        while (twistC != 0 || clsidx != 0 || tetraC != 0) {
            int idx = N_TWIST * clsidx + twistC;
            int depthMod = PruningTables.getUbigMod3(tetraC, idx);
            if (depthMod == 0) depthMod = 3;
            for (int m = 0; m < N_MOVE; m++) {
                int twist1 = MoveTables.twistMove[N_MOVE * twist_ + m] & 0xFFFF;
                int flip1 = MoveTables.flipMove[N_MOVE * flip_ + m] & 0xFFFF;
                int slicesorted1 = MoveTables.sliceSortedMove[N_MOVE * slicesorted_ + m] & 0xFFFF;
                int tetra1 = MoveTables.tetraMove[N_MOVE * tetra + m] & 0xFFFF;

                int slice1 = slicesorted1 / N_PERM_4;
                int fs1 = N_FLIP * slice1 + flip1;
                int clsidx1 = SymmetryTables.flipsliceClassidx[fs1] & 0xFFFF;
                int sym1 = SymmetryTables.flipsliceSym[fs1] & 0xFF;

                int twist1C = SymmetryTables.twistConj[(twist1 << 4) + sym1] & 0xFFFF;
                int tetra1C = SymmetryTables.tetraConj[N_SYM_D4h * tetra1 + sym1] & 0xFFFF;

                int idx1 = N_TWIST * clsidx1 + twist1C;
                int mod1 = PruningTables.getUbigMod3(tetra1C, idx1);

                if (mod1 == depthMod - 1) {
                    depth++;
                    twist_ = twist1;
                    flip_ = flip1;
                    slicesorted_ = slicesorted1;
                    slice = slice1;
                    tetra = tetra1;
                    clsidx = clsidx1;
                    twistC = twist1C;
                    tetraC = tetra1C;
                    break;
                }
            }
        }
        return depth;
    }
}
