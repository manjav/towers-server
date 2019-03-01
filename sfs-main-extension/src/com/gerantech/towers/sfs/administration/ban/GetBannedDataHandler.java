package com.gerantech.towers.sfs.administration.ban;

import com.gt.Commands;
import com.gt.utils.DBUtils;
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
public class GetBannedDataHandler extends BaseClientRequestHandler
{
	public void handleClientRequest(User sender, ISFSObject params)
    {
		Game game = ((Game)sender.getSession().getProperty("core"));
		if( !game.player.admin )
		{
			sendResponse(sender, params, MessageTypes.RESPONSE_NOT_ALLOWED);
			return;
		}

		// get udid
		String udid = DBUtils.getInstance().getUDID(params.getInt("id"));

		// create query
		String query = "SELECT players.id, players.name, banneds.message, banneds.mode, banneds.expire_at, banneds.timestamp, banneds.time FROM players INNER JOIN banneds ON players.id = banneds.player_id WHERE players.id = " + params.getInt("id");
		if( udid != null )
			query += " OR banneds.udid = '" + udid + "'";
		trace(query);
		ISFSArray bannes = null;
		try {
			bannes = getParentExtension().getParentZone().getDBManager().executeQuery(query, new Object[]{});
		} catch (SQLException e) { e.printStackTrace(); }

		// get all ban messages
		params.putSFSArray("data", bannes);

		sendResponse(sender, params, MessageTypes.RESPONSE_SUCCEED);
	}

	private void sendResponse(User sender, ISFSObject params, int responseId)
	{
		params.putInt("response", responseId);
		send(Commands.BANNED_DATA_GET, params, sender);
	}
}