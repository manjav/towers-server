package com.gerantech.towers.sfs.socials.handlers;

import com.gt.utils.LobbyDataUtils;
import com.gt.utils.LobbyUtils;
import com.gt.utils.RankingUtils;
import com.gt.data.LobbySFS;
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
            LobbySFS lobbyData = LobbyUtils.getInstance().getDataById(params.getInt("id"));
            if( lobbyData != null )
                LobbyDataUtils.getInstance().fillRoomData(lobbyData, params, users, true , false);
        }
        else
            LobbyDataUtils.getInstance().searchRooms(params, users );

        send("lobbyData", params, sender);
    }
}