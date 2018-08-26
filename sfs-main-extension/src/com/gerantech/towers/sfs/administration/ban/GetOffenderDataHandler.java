package com.gerantech.towers.sfs.administration.ban;

import com.gerantech.towers.sfs.Commands;
import com.gerantech.towers.sfs.utils.BanSystem;
import com.gt.towers.Game;
import com.gt.towers.constants.MessageTypes;
import com.smartfoxserver.v2.entities.User;
import com.smartfoxserver.v2.entities.data.ISFSArray;
import com.smartfoxserver.v2.entities.data.ISFSObject;
import com.smartfoxserver.v2.entities.data.SFSArray;
import com.smartfoxserver.v2.extensions.BaseClientRequestHandler;
import java.sql.SQLException;

/**
 * @author ManJav
 *
 */
public class GetOffenderDataHandler extends BaseClientRequestHandler
{
	public void handleClientRequest(User sender, ISFSObject params)
    {
		Game game = ((Game)sender.getSession().getProperty("core"));
		if( !game.player.admin )
		{
			sendResponse(sender, params, MessageTypes.RESPONSE_NOT_ALLOWED);
			return;
		}

		// get ban count
		ISFSArray banned = new SFSArray();
		String query = "SELECT time FROM banneds WHERE mode > 1 && player_id=" + params.getInt("id");
		try {
			banned = getParentExtension().getParentZone().getDBManager().executeQuery(query, new Object[]{});
		} catch (SQLException e) { e.printStackTrace(); }
		params.putInt("time", banned.size() > 0 ? banned.getSFSObject(0).getInt("time") : 0);

		// get all opened infractions
		params.putSFSArray("infractions", BanSystem.getInstance().getInfractions(params.getInt("id"), 0, 5, "infractions.content, infractions.offend_at"));

		sendResponse(sender, params, MessageTypes.RESPONSE_SUCCEED);
	}

	private void sendResponse(User sender, ISFSObject params, int responseId)
	{
		params.putInt("response", responseId);
		send(Commands.OFFENDER_DATA_GET, params, sender);
	}
}