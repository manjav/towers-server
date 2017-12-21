package com.gerantech.towers.sfs.handlers;

import com.gerantech.towers.sfs.Commands;
import com.gt.towers.Game;
import com.gt.towers.Player;
import com.gt.towers.constants.ResourceType;
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
public class UserPrefsRequestHandler extends BaseClientRequestHandler
{
	public void handleClientRequest(User sender, ISFSObject params)
    {
		IDBManager dbManager = getParentExtension().getParentZone().getDBManager();
		Player player = ((Game)sender.getSession().getProperty("core")).player;
		if( params.containsKey("k") ) {
			String queryStr = "INSERT INTO userprefs (k, v, player_id) VALUES ("+ params.getInt("k")+",'"+params.getText("v")+"',"+player.id+") ON DUPLICATE KEY UPDATE v='" + params.getText("v")+"'";
			try {
				dbManager.executeUpdate(queryStr, new Object[]{});
			} catch (SQLException e) {
				e.printStackTrace();
			}
			params.removeElement("k");
			params.removeElement("v");
		}
		else
		{
			try {
				ISFSArray data = dbManager.executeQuery("SELECT `k`,`v` FROM userprefs WHERE player_id=" + player.id, new Object[]{});
				params.putSFSArray("map", data);
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}

		send(Commands.PREFS, params, sender);
    }
}