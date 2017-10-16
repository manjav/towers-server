package com.gerantech.towers.sfs.socials.handlers;

import com.gerantech.towers.sfs.Commands;
import com.gerantech.towers.sfs.utils.NPCTools;
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
public class LobbyInfoHandler extends BaseClientRequestHandler
{
    public void handleClientRequest(User sender, ISFSObject params)
    {
        IMap<Integer, RankData> users = NPCTools.fill(Hazelcast.getOrCreateHazelcastInstance(new Config("aaa")).getMap("users"), (Game) sender.getSession().getProperty("core"), getParentExtension());
        new LobbyDataHandler().fillRoomData(getParentExtension().getParentRoom(), params, users, true);
        send(Commands.LOBBY_INFO, params, sender);
    }
}