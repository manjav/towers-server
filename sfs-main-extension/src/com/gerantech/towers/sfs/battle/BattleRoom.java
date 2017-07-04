package com.gerantech.towers.sfs.battle;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import com.gerantech.towers.sfs.battle.handlers.BattleRoomFightRequestHandler;
import com.gerantech.towers.sfs.battle.handlers.BattleRoomHitRequestHandler;
import com.gerantech.towers.sfs.battle.handlers.BattleRoomImproveRequestHandler;
import com.gerantech.towers.sfs.battle.handlers.BattleRoomLeaveRequestHandler;
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
	public static final int STATE_WAITING = 0;
	public static final int STATE_CREATED = 1;
	public static final int STATE_BATTLE_STARTED = 2;
	public static final int STATE_BATTLE_ENDED = 2;
	public static final int STATE_DESTROYED = 3;
	
	public int state = 0;
	public Timer autoJoinTimer;
	public List<User> regidteredPlayers;
	
	private int[] reservedPopulations;
	private int[] reservedTypes;
	private int[] reservedLevels;
	private int[] reservedTroopTypes;
	
	private Room room;
	private boolean singleMode;

	private Timer timer;
	private BattleField battleField;

	//private boolean destroyed;
	private boolean isQuest;
	private AIEnemy aiEnemy;

	private int timerCount = 11;

	private long startBattleAt;

	
	public void init() 
	{
		room = getParentRoom();
		
		addEventHandler(SFSEventType.ROOM_REMOVED, BattleRoomServerEventsHandler.class);
		addEventHandler(SFSEventType.USER_JOIN_ROOM, BattleRoomServerEventsHandler.class);
		//addEventHandler(SFSEventType.USER_LEAVE_ROOM, BattleRoomServerEventsHandler.class);
		
		addRequestHandler("h", BattleRoomHitRequestHandler.class);
		addRequestHandler("f", BattleRoomFightRequestHandler.class);
		addRequestHandler("i", BattleRoomImproveRequestHandler.class);
		addRequestHandler("leave", BattleRoomLeaveRequestHandler.class);
	}

	public void createGame(Game game, String mapName, Boolean isQuest) 
	{
		state = STATE_CREATED;
		this.isQuest = isQuest;
		regidteredPlayers = new ArrayList<User>();
	    for (int i=0; i < room.getPlayersList().size(); i++)
	    	regidteredPlayers.add( room.getPlayersList().get(i));
		
		startBattleAt = Instant.now().getEpochSecond();
		if(autoJoinTimer != null)
			autoJoinTimer.cancel();
		
		battleField = new BattleField(game, mapName, 0);
		reservedTypes = new int[battleField.places.size()];
		reservedLevels = new int[battleField.places.size()];
		reservedTroopTypes = new int[battleField.places.size()];
		reservedPopulations = new int[battleField.places.size()];
		
		singleMode = room.getMaxUsers()==1 || room.getUserByName("npc") != null;
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
		    	if(singleMode && state==STATE_BATTLE_STARTED && aiEnemy.doAction(timerCount % 15))
		    	{
		    		if(aiEnemy.actionType == "fight")
		    		{
		    			Object[] ss = new Object[aiEnemy.sources.size()];
						for(int j = 0; j<ss.length; j++)
							ss[j] = aiEnemy.sources.get(j);
    					//trace(singleMode, timerCount, aiEnemy.destination, aiEnemy.sources.get(0), aiEnemy.actionType);
    					fight(ss, aiEnemy.target);
		    		}
//		    		else if(aiEnemy.actionType == "improve")
//		    		{
//    					improveBuilding(sender, params)(ss, aiEnemy.target);
//		    		}
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
	        	int battleDuration = (int) ((int)Instant.now().getEpochSecond() - startBattleAt);
				if( battleDuration > battleField.map.times.get(2) || numBuildings[0] == 0 || numBuildings[1] == 0 )
					endBattle(numBuildings, battleDuration);
		    	
		    	timerCount ++;
			}


		}, 0, 500);
		
		trace("createGame");
	}
	

	public void leaveGame(User user) 
	{
		if(!isQuest)
			return;
	    
		state = STATE_BATTLE_ENDED;
		if(timer != null)
			timer.cancel();
		timer = null;		
		
	    for (int i=0; i < room.getPlayersList().size(); i++)
	    	if( room.getPlayersList().get(i).getId() == user.getId() ) 
	    		calculateEndBattleResponse(user, 0);
	}
	
	private void endBattle(int[] numBuildings, int battleDuration)
	{
		state = STATE_BATTLE_ENDED;
		if(timer != null)
			timer.cancel();
		timer = null;
		
		trace("Battle Ended", "b0:"+numBuildings[0], "b1:"+numBuildings[1], "duration:"+battleDuration, "("+battleField.map.times.get(0)+","+battleField.map.times.get(1)+","+battleField.map.times.get(2)+")");
		
	    for (int i=0; i < room.getPlayersList().size(); i++)
	    {
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
	        calculateEndBattleResponse( room.getPlayersList().get(i), score );
	    }
	}

	private void calculateEndBattleResponse(User user, int score)
	{
        // consume outcomes and set quest score
		IntIntMap outcomes = null;
    	Player player = ((Game)user.getSession().getProperty("core")).player;
    	try 
    	{
    		trace("isQuest", isQuest, player.quests.get( battleField.map.index ) , score);
        	if ( isQuest)
        	{
		        // get outcomes
		        outcomes = BattleOutcome.get_outcomes(player, battleField.map, score);
        		if( player.quests.get( battleField.map.index ) < score )
        		{
        			UserManager.setQuestScore(getParentZone().getExtension(), player, battleField.map.index, score);
        			player.quests.set(battleField.map.index, score);
 
		        	BattleOutcome.consume_outcomes(player, outcomes);
		        	UserManager.updateResources(getParentZone().getExtension(), player, outcomes.keys());
    		    	sendEndBattleResponse(user, outcomes, score);
        		}
    		}
        	else
        	{
        		trace("score", score);
        		if(score > 0)
        		{
        		    for (int i=0; i < room.getPlayersList().size(); i++)
        		    {
        		    	score = (user.getId()==room.getPlayersList().get(i).getId()?1:-1) * score;
        		    	outcomes = BattleOutcome.get_outcomes(player, battleField.map, score);
			        	BattleOutcome.consume_outcomes(player, outcomes);
			        	UserManager.updateResources(getParentZone().getExtension(), player, outcomes.keys());
        		    	sendEndBattleResponse(room.getPlayersList().get(i), outcomes, score);
        		    }
        		}
        	}
        	
		}
    	catch (Exception e) {
			trace(e.getMessage());
		}
    		
	}

	private void sendEndBattleResponse(User user, IntIntMap outcomes, int score)
	{
    	// provide sfs rewards map
    	SFSObject sfsO = SFSObject.newInstance();
    	
    	sfsO.putInt("id", ((Game)user.getSession().getProperty("core")).player.id);
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
    	
        sfsO.putBool( "youWin", score>0 );
        sfsO.putInt( "score", score );trace(sfsO.getDump());
    	send( "endBattle", sfsO, user );		
	}

	public void fight(Object[] objects, int destination) 
	{
		state = STATE_BATTLE_STARTED;
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
		state = STATE_BATTLE_STARTED;
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
		if(state >= STATE_DESTROYED)
			return;
			
		state = STATE_DESTROYED;
		
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