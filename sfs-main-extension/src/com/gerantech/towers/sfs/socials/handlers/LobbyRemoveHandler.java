package com.gerantech.towers.sfs.socials.handlers;

import com.gt.Commands;
import com.gt.utils.LobbyUtils;
import com.gt.data.LobbyData;
import com.gt.towers.Game;
import com.gt.towers.constants.MessageTypes;
import com.smartfoxserver.v2.entities.Room;
import com.smartfoxserver.v2.entities.User;
import com.smartfoxserver.v2.entities.data.ISFSObject;
import com.smartfoxserver.v2.extensions.BaseClientRequestHandler;

import java.util.Map;

/**
 * Created by ManJav on 8/24/2017.
 */
public class LobbyRemoveHandler extends BaseClientRequestHandler
{
    public void handleClientRequest(User sender, ISFSObject params)
    {
        Game game = ((Game)sender.getSession().getProperty("core"));
        if( !game.player.admin )
        {
            sendResponse(MessageTypes.RESPONSE_NOT_ALLOWED, params, sender);
            return;
        }

        Map<Integer, LobbyData> all = LobbyUtils.getInstance().getAllData();
        if( !LobbyUtils.getInstance().getAllData().containsKey(params.getInt("id")) )
        {
            sendResponse(MessageTypes.RESPONSE_NOT_FOUND, params, sender);
            return;
        }

        LobbyData data = LobbyUtils.getInstance().getDataById(params.getInt("id"));
        Room lobby = getParentExtension().getParentZone().getRoomByName(data.getName());
        if( lobby != null )
            for( User u : lobby.getUserList() )
                getApi().disconnectUser(u);

        LobbyUtils.getInstance().remove(params.getInt("id"));
        sendResponse(MessageTypes.RESPONSE_SUCCEED, params, sender);
    }

    private void sendResponse(int response, ISFSObject params, User sender)
    {
        params.putInt("response", response);
        send(Commands.LOBBY_REMOVE, params, sender);
    }
}