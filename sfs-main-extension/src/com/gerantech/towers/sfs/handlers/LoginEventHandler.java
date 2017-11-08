package com.gerantech.towers.sfs.handlers;
import com.gerantech.towers.sfs.utils.RankingUtils;
import com.gt.hazel.RankData;
import com.gt.towers.constants.ResourceType;
import com.gt.towers.exchanges.Exchanger;
import com.hazelcast.config.Config;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.IMap;
import com.smartfoxserver.v2.core.SFSConstants;
import haxe.root.Array;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import com.gerantech.towers.sfs.utils.Logger;
import com.gerantech.towers.sfs.utils.PasswordGenerator;
import com.gerantech.towers.sfs.utils.UserManager;
import com.gt.towers.Game;
import com.gt.towers.InitData;
import com.gt.towers.LoginData;
import com.gt.towers.constants.ExchangeType;
import com.gt.towers.exchanges.Exchange;
import com.gt.towers.utils.Tracer;
import com.smartfoxserver.bitswarm.sessions.ISession;
import com.smartfoxserver.v2.core.ISFSEvent;
import com.smartfoxserver.v2.core.SFSEventParam;
import com.smartfoxserver.v2.db.IDBManager;
import com.smartfoxserver.v2.entities.Room;
import com.smartfoxserver.v2.entities.data.ISFSArray;
import com.smartfoxserver.v2.entities.data.ISFSObject;
import com.smartfoxserver.v2.entities.data.SFSArray;
import com.smartfoxserver.v2.entities.data.SFSObject;
import com.smartfoxserver.v2.exceptions.SFSErrorCode;
import com.smartfoxserver.v2.exceptions.SFSException;
import com.smartfoxserver.v2.extensions.BaseServerEventHandler;
import com.smartfoxserver.v2.extensions.ExtensionLogLevel;

/**
 * @author ManJav
 */
public class LoginEventHandler extends BaseServerEventHandler 
{

	public static int UNTIL_MAINTENANCE = 1510006666;
	private static int CORE_SIZE = 0;

