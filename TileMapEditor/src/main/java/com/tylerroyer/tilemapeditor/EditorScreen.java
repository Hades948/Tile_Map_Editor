package com.tylerroyer.tilemapeditor;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.awt.Point;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Scanner;
import java.util.Map.Entry;

import javax.imageio.ImageIO;

import com.tylerroyer.molasses.*;
import com.tylerroyer.molasses.events.DecrementIntegerEvent;
import com.tylerroyer.molasses.events.Event;
import com.tylerroyer.molasses.events.IncrementIntegerEvent;
import com.tylerroyer.molasses.events.MultiplyDoubleEvent;
import com.tylerroyer.molasses.events.SetIntegerEvent;
import com.tylerroyer.molasses.events.ToggleOnEvent;

import org.apache.commons.lang3.mutable.MutableBoolean;
import org.apache.commons.lang3.mutable.MutableDouble;
import org.apache.commons.lang3.mutable.MutableInt;

public class EditorScreen extends Screen {
    private final int MODE_MOVE = 0;
    private final int MODE_PAINT = 1;
    private final int MODE_PROPERTIES = 2;
    private MutableInt mode = new MutableInt(MODE_MOVE);

    private boolean wasMouseDownInViewport = false;
    private double[] zoomLevels = { 0.03125, 0.0625, 0.125, 0.25 };
    private Point mouseRelativeToMap = new Point();
    private Point hoveredTileLocation = new Point();
    private Point clickDownPoint = new Point();
    private Color backgroundColor = new Color(190, 205, 190);
    private MutableInt zoom = new MutableInt(3);
    private MutableInt paintTileIndex = new MutableInt(-1);
    private MutableInt tilePage = new MutableInt(0);
    private MutableDouble cameraOffsetX = new MutableDouble(0.0);
    private MutableDouble cameraOffsetY = new MutableDouble(0.0);
    private MutableBoolean readyToSave = new MutableBoolean(false);
    private MutableBoolean readyToGenerateMap = new MutableBoolean(false);
    private ArrayList<Button> paintTileButtons = new ArrayList<>();
    private TileMap tileMap;
    private Camera camera = new Camera(cameraOffsetX, cameraOffsetY);
    private BasicStroke buttonOutline = new BasicStroke(2);
    private Rectangle paintSelection = null;
    private ArrayList<Point> selectedTileLocations = new ArrayList<>();
    private ArrayList<String> tileNames = new ArrayList<>();

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
    public void loadResources() {
        tileMap = new TileMap("map.dat");
        tileNames = tileMap.getTileNames();

        for (int i = 0; i < tileNames.size(); i++) {
            String tileName = tileNames.get(i);
            BufferedImage baseImage = Resources.loadGraphicalImage(tileName);
            baseImage = Resources.scaleImage(baseImage, 128/baseImage.getWidth(), 128/baseImage.getHeight());
            Resources.addGraphicalResource(tileName, baseImage);
            for (int j = 0; j < zoomLevels.length; j++) {
                Resources.addGraphicalResource(tileName + "_zoom" + (j+1), Resources.scaleImage(baseImage, zoomLevels[j], zoomLevels[j]));
            }

            int x;
            int y;
            if (i % 2 == 0) {
                x = MAP_OFFSET_X + MAP_VIEWPORT_SIZE + 18;
                y = MAP_OFFSET_Y + (i%8) / 2 * 140;
            } else {
                x = MAP_OFFSET_X + MAP_VIEWPORT_SIZE + 128 + 36;
                y = MAP_OFFSET_Y + (i%8) / 2 * 140;
            }
            Button b = new Button(baseImage, x, y, new SetIntegerEvent(paintTileIndex, i));
            b.setOutline(buttonOutline);
            paintTileButtons.add(b);
        }
        
        Game.getWindow().setBackgroundColor(backgroundColor);

        zoomInEvent = new IncrementIntegerEvent(zoom, 1, zoomLevels.length);
        zoomOutEvent = new DecrementIntegerEvent(zoom, 1, 1);
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
        BufferedImage moveUnpressed = Resources.loadGraphicalImage("move_button_unpressed.png");
        BufferedImage paintUnpressed = Resources.loadGraphicalImage("paint_button_unpressed.png");
        BufferedImage propertiesUnpressed = Resources.loadGraphicalImage("properties_button_unpressed.png");
        BufferedImage saveUnpressed = Resources.loadGraphicalImage("save_button_unpressed.png");
        BufferedImage mapUnpressed = Resources.loadGraphicalImage("map_button_unpressed.png");
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
    }

