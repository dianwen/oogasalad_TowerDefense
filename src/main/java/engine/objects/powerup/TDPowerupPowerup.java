package main.java.engine.objects.powerup;

import main.java.engine.EnvironmentKnowledge;
import main.java.engine.objects.TDObject;

/**
 * Abstract super class for tower defense one-time items.
 * Examples include: bomb, instant kill, freeze all monsters, etc. 
 * 
 * @author Lawrence
 *
 */
public abstract class TDPowerupPowerup extends TDObject {

	public static final int ITEM_CID = 3;
	
	protected double cost;
	protected double buildupTime;
	protected double timeCounter;
	private boolean isDead;
	protected double damage;
	protected String image;
	protected int flash_interval;

	public TDPowerupPowerup(String name, double x, double y, 
			String gfxname, double cost, double buildupTime,
			double damage, int flash_interval) {
		super(name, x, y, ITEM_CID, gfxname);
		this.cost = cost;
		this.buildupTime = buildupTime;
		this.timeCounter = 0;
		this.isDead = false;
		this.damage = damage;
		this.flash_interval = flash_interval;
	}
	
	/**
	 * This method defines what the object should do 
	 * and will be called upon creation of the object. 
	 * 
	 * @param environmentKnowledge
	 */
	public abstract void doAction(EnvironmentKnowledge environmentKnowledge);
	
	protected void terminateItem() {
		isDead = true;
	}
	
	public boolean isDead() {
		return isDead;
	}
	
	public double getCost() {
		return cost;
	}

}
