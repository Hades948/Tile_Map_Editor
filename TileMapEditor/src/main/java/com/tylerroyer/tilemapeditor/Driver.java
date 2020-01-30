package com.tylerroyer.tilemapeditor;

import com.tylerroyer.molasses.Config;
import com.tylerroyer.molasses.Game;

public class Driver {
    public static void main(String[] args) {
        Config.windowWidth = 712;   Config.windowHeight = 750;
        Config.windowTitle = "Molasses Tile Editor";
        Config.firstScreen = new EditorScreen();

        Game.start();
    }
}
