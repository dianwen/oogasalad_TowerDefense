package main.java.schema;

import main.java.engine.objects.tower.Tower;

public abstract class TowerSchema extends TDObjectSchema {
	
    public static final String LOCATION = "location";
    public static final String HEALTH = "health";
    public static final String DAMAGE = "damage";
    public static final String RANGE = "range";
    public static final String COST = "cost";
    public static final String BUILDUP = "buildup";
	
    protected TowerSchema(Class<? extends Tower> myConcreteType) {
        super(myConcreteType);
		myAttributeSet.add(TowerSchema.BUILDUP);
		myAttributeSet.add(TowerSchema.COST);
		myAttributeSet.add(TowerSchema.DAMAGE);
		myAttributeSet.add(TowerSchema.HEALTH);
		myAttributeSet.add(TowerSchema.RANGE);
		myAttributeSet.add(TowerSchema.LOCATION);
		myAttributeSet.add(TowerSchema.NAME);
    }
}
