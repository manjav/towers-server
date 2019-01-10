package com.gerantech.towers.sfs.handlers;
import com.gerantech.towers.sfs.battle.BattleUtils;
import com.gerantech.towers.sfs.challenges.ChallengeUtils;
import com.gerantech.towers.sfs.quests.QuestsUtils;
import com.gerantech.towers.sfs.socials.LobbyUtils;
import com.gerantech.towers.sfs.utils.*;
import com.google.common.base.Charsets;
import com.gt.data.RankData;
import com.gt.towers.Game;
import com.gt.towers.InitData;
import com.gt.towers.LoginData;
import com.gt.towers.Player;
import com.gt.towers.constants.ExchangeType;
import com.gt.towers.constants.ResourceType;
import com.gt.towers.exchanges.ExchangeItem;
import com.gt.towers.exchanges.ExchangeUpdater;
import com.gt.towers.exchanges.Exchanger;
import com.gt.towers.scripts.ScriptEngine;
import com.gt.towers.utils.maps.IntIntMap;
import com.hazelcast.config.Config;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.IMap;
import com.smartfoxserver.bitswarm.sessions.ISession;
import com.smartfoxserver.v2.core.ISFSEvent;
import com.smartfoxserver.v2.core.SFSConstants;
import com.smartfoxserver.v2.core.SFSEventParam;
import com.smartfoxserver.v2.db.IDBManager;
import com.smartfoxserver.v2.entities.Room;
import com.smartfoxserver.v2.entities.data.ISFSArray;
import com.smartfoxserver.v2.entities.data.ISFSObject;
import com.smartfoxserver.v2.entities.data.SFSArray;
import com.smartfoxserver.v2.entities.data.SFSObject;
import com.smartfoxserver.v2.exceptions.SFSException;
import com.smartfoxserver.v2.extensions.BaseServerEventHandler;
import org.apache.commons.io.IOUtils;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClients;

import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.time.Instant;

/**
 * @author ManJav
 */
public class LoginEventHandler extends BaseServerEventHandler 
{
	public static int UNTIL_MAINTENANCE = 1545769843;
	public static int STARTING_STATE = 0;
	private static int CORE_SIZE = 0;
	private DBUtils dbUtils;

