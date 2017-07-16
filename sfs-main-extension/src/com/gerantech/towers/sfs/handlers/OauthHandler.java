package com.gerantech.towers.sfs.handlers;

import com.gt.towers.Game;
import com.smartfoxserver.v2.db.IDBManager;
import com.smartfoxserver.v2.entities.User;
import com.smartfoxserver.v2.entities.data.ISFSArray;
import com.smartfoxserver.v2.entities.data.ISFSObject;
import com.smartfoxserver.v2.extensions.BaseClientRequestHandler;

/**
 * @author ManJav
 *
 */
public class OauthHandler extends BaseClientRequestHandler 
{
	public OauthHandler() {}
	public void handleClientRequest(User sender, ISFSObject params)
    {
		int playerId = ((Game)sender.getSession().getProperty("core")).player.id;
		String playerName = null;
		String playerPassword = null;
		
		boolean needInsert = true;
		IDBManager dbManager = getParentExtension().getParentZone().getDBManager();
	
		try {
			
			// retrieve user that saved account before
	        ISFSArray res = dbManager.executeQuery("SELECT type, player_id FROM accounts WHERE id='"+params.getText("accountId")+"' OR player_id="+playerId, new Object[] {});
	        if(res.size() > 0)
	        {
	        	playerId = res.getSFSObject(0).getInt("player_id");
	        	
	        	needInsert = !linkExists(res, params.getInt("accountType"));
		        ISFSArray res2 = dbManager.executeQuery("SELECT name, password FROM players WHERE id="+playerId, new Object[] {});
		        if(res2.size() > 0)
		        {
		        	playerName = res2.getSFSObject(0).getText("name");
		        	playerPassword = res2.getSFSObject(0).getText("password");
		        }
	        }
	        if( needInsert )
	        {
	        	String q = "INSERT INTO accounts (`player_id`, `type`, `id`, `name`, `image_url`) VALUES ('" + playerId + "', '" + params.getInt("accountType") + "', '" + params.getText("accountId") + "', '" + params.getText("accountName") + "', '" + params.getText("accountImageURL") + "');";
	        	dbManager.executeInsert(q, new Object[] {});
	        }
		} catch (Exception e) {
			trace(e.getMessage());
		}
		
		// sent params to sender
		params.putInt("playerId", playerId);
		if( playerName != null )
			params.putText("playerName", playerName);
		if( playerPassword != null )
			params.putText("playerPassword", playerPassword);
		send("oauth", params, sender);
		trace(params.getDump());

    }
	private boolean linkExists(ISFSArray res, int accountType )
	{
		boolean ret = false;
		for(int i=0; i<res.size(); i++)
    		if( res.getSFSObject(i).getInt("type") == accountType )
				ret = true;
		return ret;
	}
}