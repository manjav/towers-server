package com.gerantech.towers.sfs.battle;

import com.gerantech.towers.sfs.battle.handlers.*;
import com.gerantech.towers.sfs.utils.NPCTools;
import com.gerantech.towers.sfs.utils.UserManager;
import com.gt.towers.Game;
import com.gt.towers.battle.AIEnemy;
import com.gt.towers.battle.BattleField;
import com.gt.towers.battle.BattleOutcome;
import com.gt.towers.buildings.Building;
import com.gt.towers.constants.ExchangeType;
import com.gt.towers.constants.StickerType;
import com.gt.towers.exchanges.ExchangeItem;
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

import java.time.Instant;
import java.util.*;

public class BattleRoom extends SFSExtension 
{
	public static final int STATE_WAITING = 0;
	public static final int STATE_CREATED = 1;
	public static final int STATE_BATTLE_STARTED = 2;
	public static final int STATE_BATTLE_ENDED = 3;
	public static final int STATE_DESTROYED = 4;

	private static final double TIMER_PERIOD = 0.5;


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
	private ISFSObject stickerParams;
	private ArrayList<Game> registeredPlayers;

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

		// reserve player data
		registeredPlayers = new ArrayList();
		List<User> players = getPlayers();
        for (User u: players)
        	if(!u.isNpc())
				registeredPlayers.add( ((Game)u.getSession().getProperty("core")) );
        room.setProperty("registeredPlayers", registeredPlayers);

