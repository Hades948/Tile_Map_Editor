package com.tylerroyer.tilemapeditor;

import com.tylerroyer.molasses.Config;
import com.tylerroyer.molasses.Game;

public class Driver {
    public static void main(String[] args) {
        Config.windowWidth = 1400;   Config.windowHeight = 800;
        Config.windowTitle = "Molasses Tile Editor";
        Config.firstScreen = new EditorScreen();

        Game.start();
    }
}