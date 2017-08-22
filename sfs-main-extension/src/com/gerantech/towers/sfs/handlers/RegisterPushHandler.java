package com.gerantech.towers.sfs.handlers;

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
public class RegisterPushHandler extends BaseClientRequestHandler
{

	public RegisterPushHandler() {}

	public void handleClientRequest(User sender, ISFSObject params)
    {
		IDBManager dbManager = getParentExtension().getParentZone().getDBManager();
		try{
			String queryStr = "INSERT INTO pushtokens (player_id, os_pid, os_token) VALUES ("+((Game)sender.getSession().getProperty("core")).player.id+", '"+params.getText("oneSignalUserId")+"', '"+params.getText("oneSignalPushToken")+"') ON DUPLICATE KEY UPDATE os_pid=VALUES(os_pid), os_token=VALUES(os_token)";
			dbManager.executeUpdate(queryStr, new Object[]{});
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
}