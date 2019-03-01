package com.gerantech.towers.sfs.handlers;

import com.gt.Commands;
import com.gt.utils.DBUtils;
import com.gt.towers.Game;
import com.smartfoxserver.v2.db.IDBManager;
import com.smartfoxserver.v2.entities.User;
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
		Game game = ((Game)sender.getSession().getProperty("core"));
		if( params.containsKey("k") ) {
			String queryStr = "INSERT INTO userprefs (k, v, player_id) VALUES ("+ params.getInt("k")+",'"+params.getText("v")+"',"+game.player.id+") ON DUPLICATE KEY UPDATE v='" + params.getText("v")+"'";
			try {
				dbManager.executeUpdate(queryStr, new Object[]{});
			} catch (SQLException e) { e.printStackTrace(); }
			params.removeElement("k");
			params.removeElement("v");
		}
		else
		{
			params.putSFSArray("map", DBUtils.getInstance().getPrefs(game.player.id, game.appVersion));
		}

		send(Commands.PREFS, params, sender);
    }
}