package main.java.engine.factories;

import main.java.engine.objects.monster.SimpleMonster;
import jgame.impl.JGEngineInterface;

public class MonsterFactory {
   private JGEngineInterface engine;

   public MonsterFactory(JGEngineInterface engine) {
        this.engine = engine;
    }
   
   public MonsterFactory() {
	   this(null);
   }

   public void placeMoster() {
       new SimpleMonster();
   }
 
}