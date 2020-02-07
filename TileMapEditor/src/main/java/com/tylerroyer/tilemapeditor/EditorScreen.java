package com.tylerroyer.tilemapeditor;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.image.BufferedImage;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.awt.Point;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map.Entry;

import javax.imageio.ImageIO;

import com.tylerroyer.molasses.*;
import com.tylerroyer.molasses.events.*;

import org.apache.commons.lang3.mutable.MutableBoolean;
import org.apache.commons.lang3.mutable.MutableDouble;
import org.apache.commons.lang3.mutable.MutableInt;

public class EditorScreen extends Screen {
    private final int MODE_MOVE = 0;
    private final int MODE_PAINT = 1;
    private final int MODE_PROPERTIES = 2;
    private MutableInt mode = new MutableInt(MODE_MOVE);

    private boolean wasMouseDownInViewport = false;
    private Point mouseRelativeToMap = new Point();
    private Point hoveredTileLocation = new Point();
    private Point clickDownPoint = new Point();
    private Color backgroundColor = new Color(190, 205, 190);
    private StringBuffer paintTileName = new StringBuffer();
    private MutableInt tilePage = new MutableInt(0);
    private MutableDouble cameraOffsetX = new MutableDouble(0.0);
    private MutableDouble cameraOffsetY = new MutableDouble(0.0);
    private MutableInt zoomIndex = new MutableInt(3);
    private MutableBoolean readyToSave = new MutableBoolean(false);
    private MutableBoolean readyToGenerateMap = new MutableBoolean(false);
    private ArrayList<Button> paintTileButtons = new ArrayList<>();
    private TileMap tileMap;
    private Camera camera = new Camera(cameraOffsetX, cameraOffsetY);
    private BasicStroke buttonOutline = new BasicStroke(2);
    private Rectangle paintSelection = null;
    private ArrayList<Point> selectedTileLocations = new ArrayList<>();

    private final int MAP_OFFSET_X = 54, MAP_OFFSET_Y = 18;
    private final int MAP_VIEWPORT_SIZE = 640;
    private final int BOARDER_WIDTH = 2;

    private Event zoomInEvent, zoomOutEvent;
    private Button zoomInButton, zoomOutButton;
    private Button moveButton, paintButton, propertiesButton;
    private Button saveButton, mapButton;
    private Button prevButton, nextButton;

    public EditorScreen() {}

