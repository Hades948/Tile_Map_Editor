package com.tylerroyer.tilemapeditor;

import java.awt.Color;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.Font;
import java.awt.image.BufferedImage;
import java.awt.Rectangle;
import java.util.ArrayList;

import com.tylerroyer.molasses.*;

public class EditorScreen extends Screen implements KeyListener{
    private TileMap tileMap;
    private int zoom = 2;
    private final int MAP_OFFSET_X = 800, MAP_OFFSET_Y = 88;
    private final int MAP_VIEWPORT_SIZE = 512;

    public EditorScreen() {
        tileMap = new TileMap(100, 100);
    }

    @Override
    public void onButtonClick(Button button) {
    }

    @Override
    public void loadResources() {
        Resources.loadGraphicalImage("grass.png");
        Resources.addGraphicalResource("grass.png_zoom1", Resources.scaleImage(Resources.getGraphicalResource("grass.png"), 0.125, 0.125));
        Resources.addGraphicalResource("grass.png_zoom2", Resources.scaleImage(Resources.getGraphicalResource("grass.png"), 0.25, 0.25));
        Resources.addGraphicalResource("grass.png_zoom3", Resources.scaleImage(Resources.getGraphicalResource("grass.png"), 0.5, 0.5));
        Resources.addGraphicalResource("grass.png_zoom4", Resources.getGraphicalResource("grass.png"));
        Resources.addGraphicalResource("grass.png_zoom5", Resources.scaleImage(Resources.getGraphicalResource("grass.png"), 2.0, 2.0));

        for (ArrayList<Tile> row : tileMap.getTiles()) {
            for (Tile tile : row) {
                tile.setImageName("grass.png");
            }
        }
    }

    @Override
    public void render(GameGraphics g) {
        Tile t;
        g.setClip(new Rectangle(MAP_OFFSET_X, MAP_OFFSET_Y, MAP_VIEWPORT_SIZE, MAP_VIEWPORT_SIZE));
        for (int i = 0; i < tileMap.getTiles().size(); i++) {
            for (int j = 0; j < tileMap.getTiles().get(i).size(); j++) {
                t = tileMap.getTiles().get(i).get(j);
                BufferedImage image = Resources.getGraphicalResource(t.getImageName() + "_zoom" + zoom);
                int tileSize = image.getWidth();
                g.drawImage(image, (int) (i * tileSize * (1 / Resources.scaleX) + MAP_OFFSET_X),
                        (int) (j * tileSize * (1 / Resources.scaleY) + MAP_OFFSET_Y), Game.getWindow());
            }
        }
        Rectangle windowBounds = Game.getWindow().getBounds();
        g.setClip(new Rectangle(0, 0, (int) windowBounds.getWidth(), (int) windowBounds.getHeight()));

        g.setColor(Color.WHITE);
        g.setFont(new Font("Helvetica", Font.PLAIN, 42));
        g.drawString("Zoom: " + zoom, MAP_OFFSET_X, MAP_OFFSET_Y - 20);
    }

    boolean first = true;   // FIXME Ideally, I would fix KeyboardHandler and wouldn't need any of this :/
    @Override
    public void update() {
        if (first) {
            Game.getWindow().addKeyListener(this);
            first = false;
        }
    }

    @Override
    public void keyTyped(KeyEvent e) {}

    @Override
    public void keyPressed(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_LEFT) {
            zoom--;
            if (zoom < 1) zoom = 1;
        } else if (e.getKeyCode() == KeyEvent.VK_RIGHT) {
            zoom++;
            if (zoom > 5) zoom = 5;
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {}
}