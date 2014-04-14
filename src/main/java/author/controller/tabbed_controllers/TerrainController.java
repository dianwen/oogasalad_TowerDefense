package main.java.author.controller.tabbed_controllers;

import main.java.author.controller.MainController;
import main.java.author.controller.TabController;
import main.java.schema.map.GameMap;

public class TerrainController extends TabController {

	public TerrainController(MainController superController) {
		super(superController);
		// TODO Auto-generated constructor stub
	}
	
	public void addMaps(GameMap gameMap) {
		mySuperController.addGameMapsToModel(gameMap);
	}

}
