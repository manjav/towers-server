package com.gerantech.towers.sfs.handlers;

import java.sql.SQLException;

import com.gerantech.towers.sfs.Commands;
import com.gerantech.towers.sfs.TowerExtension;
import com.gerantech.towers.sfs.utils.ExchangeManager;
import com.gt.towers.Game;
import com.gt.towers.constants.ExchangeType;
import com.gt.towers.constants.MessageTypes;
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
		// forbidden size ...
		if( name.length() < game.loginData.nameMinLen || name.length() > game.loginData.nameMaxLen )
		{
			params.putInt("response", game.appVersion > 3300 ? MessageTypes.RESPONSE_UNKNOWN_ERROR : RESPONSE_WRONG_SIZE);
			send(Commands.SELECT_NAME, params, sender);
			return;
		}

		// forbidden characters ...
		if( name.indexOf("'") > -1 )
		{
			params.putInt("response", MessageTypes.RESPONSE_NOT_ALLOWED);
			send(Commands.SELECT_NAME, params, sender);
			return;
		}

		if( !game.player.nickName.equals("guest") )
		{
			int res = ExchangeManager.getInstance().process(game, game.exchanger.items.get(ExchangeType.C42_RENAME), 0, 0);
			if( res != MessageTypes.RESPONSE_SUCCEED )
			{
				params.putInt("response", game.appVersion > 3300 ? MessageTypes.RESPONSE_NOT_ENOUGH_REQS : RESPONSE_NOT_ENOUG_REQUIREMENTS);
				send(Commands.SELECT_NAME, params, sender);
				return;
			}
		}

		IDBManager dbManager = getParentExtension().getParentZone().getDBManager();
  		try {
			dbManager.executeUpdate("UPDATE `players` SET `name`='" + name + "' WHERE `id`=" + game.player.id + ";", new Object[] {});
		} catch (SQLException e) {
			params.putText("errorCode", e.getErrorCode() + "");
			trace(e.getMessage());
		}
		game.player.nickName = name;
		params.putInt("response", game.appVersion > 3300 ? MessageTypes.RESPONSE_SUCCEED : RESPONSE_SUCCEED);
		send(Commands.SELECT_NAME, params, sender);
    }


}