package cube.gui;

import javax.swing.*;
import java.awt.*;

/**
 * Represents a single cell (facelet) on the Rubik's Cube GUI.
 */
public class CubeCell {
    private Color color = Color.WHITE;
    private JButton button;

    public void setColor(Color color) {
        this.color = color;
    }

    public Color getColor() {
        return color;
    }

    public void setButton(JButton button) {
        this.button = button;
    }

    public JButton getButton() {
        return button;
    }
}