    @Override
    public void onFocus() {
        tileMap = new TileMap(MAP_OFFSET_X, MAP_OFFSET_Y, "map.mtm");
        HashMap<String, FlipBook> tileMappings = tileMap.getTileMappings();

        int i = 0;
        for (Entry<String, FlipBook> tileMapping : tileMappings.entrySet()) {
            int x;
            int y;
            if (i % 2 == 0) {
                x = MAP_OFFSET_X + MAP_VIEWPORT_SIZE + 18;
                y = MAP_OFFSET_Y + (i%8) / 2 * 140;
            } else {
                x = MAP_OFFSET_X + MAP_VIEWPORT_SIZE + 128 + 36;
                y = MAP_OFFSET_Y + (i%8) / 2 * 140;
            }
            Button b = new Button(new FlipBook(tileMapping.getValue()), x, y, new SetStringEvent(paintTileName, tileMapping.getKey()));
            b.setOutline(buttonOutline);
            paintTileButtons.add(b);

            i++;
        }
        
        Game.getWindow().setBackgroundColor(backgroundColor);

        ArrayList<Double> zoomLevels = new ArrayList<>();
        zoomLevels.add(0.03125);
        zoomLevels.add(0.0625);
        zoomLevels.add(0.125);
        zoomLevels.add(0.25);
        zoomLevels.add(0.5);
        zoomLevels.add(1.0);
        zoomLevels.add(2.0);

        zoomOutEvent = new DecrementIntegerEvent(zoomIndex, 1, 0);
        zoomInEvent = new IncrementIntegerEvent(zoomIndex, 1, zoomLevels.size()-1);
        Event prevEvent = new DecrementIntegerEvent(tilePage, 1, 0);
        Event nextEvent = new IncrementIntegerEvent(tilePage, 1, (paintTileButtons.size()-1) / 8);
        zoomInButton = new Button("Zoom in", Config.gameFont, new Color(128, 128, 128), Color.BLACK, 200, 50, MAP_OFFSET_X + MAP_VIEWPORT_SIZE - 420, MAP_OFFSET_Y + MAP_VIEWPORT_SIZE + 20, zoomInEvent);
        zoomInButton.addEvent(new MultiplyDoubleEvent(cameraOffsetX, 2.0));
        zoomInButton.addEvent(new MultiplyDoubleEvent(cameraOffsetY, 2.0));
        zoomOutButton = new Button("Zoom out", Config.gameFont, new Color(128, 128, 128), Color.BLACK, 200, 50, MAP_OFFSET_X + MAP_VIEWPORT_SIZE - 200, MAP_OFFSET_Y + MAP_VIEWPORT_SIZE + 20, zoomOutEvent);
        zoomOutButton.addEvent(new MultiplyDoubleEvent(cameraOffsetX, 0.5));
        zoomOutButton.addEvent(new MultiplyDoubleEvent(cameraOffsetY, 0.5));
        prevButton = new Button("Prev", Config.gameFont, new Color(128, 128, 128), Color.BLACK, 110, 50, MAP_OFFSET_X + MAP_VIEWPORT_SIZE + 35, MAP_OFFSET_Y + MAP_VIEWPORT_SIZE - 65, prevEvent);
        nextButton = new Button("Next", Config.gameFont, new Color(128, 128, 128), Color.BLACK, 110, 50, MAP_OFFSET_X + MAP_VIEWPORT_SIZE + 164, MAP_OFFSET_Y + MAP_VIEWPORT_SIZE - 65, nextEvent);
        FlipBook moveUnpressed = new FlipBook(0, new Page("move_button_unpressed.png"));
        FlipBook paintUnpressed = new FlipBook(0, new Page("paint_button_unpressed.png"));
        FlipBook propertiesUnpressed = new FlipBook(0, new Page("properties_button_unpressed.png"));
        FlipBook saveUnpressed = new FlipBook(0, new Page("save_button_unpressed.png"));
        FlipBook mapUnpressed = new FlipBook(0, new Page("map_button_unpressed.png"));
        moveButton = new Button(moveUnpressed, 9, 17, new SetIntegerEvent(mode, MODE_MOVE));
        paintButton = new Button(paintUnpressed, 9, 64, new SetIntegerEvent(mode, MODE_PAINT));
        propertiesButton = new Button(propertiesUnpressed, 9, 111, new SetIntegerEvent(mode, MODE_PROPERTIES));
        saveButton = new Button(saveUnpressed, 9, MAP_OFFSET_Y + MAP_VIEWPORT_SIZE - 81, new ToggleOnEvent(readyToSave));
        mapButton = new Button(mapUnpressed, 9, MAP_OFFSET_Y + MAP_VIEWPORT_SIZE - 36, new ToggleOnEvent(readyToGenerateMap));
        saveButton.setOutline(buttonOutline);
        mapButton.setOutline(buttonOutline);
        zoomInButton.setOutline(buttonOutline);
        zoomOutButton.setOutline(buttonOutline);
        prevButton.setOutline(buttonOutline);
        nextButton.setOutline(buttonOutline);

        tileMap.prepareTileMapForScaling(zoomIndex, 5, zoomLevels);
    }

