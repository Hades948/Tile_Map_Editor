package com.tylerroyer.tilemapeditor;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.image.BufferedImage;
import java.awt.Point;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Random;

import com.tylerroyer.molasses.*;
import com.tylerroyer.molasses.events.DecrementIntegerEvent;
import com.tylerroyer.molasses.events.Event;
import com.tylerroyer.molasses.events.IncrementIntegerEvent;
import com.tylerroyer.molasses.events.SetIntegerEvent;

import org.apache.commons.lang3.mutable.MutableInt;

public class EditorScreen extends Screen {
    private final int MODE_MOVE = 0;
    private final int MODE_PAINT = 1;
    private final int MODE_PROPERTIES = 2;
    private MutableInt mode = new MutableInt(MODE_MOVE);

    private boolean showTileInfo;
    private boolean wasMouseDownInViewport = false;
    private double[] zoomLevels = {0.03125, 0.0625, 0.125, 0.25};
    private Point mouseRelativeToMap = new Point();
    private Point hoveredTileLocation = new Point();
    private Point clickDownPoint = new Point();
    private Color backgroundColor = new Color(190, 205, 190);
    private MutableInt zoom = new MutableInt(3);
    private TileMap tileMap;
    private Camera camera = new Camera();
    private BasicStroke buttonOutline = new BasicStroke(2);

    private final int MAP_OFFSET_X = 54, MAP_OFFSET_Y = 18;
    private final int MAP_VIEWPORT_SIZE = 640;
    private final int BOARDER_WIDTH = 2;

    private Event zoomInEvent, zoomOutEvent;
    private Button zoomInButton, zoomOutButton;
    private Button moveButton, paintButton, propertiesButton;

    public EditorScreen() {
        tileMap = new TileMap(100, 100);
    }

