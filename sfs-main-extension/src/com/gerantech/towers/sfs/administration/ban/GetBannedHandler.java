package com.gerantech.towers.sfs.administration.ban;

import com.gerantech.towers.sfs.Commands;
import com.gt.towers.Game;
import com.gt.towers.constants.MessageTypes;
import com.smartfoxserver.v2.entities.User;
import com.smartfoxserver.v2.entities.data.ISFSArray;
import com.smartfoxserver.v2.entities.data.ISFSObject;
import com.smartfoxserver.v2.extensions.BaseClientRequestHandler;
import java.sql.SQLException;

/**
 * @author ManJav
 *
 */
public class GetBannedHandler extends BaseClientRequestHandler
{
	public void handleClientRequest(User sender, ISFSObject params)
    {
		Game game = ((Game)sender.getSession().getProperty("core"));
		if( !game.player.admin )
		{
			sendResponse(sender, params, MessageTypes.RESPONSE_NOT_ALLOWED);
			return;
		}

		// get name
		ISFSArray banned = null;
		String query = "SELECT time FROM banneds WHERE mode > 1 && player_id=" + params.getInt("id");
		try {
			banned = getParentExtension().getParentZone().getDBManager().executeQuery(query, new Object[]{});
		} catch (SQLException e) { e.printStackTrace(); }
		if( banned == null || banned.size() == 0 )
		{
			sendResponse(sender, params, MessageTypes.RESPONSE_NOT_FOUND);
			params.putInt("time", 0);
			return;
		}

		params.putInt("time", banned.getSFSObject(0).getInt("time"));
		sendResponse(sender, params, MessageTypes.RESPONSE_SUCCEED);
	}

	private void sendResponse(User sender, ISFSObject params, int responseId)
	{
		params.putInt("response", responseId);
		send(Commands.BAN_GET, params, sender);
	}
}