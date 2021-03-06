package com.gerantech.towers.sfs.battle;

import com.gt.Commands;
import com.gerantech.towers.sfs.battle.bots.BattleBot;
import com.gerantech.towers.sfs.battle.handlers.*;
import com.gt.utils.ChallengeUtils;
import com.gt.utils.BattleUtils;
import com.gt.utils.DBUtils;
import com.gt.utils.LobbyUtils;
import com.gt.utils.RankingUtils;
import com.gt.data.ChallengeSFS;
import com.gt.data.LobbySFS;
import com.gt.towers.Game;
import com.gt.towers.InitData;
import com.gt.towers.Player;
import com.gt.towers.battle.BattleField;
import com.gt.towers.buildings.Building;
import com.gt.towers.constants.ExchangeType;
import com.gt.towers.constants.MessageTypes;
import com.gt.towers.constants.ResourceType;
import com.gt.towers.exchanges.ExchangeItem;
import com.gt.towers.socials.Challenge;
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
	private Room room;
	private ScheduledFuture<?> timer;

	private BattleBot bot;
	private boolean isOperation;
	private boolean singleMode;
	private long buildingsUpdatedAt;
	private long clientTimeUpdatedAt;
	private ISFSObject stickerParams;
	private ArrayList<Game> registeredPlayers;

	public void init() 
	{
		room = getParentRoom();
		setState( STATE_WAITING );
		
		addEventHandler(SFSEventType.USER_JOIN_ROOM, BattleRoomServerEventsHandler.class);
		addEventHandler(SFSEventType.USER_DISCONNECT, BattleRoomServerEventsHandler.class);
		addRequestHandler(Commands.FIGHT, BattleRoomFightRequestHandler.class);
		addRequestHandler(Commands.LEAVE, BattleRoomLeaveRequestHandler.class);
		addRequestHandler(Commands.SEND_STICKER, BattleRoomStickerRequestHandler.class);
		addRequestHandler(Commands.BUILDING_IMPROVE, BattleRoomImproveRequestHandler.class);
	}

	public void createGame(String mapName, Boolean opponentNotFound)
	{
		if( autoJoinTimer != null )
			autoJoinTimer.cancel(true);
		autoJoinTimer = null;

		setState( STATE_CREATED );
		List<User> players = getRealPlayers();
		this.isOperation = (boolean) room.getProperty("isOperation");
		this.singleMode = opponentNotFound || isOperation || players.size() == 1;
		room.setProperty("singleMode", singleMode);

		// reserve player data
		registeredPlayers = new ArrayList<>();
		for (User u: players)
		{
			Game g = (Game)u.getSession().getProperty("core");
			g.inBattleChallengMode = (int) u.getSession().getProperty("challengeType");
			registeredPlayers.add(g);
		}

		if( singleMode )
		{
			InitData data = new InitData();
			data.id = (int) (Math.random() * 9999);
			data.nickName = RankingUtils.getInstance().getRandomName();
			data.resources.set(ResourceType.R2_POINT, 0);
			Game botGame = new Game();
			botGame.init(data);
			registeredPlayers.add( botGame );
		}
        room.setProperty("registeredPlayers", registeredPlayers);

		trace(registeredPlayers.get(0), registeredPlayers.get(1), mapName);
		battleField = new BattleField(registeredPlayers.get(0), registeredPlayers.get(1), mapName, 0, room.containsProperty("hasExtraTime"));
		//battleField.troopHitCallback = new HitTroopCallback(getParentZone().getExtension());
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
			if( singleMode && (battleField.difficulty > 5 || Math.random() > 0.5) && !battleField.map.isOperation && !registeredPlayers.get(0).player.inTutorial() )
				setState(STATE_BATTLE_STARTED);
		}

		timer = SmartFoxServer.getInstance().getTaskScheduler().scheduleAtFixedRate(new TimerTask() {
			@Override
			public void run() {
try {

				if( getState() < STATE_CREATED || getState() > STATE_BATTLE_ENDED )
					return;
				battleField.update();
				long battleDuration = battleField.getDuration();
				if( battleField.now - buildingsUpdatedAt >= 500 )
				{
					updateReservesData(battleDuration);
					if( battleField.getDuration() > 3 )
					    pokeBot();
					buildingsUpdatedAt = battleField.now;
				}
				checkEnding(battleDuration);

} catch (Exception | Error e) { e.printStackTrace(); }

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

		// update client time every 5 seconds
		if( battleField.now - clientTimeUpdatedAt >= 5000 )
		{
			clientTimeUpdatedAt = battleField.now;
			vars.addLong(battleField.now);
		}

		// Set variables
		if( vars.size() > 0 )
		{
			List<RoomVariable> listOfVars = new ArrayList<>();
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

		int numPlaces = battleField.places.size() - 1;
		int numFighters = fighters.size();
		//for(int i = 0; i<srcLen; i++)
		//	trace(i, " fighter index:", fighters.getInt(i), "pLen", pLen);
		if( target < 0 || target > numPlaces )
			return;

		if( singleMode && !fighterIsBot )
		{
			bot.offenders = fighters;
			bot.coverPoint = target;
		}

		for( int i = 0; i<numFighters; i++ )
		{
			if( fighters.getInt(i) > -1 && fighters.getInt(i) <= numPlaces )
			{
				if( battleField.places.get(fighters.getInt(i)) == null || battleField.places.get(target) == null )
					continue;
				//trace(i, " fighter index:", fighters.getInt(i), "target index:", target, " num places:", battleField.places.size());
				battleField.places.get(fighters.getInt(i)).fight(battleField.places.get(target), battleField.places, troopsDivision);
			}
		}

		// Set variables
		List<RoomVariable> listOfVars = new ArrayList<>();
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
				bot.chatAnswering(params);

			if( sender == null || u.getId() != sender.getId() )
				send("ss", params, u);
		}
	}

	// improve =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-
	public void improveBuilding(ISFSObject params)
	{
		if( getState() == STATE_CREATED )
			setState( STATE_BATTLE_STARTED );
		if( getState() != STATE_BATTLE_STARTED )
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

	// leave =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-
	public void leave(User user, boolean retryMode)
	{
		if( user.isSpectator(room) )
		{
			getApi().leaveRoom(user, room);
			return;
		}

		if( isOperation )
		{
			setState( STATE_BATTLE_ENDED );
			if( retryMode )
			{
				close();
				BattleUtils.getInstance().removeRoom(room);
				return;
			}
			int[] scores = new int[2];
			int[] numBuildings = new int[2];
			scores[1] = scores[0] = 0;
			calculateResult(scores, numBuildings);
			close();
			BattleUtils.getInstance().removeRoom(room);
		}
		else
		{
			Player player = ((Game) user.getSession().getProperty("core")).player;
			if( getState() > STATE_BATTLE_STARTED || room.containsProperty("isFriendly") )
				return;
			int playerIndex = -1;
			for (int i=0; i < registeredPlayers.size(); i++)
			{
				if( player.id == registeredPlayers.get(i).player.id )
				{
					playerIndex = i;
					break;
				}
			}

			int[] numBuildings = new int[2];
			numBuildings[playerIndex] = 0;
			numBuildings[playerIndex == 0 ? 1 : 0] = battleField.places.size();
			end(numBuildings, battleField.getDuration());
			//getApi().leaveRoom(user, room);
		}

	}
	private void checkEnding(long battleDuration)
	{
		if( battleDuration < 3 )
			return;
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

		//fast win
		/*numBuildings[0] = battleField.places.size();
		numBuildings[1] = 0;*/

		if( numBuildings[0] == 0 || numBuildings[1] == 0 )
		{
			end(numBuildings, battleDuration);
			return;
		}

		if( ( battleDuration > battleField.getTime(3) && !isOperation) || ( battleDuration > battleField.getTime(2) && isOperation) )
			end(numBuildings, battleDuration);
	}

	private void end(int[] numBuildings, double battleDuration)
	{
		setState( STATE_BATTLE_ENDED );
		trace(room.getName(), "ended", "b0:"+numBuildings[0], "b1:"+numBuildings[1], "duration:"+battleDuration, "("+battleField.map.times.get(0)+","+battleField.map.times.get(1)+","+battleField.map.times.get(2)+","+battleField.map.times.get(3)+")");

		float numOccupied = numBuildings[0] + numBuildings[1] - 1;
		int[] scores = new int[2];
		for ( int i=0; i < 2; i++ )
		{
			if( isOperation )
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
			else
			{
				scores[i] = (int) Math.floor( Math.max(0, numBuildings[i] - 1) * 3 / numOccupied );
				//trace(scores[i],  Math.max(0, numBuildings[i] - 1) * 3, numOccupied);
			}
		}

	    calculateResult(scores, numBuildings);
		close();
		if( isOperation || room.getPlayersList().size() == 0 )
			BattleUtils.getInstance().removeRoom(room);
	}

	private void calculateResult(int[] scores, int[] numBuildings)
	{
		DBUtils dbUtils = DBUtils.getInstance();
		SFSArray outcomesSFSData = new SFSArray();
		int now = (int) Instant.now().getEpochSecond();

		IntIntMap[] outcomesList = new IntIntMap[registeredPlayers.size()];
	    for (int i=0; i < registeredPlayers.size(); i++)
	    {
			Game game = registeredPlayers.get(i);

			SFSObject outcomeSFS = new SFSObject();
			outcomeSFS.putInt("id", game.player.id);
			outcomeSFS.putText("name", game.player.nickName);
			outcomeSFS.putInt("score", scores[i]);

			outcomesList[i] = Outcome.get( game, battleField.map, scores[i], (float)numBuildings[i] / (float)numBuildings[i==0?1:0], now );
			//trace("i:", i, "score:"+scores[i], "ratio:"+(float)numBuildings[i] / (float)numBuildings[i==0?1:0] );
			if( isOperation )
			{
				if( game.player.isBot() )
					continue;

				if( game.player.operations.get( battleField.map.index ) < scores[i] )
				{
					try {
						dbUtils.setOperationScore(game.player, battleField.map.index, scores[i]);
					} catch (Exception e) { e.printStackTrace(); }
					game.player.operations.set(battleField.map.index, scores[i]);
				}
			}

			IntIntMap insertMap = new IntIntMap();
			IntIntMap updateMap = new IntIntMap();
			ExchangeItem earnedBook = null;

			int[] ouyKeys = outcomesList[i].keys();
			for ( int r : ouyKeys )
			{
				if( game.player.resources.exists(r) )
					updateMap.set(r, outcomesList[i].get(r));
				else
					insertMap.set(r, outcomesList[i].get(r));

				if( game.player.isBot() )
					continue;

				// update exchange
				if( ResourceType.isBook(r) )
				{
					earnedBook = game.exchanger.items.get(outcomesList[i].get(r));
					earnedBook.outcomesStr = r + ":" + game.player.get_arena(0);
					earnedBook.expiredAt = 0;
				}

				if( r == ResourceType.R17_STARS && game.player.get_battleswins() > 3 )
				{
					int res = game.exchanger.collectStars(outcomesList[i].get(r), (int) (battleField.now / 1000));
					ExchangeItem stars = game.exchanger.items.get(ExchangeType.C104_STARS);
					if( res == MessageTypes.RESPONSE_SUCCEED )
						dbUtils.updateExchange(ExchangeType.C104_STARS, game.player.id, stars.expiredAt, stars.numExchanges, "", "");
				}

				outcomeSFS.putInt(r + "", outcomesList[i].get(r));
			}

			outcomesSFSData.addSFSObject(outcomeSFS);

			// update DB
			if( !game.player.isBot() )
			{
				//trace("battle outcomes:", outcomesList[i].toString());
				// increase daily battles
				if( game.player.get_battleswins() > 0 && !game.player.inFriendlyBattle && !battleField.map.isOperation )
				{
					ExchangeItem dailyBattles = game.exchanger.items.get(ExchangeType.C29_DAILY_BATTLES);
					if( dailyBattles == null )
						dailyBattles = new ExchangeItem(ExchangeType.C29_DAILY_BATTLES, 0, 0, "", "");
					dailyBattles.numExchanges ++;
					dbUtils.updateExchange(ExchangeType.C29_DAILY_BATTLES, game.player.id, dailyBattles.expiredAt, dailyBattles.numExchanges, "", "");
				}

				// add rewards
				game.player.addResources(outcomesList[i]);
				if( earnedBook != null )
					dbUtils.updateExchange(earnedBook.type, game.player.id,0, earnedBook.numExchanges, earnedBook.outcomesStr, "");
				dbUtils.updateResources(game.player, updateMap);
				dbUtils.insertResources(game.player, insertMap);
			}
		}
		updateChallenges(outcomesList, now);
		updateLobbies();
	    sendData(outcomesSFSData);
	}

	private void updateChallenges(IntIntMap[] outcomesList, int now)
	{
		if( isOperation || room.containsProperty("isFriendly") )
			return;
		for( int i=0; i < registeredPlayers.size(); i++ )
		{
			Game game = registeredPlayers.get(i);
			if( game.player.isBot() || outcomesList[i].get(ResourceType.R2_POINT) <= 0 ) // update active challenges
				continue;

			ISFSArray challenges = ChallengeUtils.getInstance().getChallengesOfAttendee(-1, game.player, false);
			for( int c = 0; c < challenges.size(); c++ )
			{
				ChallengeSFS challenge = (ChallengeSFS) challenges.getSFSObject(c);
				if( challenge.base.getState(now) != Challenge.STATE_STARTED || game.inBattleChallengMode != challenge.base.type )
					continue;
				ISFSObject attendee = ChallengeUtils.getInstance().getAttendee(game.player.id, challenge);
				attendee.putInt("point", attendee.getInt("point") + 1);
				attendee.putInt("updateAt", now);
				ChallengeUtils.getInstance().scheduleSave(challenge);
			}
		}
	}

	private void updateLobbies()
	{
		if( isOperation )
			return;
		LobbySFS lobbySFS;
		for( int i=0; i < registeredPlayers.size(); i++ )
		{
			Game game = registeredPlayers.get(i);
			lobbySFS = LobbyUtils.getInstance().getDataByMember(game.player.id);
			if( lobbySFS == null )
				return;

			int index = LobbyUtils.getInstance().getMemberIndex(lobbySFS, game.player.id);
			int activeness = lobbySFS.getMembers().getSFSObject(index).containsKey("ac") ? lobbySFS.getMembers().getSFSObject(index).getInt("ac") : 0;
			lobbySFS.getMembers().getSFSObject(index).putInt("ac", activeness + 1);
			LobbyUtils.getInstance().save(lobbySFS, null, null, -1, -1, -1, -1, lobbySFS.getMembersBytes(), null);
		}
	}

	private void sendData(SFSArray outcomesSFSData)
	{
		SFSObject params = new SFSObject();
		params.putSFSArray("outcomes", outcomesSFSData);//trace(outcomesSFSData.getDump());
		List<User> users = room.getUserList();
		for( int i=0; i < users.size(); i++ )
			send( Commands.BATTLE_END, params, users.get(i) );
	}

	public void close()
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
		if( getState() < STATE_DESTROYED )
			setState(STATE_DESTROYED);

		trace(room.getName(), "destroyed.");
		BattleUtils.getInstance().removeReferences(room);
		super.destroy();
	}
}