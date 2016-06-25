package com.mygdx.game.units;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Vector2;
import com.mygdx.engine.Engine;
import com.mygdx.engine.Renderer;
import com.mygdx.engine.Scene;
import com.mygdx.engine.Transform;
import com.mygdx.engine.UI;
import com.mygdx.game.units.BasicStateMachine.StateName;
import com.mygdx.game.units.Unit.Mode;
import com.mygdx.ia.BotScript;
import com.mygdx.ia.Path;
import com.mygdx.ia.Pathfinding;
import com.mygdx.ia.behaviours.delgate.PathFollowing;
import com.mygdx.ia.statemachine.Action;
import com.mygdx.ia.statemachine.Condition;
import com.mygdx.ia.statemachine.State;
import com.mygdx.ia.statemachine.StateMachine;
import com.mygdx.ia.statemachine.Transition;

public class TacticStateMachine extends StateMachine {

	private long lastTime;
	private float timeout; // en segundos
	
	public TacticStateMachine(final Unit unit) {
		
		this.timeout = 3;
		this.lastTime = 0;

		/*
		 * STATES
		 */
		
		// BASIC
		final State basicState = new State();
		
		basicState.setStateMachine(new BasicStateMachine(unit));
		
		basicState.setEntryAction(new Action() {
			
			@Override
			public void run() {
				
				/*
				 * Cada vez que entramos a un estado nuevo, renovamos el contador del tiempo.
				 */
				long now = System.currentTimeMillis();
				lastTime = now;
				
			}
		});
		
		basicState.setExitAction(new Action() {
			
			@Override
			public void run() {
				unit.setMode(Mode.NONE);
				
			}
		});
		
		//OFFENSIVE
		State offensiveState = new State();
		
		offensiveState.setEntryAction(new Action() {
			
			@Override
			public void run() {
				
				long now = System.currentTimeMillis();
				lastTime = now;
				
				unit.onOffensive();
				
			}
		});
		
		//DEFENSIVE
		State defensiveState = new State();
		
		defensiveState.setEntryAction(new Action() {
			
			@Override
			public void run() {
				
				long now = System.currentTimeMillis();
				lastTime = now;
				
				unit.onDefensive();
				
			}
		});
		
		//RUNAWAY
		State runAwayState = new State();
		
		runAwayState.setEntryAction(new Action() {
			
			@Override
			public void run() {
				
				long now = System.currentTimeMillis();
				lastTime = now;
				
			}
		});
		
		runAwayState.setAction(new Action() {
			
			@Override
			public void run() {
				Vector2 pos = unit.getComponent(Transform.class).position;
				float w = unit.getComponent(Renderer.class).getSprite().getWidth();
				UI.getInstance().drawTextWorld("RUNAWAY!", pos.x-(w/2),pos.y-(1*Scene.SCALE), Color.BLUE);
				
			}
		});
		
		
		runAwayState.setEntryAction(new Action() {
			
			@Override
			public void run() {
				
				
				unit.onRunaway();
				
				Path path = new Path(0.5f*Scene.SCALE);
				
				BotScript bot = unit.getComponent(BotScript.class);
				
				Vector2 botPosition = bot.getPosition();
				
				// PATHFINDING
				Pathfinding pathfinding = new Pathfinding(Engine.getInstance().getCurrentScene(), unit, botPosition ,unit.getDestiny(), unit.getTerrainMap());
				path.setParams(pathfinding.generate());
				
//				bot.clearBehaviours(PathFollowing.class);
				bot.clearSingleBehaviours();
				
				if( ! path.isEmpty())
					bot.addBehaviour(new PathFollowing(bot, path, 0f));
				
				unit.setDestiny(null);
				unit.setMode(Mode.NONE);
								
			}
		});
		
		
		/*
		 * TRANSITIONS
		 */
		
		
		
		
		
		//BASIC -> OFFENSIVE
		Transition basicToOffensive = new Transition(offensiveState);
		
		basicToOffensive.setCondition(new Condition() {
			
			@Override
			public boolean test() {
				
				long now = System.currentTimeMillis();
				
				if((now-lastTime)/1000 > timeout){
					
					if(((BasicStateMachine)basicState.getStateMachine()).getCurrentState() == StateName.WAIT){
						lastTime = now;
						
						return true;
						
					}
				}
				
				return unit.getMode() == Mode.OFFENSIVE;
			}
		});
		
		basicState.addTransition(basicToOffensive);
		
		//BASIC -> DEFENSIVE
		Transition basicToDefensive = new Transition(defensiveState);
		
		basicToDefensive.setCondition(new Condition() {
			
			@Override
			public boolean test() {
				return unit.getMode() == Mode.DEFENSIVE;
			}
		});
		
		basicState.addTransition(basicToDefensive);
	
		//BASIC -> RUNAWAY
		Transition basicToRunaway = new Transition(runAwayState);
		
		basicToRunaway.setCondition(new Condition() {
			
			@Override
			public boolean test() {
				// si nuestra vida es baja
				return unit.getLife() < unit.getMaxLife()*0.5;
			}
		});
		
		basicState.addTransition(basicToRunaway);
		
		//OFFENSIVE -> BASIC
		Transition offensiveToBasic = new Transition(basicState);
		
		offensiveToBasic.setCondition(new Condition() {
			
			@Override
			public boolean test() {
				return true;
			}
		});
		
		offensiveToBasic.setAction(new Action() {
			
			@Override
			public void run() {
				unit.setMode(Mode.NONE);
				
			}
		});
		
		offensiveState.addTransition(offensiveToBasic);
		
		//DEFENSIVE -> BASIC
		Transition defensiveToBasic = new Transition(basicState);
		
		defensiveToBasic.setCondition(new Condition() {
			
			@Override
			public boolean test() {
				return true;
			}
		});
		
		defensiveToBasic.setAction(new Action() {
			
			@Override
			public void run() {
				unit.setMode(Mode.NONE);
				
			}
		});
		
		defensiveState.addTransition(defensiveToBasic);
		
		//RUNAWAY -> BASIC
		Transition runawayToBasic = new Transition(basicState);
		
		runawayToBasic.setCondition(new Condition() {
			
			@Override
			public boolean test() {
		
				// si nos hemos curado.
				return (unit.getLife() > unit.getMaxLife()*0.5);
			}
		});
		
		runawayToBasic.setAction(new Action() {
			
			@Override
			public void run() {
				unit.getComponent(BotScript.class).clearSingleBehaviours();
				
			}
		});
		
		runAwayState.addTransition(runawayToBasic);
		
		
		
		
		
		
		/*
		 * ADD STATES
		 */
		
		this.addInitialState(basicState);
		this.addState(offensiveState);
		this.addState(defensiveState);
		this.addState(runAwayState);
	}
}
