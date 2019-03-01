package com.gerantech.towers.sfs.handlers;

import com.gt.Commands;
import com.gt.towers.Game;
import com.smartfoxserver.v2.db.IDBManager;
import com.smartfoxserver.v2.entities.User;
import com.smartfoxserver.v2.entities.data.ISFSArray;
import com.smartfoxserver.v2.entities.data.ISFSObject;
import com.smartfoxserver.v2.extensions.BaseClientRequestHandler;

import java.sql.SQLException;

/**
 * @author ManJav
 *
 */
public class OauthHandler extends BaseClientRequestHandler 
{
	public void handleClientRequest(User sender, ISFSObject params)
    {
		int playerId = ((Game)sender.getSession().getProperty("core")).player.id;
		IDBManager dbManager = getParentExtension().getParentZone().getDBManager();
	
		try {

			// retrieve user that saved account before
			ISFSArray accounts = dbManager.executeQuery("SELECT type, player_id FROM accounts WHERE id='" + params.getText("accountId") + "'", new Object[] {});
			boolean needInsert = accounts.size() == 0;
			if( !needInsert && playerId != accounts.getSFSObject(0).getInt("player_id") )// if exists player and his id equals player id
	        {
				playerId = accounts.getSFSObject(0).getInt("player_id");
				ISFSArray players = dbManager.executeQuery("SELECT name, password FROM players WHERE id=" + playerId, new Object[] {});
				if( players.size() > 0 )
				{
					params.putText("playerName",		players.getSFSObject(0).getText("name"));
					params.putText("playerPassword",	players.getSFSObject(0).getText("password"));
				}
				needInsert = !linkExists(accounts, params.getInt("accountType")); // already stored
			}

			if( needInsert )
				dbManager.executeInsert("INSERT INTO accounts (`player_id`, `type`, `id`, `name`, `image_url`) VALUES ('" + playerId + "', '" + params.getInt("accountType") + "', '" + params.getText("accountId") + "', '" + params.getText("accountName") + "', '" + params.getText("accountImageURL") + "');", new Object[] {});

		} catch (SQLException e) { e.printStackTrace(); }

		params.putInt("playerId", playerId);
		send(Commands.OAUTH, params, sender);
    }

	private boolean linkExists(ISFSArray accounts, int accountType )
	{
		boolean ret = false;
		for(int i=0; i<accounts.size(); i++)
    		if( accounts.getSFSObject(i).getInt("type") == accountType )
				ret = true;
		return ret;
	}
}