    @Override
    public void render(GameGraphics g) {
        Rectangle viewportBounds = new Rectangle(MAP_OFFSET_X, MAP_OFFSET_Y, MAP_VIEWPORT_SIZE, MAP_VIEWPORT_SIZE);
        g.setClip(viewportBounds);
        g.setCamera(camera);

        tileMap.render(g);

        double tileSize = tileMap.getTileSize();
        switch(mode.getValue()) {
            default:
            case MODE_MOVE:
                if (isMouseOverMap() && isMouseInViewport()) {
                    // Draw selector
                    g.setColor(new Color(255, 255, 255, 100));
                    g.fillRect(hoveredTileLocation.getX() * tileSize + MAP_OFFSET_X, hoveredTileLocation.getY() * tileSize + MAP_OFFSET_Y, tileSize, tileSize);
                }
                break;
            case MODE_PAINT:
                // Draw paint selector
                if (paintSelection != null) {
                    g.setColor(Color.RED);
                    g.drawRect(paintSelection.getX() + MAP_OFFSET_X, paintSelection.getY() + MAP_OFFSET_Y, paintSelection.getWidth(), paintSelection.getHeight());// TODO I'd like to just pass this as a rectangle, if possible.
                }

                // Draw highlight for selected tiles
                g.setColor(new Color(255, 255, 255, 100));
                for (Point p : selectedTileLocations) {
                    g.fillRect(p.getX() * tileSize + MAP_OFFSET_X, p.getY() * tileSize + MAP_OFFSET_Y, tileSize, tileSize);
                }
                break;
        }

        g.clearClip();
        g.setCamera(null);

        // Draw square around viewport
        g.setStroke(new BasicStroke(BOARDER_WIDTH));
        g.setColor(Color.BLACK);
        g.drawRect((int) viewportBounds.getX() - BOARDER_WIDTH/2, (int) viewportBounds.getY() - BOARDER_WIDTH/2, (int) viewportBounds.getWidth() + BOARDER_WIDTH, (int) viewportBounds.getHeight() + BOARDER_WIDTH);

        // Draw zoom level
        g.setFont(Config.gameFont);
        g.setColor(Color.BLACK);
        String zoomString = String.format("Zoom: %.4f", tileMap.getZoomLevel());
        g.drawString(zoomString, MAP_OFFSET_X - 45, MAP_OFFSET_Y + MAP_VIEWPORT_SIZE + 60);

        // Draw buttons
        zoomInButton.render(g);
        zoomOutButton.render(g);
        moveButton.render(g);
        paintButton.render(g);
        propertiesButton.render(g);
        saveButton.render(g);
        mapButton.render(g);
        if (mode.getValue() == MODE_PAINT) {
            prevButton.render(g);
            nextButton.render(g);
            for (int i = tilePage.getValue() * 8; i < tilePage.getValue() * 8 + 8 && i < paintTileButtons.size(); i++) {
                Button b = paintTileButtons.get(i);
                b.render(g);
            }
        }

        // Draw tile info
        if (isMouseOverMap() && isMouseInViewport() && mode.getValue() == MODE_MOVE) {
            try {
                int infoX = MAP_OFFSET_X + MAP_VIEWPORT_SIZE + 18, infoY = MAP_OFFSET_Y;

                g.setFont(Config.gameFont.deriveFont(16.0f));

                Tile t = tileMap.getTile(hoveredTileLocation.x, hoveredTileLocation.y);
                String flipBookName = t.getFlipBookName();
                g.drawPage(tileMap.getTileMappingsWithDefaultZoom().get(flipBookName).getCurrentPage(), infoX, infoY);
                g.drawString(flipBookName, infoX, infoY + 147);
                g.drawString("(" + hoveredTileLocation.x + ", " + hoveredTileLocation.y + ")", infoX, infoY + 172);
            } catch (IndexOutOfBoundsException e) {
                System.err.println("Trying to display info about an out-of-bounds tile!");
            }
        } else if (mode.getValue() != MODE_PAINT) {
            g.setFont(Config.gameFont.deriveFont(18.0f));
            g.drawString("Hover over a tile to see tile info.", MAP_OFFSET_X + MAP_VIEWPORT_SIZE + 18, MAP_OFFSET_Y + 20);
            g.drawString("Click and drag to move the map.", MAP_OFFSET_X + MAP_VIEWPORT_SIZE + 18, MAP_OFFSET_Y + 50);
        }
    }

    private boolean isMouseInViewport() {
        return mouseRelativeToMap.x > 0 && mouseRelativeToMap.y > 0 && mouseRelativeToMap.x < MAP_VIEWPORT_SIZE && mouseRelativeToMap.y < MAP_VIEWPORT_SIZE;
    }

    private boolean isMouseOverMap() {
        return mouseRelativeToMap.x - camera.getOffsetX() > 0
            && mouseRelativeToMap.y - camera.getOffsetY() > 0
            && mouseRelativeToMap.x - camera.getOffsetX() < tileMap.getTileSize() * tileMap.getWidth()
            && mouseRelativeToMap.y - camera.getOffsetY() < tileMap.getTileSize() * tileMap.getHeight();
    }
    