	public void handleServerEvent(ISFSEvent event) throws SFSException
	{
		String name = (String) event.getParameter(SFSEventParam.LOGIN_NAME);
		String password = (String) event.getParameter(SFSEventParam.LOGIN_PASSWORD);
		ISFSObject inData = (ISFSObject) event.getParameter(SFSEventParam.LOGIN_IN_DATA);
		ISFSObject outData = (ISFSObject) event.getParameter(SFSEventParam.LOGIN_OUT_DATA);
		ISession session = (ISession)event.getParameter(SFSEventParam.SESSION);
		int now = (int)Instant.now().getEpochSecond();

		if( now < UNTIL_MAINTENANCE && inData.getInt("id") != 10412 )
		{
			outData.putInt("umt", UNTIL_MAINTENANCE - now);
			return;
		}

		LoginData loginData = new LoginData();
		if( CORE_SIZE == 0 )
		{
			try {
				CORE_SIZE = new URL("http://localhost:8080/swfcores/core-" + loginData.coreVersion + ".swf").openStream().available();
			} catch (MalformedURLException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
			trace("LoginData.coreSize =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=- >>>>>>>>>>>>>>", CORE_SIZE);
		}

		IDBManager dbManager = getParentExtension().getParentZone().getDBManager();
		// Create new user ============================================================
		if ( inData.getInt("id") < 0 )
		{
			try
			{
				String deviceUDID = inData.getText("udid");
				String deviceModel = inData.getText("device");
				if( inData.getInt("id") == -1 )
				{
					// retrieve user that saved account before
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
				}

				password = PasswordGenerator.generate().toString();

				// Insert to DataBase
				int playerId = Math.toIntExact((Long)dbManager.executeInsert("INSERT INTO players (name, password) VALUES ('guest', '"+password+"');", new Object[] {}));
				outData.putUtfString(SFSConstants.NEW_LOGIN_NAME, playerId+"");

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

				dbManager.executeInsert(query, new Object[] {});

				// _-_-_-_-_-_-_-_-_-_-_-_-_-_-_- INSERT INITIAL SHOP ITEMS -_-_-_-_-_-_-_-_-_-_-_-_-_-_-_
				SFSArray exchanges = new SFSArray();
				int battleChestIndex = 0;
				for (int i=0; i<loginData.exchanges.size(); i++)
	    		{
					int t = loginData.exchanges.get(i);
					SFSObject so = new SFSObject();
					so.putInt("type", t);
					so.putInt("num_exchanges", 0);

					int ct = ExchangeType.getCategory(t);
					if( ct == ExchangeType.S_20_SPECIALS || ct == ExchangeType.S_30_CHEST || ct == ExchangeType.S_40_OTHERS )
						so.putInt("expired_at", now + (t== ExchangeType.S_31_CHEST?0:ExchangeType.getCooldown(t)));
					else
						so.putInt("expired_at", 0);

					// set outcome :
					if( ct == ExchangeType.CHEST_CATE_110_BATTLES )
					{
						so.putInt("outcome", Exchanger.getBattleChestType(battleChestIndex));
						if( battleChestIndex == 0 )
							so.putInt("expired_at", now);
						battleChestIndex ++;
					}
					else if( ct == ExchangeType.CHEST_CATE_120_OFFERS )
					{
						so.putInt("outcome", Exchanger.getOfferChestType(t));
					}
					else
						so.putInt("outcome", 0);

					exchanges.addSFSObject( so );
				}

				query = "INSERT INTO exchanges (`type`, `player_id`, `num_exchanges`, `expired_at`, `outcome`) VALUES ";
				for(int i=0; i<exchanges.size(); i++)
	    		{
					query += "('" + exchanges.getSFSObject(i).getInt("type") + "', '" + playerId + "', '" + exchanges.getSFSObject(i).getInt("num_exchanges") + "', '" +  exchanges.getSFSObject(i).getInt("expired_at") + "', '" +  exchanges.getSFSObject(i).getInt("outcome") + "')" ;
					query += i<exchanges.size()-1 ? ", " : ";";
				}
				dbManager.executeInsert(query, new Object[] {});
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
				if( deviceUDID != null ) {
					dbManager.executeInsert("INSERT INTO devices (`player_id`, `model`, `udid`) VALUES ('" + playerId + "', '" + deviceModel + "', '" + deviceUDID + "');", new Object[] {});
				}
			}
	        catch (SQLException e)
	        {
				e.printStackTrace();
				Logger.throwLoginException(SFSErrorCode.GENERIC_ERROR, "SQL Failed", e.getMessage());
			}
			return;
		}


		// Find player in DB ===========================================================
		try
        {
			int id = Integer.parseInt(name);
			ISFSArray res = dbManager.executeQuery("SELECT name, password, sessions_count FROM players WHERE id="+id+"", new Object[] {});

			if(res.size() != 1)
	        {
				Logger.throwLoginException(SFSErrorCode.LOGIN_BAD_USERNAME, "Login error!", "user id nou found.");
				return;
			}

			ISFSObject userData = res.getSFSObject(0);
			if(!getApi().checkSecurePassword(session, userData.getText("password"), password))
        	{
				Logger.throwLoginException(SFSErrorCode.LOGIN_BAD_PASSWORD, "Login error!", name);
				return;
			}

			// Retrieve player data from db
			outData.putInt("id", id);
			outData.putText("name", userData.getText("name"));
			outData.putInt("sessionsCount", userData.getInt("sessions_count"));
			outData.putSFSArray("resources", UserManager.getResources(getParentExtension(), id));
			outData.putSFSArray("quests", UserManager.getQuests(getParentExtension(), id));
			outData.putSFSArray("exchanges", UserManager.getExchanges(getParentExtension(), id));

    		// Find active battle room
			int joinedRoomId = findActiveBattleRoom(id);
			session.setProperty("joinedRoomId", joinedRoomId);
			outData.putBool("inBattle", joinedRoomId > -1 );

			initiateCore(session, inData, outData, loginData);
		}
        catch (SQLException e)
        {
			e.printStackTrace();
	       	Logger.throwLoginException(SFSErrorCode.GENERIC_ERROR, "SQL Failed", e.getMessage());
        }
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

		InitData initData = new InitData();
		initData.nickName = outData.getText("name");
		initData.id = outData.getInt("id");
		if( inData.containsKey("appver") )
		{
			initData.appVersion = inData.containsKey("appver") ? inData.getInt("appver") : 0;
			initData.market = inData.containsKey("market") ? inData.getText("market") : "none";
		}
		initData.sessionsCount = outData.getInt("sessionsCount");

		ISFSObject element;

		// create resources init data
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
		boolean hasNewChests = false;
		for(int i=0; i<exchanges.size(); i++)
		{
			element = exchanges.getSFSObject(i);

			int t = element.getInt("type");
			// bonus items :
			int ct = ExchangeType.getCategory(t);
			if( ct == ExchangeType.S_20_SPECIALS )
			{
				if( element.getInt("expired_at") < now )
				{
					element.putInt("expired_at", now + ExchangeType.getCooldown(t) );
					element.putInt("outcome", initData.buildingsLevel.getRandomKey() );
					element.putInt("num_exchanges", 0 );
					try {
						UserManager.updateExchange(getParentExtension(), t, initData.id, now+ExchangeType.getCooldown(t), 1, element.getInt("outcome"));
					} catch (Exception e) {
						trace(ExtensionLogLevel.ERROR, e.getMessage());
					}
				}
			}
			if( ct == ExchangeType.CHEST_CATE_110_BATTLES )
				hasNewChests = true;

			initData.exchanges.set( t, new Exchange( t, element.getInt("num_exchanges"), element.getInt("expired_at"), element.getInt("outcome")));
		}

		// add new chests for old players --> backward compatibility
		if( !hasNewChests )
		{
			SFSArray newExchanges = new SFSArray();
			for (int i = 1; i <= 3 ; i++)
			{
				addNewExchangeElement(ExchangeType.CHEST_CATE_110_BATTLES + i, exchanges, newExchanges, initData );
				addNewExchangeElement(ExchangeType.CHEST_CATE_120_OFFERS + i, exchanges, newExchanges, initData );
			}
			String query = "INSERT INTO exchanges (`type`, `player_id`, `num_exchanges`, `expired_at`, `outcome`) VALUES ";
			for(int i=0; i<newExchanges.size(); i++)
			{
				query += "('" + newExchanges.getSFSObject(i).getInt("type") + "', '" + initData.id + "', '" + newExchanges.getSFSObject(i).getInt("num_exchanges") + "', '" +  newExchanges.getSFSObject(i).getInt("expired_at") + "', '" +  newExchanges.getSFSObject(i).getInt("outcome") + "')" ;
				query += i<newExchanges.size()-1 ? ", " : ";";
			}
			trace(query);
			try { getParentExtension().getParentZone().getDBManager().executeInsert(query, new Object[] {}); } catch (SQLException e) { e.printStackTrace(); }

		}

		Tracer tracer = new Tracer() {
			public double __hx_setField_f(String arg0, double arg1, boolean arg2) {
				return 0;
			}
			public Object __hx_setField(String arg0, Object arg1, boolean arg2) {
				return null;
			}
			public double __hx_lookupSetField_f(String arg0, double arg1) {
				return 0;
			}
			public Object __hx_lookupSetField(String arg0, Object arg1) {
				return null;
			}
			public double __hx_lookupField_f(String arg0, boolean arg1) {
				return 0;
			}
			public Object __hx_lookupField(String arg0, boolean arg1, boolean arg2) {
				return null;
			}
			public Object __hx_invokeField(String arg0, Array arg1) {
				return null;
			}
			public void __hx_getFields(Array<String> arg0) {}
			public double __hx_getField_f(String arg0, boolean arg1, boolean arg2) {
				return 0;
			}
			public Object __hx_getField(String arg0, boolean arg1, boolean arg2, boolean arg3) {
				return null;
			}
			public boolean __hx_deleteField(String arg0) {
				return false;
			}
			public void log(String arg0) {
				trace(arg0);
			}
		};

		Game game = new Game(initData, tracer);
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

	private void addNewExchangeElement(int t, ISFSArray exchanges, SFSArray newExchanges, InitData initData)
	{
		SFSObject element = new SFSObject();
		element.putInt("type", t);
		element.putInt("num_exchanges", 0);
		element.putInt("expired_at", 0);
		element.putInt("outcome", ExchangeType.getCategory(t) ==  ExchangeType.CHEST_CATE_110_BATTLES ? Exchanger.getBattleChestType(0) :  Exchanger.getOfferChestType(t));
		newExchanges.addSFSObject( element );
		exchanges.addSFSObject( element );
		initData.exchanges.set( t, new Exchange( t, element.getInt("num_exchanges"), element.getInt("expired_at"), element.getInt("outcome")));
	}

	private int findActiveBattleRoom(int id)
	{
		List<Room> rList = getParentExtension().getParentZone().getRoomList();
		for (Room room : rList)
		{
			if ( !room.isFull() || room.getGroupId() != "quests" )
			{
				ArrayList<Game> registeredPlayers = (ArrayList)room.getProperty("registeredPlayers");
				if( registeredPlayers != null )
					for (Game g : registeredPlayers)
						if (g.player.id == id)
							return room.getId();
			}
		}
		return -1;
	}
}