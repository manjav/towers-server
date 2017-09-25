package com.gerantech.towers.sfs.socials.handlers;

import com.gerantech.towers.sfs.utils.NPCTools;
import com.gt.hazel.RankData;
import com.gt.towers.Game;
import com.hazelcast.config.Config;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.IMap;
import com.smartfoxserver.v2.entities.Room;
import com.smartfoxserver.v2.entities.User;
import com.smartfoxserver.v2.entities.data.ISFSArray;
import com.smartfoxserver.v2.entities.data.ISFSObject;
import com.smartfoxserver.v2.entities.data.SFSArray;
import com.smartfoxserver.v2.entities.data.SFSObject;
import com.smartfoxserver.v2.extensions.BaseClientRequestHandler;
import com.smartfoxserver.v2.extensions.ISFSExtension;

import java.util.List;

/**
 * Created by ManJav on 8/24/2017.
 */
public class LobbyDataHandler extends BaseClientRequestHandler
{
    public void handleClientRequest(User sender, ISFSObject params)
    {
        if ( params.containsKey("name") )
            searchRooms( sender, params );
        else if( params.containsKey("id") )
            getRoomInfo( sender, params );
        send("lobbyData", params, sender);
    }

    private void searchRooms(User sender, ISFSObject params)
    {
        Game game = (Game) sender.getSession().getProperty("core");
        IMap<Integer, RankData> users = NPCTools.fill(Hazelcast.getOrCreateHazelcastInstance(new Config("aaa")).getMap("users"), game, getParentExtension());
        String roomName = params.getUtfString("name");

        List<Room> rList = getParentExtension().getParentZone().getRoomListFromGroup("lobbies");
        SFSArray rooms = new SFSArray();
        SFSObject r;
        ISFSArray all;
        for (Room room : rList)
        {
            if( room.getName().indexOf( roomName ) > -1 )
            {
                int sum = 0;
                r = new SFSObject();
                r.putInt("id", room.getId());
                r.putText("name", room.getName());
                //r.putText("bio", room.getVariable("bio").getStringValue());
                //r.putInt("pic", room.getVariable("pic").getIntValue());
                //r.putInt("min", room.getVariable("min").getIntValue());
                r.putInt("max", room.getMaxUsers());
                all = room.getVariable("all").getSFSArrayValue();
                r.putInt("num", all.size());
                for ( int i=0; i<r.getInt("num"); i++ )
                    sum += users.get(all.getSFSObject(i).getInt("id")).point;
                r.putInt("sum", sum);

                rooms.addSFSObject(r);
            }
        }
        params.putSFSArray("rooms", rooms);
        params.removeElement("name");
    }

    private void getRoomInfo(User sender, ISFSObject params)
    {
         Room room = getParentExtension().getParentZone().getRoomById( params.getInt("id") );
        if( room == null )
        {
            send("lobbyData", params, sender);
            return;
        }

        //params.putText("name", room.getName());
        params.putText("bio", room.getVariable("bio").getStringValue());
        //params.putInt("pic", room.getVariable("pic").getIntValue());
        //params.putInt("max", room.getMaxUsers());
        params.putInt("min", room.getVariable("min").getIntValue());

        params.removeElement("id");
        IMap<Integer, RankData> users = NPCTools.fill(Hazelcast.getOrCreateHazelcastInstance(new Config("aaa")).getMap("users"), (Game) sender.getSession().getProperty("core"), getParentExtension());
        params.putSFSArray("all", fillUsersData(room, users));
    }

    public ISFSArray fillUsersData(Room room, IMap<Integer, RankData> users)
    {
        ISFSArray all = room.getVariable("all").getSFSArrayValue();
        for ( int i=0; i<all.size(); i++ )
        {
            all.getSFSObject(i).putText("na", users.get(all.getSFSObject(i).getInt("id")).name);
            all.getSFSObject(i).putInt("po", users.get(all.getSFSObject(i).getInt("id")).point);
        }
        return all;
    }
}