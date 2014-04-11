package main.java.engine;

import java.awt.geom.Point2D;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import jgame.platform.JGEngine;
import main.java.data.datahandler.DataHandler;
import main.java.engine.factory.TDObjectFactory;
import main.java.engine.map.TDMap;
import main.java.engine.objects.CollisionManager;
import main.java.engine.objects.Exit;
import main.java.engine.objects.monster.Monster;
import main.java.engine.objects.tower.Tower;
import main.java.exceptions.engine.InvalidParameterForConcreteTypeException;
import main.java.exceptions.engine.MonsterCreationFailureException;
import main.java.exceptions.engine.TowerCreationFailureException;
import main.java.schema.GameBlueprint;
import main.java.schema.GameSchema;
import main.java.schema.MonsterSchema;
import main.java.schema.MonsterSpawnSchema;
import main.java.schema.SimpleMonsterSchema;
import main.java.schema.SimpleTowerSchema;
import main.java.schema.TDObjectSchema;
import main.java.schema.TowerSchema;
import main.java.schema.WaveSpawnSchema;
import net.lingala.zip4j.exception.ZipException;

import com.google.gson.Gson;
import com.google.gson.stream.JsonReader;

public class Model {

    public static final String RESOURCE_PATH = "/main/resources/";

    private static final double DEFAULT_MONEY_MULTIPLIER = 0.5;

    private JGEngine engine;
    private TDObjectFactory factory;
    private Player player;
    private double gameClock;
    private Tower[][] towers;
    private List<Monster> monsters;
    private Gson gsonParser;
    private CollisionManager collisionManager;
    private GameState gameState;
    private DataHandler dataHandler;
    private LevelManager levelManager;


    public Model(JGEngine engine) {
        this.engine = engine;
        defineAllStaticImages();
        this.factory = new TDObjectFactory(engine);
        collisionManager = new CollisionManager(engine);

        levelManager = new LevelManager(factory);
        //TODO: Code entrance/exit logic into wave or monster spawn schema
        levelManager.setEntrance(0, engine.pfHeight()/2);
        levelManager.setExit(engine.pfWidth()/2, engine.pfHeight()/2);
        
        
        this.gsonParser = new Gson();
        this.gameClock = 0;
        monsters = new ArrayList<Monster>();
        towers = new Tower[engine.viewTilesX()][engine.viewTilesY()];
        gameState = new GameState();

        levelManager.setEntrance(0, engine.pfHeight()/2);
        levelManager.setExit(engine.pfWidth()/2, engine.pfHeight()/2);
	loadGameBlueprint(null);// TODO: REPLACE
	dataHandler = new DataHandler();

    }
    
    private void defineAllStaticImages () {
        engine.defineImage(Exit.NAME, "-", 1, RESOURCE_PATH + Exit.IMAGE_NAME, "-");
        //make bullet image dynamic
        engine.defineImage("red_bullet", "-", 1, RESOURCE_PATH + "red_bullet.png", "-");
    }

    /**
     * Add a new player to the engine
     */
    public void addNewPlayer() {
    	this.player = new Player();
    	levelManager.registerPlayer(player);
    }

    public void removeMonster(Monster m){
        monsters.remove(m);
    }
    
