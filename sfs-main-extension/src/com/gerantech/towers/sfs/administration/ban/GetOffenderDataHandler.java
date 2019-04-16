package com.gerantech.towers.sfs.administration.ban;

import com.gt.BBGClientRequestHandler;
import com.gt.Commands;
import com.gt.towers.Game;
import com.gt.towers.constants.MessageTypes;
import com.gt.utils.BanUtils;
import com.smartfoxserver.v2.entities.User;
import com.smartfoxserver.v2.entities.data.ISFSArray;
import com.smartfoxserver.v2.entities.data.ISFSObject;

/**
 * @author ManJav
 */
public class GetOffenderDataHandler extends BBGClientRequestHandler
{
	public void handleClientRequest(User sender, ISFSObject params)
    {
		Game game = ((Game)sender.getSession().getProperty("core"));
		if( !game.player.admin )
		{
			send(Commands.OFFENDER_DATA_GET, MessageTypes.RESPONSE_NOT_ALLOWED, params, sender);
			return;
		}

		// get ban count
		ISFSArray bannes = BanUtils.getInstance().getBannedUsers(params.getInt("id"), null, 2, 0, "time");
		params.putInt("time", bannes.size() > 0 ? bannes.getSFSObject(0).getInt("time") : 0);

		// get all opened infractions
		params.putSFSArray("infractions", BanUtils.getInstance().getInfractions(params.getInt("id"), 0, 5, "infractions.content, infractions.offend_at"));

		send(Commands.OFFENDER_DATA_GET, MessageTypes.RESPONSE_SUCCEED, params, sender);
	}
}