package com.gerantech.towers.sfs.socials.handlers;

import com.smartfoxserver.v2.entities.Room;
import com.smartfoxserver.v2.entities.User;
import com.smartfoxserver.v2.entities.data.ISFSObject;
import com.smartfoxserver.v2.exceptions.SFSJoinRoomException;
import com.smartfoxserver.v2.extensions.BaseClientRequestHandler;

/**
 * Created by ManJav on 8/24/2017.
 */
public class LobbyJoinHandler extends BaseClientRequestHandler
{
    public void handleClientRequest(User sender, ISFSObject params)
    {
        if(sender.getLastJoinedRoom() != null) {
            trace(sender.getName() + " not able to join more lobbies !");
            return;
        }
        Room room = getParentExtension().getParentZone().getRoomById(params.getInt("id"));
        try {
            getApi().joinRoom(sender, room);
        } catch (SFSJoinRoomException e) {
            e.printStackTrace();
        }
    }
}