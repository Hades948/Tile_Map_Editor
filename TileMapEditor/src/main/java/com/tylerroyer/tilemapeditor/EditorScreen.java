package com.tylerroyer.tilemapeditor;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.image.BufferedImage;
import java.awt.Point;
import java.awt.Rectangle;
import java.util.ArrayList;

import com.tylerroyer.molasses.*;
import com.tylerroyer.molasses.events.DecrementIntegerEvent;
import com.tylerroyer.molasses.events.Event;
import com.tylerroyer.molasses.events.IncrementIntegerEvent;

import org.apache.commons.lang3.mutable.MutableInt;

public class EditorScreen extends Screen {
    private TileMap tileMap;
    private MutableInt zoom = new MutableInt(2);
    private double[] zoomLevels = {0.125, 0.25, 0.5, 1.0, 2.0};
    private Point mouseRelativeToMap = new Point();
    private Point hoveredTileLocation = new Point();
    private Color backgroundColor = new Color(190, 205, 190);

    private final int MAP_OFFSET_X = 18, MAP_OFFSET_Y = 18;
    private final int MAP_VIEWPORT_SIZE = 640;
    private final int BOARDER_WIDTH = 2;

    private Event zoomInEvent, zoomOutEvent;
    private Button zoomInButton, zoomOutButton;

    public EditorScreen() {
        tileMap = new TileMap(100, 100);
    }

    @Override
    public void loadResources() {
        Resources.loadGraphicalImage("grass.png");
        Resources.addGraphicalResource("grass.png_zoom1", Resources.scaleImage(Resources.getGraphicalResource("grass.png"), zoomLevels[0], zoomLevels[0]));
        Resources.addGraphicalResource("grass.png_zoom2", Resources.scaleImage(Resources.getGraphicalResource("grass.png"), zoomLevels[1], zoomLevels[1]));
        Resources.addGraphicalResource("grass.png_zoom3", Resources.scaleImage(Resources.getGraphicalResource("grass.png"), zoomLevels[2], zoomLevels[2]));
        Resources.addGraphicalResource("grass.png_zoom4", Resources.scaleImage(Resources.getGraphicalResource("grass.png"), zoomLevels[3], zoomLevels[3]));
        Resources.addGraphicalResource("grass.png_zoom5", Resources.scaleImage(Resources.getGraphicalResource("grass.png"), zoomLevels[4], zoomLevels[4]));

        for (ArrayList<Tile> row : tileMap.getTiles()) {
            for (Tile tile : row) {
                tile.setImageName("grass.png");
            }
        }
        
        Game.getWindow().setBackgroundColor(backgroundColor);

        Font font = new Font("Helvetica", Font.PLAIN, 42);
        zoomInEvent = new IncrementIntegerEvent(zoom, 1, 5);
        zoomOutEvent = new DecrementIntegerEvent(zoom, 1, 1);
        zoomInButton = new Button("Zoom in", font, new Color(128, 128, 128), Color.BLACK, 200, 50, MAP_OFFSET_X + MAP_VIEWPORT_SIZE - 420, MAP_OFFSET_Y + MAP_VIEWPORT_SIZE + 20, zoomInEvent);
        zoomOutButton = new Button("Zoom out", font, new Color(128, 128, 128), Color.BLACK, 200, 50, MAP_OFFSET_X + MAP_VIEWPORT_SIZE - 200, MAP_OFFSET_Y + MAP_VIEWPORT_SIZE + 20, zoomOutEvent);
    }

