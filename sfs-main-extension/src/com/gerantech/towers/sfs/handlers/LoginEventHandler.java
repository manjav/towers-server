package com.gerantech.towers.sfs.handlers;
import java.sql.SQLException;

import com.gerantech.towers.sfs.utils.Logger;
import com.gerantech.towers.sfs.utils.PasswordGenerator;
import com.gerantech.towers.sfs.utils.UserManager;
import com.gt.towers.Game;
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
	    		
	            // get initial user resources
	    		SFSArray resources = new SFSArray();
	    		for (int i : Game.loginData.resources.keys())
	    		{
		    		SFSObject so = new SFSObject();
		    		
		    		so.putInt("type", i);
		    		so.putInt("count", Game.loginData.resources.get(i));
		    		if(i < 1000)
		    			so.putInt("level", Game.loginData.buildingsLevel.get(i));
		    		
		    		resources.addSFSObject( so );
	    		}
	    		
	    		// create insert query
	    		String query = "INSERT INTO resources (`player_id`, `type`, `count`) VALUES ";
	    		for(int i=0; i<resources.size(); i++)
	    		{
	    			query += "('" + playerId + "', '" + resources.getSFSObject(i).getInt("type") + "', '" + resources.getSFSObject(i).getInt("count") + "')" ;
	    			query += i<resources.size()-1 ? ", " : ";";
	    		}

	    		dbManager.executeInsert(query, new Object[] {});
	    		
	    		// send data to user
	    		outData.putLong("id", playerId);
	    		outData.putText("name", "guest");
				outData.putText("password", password);
	    		outData.putSFSArray("resources",resources);
	        	outData.putDouble("coreVersion", Game.loginData.coreVersion);
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
        	outData.putDouble("coreVersion", Game.loginData.coreVersion);
    		SFSArray resources = UserManager.getResources(getParentExtension(), id);
    		outData.putSFSArray("resources", resources);
    		//trace(res.getDump());
		}
        catch (SQLException e)
        {
        	Logger.warn(SFSErrorCode.GENERIC_ERROR, "SQL Failed", e.toString());
        }
	}

}