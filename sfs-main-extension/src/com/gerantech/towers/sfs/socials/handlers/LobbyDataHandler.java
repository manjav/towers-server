package com.gerantech.towers.sfs.socials.handlers;

import com.gerantech.towers.sfs.socials.LobbyUtils;
import com.gerantech.towers.sfs.utils.RankingUtils;
import com.gt.hazel.RankData;
import com.hazelcast.config.Config;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.IMap;
import com.smartfoxserver.v2.api.CreateRoomSettings;
import com.smartfoxserver.v2.entities.Room;
import com.smartfoxserver.v2.entities.User;
import com.smartfoxserver.v2.entities.Zone;
import com.smartfoxserver.v2.entities.data.ISFSArray;
import com.smartfoxserver.v2.entities.data.ISFSObject;
import com.smartfoxserver.v2.entities.data.SFSArray;
import com.smartfoxserver.v2.entities.data.SFSObject;
import com.smartfoxserver.v2.extensions.BaseClientRequestHandler;

import java.util.*;

/**
 * Created by ManJav on 8/24/2017.
 */
public class LobbyDataHandler extends BaseClientRequestHandler
{
    private Zone zone;
    private LobbyUtils lobbyUtils;

    public void handleClientRequest(User sender, ISFSObject params)
    {
        lobbyUtils = LobbyUtils.getInstance();
        zone = getParentExtension().getParentZone();
        IMap<Integer, RankData> users = Hazelcast.getOrCreateHazelcastInstance(new Config("aaa")).getMap("users");
        if( params.containsKey("id") )
            fillRoomInfo( zone.getRoomById(params.getInt("id")), params, users, params.containsKey("all") );
        else
             searchRooms(params, users );

        send("lobbyData", params, sender);
    }

    private void searchRooms(ISFSObject params, IMap<Integer, RankData>users)
    {
        String roomName = params.containsKey("name") ? params.getUtfString("name").toLowerCase() : null;
        if( roomName != null && roomName.equals("!@#$") )
            roomName = null;
        int mode = params.containsKey("mode") ? params.getInt("mode") : 0;
        //boolean rankMode = params.containsKey("rank");

        Map<String, CreateRoomSettings> allSettings = lobbyUtils.getAllSettings(zone);
        CreateRoomSettings roomSettings;
        SFSObject r;
        List<SFSObject> roomsList = new ArrayList();
        for (Map.Entry<String, CreateRoomSettings> entry : allSettings.entrySet())
        {
            roomSettings = entry.getValue();
            if( roomName != null && roomSettings.getName().toLowerCase().indexOf( roomName ) == -1 )
                continue;
            r = new SFSObject();
            fillRoomData(roomSettings, r, users, false);
            // if( roomName != null || r.getInt("num") < r.getInt("max") || rankMode )
                roomsList.add(r);
        }

        String[] modes = {"sum", "num", "act"};
        // sort rooms on mode by ascending order
        Collections.sort(roomsList, new Comparator<SFSObject>() {
            @Override
            public int compare(SFSObject rhs, SFSObject lhs) { return lhs.getInt(modes[mode]) - rhs.getInt(modes[mode]); }
        });

        int roomIndex = 0;
        int numRooms = Math.min(50, roomsList.size());
        SFSArray rooms = new SFSArray();
        while( roomIndex < numRooms )
        {
            rooms.addSFSObject(roomsList.get(roomIndex));
            roomIndex ++;
        }
        params.putSFSArray("rooms", rooms);
        params.removeElement("name");
    }
    public void fillRoomData(CreateRoomSettings settings, ISFSObject params, IMap<Integer, RankData> users, boolean includeMembers)
    {
        Room lobby = lobbyUtils.getLobby(settings, zone);
        if( lobby != null )
            fillRoomData(lobby, params, users, includeMembers);
    }
    public void fillRoomData(Room room, ISFSObject params, IMap<Integer, RankData> users, boolean includeMembers)
    {
        ISFSArray all = getMembers(room, users);
        params.putText("name", room.getName());
        params.putInt("id", room.getId());
        params.putInt("max", room.getMaxUsers());
        params.putInt("num", all.size());
        params.putInt("sum", getLobbyPoint(all));
        params.putInt("pic", room.getVariable("pic").getIntValue());
        params.putInt("act", (int) (room.getVariable("act").getIntValue() * 0.2 + getLobbyActiveness(all) ));
        if( includeMembers )
            params.putSFSArray("all", all);
    }

    private void fillRoomInfo(Room room, ISFSObject params, IMap<Integer, RankData>users, boolean includeMembers)
    {
        params.putText("bio", room.getVariable("bio").getStringValue());
        params.putInt("min", room.getVariable("min").getIntValue());
        params.putInt("pri", room.getVariable("pri").getIntValue());
        if( includeMembers )
            params.putSFSArray("all", getMembers(room, users));
        params.removeElement("id");
    }


    /**
     * calculate point with bellow formula
     *  |      rank         | point-coef |
     *  ==================== ============
     *  |   0.00 ~ 0.20     |    0.50    |
     *  |   0.21 ~ 0.40     |    0.25    |
     *  |   0.41 ~ 0.60     |    0.12    |
     *  |   0.61 ~ 0.80     |    0.10    |
     *  |   0.81 ~ 1.00     |    0.03    |
     *
     * @param users
     * @param all
     * @return
     */
    private static float[] RANK_COEFS = {0.50f, 0.25f, 0.12f, 0.10f, 0.03f};
    private int getLobbyPoint( ISFSArray members )
    {
        int sum = 0;
        float rankRatio;
        int index = 0;
        int size = members.size();
        while( index < size )
        {
            rankRatio = (float)index/(float)size;
            sum += members.getSFSObject(index).getInt("point") * RANK_COEFS[(int)Math.floor(rankRatio*5)];
            index ++;
        }
        return sum;
    }
    private int getLobbyActiveness( ISFSArray members )
    {
        int sum = 0;
        int size = members.size() - 1;
        if( size < 0 )
            return 0; //break operation for empty lobbies
        while( size >= 0 )
        {
            sum += members.getSFSObject(size).getInt("activity") ;
            size --;
        }
        return Math.round(sum / members.size());
    }
    private ISFSArray getMembers(Room room, IMap<Integer, RankData> users)
    {
        ISFSArray all = room.getVariable("all").getSFSArrayValue();
        RankingUtils.getInstance().fillByIds(users, all);

        // fill hazelcast data to members
        int index = 0;
        int size = all.size();
        SFSObject member;
        List<SFSObject> members = new ArrayList();
        while( index < size ) {
            member = new SFSObject();
            member.putInt("id", all.getSFSObject(index).getInt("id"));
            member.putShort("permission", all.getSFSObject(index).getShort("pr"));
            member.putText("name", users.get(member.getInt("id")).name);
            member.putInt("point", users.get(member.getInt("id")).point);
            member.putInt("activity", users.get(member.getInt("id")).xp);
            members.add(member);
            index ++;
        }

        // sort on point ascending
        Collections.sort(members, new Comparator<SFSObject>() {
            @Override
            public int compare(SFSObject rhs, SFSObject lhs) { return lhs.getInt("point") - rhs.getInt("point"); }
        });

        // add to sfs array
        index = 0;
        size = members.size();
        ISFSArray ret = new SFSArray();
        while( index < size ) {
            ret.addSFSObject(members.get(index));
            index ++;
        }
        return ret;
    }
}