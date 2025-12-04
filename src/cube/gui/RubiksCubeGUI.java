package cube.gui;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import cube.model.face.FaceCube;
import cube.model.cubie.CubieCube;
import cube.moves.MoveTables;
import cube.symmetry.SymmetryTables;
import cube.pruning.PruningTables;
import cube.solver.Solver;
import cube.solver.SolveResult;
import cube.solver.TwoPhaseSolver;
import cube.solver.OptimalSolver;

/**
 * Rubik's Cube Solver GUI.
 * Integrates the solver backend with a visual cube interface.
 * Uses standard URFDLB notation where each letter represents the color of that face's center.
 */
public class RubiksCubeGUI extends JFrame {
    private static final int CELL_SIZE = 40;
    private static final int GRID_SIZE = 3;
    private static final int NUM_FACES = 6;

    // Face order: U, R, F, D, L, B (standard cube notation)
    private static final String[] FACE_NAMES = {"U", "R", "F", "D", "L", "B"};
    private static final String[] FACE_LABELS = {"Up", "Right", "Front", "Down", "Left", "Back"};

    private CubeCell[][] cube;
    private JLabel stateStringDisplay, timeLimitLabel, targetLengthLabel;
    private JTextField stateStringInput, maneuverInput, timeLimitField, targetLengthField;
    private JTextArea solutionDisplay;
    
    // Move names for parsing
    private static final String[] MOVE_NAMES = {
        "U", "U2", "U'", "R", "R2", "R'", "F", "F2", "F'",
        "D", "D2", "D'", "L", "L2", "L'", "B", "B2", "B'"
    };
    private JComboBox<String> modeSelector;
    private JButton solveButton, cleanButton, randomButton, simulateButton, colorDisplay;
    private Color selectedColor;
    private String lastSolution = "";
    private java.util.List<Integer> lastSolutionMoves = new ArrayList<>();
    private String lastScramble = "";
    private boolean solutionValid = false;

    // Color mapping: index matches face order (U, R, F, D, L, B)
    // Standard color scheme (White top, Green front): U=White, R=Red, F=Green, D=Yellow, L=Orange, B=Blue
    private static final Color[] FACE_COLORS = {
        new Color(255, 255, 255), // U - White
        new Color(255, 0, 0),     // R - Red
        new Color(0, 128, 0),     // F - Green
        new Color(255, 255, 0),   // D - Yellow
        new Color(255, 165, 0),   // L - Orange
        new Color(0, 0, 255)      // B - Blue
    };

    // Map from Color to face character (URFDLB)
    private static final Map<Color, Character> COLOR_TO_FACE = new HashMap<>();
    private static final Map<Character, Color> FACE_TO_COLOR = new HashMap<>();
    
    static {
        for (int i = 0; i < NUM_FACES; i++) {
            COLOR_TO_FACE.put(FACE_COLORS[i], FACE_NAMES[i].charAt(0));
            FACE_TO_COLOR.put(FACE_NAMES[i].charAt(0), FACE_COLORS[i]);
        }
    }

    public RubiksCubeGUI() {
        setTitle("Rubik's Cube Solver");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false);

        // Initialize cube cells with solved state colors
        cube = new CubeCell[NUM_FACES][GRID_SIZE * GRID_SIZE];
        for (int face = 0; face < NUM_FACES; face++) {
            for (int i = 0; i < GRID_SIZE * GRID_SIZE; i++) {
                cube[face][i] = new CubeCell();
                cube[face][i].setColor(FACE_COLORS[face]);
            }
        }
        selectedColor = FACE_COLORS[0]; // Default to U color (Yellow)

        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(new EmptyBorder(10, 10, 10, 10));

        JPanel cubePanel = createCubePanel();
        JPanel controlPanel = createControlPanel();

        mainPanel.add(cubePanel, BorderLayout.WEST);
        mainPanel.add(controlPanel, BorderLayout.CENTER);

