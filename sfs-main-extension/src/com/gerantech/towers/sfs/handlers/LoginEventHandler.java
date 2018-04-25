package com.gerantech.towers.sfs.handlers;
import com.gerantech.towers.sfs.utils.*;
import com.gt.hazel.RankData;
import com.gt.towers.Game;
import com.gt.towers.InitData;
import com.gt.towers.LoginData;
import com.gt.towers.Player;
import com.gt.towers.constants.ExchangeType;
import com.gt.towers.constants.ResourceType;
import com.gt.towers.exchanges.Exchange;
import com.gt.towers.exchanges.ExchangeItem;
import com.gt.towers.exchanges.Exchanger;
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
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClients;

import java.io.IOException;
import java.sql.SQLException;
import java.time.Instant;

/**
 * @author ManJav
 */
public class LoginEventHandler extends BaseServerEventHandler 
{
	public static int UNTIL_MAINTENANCE = 1522626392;
	public static int STARTING_STATE = 0;
	private static int CORE_SIZE = 0;

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

		// check ban
		ISFSObject banData = BanSystem.getInstance().checkBan(inData.getInt("id"), inData.getText("udid"), now);
		if( banData != null )
		{
			outData.putSFSObject("ban", banData);
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

		if( CORE_SIZE == 0 )
		{
			try {
				CORE_SIZE = (int) HttpClients.createDefault().execute(new HttpGet("http://localhost:8080/swfcores/core-" + loginData.coreVersion + ".swf")).getEntity().getContentLength();
				//CORE_SIZE = new URL("http://localhost:8080/swfcores/core-" + loginData.coreVersion + ".swf").openStream().available();
			} catch (IOException e) { e.printStackTrace(); }
			trace("LoginData.coreSize =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=- >>>>>>>>>>>>>>", CORE_SIZE);
		}

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

			if( STARTING_STATE == 0 )
				STARTING_STATE = 1;

			password = PasswordGenerator.generate().toString();

			// Insert to DataBase
			int playerId = 0;
			try {
				playerId = Math.toIntExact((Long)dbManager.executeInsert("INSERT INTO players (name, password) VALUES ('guest', '"+password+"');", new Object[] {}));
				outData.putUtfString(SFSConstants.NEW_LOGIN_NAME, playerId+"");
			} catch (SQLException e) { e.printStackTrace(); }

			// _-_-_-_-_-_-_-_-_-_-_-_-_-_-_- INSERT INITIAL RESOURCES -_-_-_-_-_-_-_-_-_-_-_-_-_-_-_
			// get initial user resources
			SFSArray resources = new SFSArray();
			for (int i : loginData.resources.keys())
			{
				SFSObject so = new SFSObject();

				so.putInt("type", i);
				so.putInt("count", loginData.resources.get(i));
				so.putInt("level", i < 1000 ? loginData.buildingsLevel.get(i) : 0);

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

			// _-_-_-_-_-_-_-_-_-_-_-_-_-_-_- INSERT INITIAL SHOP ITEMS -_-_-_-_-_-_-_-_-_-_-_-_-_-_-_
			SFSArray exchanges = new SFSArray();
			for (int i=0; i<loginData.exchanges.size(); i++)
			{
				int t = loginData.exchanges.get(i);
				int ct = ExchangeType.getCategory(t);
				SFSObject so = new SFSObject();
				so.putInt("type", t);
				so.putInt("num_exchanges", 0);
				so.putInt("expired_at", 0);

				// set outcome :
				if( ct == ExchangeType.C110_BATTLES )
				{
					so.putInt("outcome", Exchanger.getBattleChestType(0));
				}
				else if( ct == ExchangeType.C100_FREES )
				{
					so.putInt("outcome", Exchanger.getChestType(t));
					if( ct == ExchangeType.C100_FREES )
					{
						if( t == ExchangeType.C101_FREE )
							so.putInt("expired_at", now);
						else
							so.putInt("expired_at", now + ExchangeType.getCooldown(so.getInt("outcome")));
					}
				}
				else
				{
					so.putInt("outcome", 0);
				}

				exchanges.addSFSObject( so );
			}

			query = "INSERT INTO exchanges (`type`, `player_id`, `num_exchanges`, `expired_at`, `outcome`) VALUES ";
			for(int i=0; i<exchanges.size(); i++)
			{
				query += "('" + exchanges.getSFSObject(i).getInt("type") + "', '" + playerId + "', '" + exchanges.getSFSObject(i).getInt("num_exchanges") + "', '" +  exchanges.getSFSObject(i).getInt("expired_at") + "', '" +  exchanges.getSFSObject(i).getInt("outcome") + "')" ;
				query += i<exchanges.size()-1 ? ", " : ";";
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
			outData.putSFSArray("quests", new SFSArray());
			outData.putSFSArray("exchanges", exchanges);
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

		if( STARTING_STATE == 0 )
			STARTING_STATE = 1;

		DBUtils dbUtils = DBUtils.getInstance();
		// Retrieve player data from db
		outData.putInt("id", id);
		outData.putText("name", userData.getText("name"));
		outData.putInt("sessionsCount", userData.getInt("sessions_count"));
		outData.putSFSArray("resources", dbUtils.getResources(id));
		outData.putSFSArray("quests", dbUtils.getQuests(id));
		outData.putSFSArray("exchanges", dbUtils.getExchanges(id));
		outData.putSFSArray("prefs", dbUtils.getPrefs(id, inData.getInt("appver")));

		// Find active battle room
		Room room = BattleUtils.getInstance().findActiveBattleRoom(id);
		int joinedRoomId = room == null ? -1 : room.getId();
		session.setProperty("joinedRoomId", joinedRoomId);
		outData.putBool("inBattle", joinedRoomId > -1 );

		initiateCore(session, inData, outData, loginData);
		//trace("initData", outData.getDump());
	}

	private void initiateCore(ISession session, ISFSObject inData, ISFSObject outData, LoginData loginData)
	{
		int now = (int)Instant.now().getEpochSecond();
		outData.putInt("serverTime", now);
		outData.putInt("noticeVersion", loginData.noticeVersion);
		outData.putInt("forceVersion", loginData.forceVersion);
		outData.putText("coreVersion", loginData.coreVersion);
		outData.putInt("coreSize", CORE_SIZE);
		outData.putText("invitationCode", PasswordGenerator.getInvitationCode(outData.getInt("id")));
		outData.putBool("hasQuests", true);
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
			if( element.getInt("type") < 1000 )
				initData.buildingsLevel.set(element.getInt("type"), element.getInt("level"));
		}

		// create quests init data
		ISFSArray quests = outData.getSFSArray("quests");
		for(int i=0; i<quests.size(); i++)
		{
			element = quests.getSFSObject(i);
			initData.quests.set(element.getInt("index"), element.getInt("score"));
		}

		// create exchanges init data
		ISFSArray exchanges = outData.getSFSArray("exchanges");
		for(int i=0; i<exchanges.size(); i++)
		{
			element = exchanges.getSFSObject(i);
			initData.exchanges.set( element.getInt("type"), new Exchange( element.getInt("type"), element.getInt("num_exchanges"), element.getInt("expired_at"), element.getInt("outcome")));
		}

		// init core
		Game game = new Game();
		game.init(initData);
		game.player.tutorialMode = outData.getInt("tutorialMode");
		game.player.hasQuests = outData.getBool("hasQuests");
		if( inData.getInt("appver") >= 2800 )
		{
			// _-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_- GEM -_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_
			addExchangeItem(game, exchanges, ExchangeType.C1_HARD, ResourceType.CURRENCY_REAL, 1000,	ResourceType.CURRENCY_HARD,		100 ,	0, 0);
			addExchangeItem(game, exchanges, ExchangeType.C2_HARD, ResourceType.CURRENCY_REAL, 2000,	ResourceType.CURRENCY_HARD, 	300 ,	0, 0);
			addExchangeItem(game, exchanges, ExchangeType.C3_HARD, ResourceType.CURRENCY_REAL, 5000,	ResourceType.CURRENCY_HARD, 	750 ,	0, 0);
			addExchangeItem(game, exchanges, ExchangeType.C4_HARD, ResourceType.CURRENCY_REAL, 10000,	ResourceType.CURRENCY_HARD,		2000,	0, 0);
			addExchangeItem(game, exchanges, ExchangeType.C5_HARD, ResourceType.CURRENCY_REAL, 50000,	ResourceType.CURRENCY_HARD, 	12000,	0, 0);
			addExchangeItem(game, exchanges, ExchangeType.C6_HARD, ResourceType.CURRENCY_REAL, 100000,	ResourceType.CURRENCY_HARD, 	30000,	0, 0);

			// _-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_- MONEY -_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_
			addExchangeItem(game, exchanges, ExchangeType.C11_SOFT, ResourceType.CURRENCY_HARD, 20,	ResourceType.CURRENCY_SOFT,		500,	0, 0);
			addExchangeItem(game, exchanges, ExchangeType.C12_SOFT, ResourceType.CURRENCY_HARD, 75,	ResourceType.CURRENCY_SOFT,		2000,	0, 0);
			addExchangeItem(game, exchanges, ExchangeType.C13_SOFT, ResourceType.CURRENCY_HARD, 350,	ResourceType.CURRENCY_SOFT,		10000,	0, 0);

			// _-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_- MAGIC -_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_
			addExchangeItem(game, exchanges, ExchangeType.C121_MAGIC, ResourceType.CURRENCY_HARD, 0,	ExchangeType.BOOKS_54_CHROME,	0	,	0, 0);
			addExchangeItem(game, exchanges, ExchangeType.C122_MAGIC, ResourceType.CURRENCY_HARD, 0,	ExchangeType.BOOKS_55_SILVER,	0	,	0, 0);
			addExchangeItem(game, exchanges, ExchangeType.C123_MAGIC, ResourceType.CURRENCY_HARD, 0,	ExchangeType.BOOKS_56_GOLD,		0	,	0, 0);
		}

		session.setProperty("core", game);

		// init and update hazel data
		IMap<Integer, RankData> users = RankingUtils.getInstance().fill(Hazelcast.getOrCreateHazelcastInstance(new Config("aaa")).getMap("users"), game);
		int wb = game.player.resources.exists(ResourceType.BATTLES_COUNT_WEEKLY) ? game.player.resources.get(ResourceType.BATTLES_COUNT_WEEKLY) : 0;
		RankData rd = new RankData(game.player.id, game.player.nickName,  game.player.get_point(), wb);
		if( users.containsKey(game.player.id))
			users.replace(game.player.id, rd);
		else
			users.put(game.player.id, rd);
	}

	private void addExchangeItem(Game game, ISFSArray exchanges, int type, int reqKey, int reqValue, int outKey, int outValue, int numExchanges, int expiredAt)
	{
		SFSObject element = new SFSObject();
		element.putInt("type", type);
		if( ExchangeType.getCategory(type) == ExchangeType.C120_MAGICS )
		{
			element.putInt("outcome", Exchanger.getChestType(type));
		}
		else
		{
			element.putInt("reqKey", reqKey);
			element.putInt("reqValue", reqValue);
			element.putInt("outKey", outKey);
			element.putInt("outValue", outValue);
		}
		exchanges.addSFSObject( element );
		game.exchanger.items.set(type, new ExchangeItem(type, reqKey, reqValue, outKey, outValue, numExchanges, expiredAt));
	}
}