    @Override
    public void loadResources() {
        Resources.loadGraphicalImage("grass.png");
        Resources.loadGraphicalImage("water.png");
        for (int i = 0; i < zoomLevels.length; i++) {
            Resources.addGraphicalResource("grass.png_zoom" + (i+1), Resources.scaleImage(Resources.getGraphicalResource("grass.png"), zoomLevels[i], zoomLevels[i]));
            Resources.addGraphicalResource("water.png_zoom" + (i+1), Resources.scaleImage(Resources.getGraphicalResource("water.png"), zoomLevels[i], zoomLevels[i]));
        }

        for (ArrayList<Tile> row : tileMap.getTiles()) {
            for (Tile tile : row) {
                tile.setImageName("grass.png");
            }
        }

        Random rand = new Random();
        for (int i = 0; i < 5; i++) {
            int w = rand.nextInt(20) + 10, h = rand.nextInt(20) + 10;
            int x = rand.nextInt(100 - w), y = rand.nextInt(100 - h);
            
            for (int j = x; j < x + w; j++) {
                for (int k = y; k < y + h; k++) {
                    tileMap.getTiles().get(j).get(k).setImageName("water.png");
                }
            }
        }
        
        Game.getWindow().setBackgroundColor(backgroundColor);

        Font font = new Font("Helvetica", Font.PLAIN, 42);
        zoomInEvent = new IncrementIntegerEvent(zoom, 1, zoomLevels.length);
        zoomOutEvent = new DecrementIntegerEvent(zoom, 1, 1);
        zoomInButton = new Button("Zoom in", font, new Color(128, 128, 128), Color.BLACK, 200, 50, MAP_OFFSET_X + MAP_VIEWPORT_SIZE - 420, MAP_OFFSET_Y + MAP_VIEWPORT_SIZE + 20, zoomInEvent);
        zoomOutButton = new Button("Zoom out", font, new Color(128, 128, 128), Color.BLACK, 200, 50, MAP_OFFSET_X + MAP_VIEWPORT_SIZE - 200, MAP_OFFSET_Y + MAP_VIEWPORT_SIZE + 20, zoomOutEvent);
        BufferedImage moveUnpressed = Resources.loadGraphicalImage("move_button_unpressed.png");
        BufferedImage paintUnpressed = Resources.loadGraphicalImage("paint_button_unpressed.png");
        BufferedImage propertiesUnpressed = Resources.loadGraphicalImage("properties_button_unpressed.png");
        moveButton = new Button(moveUnpressed, moveUnpressed, moveUnpressed, 9, 17, new SetIntegerEvent(mode, MODE_MOVE));
        paintButton = new Button(paintUnpressed, paintUnpressed, paintUnpressed, 9, 64, new SetIntegerEvent(mode, MODE_PAINT));
        propertiesButton = new Button(propertiesUnpressed, propertiesUnpressed, propertiesUnpressed, 9, 111, new SetIntegerEvent(mode, MODE_PROPERTIES));
        zoomInButton.setOutline(buttonOutline);
        zoomOutButton.setOutline(buttonOutline);
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
                g.drawImage(image, i * getTileSize() + MAP_OFFSET_X + camera.getOffsetX(), j * getTileSize() + MAP_OFFSET_Y + camera.getOffsetY(), Game.getWindow());
            }
        }

        // Draw selector
        if (isMouseOverMap() && isMouseInViewport()) {
            g.setColor(new Color(255, 255, 255, 100));
            double scaledTileSize = getTileSize();
            g.fillRect(hoveredTileLocation.getX() * scaledTileSize + MAP_OFFSET_X + camera.getOffsetX(), hoveredTileLocation.getY() * scaledTileSize + MAP_OFFSET_Y + camera.getOffsetY(), scaledTileSize, scaledTileSize);
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
        moveButton.render(g);
        paintButton.render(g);
        propertiesButton.render(g);

        // Draw tile info
        if (isMouseOverMap() && isMouseInViewport() && showTileInfo) {
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

    private boolean isMouseOverMap() {
        return mouseRelativeToMap.x - camera.getOffsetX() > 0
            && mouseRelativeToMap.y - camera.getOffsetY() > 0
            && mouseRelativeToMap.x - camera.getOffsetX() < getTileSize() * tileMap.getTiles().size()
            && mouseRelativeToMap.y - camera.getOffsetY() < getTileSize() * tileMap.getTiles().get(0).size();
    }

    private double getTileSize() {
        return Resources.getResourceSize("grass.png").getWidth() * zoomLevels[zoom.getValue() - 1];
    }
    
    @Override
    public void update() {
        zoomInButton.update();
        zoomOutButton.update();
        moveButton.update();
        paintButton.update();
        propertiesButton.update();

        mouseRelativeToMap.x = (Game.getMouseHandler().getX() - MAP_OFFSET_X);
        mouseRelativeToMap.y = (Game.getMouseHandler().getY() - MAP_OFFSET_Y);
        hoveredTileLocation.x = (int) ((mouseRelativeToMap.x - camera.getOffsetX()) / (int) getTileSize());
        hoveredTileLocation.y = (int) ((mouseRelativeToMap.y - camera.getOffsetY()) / (int) getTileSize());

        boolean isMouseDown = Game.getMouseHandler().isDown();
        showTileInfo = !isMouseDown;
        if (isMouseDown) {
            if (isMouseInViewport()) {
                switch (mode.getValue()) {
                default:
                case MODE_MOVE:
                    Game.getWindow().setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
                    if (!wasMouseDownInViewport) {
                        // Just clicked down.
                        clickDownPoint.x = (int) (Game.getMouseHandler().getX() - camera.getOffsetX());
                        clickDownPoint.y = (int) (Game.getMouseHandler().getY() - camera.getOffsetY());
                    } else {
                        Point mouseNow = new Point(Game.getMouseHandler().getX(), Game.getMouseHandler().getY());
                    
                        camera.setOffsetX(mouseNow.getX() - clickDownPoint.getX());
                        camera.setOffsetY(mouseNow.getY() - clickDownPoint.getY());
                    }
                    break;
                case MODE_PAINT:
                    break;
                case MODE_PROPERTIES:
                    break;
                }

                wasMouseDownInViewport = true;
            }
        } else {
            Game.getWindow().setCursor(Cursor.getDefaultCursor());
            wasMouseDownInViewport = false;
        }

        moveButton.setOutline((mode.getValue() == MODE_MOVE) ? buttonOutline : null);
        paintButton.setOutline((mode.getValue() == MODE_PAINT) ? buttonOutline : null);
        propertiesButton.setOutline((mode.getValue() == MODE_PROPERTIES) ? buttonOutline : null);
    }
}