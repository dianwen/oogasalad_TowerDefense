package main.java.engine;

import java.awt.geom.Point2D;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import jgame.platform.JGEngine;
import main.java.author.view.tabs.terrain.TerrainAttribute;
import main.java.data.DataHandler;
import main.java.engine.factory.TDObjectFactory;
import main.java.engine.map.TDMap;
import main.java.engine.objects.CollisionManager;
import main.java.engine.objects.Exit;
import main.java.engine.objects.TDObject;
import main.java.engine.objects.monster.Monster;
import main.java.engine.objects.monster.jgpathfinder.*;
import main.java.engine.objects.powerup.TDPowerupPowerup;
import main.java.engine.objects.tower.ITower;
import main.java.engine.objects.tower.ShootingTower;
import main.java.engine.objects.tower.TowerBehaviors;
import main.java.exceptions.engine.InvalidSavedGameException;
import main.java.exceptions.engine.MonsterCreationFailureException;
import main.java.exceptions.engine.ObjectInfoException;
import main.java.exceptions.engine.TowerCreationFailureException;
import main.java.schema.CanvasSchema;
import main.java.schema.GameBlueprint;
import main.java.schema.GameSchema;
import main.java.schema.MonsterSpawnSchema;
import main.java.schema.map.GameMapSchema;
import main.java.schema.tdobjects.TowerSchema;


/**
 * A class that handles all the game logic.
 * It's a direct point of contact of the view.
 * Its public methods are primarily called by the view.
 * 
 */

public class Model implements IModel {

	private static final double DEFAULT_MONEY_MULTIPLIER = 0.5;
	public static final String RESOURCE_PATH = "/main/resources/";

	private JGEngine engine;
	private TDObjectFactory factory;
	private Player player;
	private double gameClock;
	private ITower[][] towers;
	private List<Monster> monsters;
	private CollisionManager collisionManager;
	private DataHandler dataHandler;
	private LevelManager levelManager;
	private EnvironmentKnowledge environ;
	private List<TDPowerupPowerup> items;
	private TDMap currentMap;
	private PathfinderManager pathfinderManager;

	public Model (JGEngine engine, String pathToBlueprint) {
		this.engine = engine;
		dataHandler = new DataHandler();
		defineExitImage();
		this.factory = new TDObjectFactory(engine);
		collisionManager = new CollisionManager(engine);

		initPathfinderManager();
		levelManager = new LevelManager(factory, pathfinderManager);

		this.gameClock = 0;
		monsters = new ArrayList<Monster>();
		towers = new ITower[engine.viewTilesX()][engine.viewTilesY()];
		items = new ArrayList<TDPowerupPowerup>();

		try {
			loadGameBlueprint(pathToBlueprint);
		}
		catch (Exception e) {
			e.printStackTrace();
		}

		addNewPlayer();
	}

	private void defineExitImage () {
		// TODO: remove this method, make exit a part of wavespawnschemas
		// and define its image dynamically
		engine.defineImage(Exit.NAME, "-", 1, RESOURCE_PATH + Exit.IMAGE_NAME, "-");
	}

	/**
	 * Create the inital pathfinder with a given tilemap and heuristic.
	 */
	private void initPathfinderManager() {
		JGTileMapInterface tileMap = new JGTileMap(engine);
		JGPathfinderHeuristicInterface heuristic = new JGPathfinderHeuristic();
		pathfinderManager = new PathfinderManager(tileMap, heuristic);
	}

	/**
	 * Add a new player to the engine
	 */
	public void addNewPlayer () {
		this.player = new Player();
		levelManager.registerPlayer(player);

		environ = new EnvironmentKnowledge(monsters, player, towers, levelManager.getExit());
	}

	/**
	 * Add a tower at the specified location. If tower already exists in that cell, do nothing.
	 * 
	 * @param x x coordinate of the tower
	 * @param y y coordinate of the tower
	 * @param towerName Type tower to be placed
	 */
	public boolean placeTower (double x, double y, String towerName) {
		try {
			Point2D location = new Point2D.Double(x, y);
			int[] currentTile = getTileCoordinates(location);

			// if tower already exists in the tile clicked, do nothing
			if (isTowerPresent(currentTile)) {
				return false;
			}

			// check if tower will block paths
			if (willTowerBlockPath(currentTile)) {
				return false;
			}

			ITower newTower = factory.placeTower(location, towerName);
			if (player.getMoney() >= newTower.getCost()) {
				// FIXME: Decrease money?
				player.changeMoney(-newTower.getCost());
				towers[currentTile[0]][currentTile[1]] = newTower;
				return true;
			}
			else {
				newTower.remove();
				currentMap.revertTileCIDToOriginal(currentTile[0], currentTile[1]);
				return false;
			}
		}
		catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return false;
	}

