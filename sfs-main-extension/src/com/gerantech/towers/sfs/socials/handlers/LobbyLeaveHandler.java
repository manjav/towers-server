package com.gerantech.towers.sfs.socials.handlers;

import com.gt.hazel.RankData;
import com.gt.towers.Game;
import com.gt.towers.Player;
import com.gt.towers.constants.ResourceType;
import com.hazelcast.config.Config;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.IMap;
import com.smartfoxserver.v2.entities.Room;
import com.smartfoxserver.v2.entities.User;
import com.smartfoxserver.v2.entities.data.ISFSObject;
import com.smartfoxserver.v2.extensions.BaseClientRequestHandler;

import java.sql.SQLException;

/**
 * Created by ManJav on 8/24/2017.
 */
public class LobbyLeaveHandler extends BaseClientRequestHandler
{
    public void handleClientRequest(User sender, ISFSObject params)
    {
        if( sender.getLastJoinedRoom() == null )
        {
            trace(sender.getName() + " have not any lobby !");
            return;
        }
        for ( Room r:sender.getJoinedRooms() )
            if( r.getGroupId().equals("lobbies") )
                getApi().leaveRoom(sender, r);

        // reset weekly battles
        Player player = ((Game)sender.getSession().getProperty("core")).player;
        try {
            getParentExtension().getParentZone().getDBManager().executeUpdate("UPDATE resources SET count= 0 WHERE type=1204 AND count != 0 AND player_id = " + player.id, new Object[]{});
        } catch (SQLException e) { e.printStackTrace(); }

        IMap<Integer, RankData> users = Hazelcast.getOrCreateHazelcastInstance(new Config("aaa")).getMap("users");
        player.resources.set(ResourceType.BATTLES_COUNT_WEEKLY, 0);
        RankData rd = new RankData(player.id, player.nickName,  player.get_point(), 0);
        if( users.containsKey(player.id))
            users.replace(player.id, rd);
        else
            users.put(player.id, rd);
    }
}