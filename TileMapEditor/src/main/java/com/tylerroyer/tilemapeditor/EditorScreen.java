package com.tylerroyer.tilemapeditor;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Font;
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
import java.util.Random;
import java.util.Scanner;
import java.util.Map.Entry;

import javax.imageio.ImageIO;

import com.tylerroyer.molasses.*;
import com.tylerroyer.molasses.events.DecrementIntegerEvent;
import com.tylerroyer.molasses.events.Event;
import com.tylerroyer.molasses.events.IncrementIntegerEvent;
import com.tylerroyer.molasses.events.SetIntegerEvent;
import com.tylerroyer.molasses.events.ToggleOnEvent;

import org.apache.commons.lang3.mutable.MutableBoolean;
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
    private MutableBoolean readyToSave = new MutableBoolean(false);
    private MutableBoolean readyToGenerateMap = new MutableBoolean(false);
    private ArrayList<Button> paintTileButtons = new ArrayList<>();
    private TileMap tileMap;
    private Camera camera = new Camera();
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

    public EditorScreen() {}

    @Override
    public void loadResources() {
        // Grab tile names from file.
        try (Scanner scanner = new Scanner(new FileInputStream(new File("TileMapEditor/src/main/java/res/tile_names.dat")))) {
            while(scanner.hasNextLine()) {
                tileNames.add(scanner.nextLine());
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        // Load tile map
        try (Scanner scanner = new Scanner(new FileInputStream(new File("TileMapEditor/src/main/java/res/map.dat")))) {
            int width = Integer.parseInt(scanner.nextLine());
            int height = Integer.parseInt(scanner.nextLine());
            tileMap = new TileMap(width, height);

            String defaultTileName = scanner.nextLine();
            for (ArrayList<Tile> tiles : tileMap.getTiles()) {
                for (Tile t : tiles) {
                    t.setImageName(defaultTileName);
                }
            }

            while (scanner.hasNextLine()) {
                String name = scanner.next();
                int x = scanner.nextInt();
                int y = scanner.nextInt();

                tileMap.getTiles().get(x).get(y).setImageName(name);
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        for (int i = 0; i < tileNames.size(); i++) {
            String tileName = tileNames.get(i);
            BufferedImage baseImage = Resources.loadGraphicalImage(tileName);
            for (int j = 0; j < zoomLevels.length; j++) {
                Resources.addGraphicalResource(tileName + "_zoom" + (j+1), Resources.scaleImage(baseImage, zoomLevels[j], zoomLevels[j]));
            }

            paintTileButtons.add(new Button(baseImage, MAP_OFFSET_X + MAP_VIEWPORT_SIZE + 18, MAP_OFFSET_Y + i * 140, new SetIntegerEvent(paintTileIndex, i)));
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
        saveButton.render(g);
        mapButton.render(g);
        if (mode.getValue() == MODE_PAINT) {
            for (Button b : paintTileButtons) {
                b.render(g);
            }
        }

        // Draw tile info
        if (isMouseOverMap() && isMouseInViewport() && mode.getValue() == MODE_MOVE) {
            try {
                int infoX = MAP_OFFSET_X + MAP_VIEWPORT_SIZE + 18, infoY = MAP_OFFSET_Y;

                g.setFont(new Font("Helvetica", Font.PLAIN, 16));

                t = tileMap.getTiles().get(hoveredTileLocation.x).get(hoveredTileLocation.y);
                BufferedImage image = Resources.getGraphicalResource(t.getImageName());
                g.drawImage(image, infoX, infoY);
                g.drawString(t.getImageName(), infoX, infoY + 147);
                g.drawString("(" + hoveredTileLocation.x + ", " + hoveredTileLocation.y + ")", infoX, infoY + 172);
            } catch (IndexOutOfBoundsException e) {
                System.err.println("Trying to display info about an out-of-bounds tile!");
            }
        } else if (mode.getValue() != MODE_PAINT) {
            g.setFont(new Font("Helvetica", Font.PLAIN, 18));
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
        saveButton.update();
        mapButton.update();
        if (mode.getValue() == MODE_PAINT) {
            for (Button b : paintTileButtons) {
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

        if (mode.getValue() == MODE_PAINT && paintTileIndex.getValue() >= 0) {
            for (Point p : selectedTileLocations) {
                String imageName = tileNames.get(paintTileIndex.getValue());
                tileMap.getTiles().get((int) p.getX()).get((int) p.getY()).setImageName(imageName);
            }
            paintTileIndex.setValue(-1);
        }
        
        if (readyToSave.isTrue()) {
            System.out.println("Saving...");
            Game.getWindow().setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

            try (FileOutputStream out = new FileOutputStream("TileMapEditor/src/main/java/res/map.dat")) {
                OutputStreamWriter writer = new OutputStreamWriter(out);

                // Write map size to file.
                writer.write(tileMap.getTiles().size() + "\n");
                writer.write(tileMap.getTiles().get(0).size() + "\n");

                // Find default tile name.
                HashMap<String, Integer> occurranceMap = new HashMap<>();
                for (ArrayList<Tile> tiles : tileMap.getTiles()) {
                    for (Tile t : tiles) {
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
                for (int i = 0; i < tileMap.getTiles().size(); i++) {
                    for (int j = 0; j < tileMap.getTiles().get(0).size(); j++) {
                        Tile t = tileMap.getTiles().get(i).get(j);
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

            int width = tileMap.getTiles().size();
            int height = tileMap.getTiles().get(0).size();
            BufferedImage map = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);

            for (int x = 0; x < width; x++) {
                for (int y = 0; y < height; y++) {
                    Tile t = tileMap.getTiles().get(x).get(y);
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