        this.add(mainPanel);
        this.pack();
        this.setLocationRelativeTo(null);
    }

    private JPanel createCubePanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);

        // Layout positions for the 6 faces in a cross pattern
        int[][] positions = {
            {1, 0},  // U - top center
            {2, 1},  // R - right of center
            {1, 1},  // F - center
            {1, 2},  // D - bottom center
            {0, 1},  // L - left of center
            {3, 1}   // B - far right
        };

        for (int f = 0; f < NUM_FACES; f++) {
            JPanel facePanel = createFacePanel(f);
            gbc.gridx = positions[f][0];
            gbc.gridy = positions[f][1];
            panel.add(facePanel, gbc);
        }

        return panel;
    }

    private JPanel createFacePanel(int faceIndex) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(new TitledBorder(FACE_LABELS[faceIndex]));

        JPanel gridPanel = new JPanel(new GridLayout(GRID_SIZE, GRID_SIZE, 2, 2));
        gridPanel.setBackground(new Color(50, 50, 50));
        gridPanel.setBorder(new EmptyBorder(5, 5, 5, 5));

        for (int i = 0; i < GRID_SIZE * GRID_SIZE; i++) {
            JButton cellButton = new JButton();
            cellButton.setPreferredSize(new Dimension(CELL_SIZE, CELL_SIZE));
            cellButton.setFont(new Font("Arial", Font.BOLD, 12));

            int cellIdx = i;
            cellButton.addActionListener(e -> onCellClicked(faceIndex, cellIdx, cellButton));

            // Center cell (index 4) is fixed and shows the face name
            if (i == 4) {
                cellButton.setEnabled(false);
                cellButton.setText(FACE_NAMES[faceIndex]);
            }
            cube[faceIndex][i].setButton(cellButton);

            updateCellButton(cellButton, cube[faceIndex][i]);

            gridPanel.add(cellButton);
        }

        panel.add(gridPanel, BorderLayout.CENTER);
        return panel;
    }

    private JPanel createControlPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        JPanel colorPanel = createColorPanel();
        JPanel modePanel = createModePanel();
        JPanel statePanel = createStatePanel();
        JPanel buttonPanel = createButtonPanel();

        JPanel top = new JPanel(new BorderLayout(10, 10));
        top.add(colorPanel, BorderLayout.NORTH);
        top.add(modePanel, BorderLayout.CENTER);

        panel.add(top, BorderLayout.NORTH);
        panel.add(statePanel, BorderLayout.CENTER);
        panel.add(buttonPanel, BorderLayout.SOUTH);

        return panel;
    }

    private JPanel createColorPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(new TitledBorder("Color Selector"));

        JPanel gridPanel = new JPanel(new GridLayout(2, 3, 5, 5));
        gridPanel.setBorder(new EmptyBorder(5, 5, 5, 5));

        // Create color buttons in URFDLB order
        for (int i = 0; i < NUM_FACES; i++) {
            JButton colorButton = new JButton();
            colorButton.setPreferredSize(new Dimension(60, 40));
            Color color = FACE_COLORS[i];
            colorButton.setBackground(color);
            colorButton.setOpaque(true);
            colorButton.setBorder(new LineBorder(Color.BLACK, 2));
            colorButton.addActionListener(e -> {
                selectedColor = color;
                colorDisplay.setBackground(selectedColor);
                colorDisplay.repaint();
            });
            gridPanel.add(colorButton);
        }

        JPanel displayPanel = new JPanel(new BorderLayout(10, 0));
        displayPanel.add(new JLabel("Selected:"), BorderLayout.WEST);
        colorDisplay = new JButton();
        colorDisplay.setPreferredSize(new Dimension(60, 30));
        colorDisplay.setBackground(selectedColor);
        colorDisplay.setOpaque(true);
        colorDisplay.setEnabled(false);
        colorDisplay.setBorder(new LineBorder(Color.BLACK, 2));
        displayPanel.add(colorDisplay, BorderLayout.CENTER);

        panel.add(gridPanel, BorderLayout.CENTER);
        panel.add(displayPanel, BorderLayout.SOUTH);

        return panel;
    }

    private JPanel createModePanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(new TitledBorder("Solver Mode"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(2, 5, 2, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Mode row
        gbc.gridx = 0; gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.weightx = 0;
        panel.add(new JLabel("Mode:"), gbc);

        gbc.gridx = 1; gbc.gridy = 0;
        gbc.weightx = 1;
        modeSelector = new JComboBox<>(new String[]{"Fast", "Optimal", "Smart Optimal"});
        modeSelector.addActionListener(e -> updateFastModeFieldsVisibility());
        panel.add(modeSelector, gbc);

        // Target Length row
        gbc.gridx = 0; gbc.gridy = 1;
        gbc.weightx = 0;
        targetLengthLabel = new JLabel("Target Length:");
        panel.add(targetLengthLabel, gbc);

        gbc.gridx = 1; gbc.gridy = 1;
        gbc.weightx = 1;
        targetLengthField = new JTextField(8);
        targetLengthField.setText("20");
        panel.add(targetLengthField, gbc);

        // Time Limit row
        gbc.gridx = 0; gbc.gridy = 2;
        gbc.weightx = 0;
        timeLimitLabel = new JLabel("Time Limit (ms):");
        panel.add(timeLimitLabel, gbc);

        gbc.gridx = 1; gbc.gridy = 2;
        gbc.weightx = 1;
        timeLimitField = new JTextField(8);
        timeLimitField.setText("5000");
        panel.add(timeLimitField, gbc);

        updateFastModeFieldsVisibility();

        return panel;
    }

    private void updateFastModeFieldsVisibility() {
        boolean isFastMode = "Fast".equals(modeSelector.getSelectedItem());
        timeLimitLabel.setVisible(isFastMode);
        timeLimitField.setVisible(isFastMode);
        targetLengthLabel.setVisible(isFastMode);
        targetLengthField.setVisible(isFastMode);
    }

    private JPanel createStatePanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(new TitledBorder("Cube State & Solution"));

        // Facelet input
        JLabel inputLabel = new JLabel("Input Facelet (54 chars, URFDLB):");
        stateStringInput = new JTextField();
        stateStringInput.setFont(new Font("Monospaced", Font.PLAIN, 11));
        stateStringInput.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                syncFromString();
            }
        });

        // Maneuver input
        JLabel maneuverLabel = new JLabel("Input Maneuver (e.g. R U R' U' or R1 U1 R3 U3):");
        maneuverInput = new JTextField();
        maneuverInput.setFont(new Font("Monospaced", Font.PLAIN, 11));
        maneuverInput.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                syncFromManeuver();
            }
        });

        JLabel outputLabel = new JLabel("Current State:");
        stateStringDisplay = new JLabel();
        stateStringDisplay.setFont(new Font("Monospaced", Font.PLAIN, 11));
        updateStateDisplay();

        JLabel solutionLabel = new JLabel("Solution:");
        solutionDisplay = new JTextArea(5, 40);
        solutionDisplay.setFont(new Font("Monospaced", Font.PLAIN, 11));
        solutionDisplay.setEditable(false);
        solutionDisplay.setLineWrap(true);
        solutionDisplay.setWrapStyleWord(true);
        JScrollPane solutionScroll = new JScrollPane(solutionDisplay);

        // Input sections panel (stacked vertically)
        JPanel inputsPanel = new JPanel(new GridLayout(2, 1, 5, 5));
        
        JPanel faceletSection = new JPanel(new BorderLayout(5, 2));
        faceletSection.add(inputLabel, BorderLayout.NORTH);
        faceletSection.add(stateStringInput, BorderLayout.CENTER);
        
        JPanel maneuverSection = new JPanel(new BorderLayout(5, 2));
        maneuverSection.add(maneuverLabel, BorderLayout.NORTH);
        maneuverSection.add(maneuverInput, BorderLayout.CENTER);
        
        inputsPanel.add(faceletSection);
        inputsPanel.add(maneuverSection);

        JPanel outputSection = new JPanel(new BorderLayout(5, 5));
        outputSection.add(outputLabel, BorderLayout.NORTH);
        outputSection.add(stateStringDisplay, BorderLayout.CENTER);

        JPanel solutionSection = new JPanel(new BorderLayout(5, 5));
        solutionSection.add(solutionLabel, BorderLayout.NORTH);
        solutionSection.add(solutionScroll, BorderLayout.CENTER);

        panel.add(inputsPanel, BorderLayout.NORTH);
        panel.add(outputSection, BorderLayout.CENTER);
        panel.add(solutionSection, BorderLayout.SOUTH);

        return panel;
    }
    
    /**
     * Parse maneuver input and update the cube in real-time.
     */
    private void syncFromManeuver() {
        String input = maneuverInput.getText().trim();
        if (input.isEmpty()) {
            // Reset to solved state if empty
            onCleanClicked();
            return;
        }
        
        CubieCube cc = new CubieCube(); // Start from solved
        String[] parts = input.split("\\s+");
        
        for (String move : parts) {
            if (move.isEmpty()) continue;
            int m = parseMoveString(move);
            if (m == -1) {
                // Invalid move - show error but keep current cube state
                return;
            }
            cc.move(m);
        }
        
        // Convert to facelet and update GUI
        FaceCube fc = cc.toFaceletCube();
        String faceletString = fc.toString();
        syncFromString(faceletString);
        stateStringInput.setText(faceletString);
        clearSolution();
    }
    
    /**
     * Parse a move string to move index.
     * Case insensitive, supports R, R1, R2, R', R3 formats.
     * @param s Move string
     * @return Move index (0-17) or -1 if invalid
     */
    private int parseMoveString(String s) {
        s = s.toUpperCase().trim();
        
        // Handle R1 -> R, R3 -> R'
        if (s.length() == 2 && Character.isDigit(s.charAt(1))) {
            char face = s.charAt(0);
            char num = s.charAt(1);
            if (num == '1') {
                s = String.valueOf(face);
            } else if (num == '3') {
                s = face + "'";
            }
            // num == '2' stays as is (e.g., "R2")
        }
        
        for (int m = 0; m < MOVE_NAMES.length; m++) {
            if (MOVE_NAMES[m].equalsIgnoreCase(s)) {
                return m;
            }
        }
        return -1;
    }

    private JPanel createButtonPanel() {
        JPanel panel = new JPanel(new GridLayout(2, 2, 5, 5));
        panel.setBorder(new TitledBorder("Actions"));

        solveButton = new JButton("Solve");
        solveButton.addActionListener(e -> onSolveClicked());

        cleanButton = new JButton("Clean (Reset to Solved)");
        cleanButton.addActionListener(e -> onCleanClicked());

        randomButton = new JButton("Random");
        randomButton.addActionListener(e -> onRandomClicked());

        simulateButton = new JButton("Simulate Solution");
        simulateButton.addActionListener(e -> onSimulateClicked());
        simulateButton.setEnabled(false); // Enable only when solution exists

        panel.add(solveButton);
        panel.add(cleanButton);
        panel.add(randomButton);
        panel.add(simulateButton);

        return panel;
    }

    private void onCellClicked(int faceIndex, int cellIndex, JButton button) {
        if (cellIndex == 4) {
            JOptionPane.showMessageDialog(this, "Center color is fixed!", "Info", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        cube[faceIndex][cellIndex].setColor(selectedColor);
        updateCellButton(button, cube[faceIndex][cellIndex]);
        updateStateDisplay();
        clearSolution();
    }

    private void onSolveClicked() {
        String currentState = getStateString();
        String mode = (String) modeSelector.getSelectedItem();

        // Save the scramble state for simulation
        lastScramble = currentState;

        // Disable button during solve
        solveButton.setEnabled(false);
        simulateButton.setEnabled(false);
        solutionDisplay.setText("Solving...");

        // Run solver in background thread to keep GUI responsive
        SwingWorker<Object, Void> worker = new SwingWorker<>() {
            private String displayMessage = "";
            
            @Override
            protected Object doInBackground() {
                try {
                    SolveResult result;
                    long startTime = System.currentTimeMillis();
                    
                    if ("Fast".equals(mode)) {
                        int timeLimit;
                        int targetLength;
                        try {
                            timeLimit = Integer.parseInt(timeLimitField.getText());
                        } catch (NumberFormatException ex) {
                            displayMessage = "Error: Invalid time limit";
                            return null;
                        }
                        try {
                            targetLength = Integer.parseInt(targetLengthField.getText());
                        } catch (NumberFormatException ex) {
                            displayMessage = "Error: Invalid target length";
                            return null;
                        }
                        double timeLimitSec = timeLimit / 1000.0;
                        Solver solver = new TwoPhaseSolver();
                        result = solver.solve(currentState, targetLength, timeLimitSec);
                        
                        double elapsed = (System.currentTimeMillis() - startTime) / 1000.0;
                        displayMessage = result.getMessage() + "\n\nTime taken: " + String.format("%.3f", elapsed) + "s";
                        return result;
                    } else if ("Optimal".equals(mode)) {
                        Solver solver = new OptimalSolver();
                        result = solver.solve(currentState);
                        
                        double elapsed = (System.currentTimeMillis() - startTime) / 1000.0;
                        displayMessage = result.getMessage() + "\n\nTime taken: " + String.format("%.3f", elapsed) + "s";
                        return result;
                    } else {
                        // Smart Optimal mode
                        SmartOptimalResult smartResult = solveSmartOptimalWithResult(currentState);
                        displayMessage = smartResult.displayMessage;
                        return smartResult.result;
                    }
                } catch (Exception e) {
                    displayMessage = "Error: " + e.getMessage();
                    return null;
                }
            }

            @Override
            protected void done() {
                try {
                    Object obj = get();
                    SolveResult result = (obj instanceof SolveResult) ? (SolveResult) obj : null;
                    if (result != null && result.isSuccess()) {
                        lastSolutionMoves = new ArrayList<>(result.getMoves());
                        simulateButton.setEnabled(true);
                    } else {
                        lastSolutionMoves.clear();
                        simulateButton.setEnabled(false);
                    }
                    lastSolution = displayMessage;
                    solutionValid = true;
                    updateSolutionDisplay();
                } catch (Exception e) {
                    solutionDisplay.setText("Error: " + e.getMessage());
                    simulateButton.setEnabled(false);
                }
                solveButton.setEnabled(true);
            }
        };
        worker.execute();
    }
    
    // Container to return both result and display message from smart optimal
    private static class SmartOptimalResult {
        SolveResult result;
        String displayMessage;
        SmartOptimalResult(SolveResult r, String msg) { result = r; displayMessage = msg; }
    }
    
    private SmartOptimalResult solveSmartOptimalWithResult(String cubeString) {
        long totalStart = System.currentTimeMillis();
        StringBuilder log = new StringBuilder();
        
        // First, find an initial solution using the fast solver (target 18 moves, 5s limit)
        long start = System.currentTimeMillis();
        Solver fastSolver = new TwoPhaseSolver();
        SolveResult result = fastSolver.solve(cubeString, 18, 5);
        double t = (System.currentTimeMillis() - start) / 1000.0;
        
        int solutionLength = result.getMoveCount();
        log.append("Initial: ").append(result.getMessage()).append(" (").append(String.format("%.2f", t)).append("s)\n");

        // Try to improve to 17 moves only once, only if initial solve took less than 1s
        if (solutionLength == 18 && t < 1) {
            start = System.currentTimeMillis();
            SolveResult newResult = fastSolver.solve(cubeString, 17, 2);
            double t2 = (System.currentTimeMillis() - start) / 1000.0;
            
            if (newResult.getMoveCount() == 17) {
                result = newResult;
                solutionLength = 17;
                log.append("Optimized to 17: ").append(result.getMessage()).append(" (").append(String.format("%.2f", t2)).append("s)\n");
            } else {
                log.append("Could not optimize to 17 moves (").append(String.format("%.2f", t2)).append("s)\n");
            }
        }

        // Try optimal solver for final optimization
        log.append("Finding optimal...\n");
        Solver optimalSolver = new OptimalSolver();
        SolveResult optResult = optimalSolver.solve(cubeString, solutionLength, 60);
        
        if (optResult.isSuccess()) {
            result = optResult;
            log.append("Optimal: ").append(result.getMessage());
        } else {
            log.append(optResult.getMessage());
        }
        
        double totalElapsed = (System.currentTimeMillis() - totalStart) / 1000.0;
        log.append("\n\nTotal time taken: ").append(String.format("%.3f", totalElapsed)).append("s");
        
        return new SmartOptimalResult(result, log.toString());
    }

    private void onCleanClicked() {
        for (int f = 0; f < NUM_FACES; f++) {
            for (int i = 0; i < GRID_SIZE * GRID_SIZE; i++) {
                cube[f][i].setColor(FACE_COLORS[f]);
                updateCellButton(cube[f][i].getButton(), cube[f][i]);
            }
        }
        stateStringInput.setText("");
        updateStateDisplay();
        clearSolution();
    }

    private void onRandomClicked() {
        // Generate a random valid CubieCube
        CubieCube cc = new CubieCube();
        cc.randomize();
        
        // Convert to FaceCube to get the facelet string
        FaceCube fc = cc.toFaceletCube();
        String faceletString = fc.toString();
        
        // Update the GUI from the facelet string
        syncFromString(faceletString);
        stateStringInput.setText(faceletString);
        clearSolution();
    }

    /**
     * Get the current cube state as a 54-character string in URFDLB notation.
     * The string order is: U1-U9, R1-R9, F1-F9, D1-D9, L1-L9, B1-B9
     */
    private String getStateString() {
        StringBuilder ret = new StringBuilder();
        for (int face = 0; face < NUM_FACES; face++) {
            for (int i = 0; i < GRID_SIZE * GRID_SIZE; i++) {
                Color c = cube[face][i].getColor();
                ret.append(colorToFace(c));
            }
        }
        return ret.toString();
    }

    private void updateStateDisplay() {
        String state = getStateString();
        // Color-code the state string for readability
        // Use darker/adjusted colors for better visibility on white background
        // Current scheme: U=White, R=Red, F=Green, D=Yellow, L=Orange, B=Blue
        StringBuilder html = new StringBuilder("<html><span style='font-family:monospace;font-size:12px;'>");
        for (int i = 0; i < state.length(); i++) {
            char ch = state.charAt(i);
            String hex;
            switch (ch) {
                case 'U': hex = "#666666"; break;  // Gray (for White)
                case 'R': hex = "#CC0000"; break;  // Dark red
                case 'F': hex = "#006400"; break;  // Dark green
                case 'D': hex = "#B8860B"; break;  // Dark golden (for Yellow)
                case 'L': hex = "#FF8C00"; break;  // Dark orange
                case 'B': hex = "#0000CC"; break;  // Blue
                default: hex = "#000000"; break;
            }
            html.append("<span style='color:").append(hex).append(";font-weight:bold;'>").append(ch).append("</span>");
        }
        html.append("</span></html>");
        stateStringDisplay.setText(html.toString());
    }

    private void syncFromString() {
        String input = stateStringInput.getText().trim().toUpperCase();
        if (!input.isEmpty()) {
            syncFromString(input);
        }
    }

    private void syncFromString(String input) {
        if (input.length() != 54) {
            stateStringDisplay.setText("ERROR: String must be 54 characters");
            return;
        }
        
        boolean shouldShowWarning = false;
        StringBuilder correctedInput = new StringBuilder(input);
        
        try {
            for (int face = 0; face < NUM_FACES; face++) {
                for (int i = 0; i < GRID_SIZE * GRID_SIZE; i++) {
                    char ch = input.charAt(face * 9 + i);
                    Color color = faceToColor(ch);
                    
                    // Center cell must match the face color
                    if (i == 4) {
                        if (!color.equals(FACE_COLORS[face])) {
                            shouldShowWarning = true;
                            correctedInput.setCharAt(face * 9 + i, FACE_NAMES[face].charAt(0));
                        }
                        cube[face][i].setColor(FACE_COLORS[face]);
                    } else {
                        cube[face][i].setColor(color);
                    }
                    updateCellButton(cube[face][i].getButton(), cube[face][i]);
                }
            }
            updateStateDisplay();
            clearSolution();
        } catch (IllegalArgumentException ex) {
            stateStringDisplay.setText("ERROR: Invalid character. Use only U, R, F, D, L, B");
            return;
        }
        
        if (shouldShowWarning) {
            stateStringInput.setText(correctedInput.toString());
            JOptionPane.showMessageDialog(this, "Center colors were corrected to match face positions!", "Info", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    private void updateCellButton(JButton button, CubeCell cell) {
        Color c = cell.getColor();
        button.setBackground(c);
        button.setOpaque(true);
        button.setForeground(getContrastColor(c));
        button.setMargin(new Insets(0, 0, 0, 0));
        button.repaint();
    }

    private Color getContrastColor(Color c) {
        double r = c.getRed();
        double g = c.getGreen();
        double b = c.getBlue();
        double brightness = Math.sqrt(0.299 * r * r + 0.587 * g * g + 0.114 * b * b);
        return brightness > 130 ? Color.BLACK : Color.WHITE;
    }

    /**
     * Convert a Color to its face character (U, R, F, D, L, or B).
     */
    private char colorToFace(Color c) {
        Character face = COLOR_TO_FACE.get(c);
        if (face != null) {
            return face;
        }
        // Find closest color match
        int minDist = Integer.MAX_VALUE;
        char closest = 'U';
        for (int i = 0; i < NUM_FACES; i++) {
            Color fc = FACE_COLORS[i];
            int dist = Math.abs(c.getRed() - fc.getRed()) + 
                       Math.abs(c.getGreen() - fc.getGreen()) + 
                       Math.abs(c.getBlue() - fc.getBlue());
            if (dist < minDist) {
                minDist = dist;
                closest = FACE_NAMES[i].charAt(0);
            }
        }
        return closest;
    }

    /**
     * Convert a face character (U, R, F, D, L, or B) to its Color.
     */
    private Color faceToColor(char ch) {
        Color color = FACE_TO_COLOR.get(Character.toUpperCase(ch));
        if (color == null) {
            throw new IllegalArgumentException("Invalid face character: " + ch);
        }
        return color;
    }

    private void clearSolution() {
        lastSolution = "";
        lastSolutionMoves.clear();
        solutionValid = false;
        simulateButton.setEnabled(false);
        updateSolutionDisplay();
    }

    private void updateSolutionDisplay() {
        solutionDisplay.setText("");
        if (solutionValid && !lastSolution.isEmpty()) {
            solutionDisplay.setText(lastSolution);
        }
    }
    
    /**
     * Opens a popup window to simulate the solution step by step with full controls.
     */
    private void onSimulateClicked() {
        if (lastSolutionMoves.isEmpty() || lastScramble.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No solution to simulate!", "Info", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        
        // Create simulation dialog
        JDialog simDialog = new JDialog(this, "Solution Simulation", false);
        simDialog.setLayout(new BorderLayout(10, 10));
        simDialog.setSize(700, 750);
        simDialog.setLocationRelativeTo(this);
        
        // Create cube display panel (same layout as main window)
        JPanel cubePanel = new JPanel(new GridBagLayout());
        cubePanel.setBorder(new EmptyBorder(15, 15, 15, 15));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        
        // Create simulation cube cells - larger cells
        JButton[][] simCube = new JButton[6][9];
        int[][] positions = {{1, 0}, {2, 1}, {1, 1}, {1, 2}, {0, 1}, {3, 1}};
        
        for (int f = 0; f < 6; f++) {
            JPanel facePanel = new JPanel(new BorderLayout());
            facePanel.setBorder(BorderFactory.createTitledBorder(FACE_LABELS[f]));
            
            JPanel gridPanel = new JPanel(new GridLayout(3, 3, 2, 2));
            gridPanel.setBackground(new Color(50, 50, 50));
            gridPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
            
            for (int i = 0; i < 9; i++) {
                JButton cell = new JButton();
                cell.setPreferredSize(new Dimension(CELL_SIZE, CELL_SIZE));
                cell.setEnabled(false);
                cell.setOpaque(true);
                cell.setBorder(BorderFactory.createLineBorder(Color.DARK_GRAY, 1));
                if (i == 4) {
                    cell.setText(FACE_NAMES[f]);
                    cell.setFont(new Font("Arial", Font.BOLD, 12));
                }
                simCube[f][i] = cell;
                gridPanel.add(cell);
            }
            
            facePanel.add(gridPanel, BorderLayout.CENTER);
            gbc.gridx = positions[f][0];
            gbc.gridy = positions[f][1];
            cubePanel.add(facePanel, gbc);
        }
        
        // State tracking
        final int[] currentMoveIndex = {-1}; // -1 means showing scramble
        final boolean[] isPlaying = {false};
        final Timer[] animationTimer = {null};
        
        // Precompute all states
        final int[][] allStates = new int[lastSolutionMoves.size() + 1][54];
        FaceCube startFc = new FaceCube();
        startFc.fromString(lastScramble);
        CubieCube current = startFc.toCubieCube();
        
        // State 0: scrambled position
        FaceCube tempFc = current.toFaceletCube();
        for (int i = 0; i < 54; i++) allStates[0][i] = tempFc.getFacelet(i);
        
        // States 1 to N: after each move
        for (int m = 0; m < lastSolutionMoves.size(); m++) {
            current.move(lastSolutionMoves.get(m));
            tempFc = current.toFaceletCube();
            for (int i = 0; i < 54; i++) allStates[m + 1][i] = tempFc.getFacelet(i);
        }
        
        // Info panel
        JLabel moveLabel = new JLabel("Scrambled Position", SwingConstants.CENTER);
        moveLabel.setFont(new Font("Arial", Font.BOLD, 18));
        
        JLabel progressLabel = new JLabel("Move 0 / " + lastSolutionMoves.size(), SwingConstants.CENTER);
        progressLabel.setFont(new Font("Arial", Font.PLAIN, 14));
        
        // Solution display
        StringBuilder solStr = new StringBuilder("Solution: ");
        for (int i = 0; i < lastSolutionMoves.size(); i++) {
            solStr.append(MOVE_NAMES[lastSolutionMoves.get(i)]);
            if (i < lastSolutionMoves.size() - 1) solStr.append(" ");
        }
        JLabel solutionLabel = new JLabel(solStr.toString(), SwingConstants.CENTER);
        solutionLabel.setFont(new Font("Monospaced", Font.PLAIN, 12));
        
        // Update display function
        Runnable updateDisplay = () -> {
            int stateIdx = currentMoveIndex[0] + 1; // state 0 is scramble, state 1 is after move 0, etc.
            int[] facelets = allStates[stateIdx];
            
            for (int f = 0; f < 6; f++) {
                for (int i = 0; i < 9; i++) {
                    int colorIdx = facelets[f * 9 + i];
                    simCube[f][i].setBackground(FACE_COLORS[colorIdx]);
                    simCube[f][i].setForeground(getContrastColor(FACE_COLORS[colorIdx]));
                }
            }
            
            if (currentMoveIndex[0] < 0) {
                moveLabel.setText("Scrambled Position");
                progressLabel.setText("Move 0 / " + lastSolutionMoves.size());
            } else if (currentMoveIndex[0] < lastSolutionMoves.size()) {
                int move = lastSolutionMoves.get(currentMoveIndex[0]);
                moveLabel.setText("After: " + MOVE_NAMES[move]);
                progressLabel.setText("Move " + (currentMoveIndex[0] + 1) + " / " + lastSolutionMoves.size());
            }
            
            if (currentMoveIndex[0] >= lastSolutionMoves.size() - 1) {
                moveLabel.setText("SOLVED!");
                progressLabel.setText("Complete! (" + lastSolutionMoves.size() + " moves)");
            }
        };
        
        // Control buttons
        JButton firstBtn = new JButton("|<");
        JButton prevBtn = new JButton("<");
        JButton playBtn = new JButton("▶ Play");
        JButton stopBtn = new JButton("■ Stop");
        JButton nextBtn = new JButton(">");
        JButton lastBtn = new JButton(">|");
        JButton closeBtn = new JButton("Close");
        
        firstBtn.setToolTipText("Go to start (scrambled)");
        prevBtn.setToolTipText("Previous move");
        playBtn.setToolTipText("Play animation");
        stopBtn.setToolTipText("Stop animation");
        nextBtn.setToolTipText("Next move");
        lastBtn.setToolTipText("Go to solved");
        
        stopBtn.setEnabled(false);
        
        // Button actions
        firstBtn.addActionListener(e -> {
            currentMoveIndex[0] = -1;
            updateDisplay.run();
        });
        
        prevBtn.addActionListener(e -> {
            if (currentMoveIndex[0] > -1) {
                currentMoveIndex[0]--;
                updateDisplay.run();
            }
        });
        
        nextBtn.addActionListener(e -> {
            if (currentMoveIndex[0] < lastSolutionMoves.size() - 1) {
                currentMoveIndex[0]++;
                updateDisplay.run();
            }
        });
        
        lastBtn.addActionListener(e -> {
            currentMoveIndex[0] = lastSolutionMoves.size() - 1;
            updateDisplay.run();
        });
        
        playBtn.addActionListener(e -> {
            if (isPlaying[0]) return;
            isPlaying[0] = true;
            playBtn.setEnabled(false);
            stopBtn.setEnabled(true);
            firstBtn.setEnabled(false);
            prevBtn.setEnabled(false);
            nextBtn.setEnabled(false);
            lastBtn.setEnabled(false);
            
            // Reset to start if at end
            if (currentMoveIndex[0] >= lastSolutionMoves.size() - 1) {
                currentMoveIndex[0] = -1;
                updateDisplay.run();
            }
            
            animationTimer[0] = new Timer(1000, evt -> {
                if (currentMoveIndex[0] < lastSolutionMoves.size() - 1) {
                    currentMoveIndex[0]++;
                    updateDisplay.run();
                } else {
                    // Animation complete
                    ((Timer) evt.getSource()).stop();
                    isPlaying[0] = false;
                    playBtn.setEnabled(true);
                    stopBtn.setEnabled(false);
                    firstBtn.setEnabled(true);
                    prevBtn.setEnabled(true);
                    nextBtn.setEnabled(true);
                    lastBtn.setEnabled(true);
                }
            });
            animationTimer[0].setInitialDelay(500);
            animationTimer[0].start();
        });
        
        stopBtn.addActionListener(e -> {
            if (animationTimer[0] != null) {
                animationTimer[0].stop();
            }
            isPlaying[0] = false;
            playBtn.setEnabled(true);
            stopBtn.setEnabled(false);
            firstBtn.setEnabled(true);
            prevBtn.setEnabled(true);
            nextBtn.setEnabled(true);
            lastBtn.setEnabled(true);
        });
        
        closeBtn.addActionListener(e -> {
            if (animationTimer[0] != null) {
                animationTimer[0].stop();
            }
            simDialog.dispose();
        });
        
        // Layout
        JPanel infoPanel = new JPanel(new GridLayout(3, 1, 5, 5));
        infoPanel.setBorder(new EmptyBorder(10, 10, 5, 10));
        infoPanel.add(moveLabel);
        infoPanel.add(progressLabel);
        infoPanel.add(solutionLabel);
        
        JPanel controlPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
        controlPanel.add(firstBtn);
        controlPanel.add(prevBtn);
        controlPanel.add(playBtn);
        controlPanel.add(stopBtn);
        controlPanel.add(nextBtn);
        controlPanel.add(lastBtn);
        controlPanel.add(Box.createHorizontalStrut(20));
        controlPanel.add(closeBtn);
        
        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.add(infoPanel, BorderLayout.NORTH);
        bottomPanel.add(controlPanel, BorderLayout.SOUTH);
        
        simDialog.add(cubePanel, BorderLayout.CENTER);
        simDialog.add(bottomPanel, BorderLayout.SOUTH);
        
        // Initialize display
        updateDisplay.run();
        
        simDialog.setVisible(true);
    }

    /**
     * Initialize all solver tables with a progress dialog.
     */
    public static void initializeTables(JFrame parent) {
        JDialog dialog = new JDialog(parent, "Initializing", true);
        JProgressBar progressBar = new JProgressBar();
        progressBar.setIndeterminate(true);
        progressBar.setString("Loading solver tables...");
        progressBar.setStringPainted(true);
        
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(new EmptyBorder(20, 20, 20, 20));
        panel.add(new JLabel("Please wait while tables are loading..."), BorderLayout.NORTH);
        panel.add(progressBar, BorderLayout.CENTER);
        
        dialog.add(panel);
        dialog.pack();
        dialog.setLocationRelativeTo(parent);
        dialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);

        SwingWorker<Void, String> worker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() {
                publish("Loading Move Tables...");
                MoveTables.init();
                
                publish("Loading Symmetry Tables...");
                SymmetryTables.init();
                
                publish("Loading Pruning Tables...");
                PruningTables.init();
                
                return null;
            }

            @Override
            protected void process(java.util.List<String> chunks) {
                if (!chunks.isEmpty()) {
                    progressBar.setString(chunks.get(chunks.size() - 1));
                }
            }

            @Override
            protected void done() {
                dialog.dispose();
            }
        };

        worker.execute();
        dialog.setVisible(true);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            // Create the GUI frame first (but don't show yet)
            RubiksCubeGUI gui = new RubiksCubeGUI();
            
            // Initialize tables with progress dialog
            initializeTables(gui);
            
            // Show the main GUI
            gui.setVisible(true);
        });
    }
}

