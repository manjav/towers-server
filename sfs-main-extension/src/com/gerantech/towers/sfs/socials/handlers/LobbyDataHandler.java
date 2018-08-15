package com.gerantech.towers.sfs.socials.handlers;

import com.gerantech.towers.sfs.socials.LobbyDataUtils;
import com.gerantech.towers.sfs.socials.LobbyUtils;
import com.gt.data.LobbyData;
import com.gt.hazel.RankData;
import com.gt.towers.Game;
import com.hazelcast.config.Config;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.IMap;
import com.smartfoxserver.v2.entities.User;
import com.smartfoxserver.v2.entities.data.ISFSObject;
import com.smartfoxserver.v2.extensions.BaseClientRequestHandler;

/**
 * Created by ManJav on 8/24/2017.
 */
public class LobbyDataHandler extends BaseClientRequestHandler
{
    public void handleClientRequest(User sender, ISFSObject params)
    {
        Game game = (Game)sender.getSession().getProperty("core");
        IMap<Integer, RankData> users = Hazelcast.getOrCreateHazelcastInstance(new Config("aaa")).getMap("users");
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