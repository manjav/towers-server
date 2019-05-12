package com.gt.utils;

import com.gt.data.LobbySFS;
import com.gt.data.RankData;
import com.gt.towers.Game;
import com.gt.towers.Player;
import com.gt.towers.constants.MessageTypes;
import com.smartfoxserver.v2.api.CreateRoomSettings;
import com.smartfoxserver.v2.entities.Room;
import com.smartfoxserver.v2.entities.SFSRoomRemoveMode;
import com.smartfoxserver.v2.entities.User;
import com.smartfoxserver.v2.entities.data.ISFSArray;
import com.smartfoxserver.v2.entities.data.ISFSObject;
import com.smartfoxserver.v2.entities.data.SFSArray;
import com.smartfoxserver.v2.entities.data.SFSObject;
import com.smartfoxserver.v2.entities.variables.RoomVariable;
import com.smartfoxserver.v2.exceptions.SFSCreateRoomException;
import com.smartfoxserver.v2.exceptions.SFSJoinRoomException;
import com.smartfoxserver.v2.security.DefaultPermissionProfile;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by ManJav on 10/15/2017.
 */
public class LobbyUtils extends UtilBase
{
    public static LobbyUtils getInstance()
    {
        return (LobbyUtils)UtilBase.get(LobbyUtils.class);
    }

    public Map<Integer, LobbySFS> getAllData()
    {
        return (Map<Integer, LobbySFS>) ext.getParentZone().getProperty("lobbiesData");
    }

    /**
     * Save room to file for server restore rooms after resetting
     */
    public void loadAll()
    {
        if( ext.getParentZone().containsProperty("lobbiesData") )
            return;

        try {
            ext.getParentZone().getDBManager().executeUpdate("SET collation_connection = 'utf8mb4_bin';", new Object[]{});
        } catch (SQLException e) { e.printStackTrace(); }

        ISFSArray lobbyRows = new SFSArray();
        try {
            lobbyRows = ext.getParentZone().getDBManager().executeQuery("SELECT * FROM lobbies;", new Object[]{});
        } catch (SQLException e) { e.printStackTrace(); }

        ISFSObject lr;
        LobbySFS lobbySFS;
        Map<Integer, LobbySFS> lobbiesData = new HashMap();
        for (int i = 0; i < lobbyRows.size(); i++)
        {
            lr = lobbyRows.getSFSObject(i);
            lobbySFS = new LobbySFS(lr);
            lobbiesData.put(lr.getInt("id"), lobbySFS);
        }

        ext.getParentZone().setProperty("lobbiesData", lobbiesData);
        trace("loaded lobbies data in " + (System.currentTimeMillis() - (long)ext.getParentZone().getProperty("startTime")) + " milliseconds.");
    }

    public Room create(User owner, String name, String bio, int emblem, int capacity, int minPoint, int privacy)
    {
        LobbySFS lobbySFS = new LobbySFS(-1, name, bio, emblem, capacity, minPoint, privacy, null, null);

        // add member
        Player player = ((Game)owner.getSession().getProperty("core")).player;
        SFSObject member = new SFSObject();
        member.putInt("id", player.id);
        member.putInt("pr", DefaultPermissionProfile.ADMINISTRATOR.getId());
        lobbySFS.getMembers().addSFSObject(member);

        // insert to DB
        String query = "INSERT INTO lobbies (name, bio, emblem, capacity, min_point, privacy, members) VALUES ('"
                + name + "', '" + bio + "', " + emblem + ", " + capacity + ", " + minPoint + ", " + privacy + ", ?);";
        try {
            lobbySFS.setId(Math.toIntExact((Long) ext.getParentZone().getDBManager().executeInsert(query, new Object[]{lobbySFS.getMembersBytes()})));
        } catch (SQLException e) {  e.printStackTrace(); }

        getAllData().put(lobbySFS.getId(), lobbySFS);

        // create and join room
        Room lobby = createRoom(lobbySFS);
        try {
            ext.getApi().joinRoom(owner, lobby);
        } catch (SFSJoinRoomException e) { e.printStackTrace(); }

        return lobby;
    }

