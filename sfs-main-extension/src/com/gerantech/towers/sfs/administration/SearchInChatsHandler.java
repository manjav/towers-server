package com.gerantech.towers.sfs.administration;

import com.gerantech.towers.sfs.Commands;
import com.gerantech.towers.sfs.socials.LobbyUtils;
import com.gt.towers.Game;
import com.smartfoxserver.v2.entities.User;
import com.smartfoxserver.v2.entities.data.ISFSObject;
import com.smartfoxserver.v2.extensions.BaseClientRequestHandler;

/**
 * @author ManJav
 *
 */
public class SearchInChatsHandler extends BaseClientRequestHandler
{
	public void handleClientRequest(User sender, ISFSObject params)
    {
		Game game = ((Game)sender.getSession().getProperty("core"));
		if( !game.player.admin )
			return;

		params.putSFSArray("result", LobbyUtils.getInstance().searchInChats(params.getUtfString("p")));
		send(Commands.SEARCH_IN_CHATS, params, sender);
    }
}