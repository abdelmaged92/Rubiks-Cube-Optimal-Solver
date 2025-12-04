package cube.moves;

import cube.model.cubie.CubieCube;
import cube.model.cubie.Defs;
import java.io.*;
import java.util.concurrent.*;

import static cube.model.cubie.Defs.*;

// Movetables describe the transformation of the coordinates by cube moves
public class MoveTables {

    // All move tables use short (uint16_t in C++) to match C++ binary format
    public static short[] twistMove = new short[N_TWIST * N_MOVE];
    public static short[] flipMove = new short[N_FLIP * N_MOVE];
    public static short[] sliceSortedMove = new short[N_SLICE_SORTED * N_MOVE];
    public static short[] cornersMove = new short[N_CORNERS * N_MOVE];
    public static short[] uEdgesMove = new short[N_SLICE_SORTED * N_MOVE];
    public static short[] dEdgesMove = new short[N_SLICE_SORTED * N_MOVE];
    public static short[] udEdgesMove = new short[N_UD_EDGES * N_MOVE];
    public static short[] tetraMove = new short[N_TETRA * N_MOVE];

    private static boolean initialized = false;
    private static final int NUM_THREADS = Runtime.getRuntime().availableProcessors();

    private static void buildMoveTwist() {
        System.out.println("creating move_twist table.");
        CubieCube a = new CubieCube();
        CubieCube[] basic = CubieCube.basicMoveCube;
        for (int i = 0; i < N_TWIST; i++) {
            a.setTwist(i);
            for (int j = 0; j < 6; j++) {
                for (int k = 0; k < 3; k++) {
                    a.cornerMultiply(basic[j]);
                    twistMove[N_MOVE * i + 3 * j + k] = (short) a.getTwist();
                }
                a.cornerMultiply(basic[j]);
            }
        }
        saveShortArray("move_twist", twistMove);
    }

    private static void buildMoveFlip() {
        System.out.println("creating move_flip table.");
        CubieCube a = new CubieCube();
        CubieCube[] basic = CubieCube.basicMoveCube;
        for (int i = 0; i < N_FLIP; i++) {
            a.setFlip(i);
            for (int j = 0; j < 6; j++) {
                for (int k = 0; k < 3; k++) {
                    a.edgeMultiply(basic[j]);
                    flipMove[N_MOVE * i + 3 * j + k] = (short) a.getFlip();
                }
                a.edgeMultiply(basic[j]);
            }
        }
        saveShortArray("move_flip", flipMove);
    }

    private static void buildMoveSliceSorted() {
        System.out.println("creating move_slice_sorted table (" + NUM_THREADS + " threads)...");
        ExecutorService exec = Executors.newFixedThreadPool(NUM_THREADS);
        int chunkSize = N_SLICE_SORTED / NUM_THREADS;
        
        for (int t = 0; t < NUM_THREADS; t++) {
            final int start = t * chunkSize;
            final int end = (t == NUM_THREADS - 1) ? N_SLICE_SORTED : start + chunkSize;
            exec.submit(() -> {
                CubieCube a = new CubieCube();
                CubieCube[] basic = CubieCube.basicMoveCube;
                for (int i = start; i < end; i++) {
                    a.setSliceSorted(i);
                    for (int j = 0; j < 6; j++) {
                        for (int k = 0; k < 3; k++) {
                            a.edgeMultiply(basic[j]);
                            sliceSortedMove[N_MOVE * i + 3 * j + k] = (short) a.getSliceSorted();
                        }
                        a.edgeMultiply(basic[j]);
                    }
                }
            });
        }
        exec.shutdown();
        try { exec.awaitTermination(10, TimeUnit.MINUTES); } catch (InterruptedException e) {}
        System.out.println("done.");
        saveShortArray("move_slice_sorted", sliceSortedMove);
    }

    private static void buildMoveCorners() {
        System.out.println("creating move_corners table (" + NUM_THREADS + " threads)...");
        ExecutorService exec = Executors.newFixedThreadPool(NUM_THREADS);
        int chunkSize = N_CORNERS / NUM_THREADS;
        
        for (int t = 0; t < NUM_THREADS; t++) {
            final int start = t * chunkSize;
            final int end = (t == NUM_THREADS - 1) ? N_CORNERS : start + chunkSize;
            exec.submit(() -> {
                CubieCube a = new CubieCube();
                CubieCube[] basic = CubieCube.basicMoveCube;
                for (int i = start; i < end; i++) {
                    a.setCorners(i);
                    for (int j = 0; j < 6; j++) {
                        for (int k = 0; k < 3; k++) {
                            a.cornerMultiply(basic[j]);
                            cornersMove[N_MOVE * i + 3 * j + k] = (short) a.getCorners();
                        }
                        a.cornerMultiply(basic[j]);
                    }
                }
            });
        }
        exec.shutdown();
        try { exec.awaitTermination(10, TimeUnit.MINUTES); } catch (InterruptedException e) {}
        System.out.println("done.");
        saveShortArray("move_corners", cornersMove);
    }