	private boolean willTowerBlockPath(int currentTile[]) {
		try {
			currentMap.setTileCID(currentTile[0], currentTile[1],
					TerrainAttribute.Unwalkable.getIndex()); // TODO: get from schema
			pathfinderManager.updatePaths(monsters);
		} catch (NoPossiblePathException e) {
			currentMap.revertTileCIDToOriginal(currentTile[0], currentTile[1]);
			System.out.println("Cannot place tower as it will block path");
			return true;
		}

		return false;
	}

	/**
	 * Return a two element int array with the tile coordinates that a given point is on, for use
	 * with Tower[][]
	 * 
	 * @param location
	 * @return the row, col of the tile on which the location is situated
	 */
	private int[] getTileCoordinates (Point2D location) {
		int curXTilePos = (int) (location.getX() / engine.tileWidth());
		int curYTilePos = (int) (location.getY() / engine.tileHeight());

		return new int[] { curXTilePos, curYTilePos };
	}

	/**
	 * Check if there's a tower present at the specified coordinates
	 * 
	 * @param coordinates
	 * @return true if there is a tower
	 */
	private boolean isTowerPresent (int[] coordinates) {
		return towers[coordinates[0]][coordinates[1]] != null;
	}

	/**
	 * Check if there's a tower present at the specified coordinates
	 * This is mainly for the view to do a quick check
	 * 
	 * @param x
	 * @param y
	 * @return true if there is a tower
	 */
	public boolean isTowerPresent (double x, double y) {
		return isTowerPresent(getTileCoordinates(new Point2D.Double(x, y)));
	}

	/**
	 * Get the information of the towers or monsters, if any,
	 * at the specified coordinates
	 * 
	 * @param x
	 * @param y
	 * @return The information that we want to display to the player
	 */
	public List<String> getUnitInfo (double x, double y) {
		List<String> info = new ArrayList<String>();
		if (isTowerPresent(x, y)) {
			int[] currentTile = getTileCoordinates(new Point2D.Double(x, y));
			ITower currTower = towers[currentTile[0]][currentTile[1]];
			info.add(currTower.getInfo());
		}

		Monster m;
		if ((m = monsterPresent(x, y)) != null) {
			info.add(m.getInfo());
		}
		return info;
	}
	
	/**
	 * Returns the description associated with a particular tower. 
	 * 
	 * @param towerName
	 * @return
	 */
	public String getTowerDescription(String towerName) {
		return factory.getTowerDescription(towerName);
	}

	/**
	 * Return the monster at the specified coordinates.
	 * If there's no monster at that location, null will be returned.
	 * 
	 * @param x
	 * @param y
	 * @return the monster present
	 */
	private Monster monsterPresent (double x, double y) {
		Monster monster = null;
		for (Monster m : monsters) {
			double xUpper = m.x + m.getImageBBoxConst().width;
			double yUpper = m.y + m.getImageBBoxConst().height;
			if (m.x <= x && x <= xUpper && m.y <= y && y <= yUpper) {
				monster = m;
			}
		}
		return monster;
	}

	/**
	 * Check if the current location contains any tower. If yes, remove it. If no, do nothing
	 * 
	 * @param x
	 * @param y
	 */
	public void checkAndRemoveTower (double x, double y) {
		int[] coordinates = getTileCoordinates(new Point2D.Double(x, y));
		if (isTowerPresent(coordinates)) {
			int xtile = coordinates[0];
			int ytile = coordinates[1];
			player.changeMoney(DEFAULT_MONEY_MULTIPLIER * towers[xtile][ytile].getCost());
			towers[xtile][ytile].remove();
			currentMap.revertTileCIDToOriginal(xtile, ytile);
			try {
				pathfinderManager.updatePaths(monsters);
			} catch (Exception e) {
				e.printStackTrace(); // ignore, removing a tower should never block a path
			}
			towers[xtile][ytile] = null;
		}
	}

	/**
	 * Deserialize and load into the engine the GameBlueprint obtained from the file path
	 * 
	 * @param filePath File path of the blueprint to be loaded
	 * @throws IOException
	 * @throws ClassNotFoundException
	 */

