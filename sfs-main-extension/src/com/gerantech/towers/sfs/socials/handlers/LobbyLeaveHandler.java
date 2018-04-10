package com.gerantech.towers.sfs.socials.handlers;

import com.smartfoxserver.v2.entities.Room;
import com.smartfoxserver.v2.entities.User;
import com.smartfoxserver.v2.entities.data.ISFSObject;
import com.smartfoxserver.v2.extensions.BaseClientRequestHandler;

/**
 * Created by ManJav on 8/24/2017.
 */
public class LobbyLeaveHandler extends BaseClientRequestHandler
{
    public void handleClientRequest(User sender, ISFSObject params)
    {
        if( sender.getLastJoinedRoom() == null )
        {
            trace(sender.getName() + " have not any lobby !");
            return;
        }
        for ( Room r:sender.getJoinedRooms() )
            if( r.getGroupId().equals("lobbies") )
                getApi().leaveRoom(sender, r);
    }
}