    private static void buildMoveUEdges() {
        System.out.println("creating move_u_edges table (" + NUM_THREADS + " threads)...");
        ExecutorService exec = Executors.newFixedThreadPool(NUM_THREADS);
        int chunkSize = N_SLICE_SORTED / NUM_THREADS;
        
        for (int t = 0; t < NUM_THREADS; t++) {
            final int start = t * chunkSize;
            final int end = (t == NUM_THREADS - 1) ? N_SLICE_SORTED : start + chunkSize;
            exec.submit(() -> {
                CubieCube a = new CubieCube();
                CubieCube[] basic = CubieCube.basicMoveCube;
                for (int i = start; i < end; i++) {
                    a.setUEdges(i);
                    for (int j = 0; j < 6; j++) {
                        for (int k = 0; k < 3; k++) {
                            a.edgeMultiply(basic[j]);
                            uEdgesMove[N_MOVE * i + 3 * j + k] = (short) a.getUEdges();
                        }
                        a.edgeMultiply(basic[j]);
                    }
                }
            });
        }
        exec.shutdown();
        try { exec.awaitTermination(10, TimeUnit.MINUTES); } catch (InterruptedException e) {}
        System.out.println("done.");
        saveShortArray("move_u_edges", uEdgesMove);
    }

    private static void buildMoveDEdges() {
        System.out.println("creating move_d_edges table (" + NUM_THREADS + " threads)...");
        ExecutorService exec = Executors.newFixedThreadPool(NUM_THREADS);
        int chunkSize = N_SLICE_SORTED / NUM_THREADS;
        
        for (int t = 0; t < NUM_THREADS; t++) {
            final int start = t * chunkSize;
            final int end = (t == NUM_THREADS - 1) ? N_SLICE_SORTED : start + chunkSize;
            exec.submit(() -> {
                CubieCube a = new CubieCube();
                CubieCube[] basic = CubieCube.basicMoveCube;
                for (int i = start; i < end; i++) {
                    a.setDEdges(i);
                    for (int j = 0; j < 6; j++) {
                        for (int k = 0; k < 3; k++) {
                            a.edgeMultiply(basic[j]);
                            dEdgesMove[N_MOVE * i + 3 * j + k] = (short) a.getDEdges();
                        }
                        a.edgeMultiply(basic[j]);
                    }
                }
            });
        }
        exec.shutdown();
        try { exec.awaitTermination(10, TimeUnit.MINUTES); } catch (InterruptedException e) {}
        System.out.println("done.");
        saveShortArray("move_d_edges", dEdgesMove);
    }

    private static void buildMoveUDEdges() {
        System.out.println("creating move_ud_edges table (" + NUM_THREADS + " threads)...");
        ExecutorService exec = Executors.newFixedThreadPool(NUM_THREADS);
        int chunkSize = N_UD_EDGES / NUM_THREADS;
        
        for (int t = 0; t < NUM_THREADS; t++) {
            final int start = t * chunkSize;
            final int end = (t == NUM_THREADS - 1) ? N_UD_EDGES : start + chunkSize;
            exec.submit(() -> {
                CubieCube a = new CubieCube();
                CubieCube[] basic = CubieCube.basicMoveCube;
                for (int i = start; i < end; i++) {
                    a.setUdEdges(i);
                    for (int j = 0; j < 6; j++) {
                        for (int k = 0; k < 3; k++) {
                            a.edgeMultiply(basic[j]);
                            if ((j == 1 || j == 2 || j == 4 || j == 5) && k != 1) continue;
                            udEdgesMove[N_MOVE * i + 3 * j + k] = (short) a.getUdEdges();
                        }
                        a.edgeMultiply(basic[j]);
                    }
                }
            });
        }
        exec.shutdown();
        try { exec.awaitTermination(10, TimeUnit.MINUTES); } catch (InterruptedException e) {}
        System.out.println("done.");
        saveShortArray("move_ud_edges", udEdgesMove);
    }

    private static void buildMoveTetra() {
        System.out.println("creating move_tetra table...");
        CubieCube a = new CubieCube();
        CubieCube[] basic = CubieCube.basicMoveCube;
        for (int i = 0; i < N_TETRA; i++) {
            a.setTetra(i);
            for (int j = 0; j < 6; j++) {
                for (int k = 0; k < 3; k++) {
                    a.cornerMultiply(basic[j]);
                    tetraMove[N_MOVE * i + 3 * j + k] = (short) a.getTetra();
                }
                a.cornerMultiply(basic[j]);
            }
        }
        saveShortArray("move_tetra", tetraMove);
    }

    public static void init() {
        if (initialized) return;
        
        File f;
        f = new File("move_twist");
        if (f.exists()) { System.out.println("loading move_twist table."); loadShortArray("move_twist", twistMove); }
        else buildMoveTwist();

        f = new File("move_flip");
        if (f.exists()) { System.out.println("loading move_flip table."); loadShortArray("move_flip", flipMove); }
        else buildMoveFlip();

        f = new File("move_slice_sorted");
        if (f.exists()) { System.out.println("loading move_slice_sorted table."); loadShortArray("move_slice_sorted", sliceSortedMove); }
        else buildMoveSliceSorted();

        f = new File("move_corners");
        if (f.exists()) { System.out.println("loading move_corners table."); loadShortArray("move_corners", cornersMove); }
        else buildMoveCorners();

        f = new File("move_u_edges");
        if (f.exists()) { System.out.println("loading move_u_edges table."); loadShortArray("move_u_edges", uEdgesMove); }
        else buildMoveUEdges();

        f = new File("move_d_edges");
        if (f.exists()) { System.out.println("loading move_d_edges table."); loadShortArray("move_d_edges", dEdgesMove); }
        else buildMoveDEdges();

        f = new File("move_ud_edges");
        if (f.exists()) { System.out.println("loading move_ud_edges table."); loadShortArray("move_ud_edges", udEdgesMove); }
        else buildMoveUDEdges();

        f = new File("move_tetra");
        if (f.exists()) { System.out.println("loading move_tetra table."); loadShortArray("move_tetra", tetraMove); }
        else buildMoveTetra();

        initialized = true;
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
}
