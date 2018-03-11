package com.gerantech.towers.sfs.handlers;

import java.sql.SQLException;

import com.gerantech.towers.sfs.Commands;
import com.gerantech.towers.sfs.TowerExtension;
import com.gt.towers.Game;
import com.gt.towers.constants.ResourceType;
import com.gt.towers.exchanges.ExchangeItem;
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
	static int RESPONSE_SUCCEED = 0;
	static int RESPONSE_WRONG_SIZE = 1;
	static int RESPONSE_NOT_ENOUG_REQUIREMENTS = 2;

	public SelectNameRequestHandler() {}

	public void handleClientRequest(User sender, ISFSObject params)
    {
		Game game = ((Game)sender.getSession().getProperty("core"));

		String name = params.getUtfString("name");
		trace(name, game.player.id, game.player.nickName);
		if( name.length() < game.loginData.nameMinLen || name.length() > game.loginData.nameMaxLen )
		{
			params.putBool("succeed", false);
			params.putText("errorCode", "popup_select_name_size");
			params.putInt("response", RESPONSE_WRONG_SIZE);
			send(Commands.SELECT_NAME, params, sender);
			return;
		}

		if( !game.player.nickName.equals("guest") )
		{
			ExchangeItem ei = new ExchangeItem(-1, ResourceType.CURRENCY_HARD, 100, -1, -1, 1, 0);
			if( !(((TowerExtension) getParentExtension()).exchangeHandler).exchange(game, ei, 0, 0) )
			{
				params.putBool("succeed", false);
				params.putInt("response", RESPONSE_NOT_ENOUG_REQUIREMENTS);
				send(Commands.SELECT_NAME, params, sender);
				return;
			}
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
		params.putBool("succeed", true);
		params.putInt("response", RESPONSE_SUCCEED);
		send(Commands.SELECT_NAME, params, sender);
    }
}