    @Override
    public void update() {
        tileMap.update();

        // Buttons
        if (tileMap.getZoomLevelIndex() < tileMap.getNumberOfZoomLevels()-1)
            zoomInButton.update();
        if (tileMap.getZoomLevelIndex() > 0)
            zoomOutButton.update();
        moveButton.update();
        paintButton.update();
        propertiesButton.update();
        saveButton.update();
        mapButton.update();
        if (mode.getValue() == MODE_PAINT) {
            prevButton.update();
            nextButton.update();
            for (int i = tilePage.getValue() * 8; i < tilePage.getValue() * 8 + 8 && i < paintTileButtons.size(); i++) {
                Button b = paintTileButtons.get(i);
                b.update();
            }
        }
        moveButton.setOutline((mode.getValue() == MODE_MOVE) ? buttonOutline : null);
        paintButton.setOutline((mode.getValue() == MODE_PAINT) ? buttonOutline : null);
        propertiesButton.setOutline((mode.getValue() == MODE_PROPERTIES) ? buttonOutline : null);

        mouseRelativeToMap.x = (Game.getMouseHandler().getX() - MAP_OFFSET_X);
        mouseRelativeToMap.y = (Game.getMouseHandler().getY() - MAP_OFFSET_Y);
        hoveredTileLocation.x = (int) ((mouseRelativeToMap.x - camera.getOffsetX()) / (int) tileMap.getTileSize());
        hoveredTileLocation.y = (int) ((mouseRelativeToMap.y - camera.getOffsetY()) / (int) tileMap.getTileSize());

        // TODO Replace conditional with polymorphism.
        switch (mode.getValue()) {
            default:
            case MODE_MOVE:
                boolean isMouseDown = Game.getMouseHandler().isDown();
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
                            if (paintSelection != null) {
                                double x1 = (paintSelection.getX()) / tileMap.getTileSize();
                                double y1 = (paintSelection.getY()) / tileMap.getTileSize();
                                double x2 = (paintSelection.getX() + paintSelection.getWidth()) / tileMap.getTileSize();
                                double y2 = (paintSelection.getY() + paintSelection.getHeight()) / tileMap.getTileSize();

                                for (int i = (int) x1; i <= (int) x2; i++) {
                                    for (int j = (int) y1; j <= (int) y2; j++) {
                                        if (i < 0 || j < 0)
                                            continue;
                                        if (i >= tileMap.getWidth() || j >= tileMap.getHeight())
                                            continue;

                                        selectedTileLocations.add(new Point(i, j));
                                    }
                                }

                                paintSelection = null;
                            }
                        }
                        wasMouseDownInViewport = false;
                    }
                break;
        }

        if (mode.getValue() == MODE_PAINT && paintTileName.length() > 0) {
            for (Point p : selectedTileLocations) {
                tileMap.getTile((int) p.getX(), (int) p.getY()).setFlipBookName(paintTileName.toString());
            }
            paintTileName.delete(0, paintTileName.length());
        }
        
        if (readyToSave.isTrue()) {
            System.out.println("Saving...");
            Game.getWindow().setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

            try (FileOutputStream out = new FileOutputStream(Config.projectResourcePath + "map.mtm")) {
                OutputStreamWriter writer = new OutputStreamWriter(out);

                // Write tile names to file.
                for (String name : tileMap.getTileMappings().keySet()) {
                    writer.write(name + " " + tileMap.isTileSolid(name) + "\n");
                }
                writer.write(";\n");

                // Write map size to file.
                writer.write(tileMap.getWidth() + "\n");
                writer.write(tileMap.getHeight() + "\n");

                // Find default tile name.
                HashMap<String, Integer> occurranceMap = new HashMap<>();
                for (int i = 0; i < tileMap.getWidth(); i++) {
                    for (int j = 0; j < tileMap.getHeight(); j++) {
                        Tile t = tileMap.getTile(i, j);
                        Integer count = occurranceMap.get(t.getFlipBookName());
                        occurranceMap.put(t.getFlipBookName(), count == null ? 1 : count + 1);
                    }
                }
                String defaultTileName = "";
                int count = 0;
                for (Entry<String, Integer> e : occurranceMap.entrySet()) {
                    if (e.getValue() > count) {
                        count = e.getValue();
                        defaultTileName = e.getKey();
                    }
                }

                // Write tiles to file.
                writer.write(defaultTileName);
                for (int i = 0; i < tileMap.getWidth(); i++) {
                    for (int j = 0; j < tileMap.getHeight(); j++) {
                        Tile t = tileMap.getTile(i, j);
                        if (t.getFlipBookName().equals(defaultTileName))
                            continue;
                        writer.write('\n');
                        writer.write(t.getFlipBookName() + " " + i + " " + j);
                    }
                }

                writer.flush();
                writer.close();
            } catch (IOException e) {
                e.printStackTrace();
            }

            readyToSave.setFalse();
            Game.getWindow().setCursor(Cursor.getDefaultCursor());
            System.out.println("Saved!");
        }

        if (readyToGenerateMap.isTrue()) {
            System.out.println("Generating map...");
            Game.getWindow().setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

            // Sample all tile types.
            HashMap<String, Color> colorValues = new HashMap<>();
            for (String name : tileMap.getTileMappings().keySet()) {
                BufferedImage image = tileMap.getTileMappings().get(name).getCurrentPage().getImage();
                int r = 0, g = 0, b = 0, count = 0;
                for (int x = 0; x < image.getWidth(); x++) {
                    for (int y = 0; y < image.getHeight(); y++) {
                        Color color = new Color(image.getRGB(x, y));
                        r += color.getRed();
                        g += color.getGreen();
                        b += color.getBlue();
                        count++;
                    }
                }

                colorValues.put(name, new Color(r/count, g/count, b/count));
            }

            int width = tileMap.getWidth();
            int height = tileMap.getHeight();
            BufferedImage map = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);

            for (int x = 0; x < width; x++) {
                for (int y = 0; y < height; y++) {
                    Tile t = tileMap.getTile(x, y);
                    map.setRGB(x, y, colorValues.get(t.getFlipBookName()).getRGB());
                }
            }

            try {
                ImageIO.write(map, "png", new FileOutputStream(Config.projectResourcePath + "map.png"));
            } catch (IOException e) {
                e.printStackTrace();
            }
            
            readyToGenerateMap.setFalse();
            Game.getWindow().setCursor(Cursor.getDefaultCursor());
            System.out.println("Map generated!");
        }
    }
}