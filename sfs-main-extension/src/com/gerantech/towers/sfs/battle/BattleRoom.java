package com.gerantech.towers.sfs.battle;

import com.gerantech.towers.sfs.Commands;
import com.gerantech.towers.sfs.battle.bots.BattleBot;
import com.gerantech.towers.sfs.battle.handlers.*;
import com.gerantech.towers.sfs.utils.DBUtils;
import com.gerantech.towers.sfs.utils.RankingUtils;
import com.gt.towers.Game;
import com.gt.towers.InitData;
import com.gt.towers.battle.BattleField;
import com.gt.towers.battle.BattleOutcome;
import com.gt.towers.battle.Troop;
import com.gt.towers.buildings.Building;
import com.gt.towers.constants.ExchangeType;
import com.gt.towers.constants.ResourceType;
import com.gt.towers.constants.StickerType;
import com.gt.towers.exchanges.ExchangeItem;
import com.gt.towers.utils.maps.IntIntMap;
import com.smartfoxserver.v2.core.SFSEventType;
import com.smartfoxserver.v2.entities.Room;
import com.smartfoxserver.v2.entities.User;
import com.smartfoxserver.v2.entities.data.ISFSArray;
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
	public BattleField battleField;

	private int _state = -1;
	private int[] reservedPopulations;
	private int[] reservedTypes;
	//private int[] reservedLevels;
	private int[] reservedTroopTypes;
	private int[] reservedHealthes;
	private int[] scores;
	private int[] keys;

	private Room room;
	private Timer timer;
	private BattleBot bot;
	private boolean isQuest;
	private boolean singleMode;
	private ISFSObject stickerParams;
	private ArrayList<Game> registeredPlayers;

	public void init() 
	{
		room = getParentRoom();
		setState( STATE_WAITING );
		
		addEventHandler(SFSEventType.USER_JOIN_ROOM, BattleRoomServerEventsHandler.class);
		addEventHandler(SFSEventType.USER_DISCONNECT, BattleRoomServerEventsHandler.class);
		
		addRequestHandler(Commands.HIT,					BattleRoomHitRequestHandler.class);
		addRequestHandler(Commands.FIGHT,				BattleRoomFightRequestHandler.class);
		addRequestHandler(Commands.LEAVE,				BattleRoomLeaveRequestHandler.class);
		addRequestHandler(Commands.BUILDING_IMPROVE,	BattleRoomImproveRequestHandler.class);
		addRequestHandler(Commands.SEND_STICKER,		BattleRoomStickerRequestHandler.class);
		addRequestHandler(Commands.RESET_ALL,			BattleRoomResetVarsRequestHandler.class);
	}

	public void createGame(String mapName, Boolean opponentNotFound)
	{
		if( autoJoinTimer != null )
			autoJoinTimer.cancel();

		setState( STATE_CREATED );
		this.isQuest = (boolean) room.getProperty("isQuest");
		this.singleMode = opponentNotFound;

		// reserve player data
		registeredPlayers = new ArrayList();
		List<User> players = getRealPlayers();
		for (User u: players)
			registeredPlayers.add( ((Game)u.getSession().getProperty("core")) );
        if( opponentNotFound )
		{
			InitData data = new InitData();
			data.id = (int) (Math.random() * 9999);
			data.nickName = RankingUtils.getInstance().getRandomName();
			data.resources.set(ResourceType.POINT, 0);
			Game botGame = new Game();
			botGame.init(data);
			registeredPlayers.add( botGame );
		}
        room.setProperty("registeredPlayers", registeredPlayers);

        // create battlefield
		battleField = new BattleField(registeredPlayers.get(0), registeredPlayers.get(1), mapName, 0, room.containsProperty("hasExtraTime"));
		battleField.startAt = battleField.now = Instant.now().getEpochSecond();
		SFSArray decks = new SFSArray();
		for (int i = 0; i < battleField.deckBuildings.size() ; i++)
			decks.addInt(battleField.deckBuildings.get(i).building.type);
		room.setProperty("decks", decks);

		// reserving arrays
		reservedTypes = new int[battleField.places.size()];
		//reservedLevels = new int[battleField.places.size()];
		reservedTroopTypes = new int[battleField.places.size()];
		reservedPopulations = new int[battleField.places.size()];
		reservedHealthes = new int[battleField.places.size()];

		for( int i = 0; i<battleField.places.size(); i++ )
			reservedTypes[i] = battleField.places.get(i).building.type;

		if( singleMode )
			bot = new BattleBot(this);

    	timer = new Timer();
    	timer.schedule(new TimerTask() {

			@Override
			public void run() {
				if( getState() < STATE_CREATED || getState() > STATE_BATTLE_ENDED )
					return;

				Building b;
				double battleDuration = battleField.getDuration();
				SFSArray vars = SFSArray.newInstance();
				for(int i = 0; i<battleField.places.size(); i++)
				{
					b = battleField.places.get(i).building;
					b.interval();
					if( b.get_population() != reservedPopulations[i] || b.troopType != reservedTroopTypes[i] || b.get_health() != reservedHealthes[i] )
					{
						reservedPopulations[i] = b.get_population();
						reservedTroopTypes[i] = b.troopType;
						reservedHealthes[i] = b.get_health();
						vars.addText(i + "," + reservedPopulations[i] + "," + reservedTroopTypes[i] + "," + reservedHealthes[i]);
					}
					
					if( b.type != reservedTypes[i] )//b.get_level() != reservedLevels[i] ||
					{
						SFSObject params = SFSObject.newInstance();
						params.putInt("i", b.index);
						params.putInt("t", b.type);
						params.putInt("l", b.get_level());
						//params.putInt("m", building.improveLevel);
						//send("i", stickerParams, room.getUserList());  -->new but not test
						sfsApi.sendExtensionResponse("i", params, room.getUserList(), room, false);

						reservedTypes[i] = b.type;
						//reservedLevels[i] = b.get_level();
					}
				}

				// Set variables
				List<RoomVariable> listOfVars = new ArrayList();
				if( vars.size() > 0 )
					listOfVars.add( new SFSRoomVariable("towers", vars) );

				// sometimes auto start battle
				if( singleMode && ( battleField.difficulty > 5 || Math.random() > 0.5 ) && !battleField.map.isQuest && battleDuration > 0.5 && battleDuration < 1.1 )
					setState(STATE_BATTLE_STARTED);

				// fight enemy bot
				if( singleMode && getState() == STATE_BATTLE_STARTED )
				{
					// send answer of sticker from bot
					if( stickerParams != null )
					{
						if( stickerParams.getInt("wait") < 4 )
						{
							stickerParams.putInt("wait", stickerParams.getInt("wait")+1);
						}
						else
						{
							stickerParams.removeElement("wait");
							send("ss", stickerParams, room.getUserList());
							stickerParams = null;
						}
					}
					// bot scheduler
					bot.doAction();

				}
		    	// check ending battle
		    	int[] numBuildings = new int[2];
		    	//int[] populations = new int[2];
				//int[] populations = new int[2];
				for(int i = 0; i<reservedTroopTypes.length; i++)
				{
					if( reservedTroopTypes[i] >= 0 )
					{
						//trace(i, reservedTroopTypes[i], reservedPopulations[i]);
						numBuildings[reservedTroopTypes[i]] ++;
						//populations[reservedTroopTypes[i]] += reservedPopulations[i];
					}
				}

				// increase population bars
				double increaseCoef = battleDuration > battleField.getTime(2) ? 0.45 : 0.20;
				battleField.elixirBar.set(0, Math.min(BattleField.POPULATION_MAX, battleField.elixirBar.get(0) + increaseCoef ));
				battleField.elixirBar.set(1, Math.min(BattleField.POPULATION_MAX, battleField.elixirBar.get(1) + increaseCoef ));
				//if( battleField.now % 2 == 0 )
				//{
					SFSObject bars = new SFSObject();
					bars.putInt("0", (int) Math.floor(battleField.elixirBar.get(0)));
					bars.putInt("1", (int) Math.floor(battleField.elixirBar.get(1)));
					listOfVars.add(new SFSRoomVariable("bars", bars));
				//}
				sfsApi.setRoomVariables(null, room, listOfVars);

				if( checkEnding(battleDuration, numBuildings) )
					end(numBuildings, battleDuration);
				else
					battleField.now += TIMER_PERIOD;

			}


		}, 0, Math.round(TIMER_PERIOD * 1000));

		trace(room.getName(), "created.");
	}

	// fight =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-
	public void fight(ISFSArray fighters, int target, boolean fighterIsBot)
	{
		if( getState() == STATE_CREATED)
			setState( STATE_BATTLE_STARTED );
		if( getState() != STATE_BATTLE_STARTED )
			return;

		if( singleMode && !fighterIsBot)
			bot.dangerousPoint = target;

		SFSArray vars = SFSArray.newInstance();
		for(int i = 0; i<fighters.size(); i++)
		{
			//if( singleMode && fighterIsBot && battleField.places.get(fighters.getInt(i)).building.troopType != TroopType.T_1 )
			//	continue;
			String troops = "";
			Iterator<Troop> itr = battleField.places.get(fighters.getInt(i)).building.troops.iterator();
			while ( itr.hasNext() )
			{
				troops += (itr.next().buildingType + (itr.hasNext() ? ":" : "") );
			}
			trace(fighters.getInt(i) + "," + target + "," + troops);
			vars.addText(fighters.getInt(i) + "," + target + "," + troops);
			battleField.places.get(fighters.getInt(i)).fight(battleField.places.get(target), battleField.places);
		}

		// set variables
		List<RoomVariable> listOfVars = new ArrayList();
		listOfVars.add( new SFSRoomVariable("fights", vars) );
		sfsApi.setRoomVariables(null, room, listOfVars);
	}

	// stickers =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-
	public void sendSticker(User sender, ISFSObject params)
	{
		for (User u : room.getUserList())
		{
			if( u.isNpc() && sender != null )
			{
				if( Math.random() > 0.5 )
				{
					int answer = StickerType.getRandomAnswer( params.getInt("t") );
					if(answer > -1) {
						params.putInt("t", answer);
						stickerParams = params;
						stickerParams.putInt("wait", 0);
					}
				}
			}
			else if( sender == null || u.getId() != sender.getId() )
			{
				send(Commands.SEND_STICKER, params, u);
			}
		}
	}

	// transform =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-
	public void transformBuilding(User sender, ISFSObject params)
	{
		if( getState() == STATE_CREATED )
			setState( STATE_BATTLE_STARTED );
		if( getState() != STATE_BATTLE_STARTED )
			return;

		Building card = battleField.deckBuildings.get(params.getInt("c")).building;
		Building building = battleField.places.get(params.getInt("i")).building;

		//Player p = ((Game) sender.getSession().getProperty("core")).player;
		//trace("improve", building.game.player.nickName, params.getDump(), building.transformable(card), building.troopType, p.nickName);
		building.transform(card);
	}

	// hit =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-
	public void hit(int troopId, double damage)
	{
		if ( getState() != STATE_BATTLE_STARTED )
			return;

		//trace("hit troopId:", troopId, ", damage:", damage);
		battleField.hit(troopId, damage);
	}

	// leave =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-
	public void leaveGame(User user, boolean retryMode)
	{
		if( user.isSpectator(room) )
		{
			getApi().leaveRoom(user, room);
			return;
		}

		setState( STATE_BATTLE_ENDED );
		if( isQuest )
		{
			if( retryMode )
			{
				removeAllUsers();
				return;
			}
			keys = scores = new int[1];
			keys[0] = scores[0] = 0;
			sendEndResponse();
		}
	}

	private boolean checkEnding(double battleDuration, int[] numBuildings)
	{
		// Quest :
		if( isQuest )
			return battleDuration > battleField.getTime(2) || numBuildings[0] == 0 || numBuildings[1] == 0;

		// Battle :
		int headQuarterTroopType = -2;
		for ( int p = 0; p < battleField.places.size(); p++ )
		{
			if( battleField.places.get(p).mode == 2 )
			{
				if( headQuarterTroopType == battleField.places.get(p).building.troopType )
					return true;
				headQuarterTroopType = battleField.places.get(p).building.troopType;
			}
		}
		if( battleDuration > battleField.getTime(2) )
		{
			updateScores(numBuildings, battleDuration);
			if( scores[0] != scores[1] )
				return true;
		}

		return battleDuration > battleField.getTime(3);
	}
	private void end(int[] numBuildings, double battleDuration)
	{
		setState( STATE_BATTLE_ENDED );
		trace(room.getName(), "ended", "b0:"+numBuildings[0], "b1:"+numBuildings[1], "duration:"+battleDuration, "("+battleField.map.times.get(0)+","+battleField.map.times.get(1)+","+battleField.map.times.get(2)+","+battleField.map.times.get(3)+")");

		updateScores(numBuildings, battleDuration);
	    sendEndResponse();
	}

	private void updateScores(int[] numBuildings, double battleDuration)
	{
		scores = new int[2];

		if( isQuest )
		{
			for ( int i=0; i < scores.length; i++ )
			{
				scores[i] = 0;
				Boolean wins = numBuildings[i] > numBuildings[i==1?0:1] && battleDuration < battleField.map.times.get(2);
				if( wins )
				{
					if( battleDuration < battleField.map.times.get(0) )
						scores[i] = 3;
					else if( battleDuration < battleField.map.times.get(1) )
						scores[i] = 2;
					else
						scores[i] = 1;
				}
			}
			keys = scores;
		}
		// Battle :
		else
		{
			// key calculation
			keys = new int[]{0,0};
			for (int p = 0; p < battleField.places.size(); p++ )
				if( battleField.places.get(p).mode > 0 && battleField.places.get(p).building.troopType > -1 )
					keys[battleField.places.get(p).building.troopType] += battleField.places.get(p).mode;

			for (int k = 0; k < keys.length; k++ )
				keys[k] = Math.max(0, Math.min(3, keys[k] - 2));

			// score calculation
			int diff = keys[0] - keys[1];
			scores[0] = diff;
			scores[1] = -diff;
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
		//trace("scores:", scores[0], scores[1], "keys:", keys[0], keys[1]);
	}

	private void sendEndResponse()
	{
		DBUtils dbUtils = DBUtils.getInstance();
		SFSArray outcomesSFSData = new SFSArray();

		IntIntMap[] outcomesList = new IntIntMap[registeredPlayers.size()];
	    for (int i=0; i < registeredPlayers.size(); i++)
	    {
			Game game = registeredPlayers.get(i);

			SFSObject outcomeSFS = new SFSObject();
			outcomeSFS.putInt("id", game.player.id);
			outcomeSFS.putText("name", game.player.nickName);
			//outcomeSFS.putInt("score", scores[i]);
			outcomeSFS.putInt("key", keys[i]);

			outcomesList[i] = BattleOutcome.get_outcomes( game, battleField.map, scores[i], keys[i]);

			//trace("isQuest", isQuest, scores[i]);
			if( isQuest )
			{
				if( game.isBot() )
					continue;

				if( game.player.quests.get( battleField.map.index ) < scores[i] )
				{
					try {
						dbUtils.setQuestScore(game.player, battleField.map.index, scores[i]);
					} catch (Exception e) { e.printStackTrace(); }
					game.player.quests.set(battleField.map.index, scores[i]);
				}
			}

			IntIntMap insertMap = new IntIntMap();
			IntIntMap updateMap = new IntIntMap();
			int[] outk = outcomesList[i].keys();
			int r = 0;
			while ( r < outk.length )
			{
				if ( game.player.resources.exists(outk[r]) )
					updateMap.set(outk[r], outcomesList[i].get(outk[r]));
				else
					insertMap.set(outk[r], outcomesList[i].get(outk[r]));
				//trace(r, outk[r],outcomesList[i].get(outk[r]) );

				outcomeSFS.putInt(outk[r]+"", outcomesList[i].get(outk[r]));
				r ++;
			}
			outcomesSFSData.addSFSObject(outcomeSFS);

			if( !game.isBot() )
			{
				game.player.addResources(outcomesList[i]);
				ExchangeItem keysItem = game.exchanger.items.get(ExchangeType.S_41_KEYS);
				try {
					dbUtils.updateExchange(keysItem.type, game.player.id, keysItem.expiredAt, keysItem.numExchanges, keysItem.outcome);
					dbUtils.updateResources(game.player, updateMap);
					dbUtils.insertResources(game.player, insertMap);
				} catch (Exception e) { e.printStackTrace(); }
			}
		}

		// send to all users
		SFSObject params = new SFSObject();
		params.putSFSArray("outcomes", outcomesSFSData);
		List<User> users = room.getUserList();
		for (int i=0; i < users.size(); i++)
			send( Commands.END_BATTLE, params, users.get(i) );

		removeAllUsers();
	}

	private void removeAllUsers()
	{
		List<User> users = room.getUserList();
		for (int i=0; i < users.size(); i++)
			getApi().leaveRoom(users.get(i), room);

		getApi().removeRoom(room);
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

		if( timer != null )
			timer.cancel();
		timer = null;
		
		if( autoJoinTimer != null )
			autoJoinTimer.cancel();
		autoJoinTimer = null;
		
		if( getState() >= STATE_DESTROYED )
			return;
		setState( STATE_DESTROYED );

		battleField = null;
		trace(room.getName(), "destroyed.");
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

	public int getPlayerGroup(User player)
	{
		if( player == null )
			return 0;

		if( player.isSpectator(room) )
			return getPlayerGroup(room.getUserByName(player.getVariable("spectatedUser").getStringValue()));

		for (int i = 0; i < registeredPlayers.size(); i++ )
			if ( Integer.parseInt(player.getName()) == registeredPlayers.get(i).player.id )
				return i;

		return 0;
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