    private Room createRoom(LobbySFS lobbySFS)
    {
        CreateRoomSettings.RoomExtensionSettings res = new CreateRoomSettings.RoomExtensionSettings("TowerExtension", "com.gerantech.towers.sfs.socials.LobbyRoom");
        CreateRoomSettings rs = new CreateRoomSettings();
        rs.setHidden(false);
        rs.setAllowOwnerOnlyInvitation(false);
        rs.setDynamic(true);
        rs.setGroupId("lobbies");
        rs.setName(lobbySFS.getName());
        rs.setAutoRemoveMode(SFSRoomRemoveMode.NEVER_REMOVE);
        rs.setExtension(res);
        //rs.setMaxVariablesAllowed(7);
        rs.setMaxUsers(lobbySFS.getCapacity());

        rs.setRoomProperties(new HashMap(){{put("data", lobbySFS);}});

        Room lobby = null;
        try {
            lobby = ext.getApi().createRoom(ext.getParentZone(), rs, null);
        } catch (SFSCreateRoomException e) { e.printStackTrace(); }

        return lobby;
    }

    /**
     * Save room to file for server restore rooms after resetting
     * @param lobbySFS (required)
     * @param name (set null if you want not save)
     * @param bio (set null if you want not save)
     * @param emblem (set -1 if you want not save)
     * @param capacity (set -1 if you want not save)
     * @param minPoint (set -1 if you want not save)
     * @param privacy (set -1 if you want not save)
     * @param members (set null if you want not save)
     * @param messages (set null if you want not save)
     */
    public void save(LobbySFS lobbySFS, String name, String bio, int emblem, int capacity, int minPoint, int privacy, byte[] members, byte[] messages)
    {
        String query = "UPDATE lobbies SET ";
        List<String> changes =  new ArrayList();
        if( name != null )      changes.add("name = '" + name + "'");
        if( bio != null )       changes.add("bio = '" + bio + "'");
        if( emblem != -1 )      changes.add("emblem = " + emblem);
        if( capacity != -1 )    changes.add("capacity = " + capacity);
        if( minPoint != -1 )    changes.add("min_point = " + minPoint);
        if( privacy != -1 )     changes.add("privacy = " + privacy);
        if( members != null )   changes.add("members = ?");
        if( messages != null )  changes.add("messages = ?");

        for (int i = 0; i < changes.size() ; i++)
        {
            query += changes.get(i);
            query += ( i < changes.size() - 1 ) ? ", " : " ";
        }
        query += "WHERE id = " + lobbySFS.getId() + ";";
        //trace(query);

        List<Object> arguments = new ArrayList();
        if( members != null ) arguments.add(members);
        if( messages != null ) arguments.add(messages);
        try {
            ext.getParentZone().getDBManager().executeUpdate(query, arguments.toArray());
        } catch (SQLException e) { e.printStackTrace(); }
        getAllData().replace(lobbySFS.getId(), lobbySFS);
    }

    public Room getLobby(int memberId)
    {
        LobbySFS lobbySFS = getDataByMember(memberId);
        if( lobbySFS != null )
            return getLobby(lobbySFS);
        return null;
    }

    public Room getLobby(LobbySFS lobbySFS)
    {
        Room lobby = ext.getParentZone().getRoomByName(lobbySFS.getName());
        if( lobby != null )
            return lobby;
        return createRoom(lobbySFS);
    }

    public LobbySFS getDataById(Integer id)
    {
        return getAllData().get(id);
    }
    public LobbySFS getDataByMember(int memberId)
    {
        Map<Integer, LobbySFS> lobbiesData = getAllData();
        for( Map.Entry<Integer, LobbySFS> entry : lobbiesData.entrySet() )
            if( getMemberIndex(entry.getValue(), memberId) > -1 )
                return entry.getValue();
        return null;
    }

