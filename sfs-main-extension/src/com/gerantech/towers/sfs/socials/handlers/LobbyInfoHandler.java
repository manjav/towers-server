package com.gerantech.towers.sfs.socials.handlers;

import com.gerantech.towers.sfs.Commands;
import com.gerantech.towers.sfs.socials.LobbyDataUtils;
import com.gt.data.LobbyData;
import com.gt.data.RankData;;
import com.hazelcast.config.Config;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.IMap;
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
        IMap<Integer, RankData> users = Hazelcast.getOrCreateHazelcastInstance(new Config("aaa")).getMap("users");
        LobbyData data = (LobbyData) getParentExtension().getParentRoom().getProperty("data");
        LobbyDataUtils.getInstance().fillRoomData(data, params, users, true, true);
        if( params.containsKey("broadcast") )
            send(Commands.LOBBY_INFO, params, getParentExtension().getParentRoom().getUserList());
        else
            send(Commands.LOBBY_INFO, params, sender);
    }
}