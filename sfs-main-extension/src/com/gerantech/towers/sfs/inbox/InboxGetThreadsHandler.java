package com.gerantech.towers.sfs.inbox;

import com.gt.Commands;
import com.gt.utils.InboxUtils;
import com.gt.towers.Game;
import com.smartfoxserver.v2.entities.User;
import com.smartfoxserver.v2.entities.data.ISFSObject;
import com.smartfoxserver.v2.extensions.BaseClientRequestHandler;

/**
 * @author ManJav
 *
 */
public class InboxGetThreadsHandler extends BaseClientRequestHandler
{
	public void handleClientRequest(User sender, ISFSObject params)
    {
    	int id = params.containsKey("id") ?  params.getInt("id") : ((Game)sender.getSession().getProperty("core")).player.id;
		params.putSFSArray("data", InboxUtils.getInstance().getThreads(id, 50));
		send(Commands.INBOX_GET_THREADS, params, sender);
	}
}