    public int getMemberIndex(LobbySFS lobbySFS, int memberId)
    {
        ISFSArray members = lobbySFS.getMembers();
        for( int i=0; i<members.size(); i++ )
            if( members.getSFSObject(i).getInt("id").equals(memberId) )
                return i;
        return -1;
    }


    // remove lobby from DB
    public void remove(int lobbyId)
    {
        String query = "DELETE FROM lobbies WHERE id =" + lobbyId;
        try {
            ext.getParentZone().getDBManager().executeUpdate(query, new Object[]{});
        } catch (SQLException e) {  e.printStackTrace(); }
        getAllData().remove(lobbyId);
        trace(lobbyId + " removed.");
    }


    public void join(Room room, User user)
    {
        if( user == null )
            return;
        Game game = ((Game) user.getSession().getProperty("core"));
        try {
            ext.getApi().joinRoom(user, room, null, false, null);
        } catch (SFSJoinRoomException e) { e.printStackTrace(); }
    }

    public boolean addUser(LobbySFS lobbySFS, int userId)
    {
        LobbySFS oldLobby =  getDataByMember(userId);
        if( oldLobby != null )
        {
            trace(userId, "already joint in", oldLobby.getName());
            return false;
        }

        SFSObject member = new SFSObject();
        member.putInt("id", userId);
        member.putInt("ac", 0);
        member.putInt("pr", (lobbySFS.getMembers().size()==0 ? DefaultPermissionProfile.ADMINISTRATOR : DefaultPermissionProfile.GUEST).getId());
        lobbySFS.getMembers().addSFSObject(member);
        save(lobbySFS, null, null, -1, -1, -1, -1, lobbySFS.getMembersBytes(), null);
        return true;
    }



    /**
     * Remove user from room variables and save lobby. if lobby is empty then lobby removed.
     * @param lobbySFS
     * @param memberId
     */
    public void removeUser(LobbySFS lobbySFS, int memberId)
    {
        int memberIndex = getMemberIndex(lobbySFS, memberId);
        ISFSArray members = lobbySFS.getMembers();
        if( memberIndex < 0 )
            return;

        int permission = members.getSFSObject(memberIndex).getInt("pr");
        members.removeElementAt(memberIndex);

        if( members.size() == 0 )
        {
            remove(lobbySFS.getId());
            if( ext.getParentZone().getRoomManager().containsRoom(lobbySFS.getId() + "") )
                ext.getApi().removeRoom(ext.getParentZone().getRoomManager().getRoomByName(lobbySFS.getId() + ""));
            return;
        }

        // move permission to oldest member
        if( permission == DefaultPermissionProfile.ADMINISTRATOR.getId() )
            members.getSFSObject(0).putInt("pr", DefaultPermissionProfile.ADMINISTRATOR.getId());

        lobbySFS.setMembers(members);
        save(lobbySFS, null, null, -1, -1, -1, -1, lobbySFS.getMembersBytes(), null);
    }



    public String resetActivities()
    {
        String log = "reset activities:\n";
        ISFSArray members;
        ConcurrentHashMap<Integer, RankData> users = RankingUtils.getInstance().getUsers();
        Map<Integer, LobbySFS> lobbiesData = getAllData();
        for (Map.Entry<Integer, LobbySFS> entry : lobbiesData.entrySet())
        {
            members = entry.getValue().getMembers();
            int index = 0;
            int size = members.size();
            ISFSObject member;
            while( index < size )
            {
                member = members.getSFSObject(index);
                member.putInt("ac", 0);
                index ++;
            }
            save(entry.getValue(), null, null, -1, -1, -1, -1, entry.getValue().getMembersBytes(), null);
            log += (entry.getValue().getName() + "=> 0\n");
        }
        return log;
    }

    public RoomVariable getSettingsVariable(CreateRoomSettings setting, String name)
    {
        List<RoomVariable> lobbyVariables = setting.getRoomVariables();
        for( RoomVariable lv : lobbyVariables )
            if( lv.getName().equals(name) )
                return lv;
        trace(name , "not found  in", setting.getName());
        return null;
    }

