package cube.model.coord;

import cube.model.cubie.CubieCube;
import cube.model.face.Move;
import cube.symmetry.SymmetryTables;
import cube.moves.MoveTables;
import cube.pruning.PruningTables;

import static cube.model.cubie.Defs.*;

/**
 * Two-phase (fast) CoordCube representation.
 * Extends BaseCoordCube and overrides isSolved() and getDepth().
 * 
 * Phase 1 coordinates: twist, flip, slice = slice_sorted/24
 * Phase 2 coordinates: corners, ud_edges, slice_sorted%24
 */
public class TwoPhaseCoordCube extends BaseCoordCube {

    // Additional coordinates specific to two-phase solver
    protected int uEdges;       // < 11880 in phase 1, < 1680 in phase 2 (1656 for solved)
    protected int dEdges;       // same ranges as uEdges
    protected int udEdges;      // 0 .. N_UD_EDGES-1 in phase 2, or -1 if invalid

    // Symmetry-reduced coordinates
    protected int flipsliceClassidx;
    protected int flipsliceSym;
    protected int flipsliceRep;

    protected int cornerClassidx;
    protected int cornerSym;
    protected int cornerRep;

    // ========== Additional getters ==========

    public int getUEdges() {
        return uEdges;
    }

    public int getDEdges() {
        return dEdges;
    }

    public int getUdEdges() {
        return udEdges;
    }

    public int getFlipsliceClassidx() {
        return flipsliceClassidx;
    }

    public int getFlipsliceSym() {
        return flipsliceSym;
    }

    public int getCornerClassidx() {
        return cornerClassidx;
    }

    public int getCornerSym() {
        return cornerSym;
    }

    // ========== Constructors ==========

    // From identity cube
    public TwoPhaseCoordCube() {
        twist = 0;
        flip = 0;
        sliceSorted = 0;
        uEdges = 1656;
        dEdges = 0;
        corners = 0;
        udEdges = 0;

        int slice = sliceSorted / N_PERM_4;
        int flipslice = N_FLIP * slice + flip;
        flipsliceClassidx = SymmetryTables.flipsliceClassidx[flipslice] & 0xFFFF;
        flipsliceSym = SymmetryTables.flipsliceSym[flipslice] & 0xFF;
        flipsliceRep = SymmetryTables.flipsliceRep[flipsliceClassidx];

        cornerClassidx = SymmetryTables.cornerClassidx[corners] & 0xFFFF;
        cornerSym = SymmetryTables.cornerSym[corners] & 0xFF;
        cornerRep = SymmetryTables.cornerRep[cornerClassidx] & 0xFFFF;
    }

    // From a CubieCube
    public TwoPhaseCoordCube(CubieCube cc) {
        twist = cc.getTwist();
        flip = cc.getFlip();
        sliceSorted = cc.getSliceSorted();
        uEdges = cc.getUEdges();
        dEdges = cc.getDEdges();
        corners = cc.getCorners();

        if (sliceSorted < N_PERM_4) {
            udEdges = cc.getUdEdges();
        } else {
            udEdges = -1;
        }

        int slice = sliceSorted / N_PERM_4;
        int flipslice = N_FLIP * slice + flip;
        flipsliceClassidx = SymmetryTables.flipsliceClassidx[flipslice] & 0xFFFF;
        flipsliceSym = SymmetryTables.flipsliceSym[flipslice] & 0xFF;
        flipsliceRep = SymmetryTables.flipsliceRep[flipsliceClassidx];

        cornerClassidx = SymmetryTables.cornerClassidx[corners] & 0xFFFF;
        cornerSym = SymmetryTables.cornerSym[corners] & 0xFF;
        cornerRep = SymmetryTables.cornerRep[cornerClassidx] & 0xFFFF;
    }

    // ========== Abstract method implementations (polymorphism) ==========

    /**
     * Override from BaseCoordCube.
     * For two-phase solver, solved means all coordinates are zero.
     */
    @Override
    public boolean isSolved() {
        return twist == SOLVED && flip == SOLVED && sliceSorted == SOLVED 
            && corners == SOLVED && udEdges == SOLVED;
    }

    /**
     * Override from BaseCoordCube.
     * Returns the phase 1 depth estimate.
     */
    @Override
    public int getDepth() {
        return getDepthPhase1();
    }

    // ========== Move methods ==========

