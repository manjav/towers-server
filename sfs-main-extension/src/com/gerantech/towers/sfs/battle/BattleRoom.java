package com.gerantech.towers.sfs.battle;

import com.gerantech.towers.sfs.Commands;
import com.gerantech.towers.sfs.battle.bots.BattleBot;
import com.gerantech.towers.sfs.battle.factories.EndCalculator;
import com.gerantech.towers.sfs.battle.factories.HeadquarterEndCalculator;
import com.gerantech.towers.sfs.battle.factories.Outcome;
import com.gerantech.towers.sfs.battle.factories.TouchDownEndCalculator;
import com.gerantech.towers.sfs.battle.handlers.BattleLeaveRequestHandler;
import com.gerantech.towers.sfs.battle.handlers.BattleRoomServerEventsHandler;
import com.gerantech.towers.sfs.battle.handlers.BattleStickerRequestHandler;
import com.gerantech.towers.sfs.battle.handlers.BattleSummonRequestHandler;
import com.gerantech.towers.sfs.callbacks.BattleEventCallback;
import com.gerantech.towers.sfs.callbacks.HitUnitCallback;
import com.gerantech.towers.sfs.challenges.ChallengeUtils;
import com.gerantech.towers.sfs.utils.DBUtils;
import com.gerantech.towers.sfs.utils.RankingUtils;
import com.google.common.base.Charsets;
import com.gt.data.ChallengeSFS;
import com.gt.data.UnitData;
import com.gt.towers.Game;
import com.gt.towers.InitData;
import com.gt.towers.battle.BattleField;
import com.gt.towers.battle.FieldProvider;
import com.gt.towers.battle.fieldes.FieldData;
import com.gt.towers.battle.units.Card;
import com.gt.towers.battle.units.Unit;
import com.gt.towers.constants.CardTypes;
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
import org.apache.commons.io.IOUtils;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClients;

