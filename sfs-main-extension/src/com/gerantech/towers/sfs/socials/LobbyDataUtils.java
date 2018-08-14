package com.gerantech.towers.sfs.socials;

import com.gt.data.LobbyData;
import com.gt.hazel.RankData;
import com.hazelcast.core.IMap;
import com.smartfoxserver.v2.SmartFoxServer;
import com.smartfoxserver.v2.api.CreateRoomSettings;
import com.smartfoxserver.v2.entities.Room;
import com.smartfoxserver.v2.entities.data.ISFSArray;
import com.smartfoxserver.v2.entities.data.ISFSObject;
import com.smartfoxserver.v2.entities.data.SFSArray;
import com.smartfoxserver.v2.entities.data.SFSObject;
import com.smartfoxserver.v2.extensions.SFSExtension;

import java.util.*;

public class LobbyDataUtils
{
    private static LobbyDataUtils _instance;
    private final SFSExtension ext;

    public LobbyDataUtils() {
        ext = (SFSExtension) SmartFoxServer.getInstance().getZoneManager().getZoneByName("towers").getExtension();
    }
    public static LobbyDataUtils getInstance() {
        if( _instance == null )
            _instance = new LobbyDataUtils();
        return _instance;
    }

    public void searchRooms(ISFSObject params, IMap<Integer, RankData> users)
    {
        String roomName = params.containsKey("name") ? params.getUtfString("name").toLowerCase() : null;
        if( roomName != null && roomName.equals("!@#$") )
            roomName = null;
        int mode = params.containsKey("mode") ? params.getInt("mode") : 0;
        //boolean rankMode = params.containsKey("rank");

        Map<Integer, LobbyData> all = LobbyUtils.getInstance().getAllData();
        LobbyData data;
        SFSObject r;
        List<SFSObject> roomsList = new ArrayList();
        for (Map.Entry<Integer, LobbyData> entry : all.entrySet())
        {
            data = entry.getValue();
            if( roomName != null && data.getName().toLowerCase().indexOf( roomName ) == -1 )
                continue;
            r = new SFSObject();
            fillRoomData(data, r, users, false);
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
            r = roomsList.get(roomIndex);
            //roomSettings = LobbyUtils.getInstance().getDataById(r.getText("name"));
           // r.putInt("id", LobbyUtils.getInstance().getLobby(roomSettings).getId());
            rooms.addSFSObject(r);
            roomIndex ++;
        }
        params.putSFSArray("rooms", rooms);
        params.removeElement("name");
    }


    public void fillRoomData(LobbyData data, ISFSObject params, IMap<Integer, RankData> users, boolean includeMembers)
    {
        ISFSArray all = getMembers(data.getMembers(), users, includeMembers);
        params.putInt("id", data.getId());
        params.putText("name", data.getName());
        //params.putInt("id", settings.getId());
        params.putInt("max", data.getCapacity());
        params.putInt("num", all.size());
        params.putInt("sum", getLobbyPoint(all));
        params.putInt("pic", data.getEmblem());
        params.putInt("act", getLobbyActiveness(all));
        if( includeMembers )
            params.putSFSArray("all", all);
    }

    /*public void fillRoomInfo(Room lobby, ISFSObject params, IMap<Integer, RankData>users, boolean includeMembers)
    {
        params.putText("bio",lobby.getVariable("bio").getStringValue());
        params.putInt("min", lobby.getVariable("min").getIntValue());
        params.putInt("pri", lobby.getVariable("pri").getIntValue());
        if( includeMembers )
        {
            //CreateRoomSettings settings = lobbyUtils.getAllData(zone).get(lobby.getName());
            ISFSArray all = getMembers(lobby.getVariable("all").getSFSArrayValue(), users, includeMembers);
            params.putSFSArray("all", all);
            params.putInt("sum", getLobbyPoint(all));
        }
        params.removeElement("id");
    }*/


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
        int index = 0;
        float rankRatio;
        int size = members.size();
        while( index < size )
        {
            rankRatio = (float)index / (float)size;
            sum += members.getSFSObject(index).getInt("point") * RANK_COEFS[(int)Math.floor(rankRatio * 5)];
            index ++;
        }
        return sum;
    }
    private int getLobbyActiveness( ISFSArray members )
    {
        int sum = 0;
        int index = 0;
        float rankRatio;
        int size = members.size() - 1;
        if( size < 0 )
            return 0; //break operation for empty lobbies
        while( index < size )
        {
            rankRatio = (float)index / (float)size;
            sum += members.getSFSObject(index).getInt("activity") * RANK_COEFS[(int)Math.floor(rankRatio * 5)];
            index ++;
        }
        return sum;
    }

    private ISFSArray getMembers(ISFSArray all, IMap<Integer, RankData> users, boolean includeMembers)
    {
        // ISFSArray all = lobbyUtils.getSettingsVariable(settings, "all").getSFSArrayValue();
        //    if( includeMembers )
        //           RankingUtils.getInstance().fillByIds(users, all);

        // fill hazelcast data to members
        int index = 0;
        int size = all.size();
        SFSObject member;
        List<SFSObject> members = new ArrayList();
        while( index < size )
        {
            member = new SFSObject();
            member.putInt("id", all.getSFSObject(index).getInt("id"));
            member.putShort("permission", all.getSFSObject(index).getShort("pr"));
            boolean hasCache = users.containsKey(member.getInt("id"));
            member.putText("name", hasCache ? users.get(member.getInt("id")).name : "???");
            member.putInt("point", hasCache ? users.get(member.getInt("id")).point : 0);
            member.putInt("activity", hasCache ? users.get(member.getInt("id")).xp : 0);

            members.add(member);
            index ++;
        }

        // sort on activity ascending
        if( includeMembers )
        {
            Collections.sort(members, new Comparator<SFSObject>() {
                @Override
                public int compare(SFSObject rhs, SFSObject lhs) { return lhs.getInt("activity") - rhs.getInt("activity"); }
            });
        }

        // add to sfs array
        index = 0;
        size = members.size();
        ISFSArray ret = new SFSArray();
        while( index < size )
        {
            ret.addSFSObject(members.get(index));
            index ++;
        }
        return ret;
    }

}