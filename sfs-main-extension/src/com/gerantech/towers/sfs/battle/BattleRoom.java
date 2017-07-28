package com.gerantech.towers.sfs.battle;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import com.gerantech.towers.sfs.battle.handlers.*;
import com.gerantech.towers.sfs.utils.UserManager;
import com.gt.towers.Game;
import com.gt.towers.battle.AIEnemy;
import com.gt.towers.battle.BattleField;
import com.gt.towers.battle.BattleOutcome;
import com.gt.towers.buildings.Building;
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
	public static final int STATE_BATTLE_ENDED = 3;
	public static final int STATE_DESTROYED = 4;
	
	public Timer autoJoinTimer;

	private int _state = -1;
	
	private int[] reservedPopulations;
	private int[] reservedTypes;
	private int[] reservedLevels;
	private int[] reservedTroopTypes;
	private int[] scores;
	
	private Room room;
	private Timer timer;
	private AIEnemy aiEnemy;
	private BattleField battleField;

	private boolean isQuest;
	private boolean singleMode;
	public int updaterCount = 1;
	private int timerCount = 11;
	private long startBattleAt;
	
	public void init() 
	{
		room = getParentRoom();
		setState( STATE_WAITING );
		
		addEventHandler(SFSEventType.ROOM_REMOVED, BattleRoomServerEventsHandler.class);
		addEventHandler(SFSEventType.USER_JOIN_ROOM, BattleRoomServerEventsHandler.class);
		addEventHandler(SFSEventType.USER_DISCONNECT, BattleRoomServerEventsHandler.class);
		
		addRequestHandler("h", BattleRoomHitRequestHandler.class);
		addRequestHandler("f", BattleRoomFightRequestHandler.class);
		addRequestHandler("i", BattleRoomImproveRequestHandler.class);
		addRequestHandler("ss", BattleRoomStickerRequestHandler.class);
		addRequestHandler("leave", BattleRoomLeaveRequestHandler.class);
		addRequestHandler("resetAllVars", BattleRoomResetVarsRequestHandler.class);
	}

	public void createGame(Game game, String mapName, Boolean isQuest, final boolean singleMode) 
	{
		if(autoJoinTimer != null)
			autoJoinTimer.cancel();

		setState( STATE_CREATED );
		this.isQuest = isQuest;
		this.singleMode = singleMode;

		ArrayList<String> registeredPlayersId = new ArrayList<String>();
		List<User> players = getPlayers();
        for (User u: players)
        	if(!u.isNpc())
				registeredPlayersId.add( u.getName());
        room.setProperty("registeredPlayersId", registeredPlayersId);
		
		startBattleAt = Instant.now().getEpochSecond();

		battleField = new BattleField(game, mapName, 0);
		reservedTypes = new int[battleField.places.size()];
		reservedLevels = new int[battleField.places.size()];
		reservedTroopTypes = new int[battleField.places.size()];
		reservedPopulations = new int[battleField.places.size()];
		
		if(this.singleMode)
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
				if( getState() < STATE_CREATED || getState()>STATE_BATTLE_ENDED)
					return;
				Building b = null;
				SFSArray vars = SFSArray.newInstance();
				for(int i = 0; i<battleField.places.size(); i++)
				{
					b = battleField.places.get(i).building;
					if( b.get_population() != reservedPopulations[i] || b.troopType != reservedTroopTypes[i] || updaterCount == 0 )
					{
						reservedPopulations[i] = b.get_population();
						reservedTroopTypes[i] = b.troopType;
						vars.addText(i+","+b.get_population()+","+b.troopType);
						//trace(i+","+b.get_population()+","+b.troopType);
					}
					
					if( b.level != reservedLevels[i] || b.type != reservedTypes[i] || updaterCount == 0 )
					{
						sendImproveResponse(i, b.type, b.level);
						reservedTypes[i] = b.type;
						reservedLevels[i] = b.level;
					}
				}
				
				if(vars.size() > 0)
				{
					// Set variables
					//trace("vars.size()", vars.size());
					List<RoomVariable> listOfVars = new ArrayList<RoomVariable>();
					listOfVars.add( new SFSRoomVariable("towers", vars) );
					sfsApi.setRoomVariables(null, room, listOfVars);
				}

				// fight enemy bot
		    	if(singleMode && getState()==STATE_BATTLE_STARTED && aiEnemy.doAction(timerCount % 15))
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
		    	updaterCount ++;
			}


		}, 0, 500);
		
		trace("createGame");
	}
	

	public void leaveGame(User user) 
	{
		//if(!isQuest)
		//	return;
	    
		setState( STATE_BATTLE_ENDED );
//		if(timer != null)
//			timer.cancel();
//		timer = null;		

		if(isQuest)
		{
		scores = new int[1];
		scores[0] = 0;
		calculateEndBattleResponse();
		}
		else
		{
			scores = new int[2];
			calculateEndBattleResponse();
		}
	}
	
	private void endBattle(int[] numBuildings, int battleDuration)
	{
		setState( STATE_BATTLE_ENDED );
//		if(timer != null)
//			timer.cancel();
//		timer = null;
		
		trace("Battle Ended", "b0:"+numBuildings[0], "b1:"+numBuildings[1], "duration:"+battleDuration, "("+battleField.map.times.get(0)+","+battleField.map.times.get(1)+","+battleField.map.times.get(2)+")");
		
		scores = new int[2];
	    for (int i=0; i < scores.length; i++)
	    {
        	scores[i] = 0;
	        Boolean wins = numBuildings[i]>numBuildings[i==1?0:1] && battleDuration < battleField.map.times.get(2);
	        if(wins)
	        {
	        	if(battleDuration < battleField.map.times.get(0))
	        		scores[i] = 3;
	        	else if(battleDuration < battleField.map.times.get(1))
	        		scores[i] = 2;
	        	else
	        		scores[i] = 1;
	        }
	    }
	    
	    // balance live battle scores
	    if( !isQuest )
	    {
		    for (int i=0; i < scores.length; i++)
		    {
		    	if( scores[i] == 0 )
		    		scores[i] = -scores[i==0?1:0];
		    }
	    }
	    
	    calculateEndBattleResponse();
	  //  destroyGame();
	}

	private void calculateEndBattleResponse()
	{
		List<User> users = getPlayers();
	    for (int i=0; i < users.size(); i++)
	    {
	    	User user = users.get(i);
	    	if( !user.isNpc() )
	    	{
	    		Game game = ((Game)user.getSession().getProperty("core"));
	    		IntIntMap outcomes = BattleOutcome.get_outcomes( game, battleField.map, scores[i] );
	    		trace("isQuest", isQuest, scores[i]);
try {
	        	if ( isQuest )
	        	{
	        		if( game.player.quests.get( battleField.map.index ) < scores[i] )
	        		{
						UserManager.setQuestScore(getParentZone().getExtension(), game.player, battleField.map.index, scores[i]);
						game.player.quests.set(battleField.map.index, scores[i]);
	        		}
	        	}

	        	IntIntMap insertMap = new IntIntMap();
                IntIntMap updateMap = new IntIntMap();

            	int[] outk = outcomes.keys();
	        	int r = 0;
	        	while ( r < outk.length )
				{
					if ( game.player.resources.exists(outk[r]) )
                        updateMap.set(outk[r], outcomes.get(outk[r]));
					else
						insertMap.set(outk[r], outcomes.get(outk[r]));
					trace(r, outk[r],outcomes.get(outk[r]) );
                    r ++;
				}

	        	BattleOutcome.consume_outcomes(game, outcomes);
                trace(UserManager.updateResources(getParentZone().getExtension(), game.player, updateMap));
                trace(UserManager.insertResources(getParentZone().getExtension(), game.player, insertMap));
	        	sendEndBattleResponse(user, outcomes, scores[i]);
	        	
} catch (Exception e) {
	trace(e.getMessage());
}
	    	}
	    	getApi().leaveRoom(user, room);
	    	if(user.isNpc())
	    		getApi().disconnect(user.getSession());
	    }
	}

	private void sendEndBattleResponse(User user, IntIntMap outcomes, int score)
	{
    	// provide sfs rewards map
    	SFSObject sfsO = SFSObject.newInstance();
    	
    	//sfsO.putInt("id", ((Game)user.getSession().getProperty("core")).player.id);
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
        sfsO.putInt( "score", score );//trace(sfsO.getDump());
    	send( "endBattle", sfsO, user );		
	}

	// fight =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-
	public void fight(Object[] objects, int destination) 
	{
		if(getState() == STATE_CREATED)
			setState( STATE_BATTLE_STARTED );
		if ( getState() != STATE_BATTLE_STARTED )
			return;
		
		SFSArray srcs = SFSArray.newInstance();
		trace("fight to", destination);

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
		if(getState() == STATE_CREATED)
			setState( STATE_BATTLE_STARTED );
		if ( getState() != STATE_BATTLE_STARTED )
			return;
		
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
		sfsApi.sendExtensionResponse("i", params, getPlayers(), room, false);

	}
	// hit =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-
	public void hit(int troopId)
	{
		if ( getState() != STATE_BATTLE_STARTED )
			return;
		
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
		clearAllHandlers();

		if(timer != null)
			timer.cancel();
		timer = null;
		
		if(autoJoinTimer != null)
			autoJoinTimer.cancel();
		autoJoinTimer = null;
		
		if(getState() >= STATE_DESTROYED)
			return;
		setState( STATE_DESTROYED );
			
		
		//GTimer.stopAll();

		battleField.dispose();
		battleField = null;
		trace("destroyGame");
	}


	public List<User> getPlayers()
	{
		List<User> players = room.getPlayersList();
//		for (int i=0; i < players.size(); i++)
//	    	trace("===>", i, players.get(i).getName());	
		
		boolean needForReverse = ( players.size()==2 && Integer.parseInt(players.get(0).getName()) > Integer.parseInt(players.get(1).getName()) );
		if( needForReverse )
			Collections.reverse(players);

		return players;
	}
	
	private void setState(int value)
	{
		if(_state == value)
			return;
		
		_state = value;
		room.setProperty("state", _state);
	}
	private int getState()
	{
		return _state;
	}

}