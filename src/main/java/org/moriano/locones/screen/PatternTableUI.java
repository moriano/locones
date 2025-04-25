package org.moriano.locones.screen;


import org.moriano.locones.memory.PatternTables;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.concurrent.Flow;

/**
 * This is just a window to display the pattern tables.
 *
 * There are two pattern tables: Left and Right.
 *
 * Each one is composed of 16x16 tiles.
 *
 * Each tile is 8x8 pixels
 */
public class PatternTableUI extends JFrame  {
    private static final Logger log = LoggerFactory.getLogger(PatternTableUI.class);


    private JPanel leftPanel = new JPanel();
    private JPanel rightPanel = new JPanel();
    private Tile[] leftTiles;
    private Tile[] rightTiles;

    public PatternTableUI(PatternTables patternTables)  {
        super();
        this.leftTiles = patternTables.getLeftPatternTableTiles();
        this.rightTiles = patternTables.getRightPatternTableTiles();
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.leftPanel.setLayout(new GridLayout(16, 16));

        this.rightPanel.setLayout(new GridLayout(16, 16));
        this.setTitle("Pattern tables");


        this.drawTiles();
        this.setSize(800, 600);
        this.pack();
        this.setVisible(true);
        int a = 1;

    }

    private void drawTiles() {
        JPanel mainPanel = new JPanel(new GridLayout(1, 2, 10, 0));
        int tileIdx = 0;
        for (int rowIdx = 0; rowIdx < 16; rowIdx++) {
            String toLog = "";
            for(int colIdx =0; colIdx<16; colIdx++) {
                Tile leftTile = this.leftTiles[tileIdx];
                ImageIcon leftImage = this.drawTile(leftTile);
                this.leftPanel.add(new JLabel(leftImage));

                Tile rightTile = this.rightTiles[tileIdx];
                ImageIcon rightImage = this.drawTile(rightTile);
                this.rightPanel.add(new JLabel(rightImage));
                tileIdx++;
            }
            log.info(toLog);

        }
        mainPanel.add(this.leftPanel);
        mainPanel.add(this.rightPanel);

        this.add(mainPanel);
    }

    private ImageIcon drawTile(Tile tile) {
        BufferedImage tileImage = new BufferedImage(8, 8, BufferedImage.TYPE_INT_RGB);
        int[][] tileBytes = tile.getTileData();
        for (int rowIdx = 0; rowIdx<8; rowIdx++) {
            for (int colIdx = 0; colIdx <8; colIdx++) {
                int tileByte = tileBytes[rowIdx][colIdx];
                int colorRGB = Color.TRANSLUCENT;
                if (tileByte == 1) {
                    colorRGB = Color.RED.getRGB();
                } else if (tileByte == 2) {
                    colorRGB = Color.YELLOW.getRGB();
                } else if (tileByte == 3) {
                    colorRGB = Color.WHITE.getRGB();
                }
                tileImage.setRGB(colIdx, rowIdx, colorRGB);
            }
        }
        ImageIcon imageIcon = new ImageIcon(tileImage);
        Image scaledImage = imageIcon.getImage().getScaledInstance(32, 32, Image.SCALE_FAST);
        ImageIcon scaledIcon = new ImageIcon(scaledImage);

        return scaledIcon;

    }


}