    @Override
    public void render(GameGraphics g) {
        g.setCamera(camera);

        Tile t;
        Rectangle viewportBounds = new Rectangle(MAP_OFFSET_X, MAP_OFFSET_Y, MAP_VIEWPORT_SIZE, MAP_VIEWPORT_SIZE);
        g.setClip(viewportBounds);
        for (int i = 0; i < tileMap.getWidth(); i++) {
            for (int j = 0; j < tileMap.getHeight(); j++) {
                t = tileMap.getTile(i, j);
                BufferedImage image = Resources.getGraphicalResource(t.getImageName() + "_zoom" + zoom);
                g.drawImage(image, i * getTileSize() + MAP_OFFSET_X, j * getTileSize() + MAP_OFFSET_Y, Game.getWindow());
            }
        }

        switch(mode.getValue()) {
            default:
            case MODE_MOVE:
                if (isMouseOverMap() && isMouseInViewport()) {
                    // Draw selector
                    g.setColor(new Color(255, 255, 255, 100));
                    g.fillRect(hoveredTileLocation.getX() * getTileSize() + MAP_OFFSET_X, hoveredTileLocation.getY() * getTileSize() + MAP_OFFSET_Y, getTileSize(), getTileSize());
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
                    g.fillRect(p.getX() * getTileSize() + MAP_OFFSET_X, p.getY() * getTileSize() + MAP_OFFSET_Y, getTileSize(), getTileSize());
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
        g.drawString("Zoom: " + zoom.getValue(), MAP_OFFSET_X + 20, MAP_OFFSET_Y + MAP_VIEWPORT_SIZE + 60);

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

                t = tileMap.getTile(hoveredTileLocation.x, hoveredTileLocation.y);
                BufferedImage image = Resources.getGraphicalResource(t.getImageName());
                g.drawImage(image, infoX, infoY);
                g.drawString(t.getImageName(), infoX, infoY + 147);
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
            && mouseRelativeToMap.x - camera.getOffsetX() < getTileSize() * tileMap.getWidth()
            && mouseRelativeToMap.y - camera.getOffsetY() < getTileSize() * tileMap.getHeight();
    }

    private double getTileSize() {
        return Resources.getResourceSize("grass.png").getWidth() * zoomLevels[zoom.getValue() - 1];
    }
    
    @Override
    public void update() {
        // Buttons
        if (zoom.getValue() < zoomLevels.length)
            zoomInButton.update();
        if (zoom.getValue() > 1)
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
        hoveredTileLocation.x = (int) ((mouseRelativeToMap.x - camera.getOffsetX()) / (int) getTileSize());
        hoveredTileLocation.y = (int) ((mouseRelativeToMap.y - camera.getOffsetY()) / (int) getTileSize());

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
                                double x1 = (paintSelection.getX()) / getTileSize();
                                double y1 = (paintSelection.getY()) / getTileSize();
                                double x2 = (paintSelection.getX() + paintSelection.getWidth()) / getTileSize();
                                double y2 = (paintSelection.getY() + paintSelection.getHeight()) / getTileSize();

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

        if (mode.getValue() == MODE_PAINT && paintTileIndex.getValue() >= 0) {
            for (Point p : selectedTileLocations) {
                String imageName = tileNames.get(paintTileIndex.getValue());
                tileMap.getTile((int) p.getX(), (int) p.getY()).setImageName(imageName);
            }
            paintTileIndex.setValue(-1);
        }
        
        if (readyToSave.isTrue()) {
            System.out.println("Saving...");
            Game.getWindow().setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

            try (FileOutputStream out = new FileOutputStream("TileMapEditor/src/main/java/res/map.dat")) {
                OutputStreamWriter writer = new OutputStreamWriter(out);

                // Write tile names to file.
                for (String name : tileNames) {
                    writer.write(name + "\n");
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
                        Integer count = occurranceMap.get(t.getImageName());
                        occurranceMap.put(t.getImageName(), count == null ? 1 : count + 1);
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
                        if (t.getImageName().equals(defaultTileName))
                            continue;
                        writer.write('\n');
                        writer.write(t.getImageName() + " " + i + " " + j);
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
            for (String name : tileNames) {
                BufferedImage image = Resources.getGraphicalResource(name);
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
                    map.setRGB(x, y, colorValues.get(t.getImageName()).getRGB());
                }
            }

            try {
                ImageIO.write(map, "png", new FileOutputStream("TileMapEditor/src/main/java/res/map.png"));
            } catch (IOException e) {
                e.printStackTrace();
            }
            
            readyToGenerateMap.setFalse();
            Game.getWindow().setCursor(Cursor.getDefaultCursor());
            System.out.println("Map generated!");
        }
    }
}