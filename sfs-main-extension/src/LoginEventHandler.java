import java.sql.SQLException;

import com.smartfoxserver.bitswarm.sessions.ISession;
import com.smartfoxserver.v2.core.ISFSEvent;
import com.smartfoxserver.v2.core.SFSEventParam;
import com.smartfoxserver.v2.db.IDBManager;
import com.smartfoxserver.v2.entities.data.ISFSArray;
import com.smartfoxserver.v2.entities.data.ISFSObject;
import com.smartfoxserver.v2.exceptions.SFSErrorCode;
import com.smartfoxserver.v2.exceptions.SFSErrorData;
import com.smartfoxserver.v2.exceptions.SFSException;
import com.smartfoxserver.v2.exceptions.SFSLoginException;
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
			outData.putText("password", password);
			
	        try
	        {
	        	// Insert to DB
	            long playerId = (Long) dbManager.executeInsert("INSERT INTO players (name, password) VALUES ('guest', '"+password+"');", new Object[] {});
				reterivePlayerData(outData, playerId);
	        }
	        catch (SQLException e)
	        {
	        	//trace(ExtensionLogLevel.WARN, "SQL Failed: " + e.toString());
	        	warn(SFSErrorCode.GENERIC_ERROR, "SQL Failed", e.toString());
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
	        	warn(SFSErrorCode.LOGIN_BAD_USERNAME, "Login error!", "user id nou found.");
	        	return;
	        }
	        
        	if(!getApi().checkSecurePassword(session, res.getSFSObject(0).getText("password"), password))
        	{
	        	warn(SFSErrorCode.LOGIN_BAD_PASSWORD, "Login error!", name);
	        	return;
        	}
			reterivePlayerData(outData, id);
        }
        catch (SQLException e)
        {
        	warn(SFSErrorCode.GENERIC_ERROR, "SQL Failed", e.toString());
        }
	}

	
	private void warn(SFSErrorCode errorCode, String message, String param) throws SFSException 
	{
    	//trace(ExtensionLogLevel.WARN, "SQL Failed: " + e.toString());
		SFSErrorData errData = new SFSErrorData(errorCode);
		errData.addParameter(param);
		throw new SFSLoginException(message, errData);		
	}

	private void reterivePlayerData(ISFSObject outData, long playerId) 
	{
		outData.putLong("id", playerId);
		// load player data from db
	}
}