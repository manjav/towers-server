package com.gerantech.towers.sfs.handlers;
import java.sql.SQLException;
import java.time.Instant;

import com.gerantech.towers.sfs.utils.Logger;
import com.gerantech.towers.sfs.utils.PasswordGenerator;
import com.gerantech.towers.sfs.utils.UserManager;
import com.gt.towers.Game;
import com.gt.towers.InitData;
import com.gt.towers.constants.ExchangeType;
import com.gt.towers.exchanges.Exchange;
import com.smartfoxserver.bitswarm.sessions.ISession;
import com.smartfoxserver.v2.core.ISFSEvent;
import com.smartfoxserver.v2.core.SFSEventParam;
import com.smartfoxserver.v2.db.IDBManager;
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
	public void handleServerEvent(ISFSEvent event) throws SFSException 
	{
		String name = (String) event.getParameter(SFSEventParam.LOGIN_NAME);
		String password = (String) event.getParameter(SFSEventParam.LOGIN_PASSWORD);
		ISFSObject outData = (ISFSObject) event.getParameter(SFSEventParam.LOGIN_OUT_DATA);
        ISession session = (ISession)event.getParameter(SFSEventParam.SESSION);

		IDBManager dbManager = getParentExtension().getParentZone().getDBManager();

		// Create new user ============================================================
		if (name.equals("-1")) 
		{
			password = PasswordGenerator.generate().toString();
			
	        try
	        {
	        	// Insert to DataBase
	            long playerId = (Long) dbManager.executeInsert("INSERT INTO players (name, password) VALUES ('guest', '"+password+"');", new Object[] {});
	    		
	            // _-_-_-_-_-_-_-_-_-_-_-_-_-_-_- INSERT INITIAL RESOURCES -_-_-_-_-_-_-_-_-_-_-_-_-_-_-_
	            // get initial user resources
	    		SFSArray resources = new SFSArray();
	    		for (int i : Game.loginData.resources.keys())
	    		{
		    		SFSObject so = new SFSObject();
		    		
		    		so.putInt("type", i);
		    		so.putInt("count", Game.loginData.resources.get(i));
		    		so.putInt("level", i < 1000 ? Game.loginData.buildingsLevel.get(i) : 0);
		    		
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
	    		long now = Instant.now().toEpochMilli();
	    		for (int t=0; t<Game.loginData.exchanges.size(); t++)
	    		{
		    		SFSObject so = new SFSObject();
		    		so.putInt("type", t);
		    		so.putInt("num_exchanges", 1);
		    		so.putInt("outcome", 0);
	    		
		    		// bonus :
					if( ExchangeType.getCategory(t) == ExchangeType.S_20_BUILDING )
					{
			    		so.putInt("outcome", Game.loginData.buildingsLevel.getRandomKey());
			    		so.putLong("expired_at", now + ExchangeType.getCooldown(t));
					}
					// chests :
					if( t == ExchangeType.S_31_CHEST )
			    		so.putLong("expired_at", now + ExchangeType.getCooldown(t));
					else if( t == ExchangeType.S_32_CHEST )
			    		so.putLong("expired_at", now + ExchangeType.getCooldown(t));
					else if( t == ExchangeType.S_33_CHEST )
			    		so.putLong("expired_at", now + ExchangeType.getCooldown(t));
					
					exchanges.addSFSObject( so );
	    		}
	    		
	    		query = "INSERT INTO exchanges (`type`, `player_id`, `num_exchanges`, `expired_at`, `outcome`) VALUES ";
	    		for(int i=0; i<exchanges.size(); i++)
	    		{
	    			query += "('" + exchanges.getSFSObject(i).getInt("type") + "', '" + playerId + "', '" + exchanges.getSFSObject(i).getInt("num_exchanges") + "', '" +  new java.sql.Timestamp(exchanges.getSFSObject(i).getLong("expired_at")) + "', '" +  exchanges.getSFSObject(i).getInt("outcome") + "')" ;
	    			query += i<exchanges.size()-1 ? ", " : ";";
	    		}
	    		dbManager.executeInsert(query, new Object[] {});
	    		trace(query);
	    		
	    		// send data to user
	    		outData.putLong("id", playerId);
	    		outData.putText("name", "guest");
				outData.putText("password", password);
				outData.putText("coreVersion", Game.loginData.coreVersion);
	    		outData.putSFSArray("resources", resources);
	    		outData.putSFSArray("quests", new SFSArray());
	    		outData.putSFSArray("exchanges", exchanges);
	    		initiateCore(session, outData);
	        }
	        catch (SQLException e)
	        {
	        	//trace(ExtensionLogLevel.WARN, "SQL Failed: " + e.toString());
	        	Logger.warn(SFSErrorCode.GENERIC_ERROR, "SQL Failed", e.toString());
	        }
			return;
		} 
		
		
		// Find player in DB ===========================================================
		try
        {
			long id = Integer.parseInt(name);
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
        	outData.putLong("id", id);
        	outData.putText("name", userData.getText("name"));
        	outData.putText("coreVersion", Game.loginData.coreVersion);
    		outData.putSFSArray("resources", UserManager.getResources(getParentExtension(), id));
    		outData.putSFSArray("quests", UserManager.getQuests(getParentExtension(), id));
    		outData.putSFSArray("exchanges", UserManager.getExchanges(getParentExtension(), id));
    		
    		initiateCore(session, outData);
		}
        catch (SQLException e)
        {
        	Logger.warn(SFSErrorCode.GENERIC_ERROR, "SQL Failed", e.getMessage());
        }
		//trace("initData", outData.getDump());
	}

	private void initiateCore(ISession session, ISFSObject outData)
	{
		long now = Instant.now().toEpochMilli();
		outData.putLong("serverTime", now);
		
		InitData initData = new InitData();
		initData.nickName = outData.getText("name");
		initData.id = outData.getLong("id").intValue();
		
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
				if( element.getLong("expired_at") < now )
				{
					element.putLong("expired_at", now + ExchangeType.getCooldown(t) );
					element.putInt("outcome", initData.buildingsLevel.getRandomKey() );
					element.putInt("num_exchanges", 1 );
					try {
						UserManager.updateExchange(getParentExtension(), t, initData.id, now+ExchangeType.getCooldown(t), 1, element.getInt("outcome"));
					} catch (SQLException e) {
						trace(ExtensionLogLevel.ERROR, e.getMessage());
					}
		      		trace("UPDATE `exchanges` SET `expired_at`='" + new java.sql.Timestamp(now+ExchangeType.getCooldown(t)) + "', `num_exchanges`='" + 0 + "', `outcome`='" + element.getInt("outcome") + "' WHERE `type`=" + t + " AND `player_id`=" + initData.id + ";");
				}
			}
			initData.exchanges.set( t, new Exchange( t, element.getInt("num_exchanges"), (float)element.getLong("expired_at"), element.getInt("outcome")));
		}

		session.setProperty("core", new Game(initData));
	}		
}