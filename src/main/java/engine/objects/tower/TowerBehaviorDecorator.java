package main.java.engine.objects.tower;

import java.awt.geom.Point2D;
import java.io.Serializable;
import main.java.engine.EnvironmentKnowledge;


/**
 * Abstract decorator class to "wrap" layers of behaviors/functionality around towers.
 * Allows mix and match of tower behaviors, e.g. shooting and money farming.
 * @author Austin
 *
 */
abstract class TowerBehaviorDecorator implements ITower, Serializable {
    /**
     * The base tower that will have behaviors added to it ("decorations")
     * This is not necessarily a SimpleTower/TDObject, but could be another TowerBehaviorDecorator!
     */
    protected ITower baseTower;

    public TowerBehaviorDecorator (ITower baseTower) {
        this.baseTower = baseTower;
    }

    @Override
    public boolean atInterval (int intervalFrequency) {
        return baseTower.atInterval(intervalFrequency);
    }

    @Override
    public double getXCoordinate () {
        return baseTower.getXCoordinate();
    }

    @Override
    public double getYCoordinate () {
        return baseTower.getYCoordinate();
    }

    @Override
    public double getCost () {
        return baseTower.getCost();
    }

    @Override
    public void remove () {
        baseTower.remove();
    }

    @Override
    public boolean callTowerActions (EnvironmentKnowledge environ) {
        if (baseTower.callTowerActions(environ)) {
            // in addition to base tower's behavior, also do additional behavior
            doDecoratedBehavior(environ);
            return true;
        }
        return false;
    }
    
    @Override
    public String getUpgradeTowerName () {
        return baseTower.getUpgradeTowerName();
    }
    
    @Override
    public Point2D centerCoordinate () {
        return baseTower.centerCoordinate();
    }
    
    
    /**
     * Do the additional behavior granted by this behavior decoration.
     * @param environ
     */
    abstract void doDecoratedBehavior (EnvironmentKnowledge environ);
    
}
