package com.gerantech.towers.sfs.handlers;
import haxe.root.Array;

import java.sql.SQLException;
import java.time.Instant;
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

	/*
	 * (non-Javadoc)
	 * @see
	 * com.smartfoxserver.v2.extensions.IServerEventHandler#handleServerEvent
	 * (com.smartfoxserver.v2.core.ISFSEvent)
	 */
	@SuppressWarnings("unchecked")
	public void handleServerEvent(ISFSEvent event) throws SFSException 
	{
		String name = (String) event.getParameter(SFSEventParam.LOGIN_NAME);
		String password = (String) event.getParameter(SFSEventParam.LOGIN_PASSWORD);
		ISFSObject outData = (ISFSObject) event.getParameter(SFSEventParam.LOGIN_OUT_DATA);
        ISession session = (ISession)event.getParameter(SFSEventParam.SESSION);
        LoginData loginData = new LoginData();

		IDBManager dbManager = getParentExtension().getParentZone().getDBManager();

		// Create new user ============================================================
		if (name.equals("-1")) 
		{
			password = PasswordGenerator.generate().toString();
			
	        try
	        {
	        	// Insert to DataBase
	            int playerId = Math.toIntExact((Long)dbManager.executeInsert("INSERT INTO players (name, password) VALUES ('guest', '"+password+"');", new Object[] {}));
	    		
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
	    		int now = (int)Instant.now().getEpochSecond();
	    		for (int i=0; i<loginData.exchanges.size(); i++)
	    		{
	    			int t = loginData.exchanges.get(i);
		    		SFSObject so = new SFSObject();
		    		so.putInt("type", t);
		    		so.putInt("num_exchanges", 1);
		    		so.putInt("outcome", 0);
		    		
					if( ExchangeType.getCategory(t) == ExchangeType.S_20_BUILDING || ExchangeType.getCategory(t) == ExchangeType.S_30_CHEST)
						so.putInt("expired_at", now + (t==ExchangeType.S_31_CHEST?0:ExchangeType.getCooldown(t)));
					else
						so.putInt("expired_at", 0);
	    		
		    		// bonus :
					if( ExchangeType.getCategory(t) == ExchangeType.S_20_BUILDING )
			    		so.putInt("outcome", loginData.buildingsLevel.getRandomKey());

					exchanges.addSFSObject( so );
	    		}
	    		trace(exchanges.getDump());
	    		query = "INSERT INTO exchanges (`type`, `player_id`, `num_exchanges`, `expired_at`, `outcome`) VALUES ";
	    		for(int i=0; i<exchanges.size(); i++)
	    		{
	    			query += "('" + exchanges.getSFSObject(i).getInt("type") + "', '" + playerId + "', '" + exchanges.getSFSObject(i).getInt("num_exchanges") + "', '" +  exchanges.getSFSObject(i).getInt("expired_at") + "', '" +  exchanges.getSFSObject(i).getInt("outcome") + "')" ;
	    			query += i<exchanges.size()-1 ? ", " : ";";
	    		}
	    		dbManager.executeInsert(query, new Object[] {});
	    		trace(query);
	    		
	    		// send data to user
	    		outData.putInt("id", playerId);
	    		outData.putText("name", "guest");
				outData.putText("password", password);
	    		outData.putSFSArray("resources", resources);
	    		outData.putSFSArray("quests", new SFSArray());
	    		outData.putSFSArray("exchanges", exchanges);
	    		initiateCore(session, outData, loginData);
	        }
	        catch (SQLException e)
	        {
	        	//trace(ExtensionLogLevel.WARN, "SQL Failed: " + e.toString());
	        	Logger.warn(SFSErrorCode.GENERIC_ERROR, "SQL Failed", e.getMessage());
	        }
			return;
		} 
		
		
		// Find player in DB ===========================================================
		try
        {
			int id = Integer.parseInt(name);
	        ISFSArray res = dbManager.executeQuery("SELECT * FROM players WHERE id="+id+"", new Object[] {});

	        if(res.size() != 1)
	        {
	        	//trace("name", name, "id", id, "password", password);
	        	Logger.warn(SFSErrorCode.LOGIN_BAD_USERNAME, "Login error!", "user id nou found.");
	        	return;
	        }
	        
	        ISFSObject userData = res.getSFSObject(0);
        	if(!getApi().checkSecurePassword(session, userData.getText("password"), password))
        	{
        		Logger.warn(SFSErrorCode.LOGIN_BAD_PASSWORD, "Login error!", name);
	        	return;
        	}

        	// Retrieve player data from db
        	outData.putInt("id", id);
        	outData.putText("name", userData.getText("name"));
    		outData.putSFSArray("resources", UserManager.getResources(getParentExtension(), id));
    		outData.putSFSArray("quests", UserManager.getQuests(getParentExtension(), id));
    		outData.putSFSArray("exchanges", UserManager.getExchanges(getParentExtension(), id));
    		
    		// find active battle rooms
            List<Room> rList = getParentExtension().getParentZone().getRoomList();
            for (Room room : rList)
            {
                if (!room.isFull() || room.getGroupId()!="quests")
                {
					if (((List<String>) room.getProperty("registeredPlayersId")).contains(id + ""))
                	{
                		outData.putBool("inBattle", true);
                		break;
                	}
                }
            }
    		
    		initiateCore(session, outData, loginData);
		}
        catch (SQLException e)
        {
        	Logger.warn(SFSErrorCode.GENERIC_ERROR, "SQL Failed", e.getMessage());
        }
		//trace("initData", outData.getDump());
	}

	private void initiateCore(ISession session, ISFSObject outData, LoginData loginData)
	{
		int now = (int)Instant.now().getEpochSecond();
		outData.putInt("serverTime", now);
		outData.putInt("noticeVersion", loginData.noticeVersion);
		outData.putInt("forceVersion", loginData.forceVersion);
		outData.putText("coreVersion", loginData.coreVersion);
		
		InitData initData = new InitData();
		initData.nickName = outData.getText("name");
		initData.id = outData.getInt("id");
		
		ISFSObject element;
		
		// create resources init data
		ISFSArray resources = outData.getSFSArray("resources");
		for(int i=0; i<resources.size(); i++)
		{
			element = resources.getSFSObject(i);
			initData.resources.set(element.getInt("type"), element.getInt("count"));
			if(element.getInt("type") < 1000)
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
			
			int t = element.getInt("type");
			// bonus items :
			if( ExchangeType.getCategory(t) == ExchangeType.S_20_BUILDING )
			{
				if( element.getInt("expired_at") < now )
				{
					element.putInt("expired_at", now + ExchangeType.getCooldown(t) );
					element.putInt("outcome", initData.buildingsLevel.getRandomKey() );
					element.putInt("num_exchanges", 1 );
					try {
						UserManager.updateExchange(getParentExtension(), t, initData.id, now+ExchangeType.getCooldown(t), 1, element.getInt("outcome"));
					} catch (SQLException e) {
						trace(ExtensionLogLevel.ERROR, e.getMessage());
					}
		      		trace("UPDATE `exchanges` SET `expired_at`='" + (now+ExchangeType.getCooldown(t)) + "', `num_exchanges`='" + 0 + "', `outcome`='" + element.getInt("outcome") + "' WHERE `type`=" + t + " AND `player_id`=" + initData.id + ";");
				}
			}
			initData.exchanges.set( t, new Exchange( t, element.getInt("num_exchanges"), element.getInt("expired_at"), element.getInt("outcome")));
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
			@SuppressWarnings("rawtypes")
			public Object __hx_invokeField(String arg0, Array arg1) {
				return null;
			}
			public void __hx_getFields(Array<String> arg0) {
			}
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
		
		session.setProperty("core", new Game(initData, tracer));
	}		
}