package com.gerantech.towers.sfs.socials.handlers;

import com.gt.towers.Game;
import com.smartfoxserver.v2.entities.User;
import com.smartfoxserver.v2.entities.data.ISFSArray;
import com.smartfoxserver.v2.entities.data.ISFSObject;
import com.smartfoxserver.v2.entities.data.SFSArray;
import com.smartfoxserver.v2.entities.data.SFSObject;
import com.smartfoxserver.v2.entities.variables.SFSRoomVariable;
import com.smartfoxserver.v2.exceptions.SFSVariableException;
import com.smartfoxserver.v2.extensions.BaseClientRequestHandler;
import com.smartfoxserver.v2.extensions.js.SFSRoomVariableJS;

import java.time.Instant;

/**
 * Created by ManJav on 8/24/2017.
 */
public class PublicMessageHandler extends BaseClientRequestHandler
{
    public void handleClientRequest(User sender, ISFSObject params)
    {
        send("m", params, sender.getLastJoinedRoom().getUserList());
    }
}