package com.gerantech.towers.sfs.socials.handlers;

import com.gerantech.towers.sfs.Commands;
import com.gerantech.towers.sfs.socials.LobbyDataUtils;
import com.gerantech.towers.sfs.utils.RankingUtils;
import com.gt.data.LobbyData;
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
        LobbyData data = (LobbyData) getParentExtension().getParentRoom().getProperty("data");
        LobbyDataUtils.getInstance().fillRoomData(data, params, RankingUtils.getInstance().getUsers(), true, true);
        if( params.containsKey("broadcast") )
            send(Commands.LOBBY_INFO, params, getParentExtension().getParentRoom().getUserList());
        else
            send(Commands.LOBBY_INFO, params, sender);
    }
}