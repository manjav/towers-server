package com.gerantech.towers.sfs.socials.handlers;

import com.gerantech.towers.sfs.socials.LobbyDataUtils;
import com.gt.hazel.RankData;
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
        IMap<Integer, RankData> users = Hazelcast.getOrCreateHazelcastInstance(new Config("aaa")).getMap("users");
        if( params.containsKey("id") )
            LobbyDataUtils.getInstance().fillRoomInfo( getParentExtension().getParentZone().getRoomById(params.getInt("id")), params, users, params.containsKey("all") );
        else
            LobbyDataUtils.getInstance().searchRooms(params, users );

        send("lobbyData", params, sender);
    }
}