    /**
     * Add a tower at the specified location. If tower already exists in that cell, do nothing.
     * @param x	x coordinate of the tower
     * @param y	y coordinate of the tower
     */
    public boolean placeTower(double x, double y) {
        try {   
              
    		Point2D location = new Point2D.Double(x, y);
    	        int[] currentTile = getTileCoordinates(location);
    		// if tower already exists in the tile clicked, do nothing
    		if(isTowerPresent(currentTile)) return false;
    		
        	Tower newTower = factory.placeTower(location, "test-tower-1");
        	
        	if(player.getMoney() >= newTower.getCost() ) {
        	        //FIXME: Decrease money?
        		player.addMoney(-newTower.getCost());
        		towers[currentTile[0]][currentTile[1]]  = newTower;
        		return true;
        	} else {
        		destroyTower(newTower);
        		return false;
        	}
        	
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return false;
        
    }

	/**
	 * Force destroy a tower
	 * @param tower
	 */
	private void destroyTower(Tower tower) {
		tower.setImage(null);
		tower.remove();
	}

    /**
     * Return a two element int array with the tile coordinates that a given point is on, for use with Tower[][]
     * @param location
     * @return the row, col of the tile on which the location is situated
     */
    private int[] getTileCoordinates(Point2D location) {
        int curXTilePos = (int) (location.getX()/engine.tileWidth());
        int curYTilePos = (int) (location.getY()/engine.tileHeight());
        return new int[]{curXTilePos, curYTilePos};
    }
    
    /**
     * Check if there's a tower present at the specified coordinates
     * @param coordinates
     * @return true if there is a tower
     */
    private boolean isTowerPresent(int[] coordinates) {
    	return towers[coordinates[0]][coordinates[1]]!=null;
    }
    
    /**
     * Check if there's a tower present at the specified coordinates
     * This is mainly for the view to do a quick check
     * @param x
     * @param y
     * @return true if there is a tower
     */
    public boolean isTowerPresent(double x, double y) {
    	return isTowerPresent(getTileCoordinates(new Point2D.Double(x, y)));
    }
    
    /**
     * Check if the current location contains any tower. If yes, remove it. If no, do nothing 
     * @param x
     * @param y
     */
    public void checkAndRemoveTower(int x, int y) {
    	int[] coordinates = getTileCoordinates(new Point2D.Double(x, y));
    	if (isTowerPresent(coordinates)){
    		int xtile = coordinates[0];
    		int ytile = coordinates[1];
    		player.addMoney(DEFAULT_MONEY_MULTIPLIER * towers[xtile][ytile].getCost());
    		towers[xtile][ytile].remove();
    		towers[xtile][ytile] = null;
    	}
    }
    
    /**
    /**
     * Loads a map/terrain into the engine.
     *
     * @param fileName The name of the file which contains the map information
     */
    public void loadMap(String fileName) {
        try {
            JsonReader reader = new JsonReader(new InputStreamReader(getClass().getResourceAsStream(RESOURCE_PATH + fileName)));

            TDMap map = gsonParser.fromJson(reader, TDMap.class);
            map.loadIntoGame(engine);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    //TODO: use this instead of other one, will change
    public void loadMapTest(String fileName) {
        try {
            TDMap tdMap = new TDMap();
            tdMap.loadMapIntoGame(engine, fileName);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Loads the game schemas from GameBlueprint and sets the appropriate state
     *
     * @param bp
     * @throws InvalidParameterForConcreteTypeException 
     */
    public void loadGameBlueprint(GameBlueprint bp) {
//    	Map<String, String> gameAttributes = bp.getMyGameSchema().getAttributes();
//    	player = new Player(gameAttributes.get(GameSchema.MONEY), gameAttributes.get(GameSchema.LIVES));

//      GameSchema testGameSchema = new GameSchema();
//      Map<String, String> gameSchemaMap = testGameSchema.getAttributesMap();
//      player = new Player(gameSchemaMap.get(GameSchema.GOLD), gameSchemaMap.get(GameSchema.LIVES));
        player = new Player();

        List<TDObjectSchema> tdObjectSchemas = new ArrayList<>();

        SimpleTowerSchema testTowerSchema = new SimpleTowerSchema();
        testTowerSchema.addAttribute(TowerSchema.NAME, "test-tower-1");
        testTowerSchema.addAttribute(TDObjectSchema.IMAGE_NAME, "tower.gif");
        testTowerSchema.addAttribute(TowerSchema.COST, (double) 10);

        tdObjectSchemas.add(testTowerSchema);

        SimpleMonsterSchema testMonsterSchema = new SimpleMonsterSchema();
        testMonsterSchema.addAttribute(MonsterSchema.NAME, "test-monster-1");
        testMonsterSchema.addAttribute(TDObjectSchema.IMAGE_NAME, "monster.png");
        testMonsterSchema.addAttribute(MonsterSchema.REWARD, (double) 200);
        tdObjectSchemas.add(testMonsterSchema);

        factory.loadTDObjectSchemas(tdObjectSchemas);

        levelManager.addNewWave(createTestWave(testMonsterSchema, 1));
        levelManager.addNewWave(createTestWave(testMonsterSchema, 2));
        levelManager.addNewWave(createTestWave(testMonsterSchema, 3));
    }
    
    
    /**
     * Creates a wave of simple monsters for sans-factory testing ...
     * @param m1
     * @param swarmSize
     * @return
     */
    private WaveSpawnSchema createTestWave (SimpleMonsterSchema m1, int swarmSize) {
        MonsterSpawnSchema mschema = new MonsterSpawnSchema("SimpleMonster", m1, swarmSize);
        WaveSpawnSchema wschema = new WaveSpawnSchema();
        wschema.addMonsterSchema(mschema);
        return wschema;
    }
    
    
    /**
     * Loads game schemas from the GameBlueprint obtained from the filePath
     * @param fileName
     * @throws IOException 
     * @throws ClassNotFoundException 
     */
    public void loadGameSchemas(String filePath) throws ClassNotFoundException, IOException	{
		GameBlueprint bp = null;
		try {
			bp = dataHandler.loadBlueprint(filePath);
		} catch (ZipException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    	Map<String, Serializable> gameAttributes = bp.getMyGameScenario().getAttributesMap();
    	player = new Player((Integer) gameAttributes.get(GameSchema.MONEY), (Integer) gameAttributes.get(GameSchema.LIVES));
    }

    /**
     * Reset the game clock
     */
    public void resetGameClock() {
    	this.gameClock = 0;
    }
    

    
    public void addScore(double score) {
    	player.addScore(score);
    }
    
    /**
     * Get the score of the player
     * @return current score
     */
    public double getScore() {
    	return player.getScore();
    }
    
    /**
     * Check whether the game is lost
     * @return true if game is lost
     */
    public boolean isGameLost() {
    	if (getPlayerLives() <= 0) return true;
    	return false;
    }
    
    private void updateGameClockByFrame() {
    	this.gameClock++;
    }
    
    /**
     * Get the game clock
     * @return current game clock
     */
    public double getGameClock() {
    	return this.gameClock;
    }
    
    /**
     * Get the number of remaining lives of the player
     * @return number of lives left
     */
    public int getPlayerLives() {
    	return player.getLivesRemaining();
    }
    
    /**
     * Get the amount of money obtained by the player
     * @return current amount of money
     */
    public int getMoney() {
    	return player.getMoney();
    }
    /**
     * Loads a wave spawn schema into the model
     *
     * @param fileName The name of the JSON file containing wave spawn schema info
     */
    public void loadWaveSpawnSchema(String fileName) {
        try {
            JsonReader reader = new JsonReader(new InputStreamReader(getClass().getResourceAsStream(RESOURCE_PATH + fileName)));
            WaveSpawnSchema newWave = gsonParser.fromJson(reader, WaveSpawnSchema.class);
            levelManager.addNewWave(newWave); 
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    private boolean isGameWon() {
    	return levelManager.checkAllWavesFinished();
    }


    /**
     *  Spawns a new wave 
     * @throws MonsterCreationFailureException 
     */
    public void doSpawnActivity() throws MonsterCreationFailureException {
        
     //at determined intervals:
     //   if (gameClock % 100 == 0)
     //or if previous wave defeated:
        if(monsters.isEmpty())
            monsters.addAll(levelManager.spawnNextWave());
        
    }
    
	/**
	 * The model's "doFrame()" method that updates all state, spawn monsters,
	 * etc.
	 * @throws MonsterCreationFailureException 
	 */
	public void updateGame() throws MonsterCreationFailureException {
		updateGameClockByFrame();
		doSpawnActivity();
		doTowerFiring();
		removeDeadMonsters();
		gameState.updateGameStates(monsters, towers, levelManager.getCurrentWave(), levelManager.getAllWaves(), gameClock, 
				player.getMoney(), player.getLivesRemaining(), player.getScore());
	}

	/**
	 * Clean up dead monsters from monsters list and JGEngine display.
	 */
	private void removeDeadMonsters() {
		Iterator<Monster> monsterIter = monsters.iterator();
		while(monsterIter.hasNext()) {
			Monster currentMonster = monsterIter.next();
			if (currentMonster.isDead()) {
				monsterIter.remove();
				addMoney(currentMonster.getMoneyValue());
				currentMonster.remove();
			}
		}
	}

	private void addMoney(double moneyValue) {
		player.addMoney(moneyValue);
	}

	/**
	 * Call this to make each of the Towers execute firing logic
	 */
    private void doTowerFiring () {
        
            for (Tower[] towerRow : towers) {
                for (Tower t : towerRow) {
                    if (t != null) {
                        Point2D monsterCoor =
                                getNearestMonsterCoordinate(new Point2D.Double(t.x, t.y));
                        t.checkAndfireProjectile(monsterCoor);
                    }
                }
			}
		

	}

	/**
	 * Returns the coordinate of the monster nearest to the coordinate passed in
	 * @param towerCoor 
	 * @return coordinates of the nearest monster in the form of a Point2D object
	 */
	private Point2D getNearestMonsterCoordinate(Point2D towerCoor) {
		double minDistance = Double.MAX_VALUE;
		Point2D closestMonsterCoor = null;
		for(Monster m : monsters) {
			if(m.getCurrentCoor().distance(towerCoor) < minDistance) {
				minDistance = m.getCurrentCoor().distance(towerCoor);
				closestMonsterCoor = centerCoordinate(m);
			}
 		}
		return closestMonsterCoor;
	}

	/**
	 * Returns the center of the object for targeting 
	 * @param m object coordinate
	 * @return the center of the objects image according to the imageBBox
	 */
	private Point2D centerCoordinate(Monster m) {
		return new Point2D.Double(m.getCurrentCoor().getX()+m.getImageBBoxConst().width/2,
				m.getCurrentCoor().getY()+m.getImageBBoxConst().height/2);
	}

	/**
	 * Check all collisions specified by the CollisionManager
	 */
    public void checkCollisions() {
    	collisionManager.checkAllCollisions();
    }
    
    /**
     * Upgrade the tower at the specified coordinates
     * @param x
     * @param y
     * @return
     * @throws TowerCreationFailureException
     */
    public boolean upgradeTower(double x, double y) throws TowerCreationFailureException {
    	int[] coordinates = getTileCoordinates(new Point2D.Double(x, y));
    	if (isTowerPresent(coordinates)){
    		int xtile = coordinates[0];
    		int ytile = coordinates[1];
    		towers[xtile][ytile].remove();
    		Tower newTower = factory.placeTower(new Point2D.Double(x, y), "test tower 2");
    		//System.out.println(newTower.x);
    		towers[xtile][ytile] = newTower;
    		return true;
    	}
    	return false;
    }

    /**
     * Decrease player's lives by one.
     */
    public void decrementLives () {
       player.decrementLives();
    }

}
