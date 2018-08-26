package com.gerantech.towers.sfs.administration.ban;

import com.gerantech.towers.sfs.Commands;
import com.gerantech.towers.sfs.utils.BanSystem;
import com.gt.towers.Game;
import com.smartfoxserver.v2.entities.User;
import com.smartfoxserver.v2.entities.data.ISFSObject;
import com.smartfoxserver.v2.extensions.BaseClientRequestHandler;

/**
 * @author ManJav
 *
 */
public class InfractionsGetHandler extends BaseClientRequestHandler
{
	public void handleClientRequest(User sender, ISFSObject params)
    {
		Game game = ((Game)sender.getSession().getProperty("core"));
		if( !game.player.admin )
			return;
		params.putSFSArray("data", BanSystem.getInstance().getInfractions(params.containsKey("id") ? params.getInt("id") : -1, -1, 200, null));
		send(Commands.INFRACTIONS_GET, params, sender);
    }
}