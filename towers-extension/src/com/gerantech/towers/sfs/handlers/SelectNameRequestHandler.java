package com.gerantech.towers.sfs.handlers;

import com.gt.Commands;
import com.gt.towers.Game;
import com.gt.towers.constants.ExchangeType;
import com.gt.towers.constants.MessageTypes;
import com.gt.utils.BanUtils;
import com.gt.utils.DBUtils;
import com.gt.utils.ExchangeUtils;
import com.smartfoxserver.v2.db.IDBManager;
import com.smartfoxserver.v2.entities.User;
import com.smartfoxserver.v2.entities.data.ISFSObject;
import com.smartfoxserver.v2.extensions.BaseClientRequestHandler;
import com.smartfoxserver.v2.util.filters.FilteredMessage;

import java.sql.SQLException;

/**
 * @author ManJav
 *
 */
public class SelectNameRequestHandler extends BaseClientRequestHandler 
{
	public void handleClientRequest(User sender, ISFSObject params)
    {
		Game game = ((Game)sender.getSession().getProperty("core"));

		String name = params.getUtfString("name");
		trace(name, game.player.id, game.player.nickName);
		// forbidden size ...
		if( name.length() < game.loginData.nameMinLen || name.length() > game.loginData.nameMaxLen )
		{
			params.putInt("response",MessageTypes.RESPONSE_UNKNOWN_ERROR);
			send(Commands.SELECT_NAME, params, sender);
			return;
		}

		// forbidden things ...
		FilteredMessage fm = BanUtils.getInstance().filterBadWords(name, false);
		if( (fm != null && fm.getOccurrences() > 0) || name.indexOf("'") > -1 )
		{
			params.putInt("response", MessageTypes.RESPONSE_NOT_ALLOWED);
			send(Commands.SELECT_NAME, params, sender);
			return;
		}

		if( !game.player.nickName.equals("guest") )
		{
			int res = ExchangeUtils.getInstance().process(game, game.exchanger.items.get(ExchangeType.C42_RENAME), 0, 0);
			if( res != MessageTypes.RESPONSE_SUCCEED )
			{
				params.putInt("response", MessageTypes.RESPONSE_NOT_ENOUGH_REQS);
				send(Commands.SELECT_NAME, params, sender);
				return;
			}
		}

		IDBManager dbManager = getParentExtension().getParentZone().getDBManager();
  		try {
			dbManager.executeUpdate("UPDATE " + DBUtils.getInstance().liveDB + ".`players` SET `name`='" + name + "' WHERE `id`=" + game.player.id + ";", new Object[] {});
		} catch (SQLException e) {
			params.putText("errorCode", e.getErrorCode() + "");
			trace(e.getMessage());
		}
		game.player.nickName = name;
		params.putInt("response", MessageTypes.RESPONSE_SUCCEED);
		send(Commands.SELECT_NAME, params, sender);
    }
}