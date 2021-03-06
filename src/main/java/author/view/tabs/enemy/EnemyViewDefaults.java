package main.java.author.view.tabs.enemy;

import java.io.Serializable;

import main.java.schema.tdobjects.MonsterSchema;

/**
 * @author garysheng Some default constants for the EnemyEditorTab
 */
public class EnemyViewDefaults {

	public static final int HEALTH_DEFAULT = 100;
	public static final int SPEED_DEFAULT = 1;
	public static final int DAMAGE_DEFAULT = 10;
	public static final int REWARD_DEFAULT = 5;
	public static final String FLYING_OR_GROUND_DEFAULT = MonsterSchema.GROUND;
	public static final String TILE_SIZE_DEFAULT = MonsterSchema.TILE_SIZE_SMALL;
	public static final String ENEMY_DEFAULT_IMAGE = "monster.png";
	public static final int RESURRECT_QUANTITY = 0;

}
