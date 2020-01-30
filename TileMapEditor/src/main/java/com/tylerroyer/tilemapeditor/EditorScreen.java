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
    private Rectangle paintSelection = null;
    private ArrayList<Point> selectedTileLocations = new ArrayList<>();

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

        switch(mode.getValue()) {
            default:
            case MODE_MOVE:
                if (isMouseOverMap() && isMouseInViewport()) {
                    // Draw selector
                    g.setColor(new Color(255, 255, 255, 100));
                    g.fillRect(hoveredTileLocation.getX() * getTileSize() + MAP_OFFSET_X + camera.getOffsetX(), hoveredTileLocation.getY() * getTileSize() + MAP_OFFSET_Y + camera.getOffsetY(), getTileSize(), getTileSize());
                }
                break;
            case MODE_PAINT:
                //if (isMouseInViewport()) {
                    // Draw paint selector
                    if (paintSelection != null) {
                        g.setColor(Color.RED);
                        g.drawRect(paintSelection.getX() + MAP_OFFSET_X + camera.getOffsetX(), paintSelection.getY() + MAP_OFFSET_Y + camera.getOffsetY(), paintSelection.getWidth(), paintSelection.getHeight());// TODO I'd like to just pass this as a rectangle, if possible.
                    }
    
                    // Draw highlight for selected tiles
                    g.setColor(new Color(255, 255, 255, 100));
                    for (Point p : selectedTileLocations) {
                        g.fillRect(p.getX() * getTileSize() + MAP_OFFSET_X + camera.getOffsetX(), p.getY() * getTileSize() + MAP_OFFSET_Y + camera.getOffsetY(), getTileSize(), getTileSize());
                    }
                //}
                break;
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
        if (isMouseOverMap() && isMouseInViewport() && showTileInfo && mode.getValue() == MODE_MOVE) {
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
        // Buttons
        zoomInButton.update();
        zoomOutButton.update();
        moveButton.update();
        paintButton.update();
        propertiesButton.update();
        moveButton.setOutline((mode.getValue() == MODE_MOVE) ? buttonOutline : null);
        paintButton.setOutline((mode.getValue() == MODE_PAINT) ? buttonOutline : null);
        propertiesButton.setOutline((mode.getValue() == MODE_PROPERTIES) ? buttonOutline : null);

        mouseRelativeToMap.x = (Game.getMouseHandler().getX() - MAP_OFFSET_X);
        mouseRelativeToMap.y = (Game.getMouseHandler().getY() - MAP_OFFSET_Y);
        hoveredTileLocation.x = (int) ((mouseRelativeToMap.x - camera.getOffsetX()) / (int) getTileSize());
        hoveredTileLocation.y = (int) ((mouseRelativeToMap.y - camera.getOffsetY()) / (int) getTileSize());

        // TODO Replace switch statement with polymorphism.
        switch (mode.getValue()) {
            default:
            case MODE_MOVE:
                boolean isMouseDown = Game.getMouseHandler().isDown();
                showTileInfo = !isMouseDown;
                if (isMouseDown) {
                    if (isMouseInViewport()) {
                            if (!wasMouseDownInViewport) {
                                // Just clicked down.
                                clickDownPoint.x = (int) (Game.getMouseHandler().getX() - camera.getOffsetX());
                                clickDownPoint.y = (int) (Game.getMouseHandler().getY() - camera.getOffsetY());
                            } else {
                                Point mouseNow = new Point(Game.getMouseHandler().getX(), Game.getMouseHandler().getY());
                                Game.getWindow().setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
                                camera.setOffsetX(mouseNow.getX() - clickDownPoint.getX());
                                camera.setOffsetY(mouseNow.getY() - clickDownPoint.getY());
                            }
                        wasMouseDownInViewport = true;
                    }
                } else {
                    Game.getWindow().setCursor(Cursor.getDefaultCursor());
                    wasMouseDownInViewport = false;
                }
                break;
            case MODE_PAINT:
                isMouseDown = Game.getMouseHandler().isDown();
                    showTileInfo = !isMouseDown;
                    if (isMouseDown) {
                        if (isMouseInViewport()) {
                                if (!wasMouseDownInViewport) {
                                    // Just clicked down.
                                    selectedTileLocations.clear();
                                    clickDownPoint.x = (int) (Game.getMouseHandler().getX() - camera.getOffsetX());
                                    clickDownPoint.y = (int) (Game.getMouseHandler().getY() - camera.getOffsetY());
                                } else {
                                    Point mouseNow = new Point((int) (Game.getMouseHandler().getX() - camera.getOffsetX()), 
                                        (int) (Game.getMouseHandler().getY() - camera.getOffsetY()));
                                    int x = (int) Math.min(clickDownPoint.getX(), mouseNow.getX()) - MAP_OFFSET_X;
                                    int y = (int) Math.min(clickDownPoint.getY(), mouseNow.getY()) - MAP_OFFSET_Y;
                                    int width = (int) Math.abs(mouseNow.getX() - clickDownPoint.getX());
                                    int height = (int) Math.abs(mouseNow.getY() - clickDownPoint.getY());
                                    paintSelection = new Rectangle(x, y, width, height);
                                }
                            wasMouseDownInViewport = true;
                        }
                    } else {
                        if (wasMouseDownInViewport) {
                            // Just clicked up
                            double x1 = (paintSelection.getX()) / getTileSize();
                            double y1 = (paintSelection.getY()) / getTileSize();
                            double x2 = (paintSelection.getX() + paintSelection.getWidth()) / getTileSize();
                            double y2 = (paintSelection.getY() + paintSelection.getHeight()) / getTileSize();

                            for (int i = (int) x1; i <= (int) x2; i++) {
                                for (int j = (int) y1; j <= (int) y2; j++) {
                                    if (i < 0 || j < 0)
                                        continue;
                                    if (i >= tileMap.getTiles().size() || j >= tileMap.getTiles().get(0).size())
                                        continue;

                                    selectedTileLocations.add(new Point(i, j));
                                }
                            }

                            paintSelection = null;
                        }
                        wasMouseDownInViewport = false;
                    }
                break;
        }
        
    }
}