	public void loadGameBlueprint (String filePath) throws ClassNotFoundException, IOException {
		GameBlueprint blueprint = null;
		if (filePath == null) {
			//blueprint = createTestBlueprint();
		}
		else {
			try {
				blueprint = dataHandler.loadBlueprint(filePath, true);
			}
			catch (Exception e) {
				e.printStackTrace();
				return;
			}
		}

		// Initialize from game settings from game schema
		GameSchema gameSchema = blueprint.getMyGameScenario();
		Map<String, Serializable> gameSchemaAttributeMap = gameSchema.getAttributesMap();
		this.player = new Player((Integer) gameSchemaAttributeMap.get(GameSchema.MONEY),
				(Integer) gameSchemaAttributeMap.get(GameSchema.LIVES));
		Boolean survivalModeAttribute =
				(Boolean) TDObject.getValueOrDefault(gameSchemaAttributeMap,
						GameSchema.ISSURVIVALMODE, Boolean.TRUE);
		levelManager.setSurvivalMode(survivalModeAttribute);

		// Initialize factory objects
		if (blueprint.getMyTowerSchemas() != null) {
			factory.loadTowerSchemas(blueprint.getMyTowerSchemas());
		}
		if (blueprint.getMyMonsterSchemas() != null) {
			factory.loadMonsterSchemas(blueprint.getMyMonsterSchemas());
		}
		if (blueprint.getMyItemSchemas() != null) {
			factory.loadItemSchemas(blueprint.getMyItemSchemas());
		}

		// Initialize waves
		if (blueprint.getMyWaveSchemas() != null) {
			levelManager.cleanLoadWaveSchemas(blueprint.getMyWaveSchemas(), 0);
		}

		// Initialize map settings
		if (blueprint.getMyGameMapSchemas() != null) {
			currentMap = new TDMap(engine, blueprint.getMyGameMapSchemas().get(0)); // TODO: load
			// each map
			CanvasSchema myCanvasSchema =
					(CanvasSchema) blueprint.getMyGameMapSchemas().get(0).getAttributesMap()
					.get(GameMapSchema.MY_CANVAS_ATTRIBUTES);
			// TODO: Code entrance/exit logic into wave or monster spawn schema
			levelManager.setEntrance((Integer) myCanvasSchema.getAttributesMap()
					.get(CanvasSchema.ENTRY_COL) * engine.tileWidth(),
					(Integer) myCanvasSchema.getAttributesMap()
					.get(CanvasSchema.ENTRY_ROW) * engine.tileHeight());
			levelManager.setExit((Integer) myCanvasSchema.getAttributesMap()
					.get(CanvasSchema.EXIT_COL) * engine.tileWidth(),
					(Integer) myCanvasSchema.getAttributesMap()
					.get(CanvasSchema.EXIT_ROW) * engine.tileHeight());
		}
	}

	/**
	 * Reset the game clock
	 */
	public void resetGameClock () {
		this.gameClock = 0;
	}

	/**
	 * Get the score of the player
	 * 
	 * @return player's current score
	 */
	public double getScore () {
		return player.getScore();
	}

	/**
	 * Check whether the game is lost
	 * 
	 * @return true if game is lost
	 */
	public boolean isGameLost () {
		return getPlayerLives() <= 0;
	}

	private void updateGameClockByFrame () {
		this.gameClock++;
	}

	/**
	 * Get the game clock
	 * 
	 * @return current game clock
	 */
	public double getGameClock () {
		return this.gameClock;
	}

	/**
	 * Get the number of remaining lives of the player
	 * 
	 * @return number of lives left
	 */
	public int getPlayerLives () {
		return player.getLivesRemaining();
	}

	/**
	 * Get the amount of money obtained by the player
	 * 
	 * @return current amount of money
	 */
	public int getMoney () {
		return player.getMoney();
	}

	/**
	 * Returns whether or not the player has complete all waves and thus has won
	 * the game. This will always return false on survival mode.
	 * 
	 * @return boolean of whether game is won (all waves spawned and completed)
	 */
	public boolean isGameWon () {
		return !levelManager.isSurvivalMode()
				&& levelManager.zeroWavesRemaining()
				&& monsters.size() == 0;
	}

	/**
	 * Set whether or not the game is played on survival mode.
	 * 
	 * @param survivalMode
	 * @return
	 */
	public void setSurvivalMode (boolean survivalMode) {
		levelManager.setSurvivalMode(survivalMode);
	}

	/**
	 * Spawns a new wave
	 * 
	 * @throws MonsterCreationFailureException
	 */
	public void doSpawnActivity () throws MonsterCreationFailureException {
		// at determined intervals:
		// if (gameClock % 100 == 0)
		// or if previous wave defeated:
		if (monsters.isEmpty())
			monsters.addAll(levelManager.spawnNextWave());

	}