	public void handleServerEvent(ISFSEvent event) throws SFSException
	{
		String name = (String) event.getParameter(SFSEventParam.LOGIN_NAME);
		String password = (String) event.getParameter(SFSEventParam.LOGIN_PASSWORD);
		ISFSObject inData = (ISFSObject) event.getParameter(SFSEventParam.LOGIN_IN_DATA);
		ISFSObject outData = (ISFSObject) event.getParameter(SFSEventParam.LOGIN_OUT_DATA);
		ISession session = (ISession)event.getParameter(SFSEventParam.SESSION);

		int now = (int)Instant.now().getEpochSecond();
		//trace("now", now, "UNTIL_MAINTENANCE", UNTIL_MAINTENANCE);
		if( now < UNTIL_MAINTENANCE && !Player.isAdmin(inData.getInt("id")) )
		{
			outData.putInt("umt", UNTIL_MAINTENANCE - now );
			return;
		}

		if( !getParentExtension().getParentZone().containsProperty("startTime") )
			getParentExtension().getParentZone().setProperty("startTime", System.currentTimeMillis());

		// check ban
		ISFSObject banData = BanSystem.getInstance().checkBan(inData.getInt("id"), inData.getText("udid"), now);
		if( banData != null )
		{
			outData.putSFSObject("ban", banData);
			if( banData.getInt("mode") > 2 )
				return;
		}

		// check force update
		LoginData loginData = new LoginData();
		if( inData.containsKey("appver") && inData.getInt("appver") < loginData.forceVersion )
		{
			outData.putInt("forceVersion", loginData.forceVersion);
			//try {
			//	LoginErrors.dispatch (LoginErrors.LOGIN_FORCE_UPDATE, "Force Update", new String[]{loginData.forceVersion+""});
			//} catch (Exception e) { trace(inData.getInt("appver") + " needs " + e.getMessage() + " to " + loginData.forceVersion); }
			return;
		}

		if( STARTING_STATE == 1 && inData.getInt("id") != 10412 )
		{
			outData.putInt("umt", 15);
			return;
		}

		if( STARTING_STATE == 0 )
			STARTING_STATE = 1;

		// load all settings
		//RankingUtils.getInstance().fillStatistics();
		RankingUtils.getInstance().fillActives();
		LobbyUtils.getInstance().loadAll();
		ChallengeUtils.getInstance().loadAll();

		if( CORE_SIZE == 0 )
		{
			try {
				CORE_SIZE = (int) HttpClients.createDefault().execute(new HttpGet("http://localhost:8080/swfcores/core-" + loginData.coreVersion + ".swf")).getEntity().getContentLength();
				//CORE_SIZE = new URL("http://localhost:8080/swfcores/core-" + loginData.coreVersion + ".swf").openStream().available();
			} catch (IOException e) { e.printStackTrace(); }
			trace("LoginData  =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=- >>>>>>>>>>>>>> version:", loginData.coreVersion + "coreSize:", CORE_SIZE);
		}

		if( LoginEventHandler.STARTING_STATE == 1 )
			LoginEventHandler.STARTING_STATE = 2;

		dbUtils = DBUtils.getInstance();
		IDBManager dbManager = getParentExtension().getParentZone().getDBManager();
		// Create new user ============================================================
		if( inData.getInt("id") < 0 )
		{
			String deviceUDID = inData.getText("udid");
			String deviceModel = inData.getText("device");
			if( inData.getInt("id") == -1 )
			{
				// retrieve user that saved account before
				try {
				ISFSArray res = dbManager.executeQuery("SELECT player_id FROM devices WHERE udid='" + deviceUDID + "' AND model='" + deviceModel + "'", new Object[]{});
				if ( res.size() > 0 )
				{
					ISFSArray res2 = dbManager.executeQuery("SELECT id, name, password FROM players WHERE id=" + res.getSFSObject(0).getInt("player_id"), new Object[]{});
					if ( res2.size() > 0 )
					{
						outData.putBool("exists", true);
						outData.putInt("id", res2.getSFSObject(0).getInt("id"));
						outData.putText("name", res2.getSFSObject(0).getText("name"));
						outData.putText("password", res2.getSFSObject(0).getText("password"));
						return;
					}
				}
				} catch (SQLException e) { e.printStackTrace(); }
			}

			password = PasswordGenerator.generate().toString();

			// Insert to DataBase
			int playerId = 0;
			try {
				playerId = Math.toIntExact((Long)dbManager.executeInsert("INSERT INTO players (name, password) VALUES ('guest', '"+password+"');", new Object[] {}));
				outData.putUtfString(SFSConstants.NEW_LOGIN_NAME, playerId + "");
			} catch (SQLException e) { e.printStackTrace(); }

			// _-_-_-_-_-_-_-_-_-_-_-_-_-_-_- INSERT INITIAL RESOURCES -_-_-_-_-_-_-_-_-_-_-_-_-_-_-_
			// get initial user resources
			SFSArray resources = new SFSArray();
			for (int i : loginData.resources.keys())
			{
				SFSObject so = new SFSObject();

				so.putInt("type", i);
				so.putInt("count", loginData.resources.get(i));
				so.putInt("level", ResourceType.isCard(i) ? 1 : 0);

				resources.addSFSObject( so );
			}

			String query = "INSERT INTO resources (`player_id`, `type`, `count`, `level`) VALUES ";
			for(int i=0; i<resources.size(); i++)
			{
				query += "('" + playerId + "', '" + resources.getSFSObject(i).getInt("type") + "', '" + resources.getSFSObject(i).getInt("count") + "', '" + resources.getSFSObject(i).getInt("level") + "')" ;
				query += i<resources.size()-1 ? ", " : ";";
			}
			try {
			dbManager.executeInsert(query, new Object[] {});
			} catch (SQLException e) { e.printStackTrace(); }


			session.setProperty("joinedRoomId", -1);

			// send data to user
			outData.putInt("id", playerId);
			outData.putInt("sessionsCount", 0);
			outData.putText("name", "guest");
			outData.putText("password", password);
			outData.putSFSArray("resources", resources);
			outData.putSFSArray("operations", new SFSArray());
			outData.putSFSArray("exchanges", new SFSArray());
			outData.putSFSArray("prefs", new SFSArray());
			outData.putSFSArray("decks", dbUtils.createDeck(loginData, playerId));
			initiateCore(session, inData, outData, loginData);

			// add udid and device as account id for restore players
			try {
			if( deviceUDID != null )
				dbManager.executeInsert("INSERT INTO devices (`player_id`, `model`, `udid`) VALUES ('" + playerId + "', '" + deviceModel + "', '" + deviceUDID + "');", new Object[] {});
			} catch (SQLException e) { e.printStackTrace(); }
			return;
		}

		// Find player in DB ===========================================================
		int id = Integer.parseInt(name);
		ISFSArray res = null;
		try { res = dbManager.executeQuery("SELECT name, password, sessions_count FROM players WHERE id=" + id, new Object[]{});
		} catch(SQLException e) { e.printStackTrace(); }

		if( res == null || res.size() != 1 )
		{
			LoginErrors.dispatch(LoginErrors.LOGIN_BAD_USERNAME, "Login error! id=" + id + " name=" + name, new String[]{"user id not found."});
			return;
		}

		ISFSObject userData = res.getSFSObject(0);
		if( !getApi().checkSecurePassword(session, userData.getText("password"), password) )
		{
			LoginErrors.dispatch(LoginErrors.LOGIN_BAD_PASSWORD, "Login error! id" + id + " inpass " + password + " dbpass:" + userData.getText("password"), new String[]{name});
			return;
		}

		// Retrieve player data from db
		outData.putInt("id", id);
		outData.putText("name", userData.getText("name"));
		outData.putInt("sessionsCount", userData.getInt("sessions_count"));
		outData.putSFSArray("resources", dbUtils.getResources(id));
		outData.putSFSArray("operations", dbUtils.getOperations(id));
		outData.putSFSArray("exchanges", dbUtils.getExchanges(id));
		outData.putSFSArray("quests", new SFSArray());
		outData.putSFSArray("prefs", dbUtils.getPrefs(id, inData.getInt("appver")));
		outData.putSFSArray("decks", dbUtils.getDecks(id));

		// Find active battle room
		Room room = BattleUtils.getInstance().findActiveBattleRoom(id);
		int joinedRoomId = room == null ? -1 : room.getId();
		session.setProperty("joinedRoomId", joinedRoomId);
		outData.putBool("inBattle", joinedRoomId > -1 );

		Game game = initiateCore(session, inData, outData, loginData);
//		outData.putSFSArray("challenges", ChallengeUtils.getInstance().getChallengesOfAttendee(-1, game.player, false));
		//trace("initData", outData.getDump());
	}