    public ISFSArray searchInChats(String word)
    {
        ISFSObject message;
        ISFSArray ret = new SFSArray();
        Map<Integer, LobbySFS> all = getAllData();
        LobbySFS data;
        for (Map.Entry<Integer, LobbySFS> entry : all.entrySet())
        {
            data = entry.getValue();
            for(int i = 0; i < data.getMessages().size(); i++)
            {
                message = data.getMessages().getSFSObject(i);
                if( message.getInt("m") == MessageTypes.M0_TEXT && message.getUtfString("t").indexOf( word ) > -1 )
                {
                    message.putInt("li", data.getId());
                    message.putUtfString("ln", data.getName());
                    ret.addSFSObject(message);
                }
            }

        }
        return ret;
    }


    /*public JSONObject getLobbyNameById(String lobbyId)
    {
        JSONObject ret = new JSONObject();
        String roomName = ext.getParentZone().getRoomById(Integer.parseInt(lobbyId)).getName();
        ret.put("lobbyId", lobbyId);
        ret.put("lobbyName", roomName);
        return ret;
    }

    public String removeInactiveLobbies()
    {
        String ret = "";
        ISFSArray all;
        ISFSObject member;
        IMap<Integer, RankData> users = Hazelcast.getOrCreateHazelcastInstance(new Config("aaa")).getMap("users");
        DBRoomStorageConfig dbRoomStorageConfig = new DBRoomStorageConfig();
        dbRoomStorageConfig.storeInactiveRooms = true;
        dbRoomStorageConfig.tableName = "rooms";
        ext.getParentZone().initRoomPersistence(RoomStorageMode.DB_STORAGE, dbRoomStorageConfig);

        List<CreateRoomSettings> lobbies = null;
        try {
            lobbies = ext.getParentZone().getRoomPersistenceApi().loadAllRooms("lobbies");
        } catch (SFSStorageException e) { e.printStackTrace(); }

        for( CreateRoomSettings crs : lobbies )
        {
            int activeness = 0;
            all = getSettingsVariable(crs, "all").getSFSArrayValue();
            for(int i=0; i<all.size(); i++)
            {
                member = all.getSFSObject(i);
                activeness += users.containsKey(member.getInt("id")) ? users.get(member.getInt("id")).xp : 0;
            }

            if( activeness <= 0 )
            {
                ret += crs.getName() + "removed.\n";
                remove(crs.getName());
            }
        }
        return ret;
    }*/

    /*public void setMembersVariable (Room lobby, ISFSArray value)
    {
        try {
            SFSRoomVariable var = new SFSRoomVariable("all", value,  true, true, false);
            var.setHidden(true);
            lobby.setVariable( var );
        } catch (SFSVariableException e) { e.printStackTrace(); }
    }

    public void setActivenessVariable (Room lobby, int value)
    {
        try {
            SFSRoomVariable var = new SFSRoomVariable("act", value,  true, true, false);
            var.setHidden(true);
            lobby.setVariable( var );
        } catch (SFSVariableException e) { e.printStackTrace(); }

        if( value % 5 == 0 )
            save(lobby);
    }
*/

    public void removeEmptyRoom(Room r)
    {
        if( r.isEmpty() )
            ext.getApi().removeRoom(r);
//        trace(r.getName(),  "l players:", r.getPlayersList().size(), r.getUserList().size(), r.isEmpty());
    }