		battleField = new BattleField(game, mapName, 0);
		battleField.startAt = battleField.now = Instant.now().getEpochSecond();
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
				if(singleMode && getState()==STATE_BATTLE_STARTED )
				{
					// send answer of sticker from bot
					if(stickerParams != null)
					{
						if( stickerParams.getInt("wait") < 4 )
						{
							stickerParams.putInt("wait", stickerParams.getInt("wait")+1);
						}
						else
							{
							stickerParams.removeElement("wait");
							send("ss", stickerParams, getRealPlayers());
							stickerParams = null;
						}
					}

					// fight
					if (aiEnemy.actionType == AIEnemy.TYPE_FIGHT_DOUBLE)
					{
						botFight();
						aiEnemy.actionType = AIEnemy.TYPE_FIGHT;
					}
					else
					{
						//trace("aiEnemy->actionType", aiEnemy.actionType, aiEnemy.target);
						if ( aiEnemy.doAction() > 0 )
						{
							if ( aiEnemy.actionType ==  AIEnemy.TYPE_FIGHT || aiEnemy.actionType == AIEnemy.TYPE_FIGHT_DOUBLE )
								botFight();
						}
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
				if( battleField.getDuration() > battleField.map.times.get(2) || numBuildings[0] == 0 || numBuildings[1] == 0 )
					endBattle(numBuildings, battleField.getDuration());
				else
					battleField.now += TIMER_PERIOD;

		    	updaterCount ++;
			}


		}, 0, Math.round(TIMER_PERIOD*1000));
		
		trace("createGame");
	}

	// bot fight =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-
	private void botFight() {
		Object[] ss = new Object[aiEnemy.sources.size()];
		for(int j = 0; j<ss.length; j++)
			ss[j] = aiEnemy.sources.get(j);
		//trace("botFight", aiEnemy.actionType, aiEnemy.target);
		fight(ss, aiEnemy.target);
	}

	// fight =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-
	public void fight(Object[] objects, int destination)
	{
		if(getState() == STATE_CREATED)
			setState( STATE_BATTLE_STARTED );
		if ( getState() != STATE_BATTLE_STARTED )
			return;

		SFSArray srcs = SFSArray.newInstance();
		//trace("fight to", destination);

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

	// stickers =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-
	public void sendSticker(User sender, ISFSObject params)
	{
		for (User u : room.getPlayersList())
		{
			if (u.isNpc())
			{
				if(aiEnemy.actionType != AIEnemy.TYPE_FIGHT_DOUBLE && Math.random()>0.5)
				{
					int answer = StickerType.getRandomAnswer( params.getInt("t") );
					if(answer > -1) {
						params.putInt("t", answer);
						stickerParams = params;
						stickerParams.putInt("wait", 0);
					}
				}
			}
			else if (u.getId() != sender.getId())
			{
				send("ss", params, u);
			}
		}
	}

	// improve =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-
	public void improveBuilding(User sender, ISFSObject params)
	{
		if(getState() == STATE_CREATED)
			setState( STATE_BATTLE_STARTED );
		if ( getState() != STATE_BATTLE_STARTED )
			return;

		Building b = battleField.places.get(params.getInt("i")).building;
		//trace("improve", params.getDump(), b.improvable(params.getInt("t")));
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
	public void hit(int troopId, double damage)
	{
		if ( getState() != STATE_BATTLE_STARTED )
			return;

		int index = (int) Math.floor((double)(troopId/10000));
		//trace("hit index:", index, ", troopId:", troopId, ", damage:", damage);
		battleField.places.get(index).hit(troopId, damage);
	}

	// leave =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-
	public void leaveGame(User user) 
	{
		//if(!isQuest)
		//	return;
	    
		setState( STATE_BATTLE_ENDED );
//		if(timer != null)
//			timer.cancel();
//		timer = null;		

		if( isQuest )
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
	
	private void endBattle(int[] numBuildings, double battleDuration)
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
	    for (int i=0; i < registeredPlayers.size(); i++)
	    {
			Game game = registeredPlayers.get(i);
			IntIntMap outcomes = BattleOutcome.get_outcomes( game, battleField.map, scores[i] );
			trace("isQuest", isQuest, scores[i]);
			if ( isQuest )
			{
				if( game.player.quests.get( battleField.map.index ) < scores[i] )
				{
					try {
						UserManager.setQuestScore(getParentZone().getExtension(), game.player, battleField.map.index, scores[i]);
					} catch (Exception e) { trace(e.getMessage()); }
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

			game.player.addResources(outcomes);
			ExchangeItem keysItem = game.exchanger.items.get(ExchangeType.S_41_KEYS);
			try {
				trace(UserManager.updateExchange(getParentZone().getExtension(), keysItem.type, game.player.id, keysItem.expiredAt, keysItem.numExchanges, keysItem.outcome));
				trace(UserManager.updateResources(getParentZone().getExtension(), game.player, updateMap));
				trace(UserManager.insertResources(getParentZone().getExtension(), game.player, insertMap));
			} catch (Exception e) {
				e.printStackTrace();
				//trace(e.getMessage());
			}
			// send end battle response if player connected
			User p = getRealPlayer(game.player.id);
			if( p != null )
				sendEndBattleResponse(p, outcomes, scores[i]);
		}

		List<User> users = getPlayers();
		for (int i=0; i < users.size(); i++)
		{
			User user = users.get(i);
			getApi().leaveRoom(user, room);
			if ( user.isNpc() )
			{
				// return npc to npc-opponents list
				NPCTools.setXP(Integer.parseInt(user.getName()), -1);

				// remove npc
				getApi().disconnect(user.getSession());
			}
		}
	}

	private void sendEndBattleResponse(User user, IntIntMap outcomes, int score)
	{
    	// provide sfs rewards map
    	SFSObject sfsO = SFSObject.newInstance();
    	
    	//sfsO.putInt("id", ((Game)user.getSession().getProperty("core")).player.id);
    	SFSObject sfsReward;
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
			
		if( battleField != null )
			battleField.dispose();
		battleField = null;
		trace("destroyGame");
	}

	public List<User> getRealPlayers()
	{
		List<User> ret = new ArrayList<>();
		List<User> players = room.getPlayersList();
		for (int i=0; i < players.size(); i++)
	    	if( !players.get(i).isNpc() && !players.get(i).isSpectator())
				ret.add(players.get(i));
		return ret;
	}

	public User getRealPlayer(int id)
	{
		List<User> players = getRealPlayers();
		for (int i=0; i < players.size(); i++)
			if( players.get(i).getName().equals(id+"") )
				return  players.get(i);
		return null;
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