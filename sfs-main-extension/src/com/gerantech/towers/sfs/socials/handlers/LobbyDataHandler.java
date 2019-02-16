package com.gerantech.towers.sfs.socials.handlers;

import com.gerantech.towers.sfs.socials.LobbyDataUtils;
import com.gerantech.towers.sfs.socials.LobbyUtils;
import com.gerantech.towers.sfs.utils.RankingUtils;
import com.gt.data.LobbyData;
import com.gt.data.RankData;
import com.smartfoxserver.v2.entities.User;
import com.smartfoxserver.v2.entities.data.ISFSObject;
import com.smartfoxserver.v2.extensions.BaseClientRequestHandler;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by ManJav on 8/24/2017.
 */
public class LobbyDataHandler extends BaseClientRequestHandler
{
    public void handleClientRequest(User sender, ISFSObject params)
    {
        ConcurrentHashMap<Integer, RankData> users = RankingUtils.getInstance().getUsers();
        if( params.containsKey("id") )
        {
            LobbyData lobbyData = null;
            if( params.getInt("id") > 10000 )
                lobbyData = LobbyUtils.getInstance().getDataById(params.getInt("id"));
            else
                lobbyData = (LobbyData)getParentExtension().getParentZone().getRoomById(params.getInt("id")).getProperty("data");
            if( lobbyData != null )
                LobbyDataUtils.getInstance().fillRoomData(lobbyData, params, users, true , false);
        }
        else
            LobbyDataUtils.getInstance().searchRooms(params, users );

        send("lobbyData", params, sender);
    }
}