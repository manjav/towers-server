package com.gerantech.towers.sfs.socials.handlers;

import com.gerantech.towers.sfs.Commands;
import com.smartfoxserver.v2.entities.User;
import com.smartfoxserver.v2.entities.data.ISFSObject;
import com.smartfoxserver.v2.extensions.BaseClientRequestHandler;

/**
 * Created by ManJav on 8/24/2017.
 */
public class LobbyInfoHandler extends BaseClientRequestHandler
{
    public void handleClientRequest(User sender, ISFSObject params)
    {
        send(Commands.LOBBY_INFO, params, sender);
    }
}