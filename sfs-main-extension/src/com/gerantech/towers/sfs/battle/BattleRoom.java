package com.gerantech.towers.sfs.battle;

import com.gerantech.towers.sfs.Commands;
import com.gerantech.towers.sfs.battle.bots.BattleBot;
import com.gerantech.towers.sfs.battle.handlers.*;
import com.gerantech.towers.sfs.utils.BattleUtils;
import com.gerantech.towers.sfs.utils.DBUtils;
import com.gerantech.towers.sfs.utils.RankingUtils;
import com.gt.towers.Game;
import com.gt.towers.InitData;
import com.gt.towers.battle.BattleField;
import com.gt.towers.buildings.Building;
import com.gt.towers.constants.ExchangeType;
import com.gt.towers.constants.ResourceType;
import com.gt.towers.exchanges.ExchangeItem;
import com.gt.towers.utils.maps.IntIntMap;
import com.smartfoxserver.v2.SmartFoxServer;
import com.smartfoxserver.v2.core.SFSEventType;
import com.smartfoxserver.v2.entities.Room;
import com.smartfoxserver.v2.entities.SFSRoomRemoveMode;
import com.smartfoxserver.v2.entities.User;
import com.smartfoxserver.v2.entities.data.ISFSArray;
import com.smartfoxserver.v2.entities.data.ISFSObject;
import com.smartfoxserver.v2.entities.data.SFSArray;
import com.smartfoxserver.v2.entities.data.SFSObject;
import com.smartfoxserver.v2.entities.variables.RoomVariable;
import com.smartfoxserver.v2.entities.variables.SFSRoomVariable;
import com.smartfoxserver.v2.extensions.SFSExtension;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.TimerTask;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class BattleRoom extends SFSExtension 
{
	public static final int STATE_WAITING = 0;
	public static final int STATE_CREATED = 1;
	public static final int STATE_BATTLE_STARTED = 2;
	public static final int STATE_BATTLE_ENDED = 3;
	public static final int STATE_DESTROYED = 4;

	public ScheduledFuture<?> autoJoinTimer;
	public BattleField battleField;

	private int _state = -1;
	private int[] reservedPopulations;
	private int[] reservedTypes;
	private int[] reservedLevels;

	private int[] reservedTroopTypes;
	private int[] scores;
	private Room room;
	private ScheduledFuture<?> timer;

	private BattleBot bot;
	private boolean isQuest;
	private boolean singleMode;
	private long buildingsUpdatedAt;
	private ISFSObject stickerParams;
	private ArrayList<Game> registeredPlayers;

	public void init() 
	{
		room = getParentRoom();
		setState( STATE_WAITING );
		
		addEventHandler(SFSEventType.USER_JOIN_ROOM, BattleRoomServerEventsHandler.class);
		addEventHandler(SFSEventType.USER_DISCONNECT, BattleRoomServerEventsHandler.class);
		
		addRequestHandler("h", BattleRoomHitRequestHandler.class);
		addRequestHandler("f", BattleRoomFightRequestHandler.class);
		addRequestHandler("i", BattleRoomImproveRequestHandler.class);
		addRequestHandler("ss", BattleRoomStickerRequestHandler.class);
		addRequestHandler("leave", BattleRoomLeaveRequestHandler.class);
		addRequestHandler(Commands.RESET_ALL, BattleRoomResetVarsRequestHandler.class);
	}

	public void createGame(String mapName, Boolean opponentNotFound)
	{
		if( autoJoinTimer != null )
			autoJoinTimer.cancel(true);
		autoJoinTimer = null;

		setState( STATE_CREATED );
		List<User> players = getRealPlayers();
		this.isQuest = (boolean) room.getProperty("isQuest");
		this.singleMode = opponentNotFound || isQuest || players.size() == 1;
		room.setProperty("singleMode", singleMode);

		// reserve player data
		final boolean inTutorial = false;
		registeredPlayers = new ArrayList();
		for (User u: players)
			registeredPlayers.add( ((Game)u.getSession().getProperty("core")) );
		if( singleMode )
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

		trace(registeredPlayers.get(0), registeredPlayers.get(1), mapName);
		battleField = new BattleField(registeredPlayers.get(0), registeredPlayers.get(1), mapName, 0, room.containsProperty("hasExtraTime"));
		battleField.now = Instant.now().toEpochMilli();
		battleField.startAt = battleField.now / 1000;
		reservedTypes = new int[battleField.places.size()];
		reservedLevels = new int[battleField.places.size()];
		reservedTroopTypes = new int[battleField.places.size()];
		reservedPopulations = new int[battleField.places.size()];

		for( int i = 0; i<battleField.places.size(); i++ )
		{
			reservedTypes[i] = battleField.places.get(i).building.type;
			reservedLevels[i] = battleField.places.get(i).building.get_level();
		}

		if( singleMode )
		{
			bot = new BattleBot(this);

			// sometimes auto start battle
			if( singleMode && (battleField.difficulty > 5 || Math.random() > 0.5) && !battleField.map.isQuest && !registeredPlayers.get(0).player.inTutorial() )
				setState(STATE_BATTLE_STARTED);
		}

		timer = SmartFoxServer.getInstance().getTaskScheduler().scheduleAtFixedRate(new TimerTask() {
			@Override
			public void run() {
				if (getState() < STATE_CREATED || getState() > STATE_BATTLE_ENDED)
					return;

				battleField.update();
				long battleDuration = battleField.getDuration();
				if (battleField.now - buildingsUpdatedAt > 500) {
					updateReservesData(battleDuration);
					pokeBot();
					buildingsUpdatedAt = battleField.now;
				}

				checkBattleEnding(battleDuration);
			}
		}, 0, battleField.interval, TimeUnit.MILLISECONDS);

		trace(room.getName(), "created.");
	}

	private void updateReservesData(long battleDuration)
	{
		Building b = null;
		SFSArray vars = SFSArray.newInstance();
		for (int i = 0; i < battleField.places.size(); i++)
		{
			b = battleField.places.get(i).building;
			if( b.get_population() != reservedPopulations[i] || b.troopType != reservedTroopTypes[i] )
			{
				reservedPopulations[i] = b.get_population();
				reservedTroopTypes[i] = b.troopType;
				vars.addText(i + "," + b.get_population() + "," + b.troopType);
			}

			if( b.get_level() != reservedLevels[i] || b.type != reservedTypes[i] )
			{
				sendImproveResponse(i, b.type, b.get_level(), b.troopType, b.get_population());
				reservedTypes[i] = b.type;
				reservedLevels[i] = b.get_level();
			}
		}

		// Set variables
		if( vars.size() > 0 )
		{
			List<RoomVariable> listOfVars = new ArrayList();
			listOfVars.add(new SFSRoomVariable("towers", vars));
			sfsApi.setRoomVariables(null, room, listOfVars);
		}
	}

	private void pokeBot()
	{
		if( singleMode && ( getState() == STATE_BATTLE_STARTED || getState() == STATE_BATTLE_ENDED ) )
		{
			// send answer of sticker from bot
			if( stickerParams != null )
			{
				if( stickerParams.getInt("wait") < 4 )
				{
					stickerParams.putInt("wait", stickerParams.getInt("wait") + 1);
				}
				else
				{
					stickerParams.removeElement("wait");
					send("ss", stickerParams, room.getUserList());
					stickerParams = null;
				}
			}
			bot.update();
		}
	}

	// fight =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-
	public void fight(ISFSArray fighters, int target, boolean fighterIsBot, double troopsDivision)
	{
		if( getState() == STATE_CREATED)
			setState( STATE_BATTLE_STARTED );
		if( getState() != STATE_BATTLE_STARTED )
			return;

		int pLen = battleField.places.size() - 1;
		int srcLen = fighters.size();
		//for(int i = 0; i<srcLen; i++)
		//	trace(i, " fighter index:", fighters.getInt(i), "pLen", pLen);
		if( target < 0 || target > pLen )
			return;

		if( singleMode && !fighterIsBot )
		{
			bot.offenders = fighters;
			bot.coverPoint = target;
		}

		for( int i = 0; i<srcLen; i++ )
		{
			if( fighters.getInt(i) > -1 && fighters.getInt(i) <= pLen )
			{
				//trace(i, " fighter index:", fighters.getInt(i), "target index:", target, " num places:", battleField.places.size());
				battleField.places.get(fighters.getInt(i)).fight(battleField.places.get(target), battleField.places, troopsDivision);
			}
		}

		// Set variables
		List<RoomVariable> listOfVars = new ArrayList();
		listOfVars.add( new SFSRoomVariable("s", fighters) );
		listOfVars.add( new SFSRoomVariable("d", target) );
		listOfVars.add( new SFSRoomVariable("n", troopsDivision) );
		sfsApi.setRoomVariables(null, room, listOfVars);
	}

	// stickers =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-
	public void sendSticker(User sender, ISFSObject params)
	{
		for (User u : room.getUserList())
		{
			if( singleMode && sender != null )
				bot.answerChat(params);

			if( sender == null || u.getId() != sender.getId() )
				send("ss", params, u);
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
		//trace("improve", b.game.player.nickName, params.getDump(),"t:", b.type, "_population:", b._population, b.improvable(params.getInt("t")));
		b.improve(params.getInt("t"));
	}
	private void sendImproveResponse(int index, int type, int level, int troopType, int population)
	{
		SFSObject params = SFSObject.newInstance();
		params.putInt("i", index);
		params.putInt("t", type);
		params.putInt("l", level);
		params.putInt("tt", troopType);
		params.putInt("p", population);
		//send("i", stickerParams, room.getUserList());  -->new but not test
		sfsApi.sendExtensionResponse("i", params, room.getUserList(), room, false);

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
	public void leave(User user, boolean retryMode)
	{
		if( user.isSpectator(room) )
		{
			getApi().leaveRoom(user, room);
			return;
		}

		if( isQuest )
		{
			setState( STATE_BATTLE_ENDED );
			if( retryMode )
			{
				closeGame();
				BattleUtils.getInstance().removeRoom(room);
				return;
			}
			scores = new int[2];
			scores[1] = scores[0] = 0;
			calculateEndBattleResponse();
			closeGame();
			BattleUtils.getInstance().removeRoom(room);
		}
		else
		{
			getApi().leaveRoom(user, room);
		}

	}
	private void checkBattleEnding(long battleDuration)
	{
		int[] numBuildings = new int[2];
		int[] populations = new int[2];
		for (int i = 0; i < reservedTroopTypes.length; i++)
		{
			if( reservedTroopTypes[i] >= 0 )
			{
				//trace(i, reservedTroopTypes[i], reservedPopulations[i]);
				numBuildings[reservedTroopTypes[i]]++;
				populations[reservedTroopTypes[i]] += reservedPopulations[i];
			}
		}
		if( battleDuration > battleField.getTime(2) || numBuildings[0] == 0 || numBuildings[1] == 0 )
			endBattle(numBuildings, battleDuration);
	}

	private void endBattle(int[] numBuildings, double battleDuration)
	{
		setState( STATE_BATTLE_ENDED );

		trace(room.getName(), "ended", "b0:"+numBuildings[0], "b1:"+numBuildings[1], "duration:"+battleDuration, "("+battleField.map.times.get(0)+","+battleField.map.times.get(1)+","+battleField.map.times.get(2)+")");
		
		scores = new int[2];
	    for ( int i=0; i < scores.length; i++ )
	    {
        	scores[i] = 0;
	        Boolean wins = numBuildings[i]>numBuildings[i==1?0:1] && battleDuration < battleField.map.times.get(2);
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
		closeGame();
		if( isQuest )
			BattleUtils.getInstance().removeRoom(room);
	}

	private void calculateEndBattleResponse()
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
			outcomeSFS.putInt("score", scores[i]);

			outcomesList[i] = Outcome.get( game, battleField.map, scores[i] );

			//trace("isQuest", isQuest, scores[i]);
			if( isQuest )
			{
				if( game.player.isBot() )
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

			if( !game.player.isBot() )
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
		params.putSFSArray("outcomes", outcomesSFSData);//trace(outcomesSFSData.getDump());
		List<User> users = room.getUserList();
		for (int i=0; i < users.size(); i++)
			send( Commands.END_BATTLE, params, users.get(i) );
	}

	public void closeGame()
	{
		room.setAutoRemoveMode(SFSRoomRemoveMode.WHEN_EMPTY);

		if( timer != null )
			timer.cancel(true);
		timer = null;

		if( battleField != null )
			battleField.dispose();
		battleField = null;

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

		for( int i = 0; i < registeredPlayers.size(); i++ )
			if ( Integer.parseInt(player.getName()) == registeredPlayers.get(i).player.id )
				return i;
		return 0;
	}

	private void setState(int value)
	{
		if( _state == value )
			return;
		
		_state = value;
		room.setProperty("state", _state);
	}
	private int getState()
	{
		return _state;
	}

	@Override
	public void destroy()
	{
		clearAllHandlers();
		if( getState() >= STATE_DESTROYED )
			return;
		setState( STATE_DESTROYED );

		if( room.getId() % 6 == 0 )
			System.gc();
		trace(room.getName(), "destroyed.");
		super.destroy();
	}
}