    @Override
    public void render(GameGraphics g) {
        Tile t;
        Rectangle viewportBounds = new Rectangle(MAP_OFFSET_X, MAP_OFFSET_Y, MAP_VIEWPORT_SIZE, MAP_VIEWPORT_SIZE);
        g.setClip(viewportBounds);
        for (int i = 0; i < tileMap.getTiles().size(); i++) {
            for (int j = 0; j < tileMap.getTiles().get(i).size(); j++) {
                t = tileMap.getTiles().get(i).get(j);
                BufferedImage image = Resources.getGraphicalResource(t.getImageName() + "_zoom" + zoom);
                g.drawImage(image, i * getTileSize() + MAP_OFFSET_X, j * getTileSize() + MAP_OFFSET_Y, Game.getWindow());
            }
        }

        // Draw selector
        if (isMouseInViewport()) {
            g.setColor(new Color(255, 255, 255, 100));
            double scaledTileSize = getTileSize();
            g.fillRect(hoveredTileLocation.getX() * scaledTileSize + MAP_OFFSET_X, hoveredTileLocation.getY() * scaledTileSize + MAP_OFFSET_Y, scaledTileSize, scaledTileSize);
        }

        g.clearClip();

        // Draw square around viewport
        g.setStroke(new BasicStroke(BOARDER_WIDTH));
        g.setColor(Color.BLACK);
        g.drawRect((int) viewportBounds.getX() - BOARDER_WIDTH/2, (int) viewportBounds.getY() - BOARDER_WIDTH/2, (int) viewportBounds.getWidth() + BOARDER_WIDTH, (int) viewportBounds.getHeight() + BOARDER_WIDTH);

        // Draw zoom level
        g.setFont(new Font("Helvetica", Font.PLAIN, 42));
        g.setColor(Color.BLACK);
        g.drawString("Zoom: " + zoom.getValue(), MAP_OFFSET_X + 20, MAP_OFFSET_Y + MAP_VIEWPORT_SIZE + 60);

        // Draw buttons
        zoomInButton.render(g);
        zoomOutButton.render(g);

        // Draw tile info
        if (isMouseInViewport()) {
            int infoX = Game.getMouseHandler().getX() + 20, infoY = Game.getMouseHandler().getY() + 20;
            if (mouseRelativeToMap.x > 444) {
                infoX = Game.getMouseHandler().getX() - 20 - 192;
            }

            if (mouseRelativeToMap.y > 515) {
                infoY = Game.getMouseHandler().getY() - 20 - 192;
            }

            g.setFont(new Font("Helvetica", Font.PLAIN, 16));

            g.setColor(backgroundColor);
            g.fillRect(infoX, infoY, 192, 192);

            g.setStroke(new BasicStroke(BOARDER_WIDTH));
            g.setColor(Color.BLACK);
            g.drawRect(infoX, infoY, 192, 192);

            t = tileMap.getTiles().get(hoveredTileLocation.x).get(hoveredTileLocation.y);
            BufferedImage image = Resources.getGraphicalResource(t.getImageName());
            g.drawImage(image, infoX + 32, infoY + 8);
            g.drawString(t.getImageName(), infoX + 25, infoY + 155);
            g.drawString("(" + hoveredTileLocation.x + ", " + hoveredTileLocation.y + ")", infoX + 25, infoY + 180);
        }
    }

    private boolean isMouseInViewport() {
        return mouseRelativeToMap.x > 0 && mouseRelativeToMap.y > 0 && mouseRelativeToMap.x < MAP_VIEWPORT_SIZE && mouseRelativeToMap.y < MAP_VIEWPORT_SIZE;
    }

    private double getTileSize() {
        return Resources.getResourceSize("grass.png").getWidth() * zoomLevels[zoom.getValue() - 1];
    }
    
    @Override
    public void update() {
        zoomInButton.update();
        zoomOutButton.update();

        mouseRelativeToMap.x = (Game.getMouseHandler().getX() - MAP_OFFSET_X);
        mouseRelativeToMap.y = (Game.getMouseHandler().getY() - MAP_OFFSET_Y);
        hoveredTileLocation.x = mouseRelativeToMap.x / (int) getTileSize();
        hoveredTileLocation.y = mouseRelativeToMap.y / (int) getTileSize();
    }
}