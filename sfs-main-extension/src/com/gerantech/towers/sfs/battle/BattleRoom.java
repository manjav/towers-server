package com.gerantech.towers.sfs.battle;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import com.gerantech.towers.sfs.battle.handlers.BattleRoomFightRequestHandler;
import com.gerantech.towers.sfs.battle.handlers.BattleRoomHitRequestHandler;
import com.gerantech.towers.sfs.battle.handlers.BattleRoomImproveRequestHandler;
import com.gerantech.towers.sfs.battle.handlers.BattleRoomServerEventsHandler;
import com.gerantech.towers.sfs.utils.UserManager;
import com.gt.towers.Game;
import com.gt.towers.Player;
import com.gt.towers.battle.AIEnemy;
import com.gt.towers.battle.BattleField;
import com.gt.towers.battle.BattleOutcome;
import com.gt.towers.buildings.Building;
import com.gt.towers.utils.GTimer;
import com.gt.towers.utils.maps.IntIntMap;
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
	private int[] reservedLevels;
	private int[] reservedTroopTypes;
	
	private Room room;
	private Timer timer;
	private boolean destroyed;
	private BattleField battleField;

	private Boolean isQuest;
	private boolean singleMode;
	private AIEnemy aiEnemy;
	private int timerCount = 11;

	private long startBattleAt;


	
	public void init() 
	{
		room = getParentRoom();
		
		addEventHandler(SFSEventType.ROOM_REMOVED, BattleRoomServerEventsHandler.class);
		addEventHandler(SFSEventType.USER_JOIN_ROOM, BattleRoomServerEventsHandler.class);
		addEventHandler(SFSEventType.USER_LEAVE_ROOM, BattleRoomServerEventsHandler.class);
		
		addRequestHandler("h", BattleRoomHitRequestHandler.class);
		addRequestHandler("f", BattleRoomFightRequestHandler.class);
		addRequestHandler("i", BattleRoomImproveRequestHandler.class);
	}

	public void createGame(Game game, String mapName, Boolean isQuest) 
	{
		this.isQuest = isQuest;
		
		startBattleAt = Instant.now().getEpochSecond();
		if(autoJoinTimer != null)
			autoJoinTimer.cancel();
		
		battleField = new BattleField(game, mapName, 0);
		reservedTypes = new int[battleField.places.size()];
		reservedLevels = new int[battleField.places.size()];
		reservedTroopTypes = new int[battleField.places.size()];
		reservedPopulations = new int[battleField.places.size()];
		
		singleMode = room.getMaxUsers() == 1;
		if(singleMode)
			aiEnemy = new AIEnemy(battleField);
		
		for(int i = 0; i<battleField.places.size(); i++)
		{
			reservedTypes[i] = battleField.places.get(i).building.type;
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
						vars.addText(i+","+b.get_population()+","+b.troopType);
						//trace(i+","+b.get_population()+","+b.troopType);
					}
					
					if(b.level != reservedLevels[i] || b.type != reservedTypes[i] )
					{
						sendImproveResponse(i, b.type, b.level);
						reservedTypes[i] = b.type;
						reservedLevels[i] = b.level;
					}
				}
				
				if(vars.size() > 0)
				{
					// Set variables
					List<RoomVariable> listOfVars = new ArrayList<RoomVariable>();
					listOfVars.add( new SFSRoomVariable("towers", vars) );
					sfsApi.setRoomVariables(null, room, listOfVars);
				}

				// fight enemy bot
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
		    	
		    	// check ending battle
		    	int[] numBuildings = new int[2];
		    	int[] populations = new int[2];
				for(int i = 0; i<reservedTroopTypes.length; i++)
				{
					if( reservedTroopTypes[i] >= 0 )
					{
						//trace(i, reservedTroopTypes[i], reservedPopulations[i]);
						numBuildings[reservedTroopTypes[i]] ++;
						populations[reservedTroopTypes[i]] += reservedPopulations[i];
					}
				}
	        	int battleDuration = (int) ((int)Instant.now().getEpochSecond() - startBattleAt) / 1000;
				if( battleDuration > battleField.map.times.get(2) || numBuildings[0] == 0 || numBuildings[1] == 0 )
					endBattle(numBuildings, battleDuration);
		    	
		    	timerCount ++;
			}


		}, 0, 500);
		
		trace("createGame");
	}
	private void endBattle(int[] numBuildings, int battleDuration)
	{
		trace("Battle Ended", "b0:"+numBuildings[0], "b1:"+numBuildings[1], "duration:"+battleDuration, "("+battleField.map.times.get(0)+","+battleField.map.times.get(1)+","+battleField.map.times.get(2)+")");
		
	    for (int i=0; i < room.getPlayersList().size(); i++)
	    {
	    	SFSObject sfsO = SFSObject.newInstance();
	    	User user = room.getPlayersList().get(i);
        	Player player = ((Game)user.getSession().getProperty("core")).player;
        	
        	int score = 0;
	        Boolean wins = numBuildings[i]>numBuildings[i==1?0:1] && battleDuration < battleField.map.times.get(2);
	        if(wins)
	        {
	        	if(battleDuration < battleField.map.times.get(0))
	        		score = 3;
	        	else if(battleDuration < battleField.map.times.get(1))
	        		score = 2;
	        	else
	        		score = 1;
	        }
	        
	        // get outcomes
	        IntIntMap outcomes = BattleOutcome.get_outcomes(player, battleField.map, score);
	        
	        // consume outcomes and set quest score
        	try 
        	{
        		BattleOutcome.consume_outcomes(player, outcomes);
		        UserManager.updateResources(getParentZone().getExtension(), player, outcomes.keys());
				
	        	if ( isQuest )
	        	{
					UserManager.setQuestScore(getParentZone().getExtension(), player, battleField.map.index, score);
					player.quests.set(battleField.map.index, score);
	        	}
			}
        	catch (Exception e) {
				trace(e.getMessage());
			}
        	
        	// provide sfs rewards map
        	SFSObject sfsReward = null;
        	SFSArray sfsRewards  = new SFSArray();
        	for(int r : outcomes.keys())
        	{
        		sfsReward = new SFSObject();
        		sfsReward.putInt("t", r);
        		sfsReward.putInt("c", outcomes.get(r));
        		sfsRewards.addSFSObject(sfsReward);
        	}
        	sfsO.putSFSArray("rewards", sfsRewards);
        	
	        sfsO.putBool( "youWin", wins );
	        sfsO.putInt( "score", score );
	    	send( "endBattle", sfsO, user );
	    }
	    
		if(timer != null)
			timer.cancel();
		timer = null;		
	}

	public void fight(Object[] objects, int destination) 
	{
		SFSArray srcs = SFSArray.newInstance();
		
		for(int i = 0; i<objects.length; i++)
		{
			//trace("fight", (Integer)objects[i], "to", destination);
			srcs.addInt((Integer)objects[i]);
			battleField.places.get((Integer)objects[i]).fight(battleField.places.get(destination), battleField.places);
		}
		
		// Set variables
		List<RoomVariable> listOfVars = new ArrayList<RoomVariable>();
		listOfVars.add( new SFSRoomVariable("s", srcs) );
		listOfVars.add( new SFSRoomVariable("d", destination) );
		sfsApi.setRoomVariables(null, room, listOfVars);
	}

	// improve =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-
	public void improveBuilding(User sender, ISFSObject params) 
	{
		Building b = battleField.places.get(params.getInt("i")).building;
		trace("improve", params.getDump(), b.improvable(params.getInt("t")));
		b.improve(params.getInt("t"));
	}
	private void sendImproveResponse(int index, int type, int level)
	{
		SFSObject params = SFSObject.newInstance();
		params.putInt("i", index);
		params.putInt("t", type);
		params.putInt("l", level);
		sfsApi.sendExtensionResponse("i", params, room.getPlayersList(), room, false);		
	}
	// hit =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-
	public void hit(int troopId)
	{
		int index = (int) Math.floor((double)(troopId/10000));//trace("hit", index, troopId);
		battleField.places.get(index).killTroop(troopId);
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
		timer = null;
		
		if(autoJoinTimer != null)
			autoJoinTimer.cancel();
		autoJoinTimer = null;
		
		GTimer.stopAll();
		clearAllHandlers();
		
		trace("destroyGame");
	}
}