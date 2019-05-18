package com.gerantech.towers.sfs.battle;

import com.gerantech.towers.sfs.battle.bots.BattleBot;
import com.gerantech.towers.sfs.battle.factories.EndCalculator;
import com.gerantech.towers.sfs.battle.factories.HeadquarterEndCalculator;
import com.gerantech.towers.sfs.battle.factories.Outcome;
import com.gerantech.towers.sfs.battle.factories.TouchDownEndCalculator;
import com.gerantech.towers.sfs.callbacks.BattleEventCallback;
import com.gerantech.towers.sfs.callbacks.ElixirChangeCallback;
import com.gerantech.towers.sfs.callbacks.HitUnitCallback;
import com.gerantech.towers.sfs.utils.HttpTool;
import com.gt.BBGRoom;
import com.gt.Commands;
import com.gt.data.LobbySFS;
import com.gt.towers.Game;
import com.gt.towers.InitData;
import com.gt.towers.battle.BattleField;
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
import com.gt.utils.BattleUtils;
import com.gt.utils.DBUtils;
import com.gt.utils.LobbyUtils;
import com.gt.utils.RankingUtils;
import com.smartfoxserver.v2.SmartFoxServer;
import com.smartfoxserver.v2.api.CreateRoomSettings;
import com.smartfoxserver.v2.entities.SFSRoomRemoveMode;
import com.smartfoxserver.v2.entities.User;
import com.smartfoxserver.v2.entities.data.ISFSArray;
import com.smartfoxserver.v2.entities.data.ISFSObject;
import com.smartfoxserver.v2.entities.data.SFSArray;
import com.smartfoxserver.v2.entities.data.SFSObject;
import com.smartfoxserver.v2.entities.variables.RoomVariable;
import com.smartfoxserver.v2.entities.variables.SFSRoomVariable;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.TimerTask;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class BattleRoom extends BBGRoom
{
	public ScheduledFuture<?> autoJoinTimer;
	public BattleField battleField;
	public EndCalculator endCalculator;

	private BattleBot bot;
	private boolean singleMode;
	private double buildingsUpdatedAt;
	private ScheduledFuture<?> timer;
	private List<Integer> reservedUnitIds;
	private BattleEventCallback eventCallback;

	public void init(int id, CreateRoomSettings settings)
	{
		super.init(id, settings);
		battleField = new BattleField();
		setState( this.battleField.STATE_0_WAITING );
	}

	public void createGame(Boolean opponentNotFound)
	{
		if( this.autoJoinTimer != null )
			this.autoJoinTimer.cancel(true);
		this.autoJoinTimer = null;

		List<User> players = getUsersByType(BBGRoom.USER_TYPE_PLAYER);
		this.singleMode = opponentNotFound || players.size() == 1;
		this.setProperty("singleMode", singleMode);

		// reserve player data
		List<Game> registeredPlayers = new ArrayList();
		for (User u: players)
		{
			Game g = (Game)u.getSession().getProperty("core");
//			g.inBattleChallengMode = (int) u.getSession().getProperty("challengeType");
			registeredPlayers.add(g);
		}

		if( this.singleMode )
		{
			InitData data = new InitData();
			data.id = (int) (Math.random() * 9999);
			data.nickName = RankingUtils.getInstance().getRandomName();
			data.resources.set(ResourceType.R2_POINT, 0);
			Game botGame = new Game();
			botGame.init(data);
			registeredPlayers.add( botGame );
		}
        this.setProperty("registeredPlayers", registeredPlayers);


		int mode = this.getPropertyAsInt("mode");
		trace(registeredPlayers.get(0), registeredPlayers.get(1), mode);
		if( !BattleUtils.getInstance().maps.containsKey(mode) )
			BattleUtils.getInstance().maps.put(mode, HttpTool.post("http://localhost:8080/maps/map-" + mode + ".json", null, false).text);

		Instant instant = Instant.now();
		this.battleField.initialize(registeredPlayers.get(0), registeredPlayers.get(1), new FieldData(mode, BattleUtils.getInstance().maps.get(mode), "60,120,180,240"), 0, instant.getEpochSecond(), instant.toEpochMilli(), containsProperty("hasExtraTime"), this.getPropertyAsInt("friendlyMode"));
		this.battleField.unitsHitCallback = new HitUnitCallback(this);
		this.battleField.elixirUpdater.callback = new ElixirChangeCallback(this);
		this.eventCallback = new BattleEventCallback(this);
		if( this.battleField.field.mode == Challenge.MODE_1_TOUCHDOWN )
			endCalculator = new TouchDownEndCalculator(this);
		else
			endCalculator = new HeadquarterEndCalculator(this);

		if( singleMode )
		{
			bot = new BattleBot(this);

			// sometimes auto start battle
			if( singleMode && (battleField.difficulty > 5 || Math.random() > 0.5) && !registeredPlayers.get(0).player.inTutorial() )
				setState( this.battleField.STATE_2_STARTED );
		}
		setState( this.battleField.STATE_1_CREATED );

		timer = SmartFoxServer.getInstance().getTaskScheduler().scheduleAtFixedRate(new TimerTask() {
			@Override
			public void run() {

				if( getState() < battleField.STATE_1_CREATED || getState() > battleField.STATE_4_ENDED )
					return;
				try {
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
				catch (Exception e)
				{
					e.printStackTrace();
				}

			}
		}, 0, this.battleField.DELTA_TIME, TimeUnit.MILLISECONDS);

		trace(getName(), "created.");
	}

	private void updateReservesData()
	{
		int a = 0;
		double b = (double)a;
		List<RoomVariable> listOfVars = new ArrayList();

		int[] keys = getChangedUints();
		if( keys != null )
		{
			reservedUnitIds = Arrays.stream(keys).boxed().collect(Collectors.toList());
			ISFSObject units = new SFSObject();
			units.putIntArray("keys", reservedUnitIds);

/*			List<String> testData = new ArrayList<>();
			for ( int k:reservedUnitIds )
			{
				Unit unit = this.battleField.units.get(k);
				testData.add(unit.id + "," + unit.x + "," + unit.y + "," + unit.health + "," + unit.card.type + "," + unit.side + "," + unit.card.level);
			}
			units.putUtfStringArray("testData", testData);*/

			listOfVars.add(new SFSRoomVariable("units", units));
		}

		// set elixir bars
		SFSObject bars = new SFSObject();

		try {
			bars.putInt("0", (int) Math.floor((double) this.battleField.elixirUpdater.bars.__get(0)));
			bars.putInt("1", (int) Math.floor((double) this.battleField.elixirUpdater.bars.__get(1)));
		}catch(Exception e){ trace(e.getMessage()); }
		listOfVars.add(new SFSRoomVariable("bars", bars));

		//sfsApi.setRoomVariables(null, room, listOfVars);
	}

	private int[] getChangedUints()
	{
		int[] keys = this.battleField.units.keys();
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
		if( getState() == this.battleField.STATE_1_CREATED )
			setState( this.battleField.STATE_2_STARTED );
		if( getState() != this.battleField.STATE_2_STARTED )
			return MessageTypes.RESPONSE_NOT_ALLOWED;
		int id = this.battleField.summonUnit(type, side, x, y);

		if( id > -1 )
		{
			SFSArray units = new SFSArray();
			SFSObject params = new SFSObject();

			if( CardTypes.isSpell(type) )
			{
				Card card = this.battleField.games.__get(side).player.cards.get(type);
				units.addSFSObject(getSFSUnit(type, id, side, card.level, x, y));
				params.putSFSArray("units", units);
				send(Commands.BATTLE_SUMMON_UNIT, params, getUserList());
				return id;
			}

			Unit unit = this.battleField.units.get(id);
			for (int i = id; i > id - unit.card.quantity; i--)
			{
				unit = this.battleField.units.get(i);
				unit.eventCallback = eventCallback;

				units.addSFSObject(getSFSUnit(type, unit.id, side, unit.card.level, unit.x, unit.y));
			}
			params.putSFSArray("units", units);
			send(Commands.BATTLE_SUMMON_UNIT, params, getUserList());
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
			_target.putDouble("h", this.battleField.units.get(targets.get(i)).health);
			_targets.addSFSObject(_target);
		}
		params.putSFSArray("t", _targets);

		send(Commands.BATTLE_HIT, params, getUserList());
	}

	public void sendNewRoundResponse(int winner, int unitId)
	{
		SFSObject params = new SFSObject();
		params.putInt("winner", winner);
		if( this.battleField.field.mode == Challenge.MODE_1_TOUCHDOWN )
		{
			params.putInt("unitId", unitId);
			params.putInt("round", ((TouchDownEndCalculator)endCalculator).round);
		}
		params.putInt("0", endCalculator.scores[0]);
		params.putInt("1", endCalculator.scores[1]);
		send(Commands.BATTLE_NEW_ROUND, params, getUserList());

		if( singleMode )
		{
			bot.reset();
			bot.chatStarting(1/endCalculator.ratio());
		}
	}


	// stickers =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-
	public void sendSticker(User sender, ISFSObject params)
	{
		for (User u : getUserList())
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
		if( isSpectator(user) )
		{
			BattleUtils.getInstance().leave(this, user);
			return;
		}

		if( this.battleField.field.isOperation() )
		{
			setState( this.battleField.STATE_4_ENDED );
			if( retryMode )
			{
				close();
				BattleUtils.getInstance().remove(this);
				return;
			}
			endCalculator.scores[1] = endCalculator.scores[0] = 0;
			calculateResult();
			close();
			BattleUtils.getInstance().remove(this);
		}
		else
		{
			BattleUtils.getInstance().leave(this, user);
		}
	}

	private void pokeBot()
	{
		if( getState() < this.battleField.STATE_1_CREATED || getState() > this.battleField.STATE_4_ENDED )
			return;
		bot.update();
	}

	private void checkEnding(double battleDuration)
	{
		if( getState() > this.battleField.STATE_2_STARTED || battleDuration < 3 )
			return;

		//endCalculator.scores[0] = 3;
		boolean haveWinner = endCalculator.check();
		if( haveWinner )
			end(battleDuration);
		else if( battleDuration > this.battleField.getTime(2) && (endCalculator.ratio() != 1 || this.battleField.field.isOperation()) )
			end(battleDuration);
		else if( ( battleDuration > this.battleField.getTime(3) && !battleField.field.isOperation()) )
			end(battleDuration);
		//trace("duration:" + battleDuration, "t2:" + this.battleField.getTime(2), "t3:" + this.battleField.getTime(3), "ratio:" + endCalculator.ratio());
	}

	private void end(double battleDuration)
	{
		setState( this.battleField.STATE_4_ENDED );
		trace(this.getName(), "ended duration:" + battleDuration, " (" + this.battleField.field.times.toString() + ")");

	    calculateResult();
		close();
	}

	private void calculateResult()
	{
		DBUtils dbUtils = DBUtils.getInstance();
		SFSArray outcomesSFSData = new SFSArray();
		int now = (int) Instant.now().getEpochSecond();

		IntIntMap[] outcomesList = new IntIntMap[battleField.games.length];
	    for (int i=0; i < this.battleField.games.length; i++)
	    {
			Game game = this.battleField.games.__get(i);

			SFSObject outcomeSFS = new SFSObject();
			outcomeSFS.putInt("id", game.player.id);
			outcomeSFS.putText("name", game.player.nickName);
			outcomeSFS.putInt("score", endCalculator.scores[i]);

			outcomesList[i] = Outcome.get( game, this, endCalculator.scores[i], (float)endCalculator.scores[i] / (float)endCalculator.scores[i==0?1:0], now );
			//trace("i:", i, "scores:"+scores[i], "ratio:"+(float)numBuildings[i] / (float)numBuildings[i==0?1:0] );

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
				//trace(game.player.id + ": (", r, outcomesList[i].get(r) , ")" );

				if( game.player.isBot() )
					continue;

				// set battle book outcome
				if( ResourceType.isBook(r) )
				{
					earnedBook = game.exchanger.items.get(outcomesList[i].get(r));
					earnedBook.outcomesStr = r + ":" + game.player.get_arena(0);
					earnedBook.expiredAt = 0;
				}

				// update stars
				if( r == ResourceType.R17_STARS && game.player.get_arena(0) > 0 )
				{
					int res = game.exchanger.collectStars(outcomesList[i].get(r), now);
					ExchangeItem stars = game.exchanger.items.get(ExchangeType.C104_STARS);
					if( res == MessageTypes.RESPONSE_SUCCEED )
						dbUtils.updateExchange(game, ExchangeType.C104_STARS, stars.expiredAt, stars.numExchanges, "", "");
				}

				outcomeSFS.putInt(r + "", outcomesList[i].get(r));
			}

			outcomesSFSData.addSFSObject(outcomeSFS);

			// update DB
			if( !game.player.isBot() )
			{
				//trace("battle outcomes:", outcomesList[i].toString());
				// increase daily battles
				if( game.player.get_battleswins() > 0 )
				{
					ExchangeItem dailyBattles = game.exchanger.items.get(ExchangeType.C29_DAILY_BATTLES);
					if( dailyBattles == null )
						dailyBattles = new ExchangeItem(ExchangeType.C29_DAILY_BATTLES, 0, 0, "", "");
					dailyBattles.numExchanges ++;
					dbUtils.updateExchange(game, ExchangeType.C29_DAILY_BATTLES, dailyBattles.expiredAt, dailyBattles.numExchanges, "", "");
				}

				// add rewards
				game.player.addResources(outcomesList[i]);
				if( earnedBook != null )
					dbUtils.updateExchange(game, earnedBook.type,0, earnedBook.numExchanges, earnedBook.outcomesStr, "");
				dbUtils.updateResources(game.player, updateMap);
				dbUtils.insertResources(game.player, insertMap);
			}
		}

		sendData(outcomesSFSData);
		//updateChallenges(outcomesList, now);
		updateLobbies();
	}

    /*private void updateChallenges(IntIntMap[] outcomesList, int now)
    {
        if( this.battleField.field.isOperation() || this.battleField.friendlyMode > 0 )
            return;

        for (int i=0; i < this.battleField.games.length; i++)
        {
            Game game = this.battleField.games.__get(i);    // update active challenges
            if( !game.player.isBot() && outcomesList[i].get(ResourceType.R2_POINT) > 0 )
            {
                ISFSArray challenges = ChallengeUtils.getInstance().getChallengesOfAttendee(-1, game.player, false);
                for (int c = 0; c < challenges.size(); c++)
                {
                    ChallengeSFS challenge = (ChallengeSFS) challenges.getSFSObject(c);
                    if( challenge.base.getState(now) != Challenge.STATE_1_STARTED || game.inBattleChallengMode != challenge.base.type )
                        continue;
                    ISFSObject attendee = ChallengeUtils.getInstance().getAttendee(game.player.id, challenge);
                    attendee.putInt("point", attendee.getInt("point") + 1);
                    attendee.putInt("updateAt", now);
                    ChallengeUtils.getInstance().scheduleSave(challenge);
                }
            }
        }
    }*/

	private void updateLobbies()
	{
		if( this.battleField.field.isOperation() )
			return;
		LobbySFS lobbySFS;
		for( int i=0; i < this.battleField.games.length; i++ )
		{
			Game game = this.battleField.games.__get(i);
			lobbySFS = LobbyUtils.getInstance().getDataByMember(game.player.id);
			if( lobbySFS == null )
				return;

			int index = LobbyUtils.getInstance().getMemberIndex(lobbySFS, game.player.id);
			int activity = lobbySFS.getMembers().getSFSObject(index).containsKey("ac") ? lobbySFS.getMembers().getSFSObject(index).getInt("ac") : 0;
			lobbySFS.getMembers().getSFSObject(index).putInt("ac", activity + 1);
			LobbyUtils.getInstance().save(lobbySFS, null, null, -1, -1, -1, -1, lobbySFS.getMembersBytes(), null);
		}
	}
    private void sendData(SFSArray outcomesSFSData)
    {
        SFSObject params = new SFSObject();
        params.putSFSArray("outcomes", outcomesSFSData);//trace(outcomesSFSData.getDump());
        for (int i=0; i < getUserList().size(); i++)
            send( Commands.BATTLE_END, params, getUserList().get(i) );
   }



    private void close()
	{
		this.setAutoRemoveMode(SFSRoomRemoveMode.WHEN_EMPTY);
		if( this.battleField.field.isOperation() || this.getUserList().size() == 0 )
			BattleUtils.getInstance().remove(this);

		if( timer != null )
			timer.cancel(true);
		timer = null;

		if( this.battleField != null )
			battleField.dispose();
		//battleField = null;
	}

	public int getPlayerGroup(User user)
	{
		if( user == null )
			return 0;

		if( this.isSpectator(user) )
			return getPlayerGroup(this.getUserByName(user.getVariable("spectatedUser").getStringValue()));

		return this.battleField.getSide(getGame(user).player.id);
	}

	private void setState(int value)
	{
		if( this.battleField.state == value )
			return;

		battleField.state = value;
		this.setProperty("state", value);
	}
	public int getState()
	{
		return this.battleField.state;
	}

	@Override
	public void destroy()
	{
		//clearAllHandlers();
		if( getState() < this.battleField.STATE_5_DISPOSED )
			setState(BattleField.STATE_5_DISPOSED);

		trace(this.getName(), "destroyed.");
		BattleUtils.getInstance().removeReferences(this);
		super.destroy();
	}

	public String toString() {
		return String.format("[ Battle: %s, Id: %s, mode: %s, type: %s, friendlyMode: %s, state: %s ]", new Object[] { this.getName(), this.getId(), this.getPropertyAsInt("mode"), this.getPropertyAsInt("type"), this.getPropertyAsInt("friendlyMode"), this.getPropertyAsInt("state") });
	}

}