import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class BattleRoom extends SFSExtension 
{
	public ScheduledFuture<?> autoJoinTimer;
	public BattleField battleField;
	public EndCalculator endCalculator;
	public BattleEventCallback eventCallback;

	private Room room;
	private ScheduledFuture<?> timer;

	private BattleBot bot;
	private boolean singleMode;
	private double buildingsUpdatedAt;
	private Map<Integer, UnitData> reservedUnits;
	private List<Integer> reservedUnitIds;

	public void init() 
	{
		room = getParentRoom();
		battleField = new BattleField();
		setState( BattleField.STATE_0_WAITING );
		
		addRequestHandler(Commands.BATTLE_LEAVE, BattleLeaveRequestHandler.class);
		addRequestHandler(Commands.BATTLE_SUMMON_UNIT, BattleSummonRequestHandler.class);
		addRequestHandler(Commands.BATTLE_SEND_STICKER, BattleStickerRequestHandler.class);
		addEventHandler(SFSEventType.USER_JOIN_ROOM, BattleRoomServerEventsHandler.class);
		addEventHandler(SFSEventType.USER_DISCONNECT, BattleRoomServerEventsHandler.class);
	}

	public void createGame(int index, Boolean opponentNotFound)
	{
		if( autoJoinTimer != null )
			autoJoinTimer.cancel(true);
		autoJoinTimer = null;

		List<User> players = getRealPlayers();
		this.singleMode = opponentNotFound || room.getProperty("type") == FieldData.TYPE_OPERATION || players.size() == 1;
		room.setProperty("singleMode", singleMode);

		// reserve player data
		List<Game> registeredPlayers = new ArrayList();
		for (User u: players)
			registeredPlayers.add( ((Game)u.getSession().getProperty("core")) );
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

		trace(registeredPlayers.get(0), registeredPlayers.get(1), room.getProperty("type"), index);

		FieldData field = FieldProvider.getField((String)room.getProperty("type"), index);
		if( field.mapLayout == null )
		{
			try {
				InputStream stream = HttpClients.createDefault().execute(new HttpGet("http://localhost:8080/maps/" + field.mapName + ".json")).getEntity().getContent();
				field.mapLayout = IOUtils.toString(stream, String.valueOf(Charsets.UTF_8));
				trace("http://localhost:8080/maps/" + field.mapName + ".json loaded.");
			} catch (IOException e) { e.printStackTrace(); }
		}

		Instant instant = Instant.now();
		battleField.initialize(registeredPlayers.get(0), registeredPlayers.get(1), field, 0, instant.getEpochSecond(), instant.toEpochMilli(), room.containsProperty("hasExtraTime"), room.containsProperty("isFriendly"));
		battleField.unitsHitCallback = new HitUnitCallback(this);
		eventCallback = new BattleEventCallback(this);
		if( battleField.field.type.equals(FieldData.TYPE_TOUCHDOWN) )
			endCalculator = new TouchDownEndCalculator(this);
		else
			endCalculator = new HeadquarterEndCalculator(this);
		reservedUnits = new ConcurrentHashMap();

		if( singleMode )
		{
			bot = new BattleBot(this);

			// sometimes auto start battle
			if( singleMode && (battleField.difficulty > 5 || Math.random() > 0.5) && !battleField.field.type.equals(FieldData.TYPE_OPERATION) && !registeredPlayers.get(0).player.inTutorial() )
				setState( BattleField.STATE_2_STARTED );
		}
		setState( BattleField.STATE_1_CREATED );

		timer = SmartFoxServer.getInstance().getTaskScheduler().scheduleAtFixedRate(new TimerTask() {
			@Override
			public void run() {

				if( getState() < BattleField.STATE_1_CREATED || getState() > BattleField.STATE_4_ENDED )
					return;
				double battleDuration = battleField.getDuration();
				if( battleField.now - buildingsUpdatedAt >= 500 )
				{
					updateReservesData();
					if( singleMode && battleDuration > 4 )
						pokeBot();
					buildingsUpdatedAt = battleField.now;
				}
				battleField.update((int) (Instant.now().toEpochMilli() - battleField.now));
				checkEnding(battleDuration);

			}
		}, 0, BattleField.DELTA_TIME, TimeUnit.MILLISECONDS);

		trace(room.getName(), "created.");
	}

	private void updateReservesData()
	{
		List<RoomVariable> listOfVars = new ArrayList();


		int[] keys = getChangedUints();
		if( keys != null )
		{
			reservedUnitIds = Arrays.stream(keys).boxed().collect(Collectors.toList());
			ISFSObject units = new SFSObject();
			units.putIntArray("keys", reservedUnitIds);
			listOfVars.add(new SFSRoomVariable("units", units));
		}
		/*SFSArray units = SFSArray.newInstance();
		Unit unit;
		UnitData ud;
		Iterator<Map.Entry<Object, Unit>> iterator = battleField.units._map.entrySet().iterator();
		while( iterator.hasNext() )
		{
			unit = iterator.next().getValue();

		//	if( !reservedUnits.containsKey(unit.id) || ( reservedUnits.get(unit.id).x != unit.x || reservedUnits.get(unit.id).y != unit.y || (reservedUnits.get(unit.id).health != unit.health) ) )
		//	{
				if( reservedUnits.containsKey(unit.id) )
				{
					ud = reservedUnits.get(unit.id);
					ud.x = unit.x;
					ud.y = unit.y;
					ud.health = unit.health;
					reservedUnits.replace(unit.id, ud);
				}
				else
				{
					reservedUnits.put(unit.id, new UnitData(unit.x, unit.y, unit.health));
				}
				units.addText(unit.id + "," + unit.x + "," + unit.y + "," + unit.health + "," + unit.card.type + "," + unit.side);
			}
		//}
		listOfVars.add(new SFSRoomVariable("units", units));*/

		// set elixir bars
		SFSObject bars = new SFSObject();
		bars.putInt("0", (int) Math.floor(battleField.elixirBar.get(0)));
		bars.putInt("1", (int) Math.floor(battleField.elixirBar.get(1)));
		listOfVars.add(new SFSRoomVariable("bars", bars));

		sfsApi.setRoomVariables(null, room, listOfVars);
	}

	private int[] getChangedUints()
	{
		int[] keys = battleField.units.keys();
		if( reservedUnitIds == null )
			return keys;

		if( reservedUnitIds.size() != keys.length )
			return keys;

		for( int i=0; i < keys.length; i ++ )
			if( keys[i] != reservedUnitIds.get(i) )
				return keys;

		return null;
	}

	// summon unit  =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-
	public int summonUnit(int side, int type, double x, double y)
	{
		if( getState() == BattleField.STATE_1_CREATED )
			setState( BattleField.STATE_2_STARTED );
		if( getState() != BattleField.STATE_2_STARTED )
			return MessageTypes.RESPONSE_NOT_ALLOWED;
		int id = battleField.summonUnit(type, side, x, y);

		if( id > -1 )
		{
			SFSArray units = new SFSArray();
			SFSObject params = new SFSObject();

			if( CardTypes.isSpell(type) )
			{
				Card card = battleField.games.__get(side).player.cards.get(type);
				units.addSFSObject(getSFSUnit(type, id, side, card.level, x, y));
				params.putSFSArray("units", units);
				send(Commands.BATTLE_SUMMON_UNIT, params, room.getUserList());
				return id;
			}

			Unit unit = battleField.units.get(id);
			for (int i = id; i > id - unit.card.quantity; i--)
			{
				unit = battleField.units.get(i);
				unit.eventCallback = eventCallback;

				units.addSFSObject(getSFSUnit(type, unit.id, side, unit.card.level, unit.x, unit.y));
			}
			params.putSFSArray("units", units);
			send(Commands.BATTLE_SUMMON_UNIT, params, room.getUserList());
		}
		return id;
	}

	private ISFSObject getSFSUnit(int type, int id, int side, int level, double x, double y)
	{
		SFSObject u = new SFSObject();
		u.putInt("t", type);
		u.putInt("i", id);
		u.putInt("s", side);
		u.putInt("l", level);
		u.putDouble("x", x);
		u.putDouble("y", y);
		return u;
	}

	public void hitUnit(int bulletId, List<Integer> targets)
	{
		SFSObject params = new SFSObject();
		params.putInt("b", bulletId);

		ISFSObject _target;
		ISFSArray _targets = new SFSArray();
		for (int i = 0; i < targets.size(); i++)
		{
			_target = new SFSObject();
			_target.putInt("i", targets.get(i));
			_target.putDouble("h", battleField.units.get(targets.get(i)).health);
			_targets.addSFSObject(_target);
		}
		params.putSFSArray("t", _targets);

		send(Commands.BATTLE_HIT, params, room.getUserList());
	}


	// stickers =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-
	public void sendSticker(User sender, ISFSObject params)
	{
		for (User u : room.getUserList())
		{
			if( singleMode && sender != null )
				bot.chatAnswering(params);

			if( sender == null || u.getId() != sender.getId() )
				send(Commands.BATTLE_SEND_STICKER, params, u);
		}
	}


	// leave =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-
	public void leave(User user, boolean retryMode)
	{
		if( user.isSpectator(room) )
		{
			getApi().leaveRoom(user, room);
			return;
		}

		if( battleField.field.isOperation() )
		{
			setState( BattleField.STATE_4_ENDED );
			if( retryMode )
			{
				close();
				BattleUtils.getInstance().removeRoom(room);
				return;
			}
			endCalculator.scores[1] = endCalculator.scores[0] = 0;
			calculateResult();
			close();
			BattleUtils.getInstance().removeRoom(room);
		}
		else
		{
			//close();
			getApi().leaveRoom(user, room);

			/*int side = battleField.getSide(((Game) user.getSession().getProperty("core")).player.id);
			int[] numBuildings = new int[2];
			numBuildings[side] = 0;
			numBuildings[side == 0 ? 1 : 0] = battleField.places.size();
			end(numBuildings, battleField.getDuration());*/
		}

	}

	private void pokeBot()
	{
		if( getState() < BattleField.STATE_1_CREATED || getState() > BattleField.STATE_4_ENDED )
			return;
		bot.update();
	}

	private void checkEnding(double battleDuration)
	{
		if( getState() > BattleField.STATE_2_STARTED || battleDuration < 3 )
			return;

		boolean haveWinner = endCalculator.check();
		if( haveWinner )
			end(battleDuration);
		else if( battleDuration > battleField.getTime(2) && (endCalculator.ratio() != 1 || battleField.field.isOperation()) )
			end(battleDuration);
		else if( ( battleDuration > battleField.getTime(3) && !battleField.field.isOperation()) )
			end(battleDuration);
		//trace("duration:" + battleDuration, "t2:" + battleField.getTime(2), "t3:" + battleField.getTime(3), "ratio:" + endCalculator.ratio());
	}

	private void end(double battleDuration)
	{
		setState( BattleField.STATE_4_ENDED );
		trace(room.getName(), "ended duration:"+battleDuration, " ("+battleField.field.times.toString()+")");

	    calculateResult();
		close();
		if( battleField.field.isOperation() )
			BattleUtils.getInstance().removeRoom(room);
	}

	private void calculateResult()
	{
		DBUtils dbUtils = DBUtils.getInstance();
		SFSArray outcomesSFSData = new SFSArray();
		int now = (int) Instant.now().getEpochSecond();

		IntIntMap[] outcomesList = new IntIntMap[battleField.games.length];
	    for (int i=0; i < battleField.games.length; i++)
	    {
			Game game = battleField.games.__get(i);

			SFSObject outcomeSFS = new SFSObject();
			outcomeSFS.putInt("id", game.player.id);
			outcomeSFS.putText("name", game.player.nickName);
			outcomeSFS.putInt("score", endCalculator.scores[i]);

			outcomesList[i] = Outcome.get( game, this, endCalculator.scores[i], (float)endCalculator.scores[i] / (float)endCalculator.scores[i==0?1:0], now );
			//trace("i:", i, "scores:"+scores[i], "ratio:"+(float)numBuildings[i] / (float)numBuildings[i==0?1:0] );
			if( battleField.field.isOperation() )
			{
				if( game.player.isBot() )
					continue;

				if( game.player.operations.get( battleField.field.index ) < endCalculator.scores[i] )
				{
					try {
						dbUtils.setOperationScore(game.player, battleField.field.index, endCalculator.scores[i]);
					} catch (Exception e) { e.printStackTrace(); }
					game.player.operations.set(battleField.field.index, endCalculator.scores[i]);
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
				trace(game.player.id + ": (", r, outcomesList[i].get(r) , ")" );

				if( game.player.isBot() )
					continue;

				// update exchange
				if( ResourceType.isBook(r) )
				{
					earnedBook = game.exchanger.items.get(outcomesList[i].get(r));
					earnedBook.outcomesStr = r + ":" + game.player.get_arena(0);
					earnedBook.expiredAt = 0;
				}

				if( r == ResourceType.R17_STARS && game.player.get_arena(0) > 0 )
				{
					int res = game.exchanger.collectStars(outcomesList[i].get(r), (int) (battleField.now / 1000));
					try {
						ExchangeItem stars = game.exchanger.items.get(ExchangeType.C104_STARS);
						if( res == MessageTypes.RESPONSE_SUCCEED )
							dbUtils.updateExchange(ExchangeType.C104_STARS, game.player.id, stars.expiredAt, stars.numExchanges, "", "");
					} catch (Exception e) { e.printStackTrace(); }
				}

				outcomeSFS.putInt(r + "", outcomesList[i].get(r));
			}

			outcomesSFSData.addSFSObject(outcomeSFS);

			// update DB
			if( !game.player.isBot() )
			{
				game.player.addResources(outcomesList[i]);
				try {
					if( earnedBook != null )
						dbUtils.updateExchange(earnedBook.type, game.player.id, 0, earnedBook.numExchanges, earnedBook.outcomesStr, "");
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
			send( Commands.BATTLE_END, params, users.get(i) );

		for (int i=0; i < battleField.games.length; i++)
		{
			Game game = battleField.games.__get(i);    // update active challenges
			if( !game.player.isBot() && !battleField.field.isOperation() && !room.containsProperty("isFriendly") && outcomesList[i].get(ResourceType.R2_POINT) > 0 )
			{
				ISFSArray challenges = ChallengeUtils.getInstance().getChallengesOfAttendee(-1, game.player, false);
				for (int c = 0; c < challenges.size(); c++)
				{
					ChallengeSFS challenge = (ChallengeSFS) challenges.getSFSObject(c);
					if( challenge.base.getState(now) != Challenge.STATE_STARTED )
						continue;
					ISFSObject attendee = ChallengeUtils.getInstance().getAttendee(game.player.id, challenge);
					attendee.putInt("point", attendee.getInt("point") + 1);
					attendee.putInt("updateAt", now);
					ChallengeUtils.getInstance().scheduleSave(challenge);
				}
			}
		}
	}

	public void close()
	{
		room.setAutoRemoveMode(SFSRoomRemoveMode.WHEN_EMPTY);

		if( timer != null )
			timer.cancel(true);
		timer = null;

		if( battleField != null )
			battleField.dispose();
		//battleField = null;
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

	public int getPlayerGroup(User user)
	{
		if( user == null )
			return 0;

		if( user.isSpectator(room) )
			return getPlayerGroup(room.getUserByName(user.getVariable("spectatedUser").getStringValue()));

		return battleField.getSide(((Game)user.getSession().getProperty("core")).player.id);
	}

	private void setState(int value)
	{
		if( battleField.state == value )
			return;

		battleField.state = value;
		room.setProperty("state", value);
	}
	public int getState()
	{
		return battleField.state;
	}

	@Override
	public void destroy()
	{
		clearAllHandlers();
		if( getState() >= BattleField.STATE_5_DISPOSED )
			return;
		setState( BattleField.STATE_5_DISPOSED );

		trace(room.getName(), "destroyed.");
		super.destroy();
	}
}