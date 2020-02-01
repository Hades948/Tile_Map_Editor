package com.tylerroyer.tilemapeditor;

import com.tylerroyer.molasses.Config;
import com.tylerroyer.molasses.Game;

public class Driver {
    public static void main(String[] args) {
        Config.windowWidth = 1032;   Config.windowHeight = 750;
        Config.windowTitle = "Molasses Tile Editor";
        Config.firstScreen = new EditorScreen();
        Config.projectResourcePath = "TileMapEditor/src/main/java/res/";

        Game.start();
    }
}
