# Molasses Tile Editor
Hello and thanks for your interest in the Molasses Tile Editor!  The app is still in development and I've tried to at least get it to a
usable state.  You should only need to change what's in the res folder.  That folder contains the "map.dat" file, which is what gets loaded
and saved to.  And all of the needed tiles will go in that folder as well.  I've only tested PNGs, but I think it should work for most
standard files.   
   
## map.dat
The "map.dat" file currently looks like the following.  I know it's not the most efficient way of exporting the tile map, but as I said, I've
only just been trying to get the app working.  In future updates, the layout will likely change, so bear with me.   
1. The first section contains all of the needed file names.  This section is delimited with a ```\n``` character and ends with a ```;```.
    * This section also contains whether or not the tile should be solid (denoted by the true or false after the name).
2. The next two numbers following the semicolon are the width and height of the tilemap respectively.
3. The next line is the name of the "default tile."  This tile will fill the entire map and will be overwritten by the tiles that follow it.
This will likely change in the future, but it was just a quick way to significantly decrease the size of map.dat.
4. The final section of the file contains the placement of all tiles other than the default tile.  These tile names are then followed by the
tileX and tileY locations that will be overwritten to this new tile.   
   
## Attributions
Data List Icon, Diy Paint Brush Icon: http://www.iconarchive.com/artist/icons8.html   
Cursor Move Icon: http://www.iconarchive.com/artist/iconsmind.html   
Save Icon: http://www.iconarchive.com/artist/custom-icon-design.html   
Folded paper map: https://www.flaticon.com/authors/freepik   
   
**For more code by Tyler Royer, including the engine used to make the Molasses Tile Editor, visit https://github.com/Hades948?tab=repositories**
