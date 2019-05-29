package com.gerantech.towers.sfs.socials.handlers;

import com.gt.BBGClientRequestHandler;
import com.gt.Commands;
import com.gt.callbacks.MapChangeCallback;
import com.gt.towers.Game;
import com.gt.towers.constants.MessageTypes;
import com.gt.utils.DBUtils;
import com.gt.utils.LobbyUtils;
import com.smartfoxserver.v2.entities.Room;
import com.smartfoxserver.v2.entities.User;
import com.smartfoxserver.v2.entities.data.ISFSObject;

/**
 * Created by ManJav on 8/24/2017.
 */
public class LobbyCreateHandler extends BBGClientRequestHandler
{

    public void handleClientRequest(User sender, ISFSObject params)
    {
        Game game = (Game)(sender.getSession().getProperty("core"));

        MapChangeCallback mapChangeCallback = new MapChangeCallback();
        game.player.resources.changeCallback = mapChangeCallback;
        Boolean succeed = game.lobby.create();
        game.player.resources.changeCallback = null;

        if( !succeed )
        {
            send(Commands.LOBBY_CREATE, MessageTypes.RESPONSE_NOT_ENOUGH_REQS, params, sender);
            return;
        }
        String roomName = params.getUtfString("name");
        String bio = params.getUtfString("bio");
        int capacity = params.getInt("max");
        int minPoint = params.getInt("min");
        int emblem = params.getInt("pic");
        int privacy = params.getInt("pri");

        if( getParentExtension().getParentZone().getRoomByName(roomName) != null )
        {
            send(Commands.LOBBY_CREATE, MessageTypes.RESPONSE_ALREADY_SENT, params, sender);
            return;
        }

        DBUtils.getInstance().updateResources(game.player, mapChangeCallback.updates);
        Room room = LobbyUtils.getInstance().create(sender, roomName, bio, emblem, capacity, minPoint, privacy);
        if( room == null )
        {
            send(Commands.LOBBY_CREATE, MessageTypes.RESPONSE_UNKNOWN_ERROR, params, sender);
            return;
        }

        send(Commands.LOBBY_CREATE, MessageTypes.RESPONSE_SUCCEED, params, sender);
    }
}