    // Apply a move in phase 1
    public void phase1Move(int m) {
        twist = MoveTables.twistMove[N_MOVE * twist + m] & 0xFFFF;
        flip = MoveTables.flipMove[N_MOVE * flip + m] & 0xFFFF;
        sliceSorted = MoveTables.sliceSortedMove[N_MOVE * sliceSorted + m] & 0xFFFF;
        uEdges = MoveTables.uEdgesMove[N_MOVE * uEdges + m] & 0xFFFF;
        dEdges = MoveTables.dEdgesMove[N_MOVE * dEdges + m] & 0xFFFF;
        corners = MoveTables.cornersMove[N_MOVE * corners + m] & 0xFFFF;

        int flipslice = N_FLIP * (sliceSorted / N_PERM_4) + flip;
        flipsliceClassidx = SymmetryTables.flipsliceClassidx[flipslice] & 0xFFFF;
        flipsliceSym = SymmetryTables.flipsliceSym[flipslice] & 0xFF;
        flipsliceRep = SymmetryTables.flipsliceRep[flipsliceClassidx];

        cornerClassidx = SymmetryTables.cornerClassidx[corners] & 0xFFFF;
        cornerSym = SymmetryTables.cornerSym[corners] & 0xFF;
        cornerRep = SymmetryTables.cornerRep[cornerClassidx] & 0xFFFF;
    }

    // Apply a move in phase 2
    public void phase2Move(int m) {
        sliceSorted = MoveTables.sliceSortedMove[N_MOVE * sliceSorted + m] & 0xFFFF;
        corners = MoveTables.cornersMove[N_MOVE * corners + m] & 0xFFFF;
        udEdges = MoveTables.udEdgesMove[N_MOVE * udEdges + m] & 0xFFFF;
    }

    // ========== Depth calculation methods ==========

    // Compute the distance to the cube subgroup H where flip=slice=twist=0
    public int getDepthPhase1() {
        int slice = sliceSorted / N_PERM_4;
        int flip_ = flip;
        int twist_ = twist;
        int flipslice = N_FLIP * slice + flip_;
        int classidx = SymmetryTables.flipsliceClassidx[flipslice] & 0xFFFF;
        int sym = SymmetryTables.flipsliceSym[flipslice] & 0xFF;
        int depthMod3 = PruningTables.getFlipsliceTwistDepth3(
            N_TWIST * classidx + (SymmetryTables.twistConj[(twist_ << 4) + sym] & 0xFFFF));
        int depth = 0;

        while (flip_ != SOLVED || slice != SOLVED || twist_ != SOLVED) {
            if (depthMod3 == 0) depthMod3 = 3;
            for (int m = 0; m < N_MOVE; m++) {
                int twist1 = MoveTables.twistMove[N_MOVE * twist_ + m] & 0xFFFF;
                int flip1 = MoveTables.flipMove[N_MOVE * flip_ + m] & 0xFFFF;
                int slice1 = (MoveTables.sliceSortedMove[N_MOVE * (slice * N_PERM_4) + m] & 0xFFFF) / N_PERM_4;
                int flipslice1 = N_FLIP * slice1 + flip1;
                int classidx1 = SymmetryTables.flipsliceClassidx[flipslice1] & 0xFFFF;
                int sym1 = SymmetryTables.flipsliceSym[flipslice1] & 0xFF;
                if (PruningTables.getFlipsliceTwistDepth3(
                        N_TWIST * classidx1 + (SymmetryTables.twistConj[(twist1 << 4) + sym1] & 0xFFFF)) == depthMod3 - 1) {
                    depth++;
                    twist_ = twist1;
                    flip_ = flip1;
                    slice = slice1;
                    depthMod3--;
                    break;
                }
            }
        }
        return depth;
    }

    // Get distance to solved state in phase 2
    public static int getDepthPhase2(int corners_, int udEdges_) {
        int classidx = SymmetryTables.cornerClassidx[corners_] & 0xFFFF;
        int sym = SymmetryTables.cornerSym[corners_] & 0xFF;
        int depthMod3 = PruningTables.getCornersUdEdgesDepth3(
            N_UD_EDGES * classidx + (SymmetryTables.udEdgesConj[(udEdges_ << 4) + sym] & 0xFFFF));
        if (depthMod3 == 3) return 11;

        int depth = 0;
        int[] phase2Moves = {
            Move.U1.ordinal(), Move.U2.ordinal(), Move.U3.ordinal(),
            Move.R2.ordinal(), Move.F2.ordinal(),
            Move.D1.ordinal(), Move.D2.ordinal(), Move.D3.ordinal(),
            Move.L2.ordinal(), Move.B2.ordinal()
        };

        while (corners_ != SOLVED || udEdges_ != SOLVED) {
            if (depthMod3 == 0) depthMod3 = 3;
            for (int m : phase2Moves) {
                int corners1 = MoveTables.cornersMove[N_MOVE * corners_ + m] & 0xFFFF;
                int udEdges1 = MoveTables.udEdgesMove[N_MOVE * udEdges_ + m] & 0xFFFF;
                int classidx1 = SymmetryTables.cornerClassidx[corners1] & 0xFFFF;
                int sym1 = SymmetryTables.cornerSym[corners1] & 0xFF;
                if (PruningTables.getCornersUdEdgesDepth3(
                        N_UD_EDGES * classidx1 + (SymmetryTables.udEdgesConj[(udEdges1 << 4) + sym1] & 0xFFFF)) == depthMod3 - 1) {
                    depth++;
                    corners_ = corners1;
                    udEdges_ = udEdges1;
                    depthMod3--;
                    break;
                }
            }
        }
        return depth;
    }
}
