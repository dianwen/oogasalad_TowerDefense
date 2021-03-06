package main.java.engine.map;

import jgame.impl.JGEngineInterface;
import main.java.engine.Model;
import main.java.schema.map.GameMapSchema;
import main.java.schema.map.TileMapSchema;
import main.java.schema.map.TileSchema;

import java.awt.geom.Point2D;
import java.io.Serializable;
import java.util.*;

public class TDMap {
    private static final int X_OFFSET = 0;
    private static final int Y_OFFSET = 0;
    private static final int X_SKIP = 0;
    private static final int Y_SKIP = 0;
    private static final String IMG_OP = "-";

	private JGEngineInterface engine;
    private List<TileSchema> tileSchemas;
    private List<TileMapSchema> tileMapSchemas;
    private Map<String, TileMap> tileMaps;
    private Set<String> definedTiles;
	private int[][] originalTileCIDs;
    private static int tileHeight;
    private static int tileWidth;

    public TDMap(JGEngineInterface engine, GameMapSchema gameMapSchema) {
		this.engine = engine;
        tileHeight = engine.tileHeight();
        tileWidth = engine.tileWidth();
        Map<String, Serializable> gameMapAttributes = gameMapSchema.getAttributesMap();
        tileSchemas = (List<TileSchema>) gameMapAttributes.get(GameMapSchema.MY_TILES);
        tileMapSchemas = (List<TileMapSchema>) gameMapAttributes.get(GameMapSchema.MY_TILEMAPS);
        tileMaps = new HashMap<>();
        definedTiles = new HashSet<>();

		initOriginalTileCIDs();
		loadTilemaps();
		loadTiles();
	}

	/**
	 * Initialize original CID map to all zeros.
	 */
	private void initOriginalTileCIDs() {
		originalTileCIDs = new int[engine.pfTilesX()][engine.pfTilesY()];
		for (int i = 0; i < originalTileCIDs.length; i++) {
			for (int j = 0; j < originalTileCIDs[i].length; j++) {
				originalTileCIDs[i][j] = 0; // TODO: make walkable constant
			}
		}
	}

	/**
	 * Load in tilemaps.
	 */
	private void loadTilemaps() {
		for (TileMapSchema tms : tileMapSchemas) {
			TileMap tileMap = new TileMap(tms);
			tileMaps.put(tileMap.name, tileMap);

			engine.defineImageMap(tileMap.name, Model.RESOURCE_PATH + tileMap.name,
					X_OFFSET, Y_OFFSET, tileMap.pixelSize, tileMap.pixelSize,
					X_SKIP, Y_SKIP);
		}
	}

	/**
	 * Load in tiles.
	 */
	private void loadTiles() {
		for (TileSchema ts : tileSchemas) {
			Map<String, Serializable> tsAttributeMap = ts.getAttributesMap();
			String tileMapFileName = (String) tsAttributeMap.get(TileSchema.TILEMAP_FILE_NAME);

			if (tileMaps.get(tileMapFileName) == null) {
				continue; // TODO: fix
			}

			int tileRow = (Integer) tsAttributeMap.get(TileSchema.CANVAS_ROW);
			int tileCol = (Integer) tsAttributeMap.get(TileSchema.CANVAS_COL);
			int tileMapRow = (Integer) tsAttributeMap.get(TileSchema.TILEMAP_ROW);
			int tileMapCol = (Integer) tsAttributeMap.get(TileSchema.TILEMAP_COL);
			int tileCID = (Integer) tsAttributeMap.get(TileSchema.TILE_CID);
			int tileIndex = tileMapRow * tileMaps.get(tileMapFileName).numCols + tileMapCol;

			String tileName = tileMapFileName + tileMapCol + tileMapRow;

			if (!definedTiles.contains(tileName)) {
				engine.defineImage(tileName, tileMapRow + tileMapCol + "", tileCID,
						tileMapFileName, tileIndex, IMG_OP);
				definedTiles.add(tileName);
			}

			engine.setTile(tileCol, tileRow, tileMapRow + tileMapCol + "");
			originalTileCIDs[tileCol][tileRow] = tileCID;
		}
	}

	/**
	 * Set the given tile for the map to a given CID.
	 *
	 * @param xind X index of tile
	 * @param yind X index of tile
	 * @param cid CID the tile is to be set to
	 */
	public void setTileCID(int xind, int yind, int cid) {
		engine.setTileCid(xind, yind, cid);
	}

	/**
	 * Set the tile at the given index to the original CID when the map was
	 * created.
	 *
	 * @param xind X index of tile
	 * @param yind Y index of tile
	 */
	public void revertTileCIDToOriginal(int xind, int yind) {
		engine.setTileCid(xind, yind, originalTileCIDs[xind][yind]);
	}

	/**
	 * Private inner class, container for easy access of tilemaps.
	 */
	private class TileMap {
        private String name;
        private int pixelSize;
        private int numRows;
        private int numCols;

        public TileMap(TileMapSchema tileMapSchema) {
            Map<String, Serializable> tmsAttributesMap = tileMapSchema.getAttributesMap();
            name = (String) tmsAttributesMap.get(TileMapSchema.TILEMAP_FILE_NAME);
            pixelSize = (Integer) tmsAttributesMap.get(TileMapSchema.PIXEL_SIZE);
            numRows = (Integer) tmsAttributesMap.get(TileMapSchema.NUM_ROWS);
            numCols = (Integer) tmsAttributesMap.get(TileMapSchema.NUM_COLS);
        }
    }
    
    /**
     * Find the top-left corner associated with the tile associated with the given location.
     * 
     * @param location Coordinate of the map used to find the associated file
     * @return The top left corner of the tile at the given coordinate
     */
    public static Point2D findTileOrigin (Point2D location) {
        int curXTilePos = (int) location.getX() / tileWidth * tileWidth;
        int curYTilePos = (int) location.getY() / tileHeight * tileHeight;
        return new Point2D.Double(curXTilePos, curYTilePos);
    }
}
