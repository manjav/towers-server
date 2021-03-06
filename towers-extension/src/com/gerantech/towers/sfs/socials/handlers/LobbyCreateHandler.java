package com.gerantech.towers.sfs.socials.handlers;

import com.gt.Commands;
import com.gt.callbacks.MapChangeCallback;
import com.gt.utils.DBUtils;
import com.gt.towers.Game;
import com.smartfoxserver.v2.entities.Room;
import com.smartfoxserver.v2.entities.User;
import com.smartfoxserver.v2.entities.data.ISFSObject;
import com.smartfoxserver.v2.extensions.BaseClientRequestHandler;
import com.gt.utils.LobbyUtils;

/**
 * Created by ManJav on 8/24/2017.
 */
public class LobbyCreateHandler extends BaseClientRequestHandler
{
    public static final int RESPONSE_OK = 0;
    public static final int RESPONSE_ROOM_EXISTS = -1;
    public static final int RESPONSE_INTERNAL_ERROR = -2;
    public static final int RESPONSE_UNKOWN_ERROR = -10;

    public void handleClientRequest(User sender, ISFSObject params)
    {
        Game game = (Game)(sender.getSession().getProperty("core"));

        MapChangeCallback mapChangeCallback = new MapChangeCallback();
        game.player.resources.changeCallback = mapChangeCallback;
        Boolean succeed = game.lobby.create();
        game.player.resources.changeCallback = null;

        if( !succeed )
        {
            trace("lobby create failed: RESPONSE_INTERNAL_ERROR");
            params.putInt("response", RESPONSE_INTERNAL_ERROR);
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
            params.putInt("response", RESPONSE_ROOM_EXISTS);
            send("lobbyCreate", params, sender);
            return;
        }

        DBUtils.getInstance().updateResources(game.player, mapChangeCallback.updates);
        Room room = LobbyUtils.getInstance().create(sender, roomName, bio, emblem, capacity, minPoint, privacy);
        if( room == null )
        {
            //send("lobbyCreate", params, sender);
            params.putInt("response", RESPONSE_UNKOWN_ERROR);
            send("lobbyCreate", params, sender);
            return;
        }

        params.putInt("response", RESPONSE_OK);
        send(Commands.LOBBY_CREATE, params, sender);
    }
}