	private Game initiateCore(ISession session, ISFSObject inData, ISFSObject outData, LoginData loginData)
	{
		int now = (int)Instant.now().getEpochSecond();
		outData.putInt("serverTime", now);
		outData.putInt("noticeVersion", loginData.noticeVersion);
		outData.putInt("forceVersion", loginData.forceVersion);
		outData.putText("coreVersion", loginData.coreVersion);
		outData.putInt("coreSize", CORE_SIZE);
		outData.putText("invitationCode", PasswordGenerator.getInvitationCode(outData.getInt("id")));
		outData.putBool("hasQuests", true);
		outData.putBool("hasOperations", true);
		outData.putInt("tutorialMode", 1);

		InitData initData = new InitData();
		initData.nickName = outData.getText("name");
		initData.id = outData.getInt("id");
		if( inData.containsKey("appver") )
		{
			initData.appVersion = inData.containsKey("appver") ? inData.getInt("appver") : 0;
			initData.market = inData.containsKey("market") ? inData.getText("market") : "none";
		}
		initData.sessionsCount = outData.getInt("sessionsCount");
		
		// create resources init data
		ISFSObject element;
		ISFSArray resources = outData.getSFSArray("resources");
		for(int i=0; i<resources.size(); i++)
		{
			element = resources.getSFSObject(i);
			initData.resources.set(element.getInt("type"), element.getInt("count"));
			if( ResourceType.isCard(element.getInt("type")) )
				initData.buildingsLevel.set(element.getInt("type"), element.getInt("level"));
		}

		// create decks init data
		for(int i=0; i<outData.getSFSArray("decks").size(); i++)
		{
			element = outData.getSFSArray("decks").getSFSObject(i);
			if( !initData.decks.exists(element.getInt("deck_index")) )
				initData.decks.set(element.getInt("deck_index"), new IntIntMap());
			initData.decks.get(element.getInt("deck_index")).set(element.getInt("index"), element.getInt("type"));
		}

		// create operations init data
		ISFSArray operations = outData.getSFSArray("operations");
		for(int i=0; i<operations.size(); i++)
		{
			element = operations.getSFSObject(i);
			initData.operations.set(element.getInt("index"), element.getInt("score"));
		}

		// create exchanges init data
		ISFSArray exchanges = outData.getSFSArray("exchanges");
		boolean contained;
		SFSArray newExchanges = new SFSArray();
		for (int l=0; l<loginData.exchanges.size(); l++)
		{
			contained = false;
			for(int i=0; i<exchanges.size(); i++)
			{
				element = exchanges.getSFSObject(i);
				if( element.getInt("type") == loginData.exchanges.get(l) )
				{
					contained = true;
					break;
				}
			}
			if( !contained && ( initData.appVersion < 3200 || ExchangeType.getCategory(loginData.exchanges.get(l)) != ExchangeType.C20_SPECIALS || initData.resources.get(ResourceType.R2_POINT) > 100 ))// add special after arena 2
				addExchangeToDB(loginData.exchanges.get(l), exchanges, newExchanges);
		}

		/*if( newExchanges.size() > 0 )
		{
			String query = "INSERT INTO exchanges (`type`, `player_id`, `num_exchanges`, `expired_at`, `outcome`) VALUES ";
			for(int i=0; i<newExchanges.size(); i++)
				query += "('" + newExchanges.getSFSObject(i).getInt("type") + "', '" + initData.id + "', '" + newExchanges.getSFSObject(i).getInt("num_exchanges") + "', '" +  newExchanges.getSFSObject(i).getInt("expired_at") + "', '" +  newExchanges.getSFSObject(i).getText("outcome") + "')" + ( i < newExchanges.size() - 1 ? ", " : ";" );
			try { getParentExtension().getParentZone().getDBManager().executeInsert(query, new Object[] {}); } catch (SQLException e) { e.printStackTrace(); }
		}*/

		// load script
		if( ScriptEngine.script == null )
		{
			try {
				InputStream stream = HttpClients.createDefault().execute(new HttpGet("http://localhost:8080/maps/features.js")).getEntity().getContent();
				ScriptEngine.initialize(IOUtils.toString(stream, String.valueOf(Charsets.UTF_8)));
				trace("http://localhost:8080/maps/features.js loaded.");
			} catch (IOException e) { e.printStackTrace(); }
        }
        outData.putText("script", ScriptEngine.script);

		// init core
		Game game = new Game();
		game.init(initData);
		int arena = game.player.get_arena(0);
		game.player.tutorialMode = outData.getInt("tutorialMode");
		game.player.hasOperations = outData.getBool("hasOperations");
		game.exchanger.updater = new ExchangeUpdater(game);

		for(int i=0; i<exchanges.size(); i++)
		{
			element = exchanges.getSFSObject(i);

			element.putInt("num_exchanges", element.getInt("type") > 100 && element.getInt("type") < 104 && element.getInt("expired_at") < now ? 0 : element.getInt("num_exchanges"));
			addExchangeItem(game, exchanges, element.getInt("type"), element.getText("reqs"), element.getText("outcome"), element.getInt("num_exchanges"), element.getInt("expired_at"), false);
		}

		// _-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_- GEM -_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_
		addExchangeItem(game, exchanges, ExchangeType.C0_HARD, ResourceType.R5_CURRENCY_REAL + ":1",			ResourceType.R4_CURRENCY_HARD + ":1"											   ,	0, 0, true);
		addExchangeItem(game, exchanges, ExchangeType.C1_HARD, ResourceType.R5_CURRENCY_REAL + ":2000",		ResourceType.R4_CURRENCY_HARD + ":" + Exchanger.realToHard(2000)		* 0.750,	0, 0, true);
		addExchangeItem(game, exchanges, ExchangeType.C2_HARD, ResourceType.R5_CURRENCY_REAL + ":10000",		ResourceType.R4_CURRENCY_HARD + ":" + Exchanger.realToHard(10000)		* 0.875,	0, 0, true);
		addExchangeItem(game, exchanges, ExchangeType.C3_HARD, ResourceType.R5_CURRENCY_REAL + ":20000",		ResourceType.R4_CURRENCY_HARD + ":" + Exchanger.realToHard(20000)		* 1.000,	0, 0, true);
		addExchangeItem(game, exchanges, ExchangeType.C4_HARD, ResourceType.R5_CURRENCY_REAL + ":40000",		ResourceType.R4_CURRENCY_HARD + ":" + Exchanger.realToHard(40000)		* 1.125,	0, 0, true);
		addExchangeItem(game, exchanges, ExchangeType.C5_HARD, ResourceType.R5_CURRENCY_REAL + ":100000",	ResourceType.R4_CURRENCY_HARD + ":" + Exchanger.realToHard(100000)	* 1.200,	0, 0, true);
		addExchangeItem(game, exchanges, ExchangeType.C6_HARD, ResourceType.R5_CURRENCY_REAL + ":200000",	ResourceType.R4_CURRENCY_HARD + ":" + Exchanger.realToHard(200000)	* 1.250,	0, 0, true);

		// _-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_- MONEY -_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_
		addExchangeItem(game, exchanges, ExchangeType.C11_SOFT, ResourceType.R4_CURRENCY_HARD + ":" + Exchanger.softToHard(1000) * 1.2, 	"1002:1000",		0, 0, true);
		addExchangeItem(game, exchanges, ExchangeType.C12_SOFT, ResourceType.R4_CURRENCY_HARD + ":" + Exchanger.softToHard(5000) * 1.0, 	"1002:5000",		0, 0, true);
		addExchangeItem(game, exchanges, ExchangeType.C13_SOFT, ResourceType.R4_CURRENCY_HARD + ":" + Exchanger.softToHard(50000) * 0.9,	"1002:50000"	,	0, 0, true);

		// _-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_- OTHER -_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_
		if( !game.exchanger.items.exists(ExchangeType.C42_RENAME) )
			addExchangeItem(game, exchanges, ExchangeType.C42_RENAME,	"",	"" ,				0, 0, true);
		if( !game.exchanger.items.exists(ExchangeType.C43_ADS) )
			addExchangeItem(game, exchanges, ExchangeType.C43_ADS,		"",	"51:" + arena,	0, 0, true);
		if( !game.exchanger.items.exists(ExchangeType.C104_STARS) )
			addExchangeItem(game, exchanges, ExchangeType.C104_STARS,	"",	"",				0,		now,	 true);

		// _-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_- MAGIC -_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_
		IntIntMap magics = Exchanger.estimateBookOutcome(ExchangeType.BOOK_55_PIRATE, arena, game.player.splitTestCoef);
		addExchangeItem(game, exchanges, ExchangeType.C121_MAGIC, ResourceType.R4_CURRENCY_HARD + ":" + Exchanger.fixedRound(Exchanger.toHard(Exchanger.estimateBookOutcome(ExchangeType.BOOK_55_PIRATE,	arena, game.player.splitTestCoef))),	ExchangeType.BOOK_55_PIRATE	+ ":" + arena, 0, 0, true);
		addExchangeItem(game, exchanges, ExchangeType.C122_MAGIC, ResourceType.R4_CURRENCY_HARD + ":" + Exchanger.fixedRound(Exchanger.toHard(Exchanger.estimateBookOutcome(ExchangeType.BOOK_56_JUNGLE,	arena, game.player.splitTestCoef))),	ExchangeType.BOOK_56_JUNGLE	+ ":" + arena, 0, 0, true);
		addExchangeItem(game, exchanges, ExchangeType.C123_MAGIC, ResourceType.R4_CURRENCY_HARD + ":" + Exchanger.fixedRound(Exchanger.toHard(Exchanger.estimateBookOutcome(ExchangeType.BOOK_58_AMBER,	arena, game.player.splitTestCoef))),	ExchangeType.BOOK_58_AMBER	+ ":" + arena, 0, 0, true);

		session.setProperty("core", game);

		try {
		for( ExchangeItem item : game.exchanger.updater.changes )
			dbUtils.updateExchange(item.type, game.player.id, item.expiredAt, item.numExchanges, item.outcomesStr, item.requirementsStr);
		} catch (Exception e) { e.printStackTrace(); }

		// create exchange data
		SFSArray _exchanges = new SFSArray();
		for ( ExchangeItem ex : game.exchanger.items.values() )
			_exchanges.addSFSObject(ExchangeManager.toSFS(ex));
		outData.putSFSArray("exchanges", _exchanges);

		// init and update hazel data
		IMap<Integer, RankData> users = Hazelcast.getOrCreateHazelcastInstance(new Config("aaa")).getMap("users");
		RankData rd = new RankData(game.player.id, game.player.nickName,  game.player.get_point(), game.player.getResource(ResourceType.R14_BATTLES_WEEKLY));
		if( users.containsKey(game.player.id))
			users.replace(game.player.id, rd);
		else
			users.put(game.player.id, rd);

		// insert quests in registration or get in next time
		ISFSArray quests = QuestsUtils.getInstance().getAll(game.player.id);
		if( quests.size() > 0 )
			QuestsUtils.getInstance().updateAll(game.player, quests);
		else
			QuestsUtils.getInstance().insertNewQuests(game.player);
		outData.putSFSArray("quests", QuestsUtils.toSFS(game.player.quests));

		return game;
	}

	private void addExchangeToDB(int type, ISFSArray exchanges, SFSArray newExchanges)
	{
		SFSObject element = new SFSObject();
		element.putInt("type", type);
		element.putInt("num_exchanges", 0);
		element.putInt("expired_at", 1);
		element.putText("outcome", "");
		//newExchanges.addSFSObject( element );
		exchanges.addSFSObject( element );
	}

	private void addExchangeItem(Game game, ISFSArray exchanges, int type, String reqsStr, String outsStr, int numExchanges, int expiredAt, boolean addSFS)
	{
		ExchangeItem item = new ExchangeItem(type, numExchanges, expiredAt, reqsStr, outsStr);
		game.exchanger.items.set(type, item);
		game.exchanger.updater.update(item);
		if( addSFS )
			exchanges.addSFSObject( ExchangeManager.toSFS(item) );
	}
}