	/**
	 * The model's "doFrame()" method that updates all state, spawn monsters,
	 * etc.
	 * 
	 * @throws MonsterCreationFailureException
	 */
	public void updateGame () throws MonsterCreationFailureException {
		updateGameClockByFrame();
		doSpawnActivity();
		doTowerBehaviors();
		doItemActions();
		removeDeadMonsters();
	}

	private void doItemActions () {
		Iterator<TDPowerupPowerup> itemIter = items.iterator();
		while (itemIter.hasNext()) {
			TDPowerupPowerup currentItem = itemIter.next();
			if (currentItem.isDead()) {
				itemIter.remove();
				currentItem.remove();
				return;
			}
			currentItem.doAction(environ);
		}
	}

	/**
	 * Place an item at the specified location.
	 * If it costs more than the player has, do nothing.
	 * 
	 * @param name
	 * @param x
	 * @param y
	 */
	public boolean placeItem (String name, double x, double y) {
		try {
			TDPowerupPowerup newItem = factory.placeItem(new Point2D.Double(x, y), name);
			if (newItem.getCost() <= player.getMoney()) {
				items.add(newItem);
				player.changeMoney(-newItem.getCost());
				return true;
			}
			else {
				newItem.setImage(null);
				newItem.remove();
				return false;
			}
		}
		catch (Exception e) {
			e.printStackTrace();
			return false;
		}
	}

	/**
	 * Clean up dead monsters from monsters list and JGEngine display.
	 */
	private void removeDeadMonsters () {
		Iterator<Monster> monsterIter = monsters.iterator();
		List<Monster> newlyAdded = new ArrayList<Monster>();
		while (monsterIter.hasNext()) {
			Monster currentMonster = monsterIter.next();
			if (currentMonster.isDead()) {
				MonsterSpawnSchema resurrectSchema =
						currentMonster.getResurrrectMonsterSpawnSchema();
				if (resurrectSchema != null) {
					try {
						newlyAdded =
								levelManager.spawnMonsterSpawnSchema(resurrectSchema,
										currentMonster
										.getCurrentCoor());
					}
					catch (MonsterCreationFailureException e) {
						// resurrection schema could not be spawned, so ignore it.
						e.printStackTrace();
					}
				}
				monsterIter.remove();
				addMoney(currentMonster.getMoneyValue());
				currentMonster.remove();
			}
		}
		monsters.addAll(newlyAdded);
	}

	private void addMoney (double moneyValue) {
		player.changeMoney(moneyValue);
	}

	/**
	 * Call this to do the individual behavior of each Tower
	 */
	private void doTowerBehaviors () {

		for (ITower[] towerRow : towers) {
			for (ITower t : towerRow) {
				if (t != null) {
					t.callTowerActions(environ);
				}
			}
		}
	}

	/**
	 * Check all collisions specified by the CollisionManager
	 */
	public void checkCollisions () {
		collisionManager.checkAllCollisions();
	}

	/**
	 * Upgrade the tower at the specified coordinates and return true if upgraded successfully.
	 * If not possible, does nothing, and this method returns false.
	 * 
	 * @param x x-coordinate of tower to be upgraded
	 * @param y y-coordinate of tower to be upgraded
	 * @return boolean whether or not the tower was successfully upgraded
	 * @throws TowerCreationFailureException
	 */
	public boolean upgradeTower (double x, double y) throws TowerCreationFailureException {
		int[] coordinates = getTileCoordinates(new Point2D.Double(x, y));

		if (!isTowerPresent(coordinates)) { return false; }

		int xtile = coordinates[0];
		int ytile = coordinates[1];
		ITower existingTower = towers[xtile][ytile];
		String newTowerName = existingTower.getUpgradeTowerName();

		if (!isValidUpgradeTower(newTowerName)) { return false; }

		ITower newTower = factory.placeTower(new Point2D.Double(x, y), newTowerName);
		player.changeMoney(-newTower.getCost());
		// TODO: Specify cost of upgrade, calculate difference between old and new tower, or give
		// some discount?
		existingTower.remove();
		towers[xtile][ytile] = newTower;
		return true;
	}

	/**
	 * Checks if a string is a valid tower name, i.e. non-empty and in the list of possible towers
	 * defined by loaded schemas
	 * 
	 * @param newTowerName
	 * @return boolean
	 */
	private boolean isValidUpgradeTower (String newTowerName) {
		return (!newTowerName.equals("") && getPossibleTowers().contains(newTowerName));
	}

