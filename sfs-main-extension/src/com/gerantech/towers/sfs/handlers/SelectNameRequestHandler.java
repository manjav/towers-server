package com.gerantech.towers.sfs.handlers;

import java.sql.SQLException;

import com.gt.towers.Game;
import com.smartfoxserver.v2.db.IDBManager;
import com.smartfoxserver.v2.entities.User;
import com.smartfoxserver.v2.entities.data.ISFSObject;
import com.smartfoxserver.v2.extensions.BaseClientRequestHandler;

/**
 * @author ManJav
 *
 */
public class SelectNameRequestHandler extends BaseClientRequestHandler 
{
	
	public SelectNameRequestHandler() {}

	public void handleClientRequest(User sender, ISFSObject params)
    {
		params.putBool("succeed", true);
		Game game = ((Game)sender.getSession().getProperty("core"));

		String name = params.getText("name");
		trace(name, game.player.id);
		if(name.length() < game.loginData.nameMinLen || name.length() > game.loginData.nameMaxLen)
		{
			params.putBool("succeed", false);
			params.putText("errorCode", "popup_select_name_size");
			send("selectName", params, sender);
			return;
		}

		IDBManager dbManager = getParentExtension().getParentZone().getDBManager();
  		try {
			dbManager.executeUpdate("UPDATE `players` SET `name`='" + name + "' WHERE `id`=" + game.player.id + ";", new Object[] {});
		} catch (SQLException e) {
			params.putBool("succeed", false);
			params.putText("errorCode", e.getErrorCode()+"");
			trace(e.getMessage());
		}
		game.player.nickName = name;
		send("selectName", params, sender);
    }
}