package com.gerantech.towers.sfs.administration.ban;

import com.gerantech.towers.sfs.Commands;
import com.gt.towers.Game;
import com.gt.towers.constants.MessageTypes;
import com.smartfoxserver.v2.entities.User;
import com.smartfoxserver.v2.entities.data.ISFSObject;
import com.smartfoxserver.v2.extensions.BaseClientRequestHandler;
import java.sql.SQLException;

/**
 * @author ManJav
 *
 */
public class InfractionsDeleteHandler extends BaseClientRequestHandler
{
	public void handleClientRequest(User sender, ISFSObject params)
	{
		Game game = (Game) sender.getSession().getProperty("core");
		if( !game.player.admin )
			return;

		try {
			getParentExtension().getParentZone().getDBManager().executeUpdate("DELETE FROM infractions WHERE id=" + params.getInt("id"), new Object[]{});
		} catch (SQLException e) {e.printStackTrace();}

		sendResponse(sender, params, MessageTypes.RESPONSE_SUCCEED);
	}

	private void sendResponse(User sender, ISFSObject params, int response)
	{
		params.putInt("response", response);
		send(Commands.INFRACTIONS_DELETE, params, sender);
	}
}