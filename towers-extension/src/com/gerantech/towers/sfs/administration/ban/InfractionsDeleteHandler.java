package com.gerantech.towers.sfs.administration.ban;

import com.gt.BBGClientRequestHandler;
import com.gt.Commands;
import com.gt.towers.Game;
import com.gt.towers.constants.MessageTypes;
import com.smartfoxserver.v2.entities.User;
import com.smartfoxserver.v2.entities.data.ISFSObject;

import java.sql.SQLException;

/**
 * @author ManJav
 */
public class InfractionsDeleteHandler extends BBGClientRequestHandler
{
	public void handleClientRequest(User sender, ISFSObject params)
	{
		Game game = (Game) sender.getSession().getProperty("core");
		if( !game.player.admin )
		{
			send(Commands.INFRACTIONS_DELETE, MessageTypes.RESPONSE_NOT_ALLOWED, params, sender);
			return;
		}

		try {
			getParentExtension().getParentZone().getDBManager().executeUpdate("DELETE FROM infractions WHERE id=" + params.getInt("id"), new Object[]{});
		} catch (SQLException e) {e.printStackTrace();}

		send(Commands.INFRACTIONS_DELETE, MessageTypes.RESPONSE_SUCCEED, params, sender);
	}
}