	/**
	 * Decrease player's lives by one.
	 */
	public void decrementLives () {
		player.decrementLives();
	}

	/**
	 * A list of names of possible towers to create
	 * 
	 * @return
	 */
	public List<String> getPossibleTowers () {
		return Collections.unmodifiableList(factory.getPossibleTowersNames());
	}

	/**
	 * A list of names of possible items to create
	 * 
	 * @return
	 */
	public List<String> getPossibleItems () {
		return Collections.unmodifiableList(factory.getPossibleItemNames());
	}


	/**
	 * Returns the range of a tower given the schema name. Checks to see if the schema corresponds to a shooting tower, if not, method will return 0
	 * @param towerSchemaName
	 * @return Range of tower
	 * @throws ObjectInfoException 
	 */
	public double getRange(String towerSchemaName) throws ObjectInfoException {
		Map<String, Serializable> attributes = factory.getTDObjectAttributes(towerSchemaName);
		try {
			if(!attributes.containsKey(TowerSchema.TOWER_BEHAVIORS)) {//Checks that the schema exists and is a tower schema
				throw new ObjectInfoException("This is not a tower schema");
			}} catch (NullPointerException e) {
				throw new ObjectInfoException("Schema does not exist");			}
		if(((Collection<TowerBehaviors>) attributes.get(TowerSchema.TOWER_BEHAVIORS)).contains(TowerBehaviors.SHOOTING)) { //Checks that the tower shoots
			return attributes.containsKey(TowerSchema.RANGE) ? (Double) attributes.get(TowerSchema.RANGE) : ShootingTower.DEFAULT_RANGE; //Return defined or default range
		}

		return 0; //Not a shooting tower
	}

	/**
	 * Save the present game state to a loadable file.
	 * Note: all saved game files saved to under resources folder.
	 * 
	 * @param gameName the file name to save the current game under.
	 * @throws InvalidSavedGameException Problem saving the game
	 */
	public void saveGame (String gameName) throws InvalidSavedGameException {
		GameState currentGame = new GameState();
		currentGame.updateGameStates(towers,
				levelManager.getCurrentWave(),
				levelManager.getAllWaves(),
				gameClock,
				player);
		try {
			// Michael- i removed the resource_path because it was giving me an error since the
			// method should take the straight file name not the resource path
			dataHandler.saveState(currentGame, gameName);
		}
		catch (IOException ioe) {
			throw new InvalidSavedGameException(ioe);
		}
	}

	/**
	 * Clears current game and restarts a new game based on loaded saved game.
	 * Only valid saved game files in the resources folder can be loaded.
	 * Pass in the file's name only (e.g. which can be chosen through JFileChooser)
	 * 
	 * @param filename The full filename only.
	 * @throws InvalidSavedGameException issue loading the game,
	 *         (please pause and notify the player, then continue the present game).
	 */
	public void loadSavedGame (String filename) throws InvalidSavedGameException {
		try {
			// TODO: check for proper game blueprint loaded prior?
			// removed the RESOURCE_PATH variable as i think thats causing issues with actually
			// saving
			GameState newGameState = dataHandler.loadState(filename);

			// replace towers, player, clock with new state
			clearAllTowers();
			towers = newGameState.getTowers();
			player = newGameState.getPlayer();
			gameClock = newGameState.getGameClock();

			// cleanly reload waves in the level manager, and reset wave # to start at.
			levelManager.cleanLoadWaveSchemas(newGameState.getAllWaveSchemas(),
					newGameState.getCurrentWaveNumber());

		}
		catch (ClassNotFoundException | IOException e) {
			throw new InvalidSavedGameException(e);
		}

	}

	/**
	 * Clear all of the current towers.
	 * Used internally to replace current tower state with with a new loaded saved game state.
	 */
	private void clearAllTowers () {
		for (ITower[] row : towers) {
			for (ITower t : row) {
				if (t != null) {
					t.remove();
				}
			}
			// null out tower matrix row by row after jgobject removal called.
			Arrays.fill(row, null);
		}
	}

	/* (non-Javadoc)
	 * @see main.java.engine.IModel#getItemDescription(java.lang.String)
	 */
	@Override
	public String getItemDescription(String itemName) {
		return factory.getItemDescription(itemName);
	}
	
	/**
         * Call this cheat to annihilate all monsters on field.
         */
        public void annihilateMonsters() {
            Iterator<Monster> iter = monsters.iterator();
            while(iter.hasNext()) {
                Monster m = iter.next();
                m.takeDamage(Double.MAX_VALUE);
            }
        }
}
