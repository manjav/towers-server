package com.gt.utils;

import com.gt.data.LobbySFS;
import com.gt.data.RankData;
import com.gt.data.SFSDataModel;
import com.smartfoxserver.v2.entities.data.ISFSArray;
import com.smartfoxserver.v2.entities.data.ISFSObject;
import com.smartfoxserver.v2.entities.data.SFSArray;
import com.smartfoxserver.v2.entities.data.SFSObject;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class LobbyDataUtils extends UtilBase
{
    public static LobbyDataUtils getInstance()
    {
        return (LobbyDataUtils)UtilBase.get(LobbyDataUtils.class);
    }

    public void searchRooms(ISFSObject params, ConcurrentHashMap<Integer, RankData> users)
    {
        String roomName = params.containsKey("name") ? params.getUtfString("name").toLowerCase() : null;
        if( roomName != null && roomName.equals("!@#$") )
            roomName = null;
        int mode = params.containsKey("mode") ? params.getInt("mode") : 0;
        //boolean rankMode = params.containsKey("rank");

        Map<Integer, LobbySFS> all = LobbyUtils.getInstance().getAllData();
        LobbySFS data;
        SFSObject r;
        List<SFSObject> roomsList = new ArrayList();
        for (Map.Entry<Integer, LobbySFS> entry : all.entrySet())
        {
            data = entry.getValue();
            if( roomName != null && data.getName().toLowerCase().indexOf( roomName ) == -1 )
                continue;
            r = new SFSObject();
            fillRoomData(data, r, users, false, false);
            // if( roomName != null || r.getInt("num") < r.getInt("max") || rankMode )
            roomsList.add(r);
        }

        // sort rooms on mode by ascending order
        String[] modes = {"sum", "num", "act"};
        Collections.sort(roomsList, (rhs, lhs) -> lhs.getInt(modes[mode]) - rhs.getInt(modes[mode]));

        int roomIndex = 0;
        int numRooms = Math.min(50, roomsList.size());
        SFSArray rooms = new SFSArray();
        while( roomIndex < numRooms )
        {
            r = roomsList.get(roomIndex);
            //roomSettings = LobbyUtils.getInstance().getDataById(r.getText("name"));
           // r.putInt("id", LobbyUtils.getInstance().getLobby(roomSettings).getId());
            rooms.addSFSObject(r);
            roomIndex ++;
        }
        params.putSFSArray("rooms", rooms);
        params.removeElement("name");
    }

    public void fillRoomData(LobbySFS data, ISFSObject params, ConcurrentHashMap<Integer, RankData> users, boolean includeMembers, boolean includeMessages)
    {
        ISFSObject[] all = getMembers(data.getMembers(), users, includeMembers);
        params.putInt("id", data.getId());
        params.putUtfString("name", data.getName());
        params.putUtfString("bio", data.getBio());
        params.putInt("pic", data.getEmblem());
        params.putInt("pri", data.getPrivacy());
        params.putInt("max", data.getCapacity());
        params.putInt("min", data.getMinPoint());
        params.putInt("num", all.length);
        params.putInt("sum", getLobbyPoint(all, "point"));
        params.putInt("act", getLobbyPoint(all, "activity"));
        if( includeMembers )
            params.putSFSArray("all", SFSDataModel.toSFSArray(all));
        if( includeMessages )
            params.putSFSArray("messages", data.getMessages());
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
    private int getLobbyPoint(ISFSObject[] members, String attribute)
    {
        Arrays.sort(members, (rhs, lhs) -> lhs.getInt(attribute) - rhs.getInt(attribute));
        float sum = 0;
        int index = 0;
        float rankRatio;
        int size = members.length;
        if( size == 0 )
            return 0; //break operation for empty lobbies
        while( index < size )
        {
            rankRatio = (float)index / (float)size;
            sum += members[index].getInt(attribute) * RANK_COEFS[(int)Math.floor(rankRatio * 5)];
            //trace(members[index].getInt("id"), attribute, members[index].getInt("point"), rankRatio, RANK_COEFS[(int)Math.floor(rankRatio * 5)], sum);
            index ++;
        }
        return Math.round(sum) ;
    }

    private ISFSObject[] getMembers(ISFSArray all, ConcurrentHashMap<Integer, RankData> users, boolean includeMembers)
    {
        // fill hazelcast data to members
        int index = 0;
        int size = all.size();
        SFSObject member;
        SFSObject[] members = new SFSObject[size];
        while( index < size )
        {
            member = new SFSObject();
            member.putInt("id", all.getSFSObject(index).getInt("id"));
            member.putInt("permission", all.getSFSObject(index).getInt("pr"));
            member.putInt("activity", all.getSFSObject(index).containsKey("ac") ? all.getSFSObject(index).getInt("ac") : 0);
            boolean hasCache = users.containsKey(member.getInt("id"));
            member.putText("name", hasCache ? users.get(member.getInt("id")).name : "???");
            member.putInt("point", hasCache ? users.get(member.getInt("id")).point : 0);

            members[index] = member;
            index ++;
        }
        return members;
    }
}