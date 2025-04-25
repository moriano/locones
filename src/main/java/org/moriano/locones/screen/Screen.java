package org.moriano.locones.screen;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.Random;

public class Screen extends JFrame {

    private static final Logger log = LoggerFactory.getLogger(Screen.class);
    private final BufferedImage frame = new BufferedImage(256, 224, BufferedImage.TYPE_INT_RGB);
    private final JLabel label = new JLabel(new ImageIcon(frame));
    private final Random random = new Random();

    public Screen() {
        this.setTitle("LocoNes");
        this.setSize(frame.getWidth() + 10, frame.getHeight() + 10);
        this.add(label);
        this.pack();
        this.setVisible(true);
    }

    public void drawPixel(int x, int y, int value) {
        this.frame.setRGB(x, y, value);
    }

    public void resetFrame() {
        this.frame.flush();
    }

    public void generateRandomFrame() {
        for (int x = 0; x < this.frame.getWidth(); x++) {
            for (int y = 0; y < this.frame.getHeight(); y++) {
                this.frame.setRGB(x, y, random.nextInt());
            }
        }
        this.label.setIcon(new ImageIcon(this.frame));
    }

    public void showSystemPalette() {
        /*
        We will show blocks of 10x10 pixels, in 4 rows each. Each row has 0x1F columns
         */
        JFrame paletteFrame = new JFrame("Palette gui");
        BufferedImage paletteImage = new BufferedImage(16 *100, 4*100, BufferedImage.TYPE_INT_RGB);
        JLabel paletteLabel = new JLabel(new ImageIcon(paletteImage));

        paletteFrame.setTitle("Palete gui");
        paletteFrame.setSize(16 *100, 0x4*100);
        paletteFrame.add(paletteLabel);
        paletteFrame.pack();
        paletteFrame.setVisible(true);

        int[] paleteColorsRGB = SystemPalette.INSTANCE.getColorsAsRGBInts();

        int x = 0;
        int y = 0;

        for (int colorIdx = 0; colorIdx < paleteColorsRGB.length; colorIdx++) {
            int color = paleteColorsRGB[colorIdx]; //new Color(colorRaw[0], colorRaw[1], colorRaw[2]).getRGB();
            for (int drawX = x; drawX<x+100; drawX++) {
                for (int drawY = y; drawY<y+100; drawY++) {
                        paletteImage.setRGB(drawX, drawY, color);

                }
            }
            x = x+100;
            if (x == 1600) {
                y = y+100;
                x = 0;
            }

        }
        paletteLabel.setIcon(new ImageIcon(paletteImage));
        paletteFrame.setLocation(400, 0); // This is so that it does not overlap the actual emulation image

    }


}