    /*
    public Room getLobbyOfOfflineUser(int id)
    {
        return LobbyUtils.getLobbyOfOfflineUser(ext.getParentZone(), id);
    }

    public static Room getLobbyOfOfflineUser(Zone zone, int id)
    {
        ISFSObject member;
        List<Room> lobbies = zone.getRoomListFromGroup("lobbies");
        for (Room room : lobbies)
        {
            ISFSArray all = room.getVariable("all").getSFSArrayValue();
            for(int i=0; i<all.size(); i++)
            {
                member = all.getSFSObject(i);
                if( member.getInt("id").equals(id) )
                    return  room;
            }
        }
        return null;
    }

    private boolean hasMember(List<RoomVariable> vars)
    {
        for (int i = 0; i < vars.size(); i++)
            if( vars.get(i).getName().equals("all") && vars.get(i).getSFSArrayValue().size() > 0 )
                return true;
        return false;
    }

    public String cleanLobbyVars()
    {
        Zone zone = ext.getParentZone();
        String result = "Start lobby cleaning:";

        try {
            DBRoomStorageConfig dbRoomStorageConfig = new DBRoomStorageConfig();
            dbRoomStorageConfig.storeInactiveRooms = true;
            dbRoomStorageConfig.tableName = "rooms";
            zone.initRoomPersistence(RoomStorageMode.DB_STORAGE, dbRoomStorageConfig);
            List<CreateRoomSettings> lobbies = zone.getRoomPersistenceApi().loadAllRooms("lobbies");
            for ( CreateRoomSettings crs : lobbies )
            {
                crs.setMaxVariablesAllowed(7);
                List<RoomVariable> listOfVars = crs.getRoomVariables();
                SFSRoomVariable var = new SFSRoomVariable("pri", 0, false, true, false);
                var.setHidden(true);
                listOfVars.add(var);
                crs.setRoomVariables(listOfVars);

                Room lobby = ext.getApi().createRoom(zone, crs, null);
                save(zone, lobby);
                ext.getApi().removeRoom(lobby);
                trace("vars of " + crs.getName() + " cleaned.");
                result += "\n vars of " + crs.getName() + " cleaned.";
            }
        }
        catch (SFSStorageException e) {
            e.printStackTrace();
        } catch (SFSCreateRoomException e) {
            e.printStackTrace();
        }
        return result;
    }

    public String migrateToDB()
    {
        String result = "Start migrating lobbies:";
        Zone zone = ext.getParentZone();

        try {
            List<CreateRoomSettings> lobbies = zone.getRoomPersistenceApi().loadAllRooms("lobbies");
            List<CreateRoomSettings> activeLobbies = new ArrayList();
            String query = "INSERT INTO `rooms`(`name`, `groupId`, `roomdata`) VALUES ";
            for ( CreateRoomSettings crs : lobbies )
            {
                List<RoomVariable> listOfVars = crs.getRoomVariables();
                boolean target = false;
                for (RoomVariable var : listOfVars )
                    if( var.getName().equals("all") && var.getSFSArrayValue().size() > 1 )
                        target = true;

                if( target )
                    activeLobbies.add(crs);
            }
            for (int i = 0; i < activeLobbies.size(); i++)
                query += "\n('" + activeLobbies.get(i).getName() + "', 'lobbies', '" + (i==activeLobbies.size()-1?"');":"'), ");

            result += "\n\n" + query;
            zone.getDBManager().executeInsert(query, new Object[]{});


            for (int i = 0; i < activeLobbies.size(); i++) {
                Path path = Paths.get("data/roomData/746f77657273/" + getRoomFileName(activeLobbies.get(i)));
                String fileStr = "0x" + new HexBinaryAdapter().marshal(Files.readAllBytes(path));

                query = "UPDATE rooms SET `roomdata` = " + fileStr + " WHERE `name` = '" + activeLobbies.get(i).getName() + "';";
                result += "\n\n" + query;
                zone.getDBManager().executeUpdate(query, new Object[]{});
            }

            } catch (Exception e) {
            e.printStackTrace();
        }
        result += "\n\n ==> Lobbies migrated completely.";
        return result;
    }
    private String getRoomFileName(CreateRoomSettings theRoom)
    {
        return CryptoUtils.getHexFileName(new StringBuilder(String.valueOf(theRoom.getGroupId())).append(theRoom.getName()).toString()) + ".room";
    }*/
}



