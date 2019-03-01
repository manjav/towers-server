package com.gerantech.towers.sfs.inbox;

import com.gt.utils.InboxUtils;
import com.smartfoxserver.v2.entities.User;
import com.smartfoxserver.v2.entities.data.ISFSObject;
import com.smartfoxserver.v2.extensions.BaseClientRequestHandler;

/**
 * @author ManJav
 *
 */
public class InboxOpenHandler extends BaseClientRequestHandler
{
	public void handleClientRequest(User sender, ISFSObject params)
    {
    	if( params.getInt("receiverId") > 1000 )
			InboxUtils.getInstance().open(params.getInt("id"));
	}
}