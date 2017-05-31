package com.gerantech.towers.sfs.battle;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import com.gerantech.towers.sfs.battle.handlers.BattleRoomFightRequestHandler;
import com.gerantech.towers.sfs.battle.handlers.BattleRoomServerEventsHandler;
import com.gerantech.towers.sfs.battle.handlers.BattleRoomUpgradeRequestHandler;
import com.gt.towers.battle.AIEnemy;
import com.gt.towers.battle.BattleField;
import com.gt.towers.buildings.Building;
import com.gt.towers.utils.GTimer;
import com.smartfoxserver.v2.core.SFSEventType;
import com.smartfoxserver.v2.entities.Room;
import com.smartfoxserver.v2.entities.User;
import com.smartfoxserver.v2.entities.data.ISFSObject;
import com.smartfoxserver.v2.entities.data.SFSArray;
import com.smartfoxserver.v2.entities.data.SFSObject;
import com.smartfoxserver.v2.entities.variables.RoomVariable;
import com.smartfoxserver.v2.entities.variables.SFSRoomVariable;
import com.smartfoxserver.v2.extensions.SFSExtension;

public class BattleRoom extends SFSExtension 
{
	public Timer autoJoinTimer;
	
	private int[] reservedPopulations;
	private int[] reservedTypes;
	
	private Timer timer;
	private boolean destroyed;
	private BattleField battleField;

	private Room room;
	private int[] reservedLevels;
	private int[] reservedTroopTypes;
	private boolean singleMode;
	private AIEnemy aiEnemy;
	private int timerCount = 11;
	
	
	
	public void init() 
	{
		room = getParentRoom();
		
		addEventHandler(SFSEventType.USER_JOIN_ROOM, BattleRoomServerEventsHandler.class);
		addEventHandler(SFSEventType.USER_LEAVE_ROOM, BattleRoomServerEventsHandler.class);
		addEventHandler(SFSEventType.ROOM_REMOVED, BattleRoomServerEventsHandler.class);
		
		addRequestHandler("f", BattleRoomFightRequestHandler.class);
		addRequestHandler("u", BattleRoomUpgradeRequestHandler.class);
	}


	public void createGame() 
	{
		if(autoJoinTimer != null)
			autoJoinTimer.cancel();
		
		battleField = new BattleField();
		reservedTypes = new int[battleField.places.size()];
		reservedLevels = new int[battleField.places.size()];
		reservedTroopTypes = new int[battleField.places.size()];
		reservedPopulations = new int[battleField.places.size()];
		
		singleMode = room.getMaxUsers() == 1;
		if(singleMode)
			aiEnemy = new AIEnemy(battleField);
		
		for(int i = 0; i<battleField.places.size(); i++)
		{
			reservedTypes[i] = battleField.places.get(i).building.get_type();
			reservedLevels[i] = battleField.places.get(i).building.level;
		}
		
    	timer = new Timer();
    	timer.schedule(new TimerTask() {
			@Override
			public void run() {
				Building b = null;
				SFSArray vars = SFSArray.newInstance();
				for(int i = 0; i<battleField.places.size(); i++)
				{
					b = battleField.places.get(i).building;
					if(b.get_population() != reservedPopulations[i] || b.troopType != reservedTroopTypes[i])
					{
						reservedPopulations[i] = b.get_population();
						reservedTroopTypes[i] = b.troopType;
						//reservedLevels[i] = b.level;
						vars.addText(i+","+b.get_population()+","+b.troopType);
						//trace(i+","+b.get_population()+","+b.troopType);
					}
					
					if(b.level != reservedLevels[i] || b.get_type() != reservedTypes[i] )
					{
						sendImproveResponse(i, b.get_type(), b.level);
						reservedTypes[i] = b.get_type();
						reservedLevels[i] = b.level;
					}
				}
				
				if(vars.size() > 0)
				{
					List<RoomVariable> listOfVars = new ArrayList<RoomVariable>();
					listOfVars.add( new SFSRoomVariable("towers", vars) );

					// Set variables
					sfsApi.setRoomVariables(null, room, listOfVars);
				}
								// fight enemy
		    	if(singleMode && aiEnemy.doAction(timerCount % 15))
		    	{
		    		if(aiEnemy.actionType == "fight")
		    		{
		    			Object[] ss = new Object[aiEnemy.sources.size()];
						for(int j = 0; j<ss.length; j++)
							ss[j] = aiEnemy.sources.get(j);
    					//trace(singleMode, timerCount, aiEnemy.destination, aiEnemy.sources.get(0), aiEnemy.actionType);
    					fight(ss, aiEnemy.destination);
    					
//    					trace("dests", aiEnemy.destinations.size());
//						for(Object d:aiEnemy.destinations._list)
//							trace("dest", d);
		    		}
		    	}
		    	
		    	timerCount ++;
			}
		}, 0, 500);
		
		trace("createGame");
	}

	
	public void fight(Object[] objects, int destination) 
	{
		SFSArray srcs = SFSArray.newInstance();
		
		for(int i = 0; i<objects.length; i++)
		{
			//trace("fight", (Integer)objects[i], "to", destination);
			try {
				srcs.addInt((Integer)objects[i]);
				battleField.places.get((Integer)objects[i]).fight(battleField.places.get(destination), battleField.places);
			} catch (Exception e) {
				trace(e.getMessage());
			}
		}
		
		// Set variables
		List<RoomVariable> listOfVars = new ArrayList<RoomVariable>();
		listOfVars.add( new SFSRoomVariable("s", srcs) );
		listOfVars.add( new SFSRoomVariable("d", destination) );
		sfsApi.setRoomVariables(null, room, listOfVars);
	}

	public void improveBuilding(User sender, ISFSObject params) 
	{
		Building b = battleField.places.get(params.getInt("i")).building;
		trace("improve", params.getDump());
		if(!b.improve(params.getInt("t")))
			return;

		//sendImproveResponse(index, b.get_type(), b.level);
	}

	private void sendImproveResponse(int index, int type, int level)
	{
		SFSObject params = SFSObject.newInstance();
		params.putInt("i", index);
		params.putInt("t", type);
		params.putInt("l", level);
		sfsApi.sendExtensionResponse("u", params, room.getPlayersList(), room, false);		
	}


	@Override
	public void destroy()
	{
		destroyGame();
		super.destroy();
	}
	public void destroyGame()
	{
		if(destroyed)
			return;
		destroyed = true;
		
		if(timer != null)
			timer.cancel();
		
		if(autoJoinTimer != null)
			autoJoinTimer.cancel();
		
		GTimer.stopAll();
		clearAllHandlers();
		
		trace("destroyGame");
	}
}