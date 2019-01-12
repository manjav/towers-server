package com.gerantech.towers.sfs.socials;

import com.gerantech.towers.sfs.utils.DBUtils;
import com.gt.data.LobbyData;
import com.gt.data.RankData;;
import com.gt.towers.Game;
import com.gt.towers.Player;
import com.gt.towers.constants.MessageTypes;
import com.gt.towers.constants.ResourceType;
import com.hazelcast.config.Config;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.IMap;
import com.smartfoxserver.v2.SmartFoxServer;
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
import com.smartfoxserver.v2.extensions.SFSExtension;
import com.smartfoxserver.v2.persistence.room.DBRoomStorageConfig;
import com.smartfoxserver.v2.persistence.room.RoomStorageMode;
import com.smartfoxserver.v2.persistence.room.SFSStorageException;
import com.smartfoxserver.v2.security.DefaultPermissionProfile;

import java.sql.SQLException;
import java.util.*;

/**
 * Created by ManJav on 10/15/2017.
 */
public class LobbyUtils
{
    private static LobbyUtils _instance;
    private final SFSExtension ext;
    public LobbyUtils() {
        ext = (SFSExtension) SmartFoxServer.getInstance().getZoneManager().getZoneByName("towers").getExtension();
    }
    public static LobbyUtils getInstance()
    {
        if( _instance == null )
            _instance = new LobbyUtils();
        return _instance;
    }


    public Map<Integer, LobbyData> getAllData()
    {
        return (Map<Integer, LobbyData>) ext.getParentZone().getProperty("lobbiesData");
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
        /*if( lobbyRows.size() == 0 )
        {
            migrateToLobbies();
            loadAll();
            return;
        }*/

        ISFSObject lr;
        LobbyData lobbySFS;
        Map<Integer, LobbyData> lobbiesData = new HashMap();
        for (int i = 0; i < lobbyRows.size(); i++)
        {
            lr = lobbyRows.getSFSObject(i);
            lobbySFS = new LobbyData(lr);
            lobbiesData.put(lr.getInt("id"), lobbySFS);
        }

        ext.getParentZone().setProperty("lobbiesData", lobbiesData);
        ext.trace("loaded lobbies data in " + (System.currentTimeMillis() - (long)ext.getParentZone().getProperty("startTime")) + " milliseconds.");
    }

    public Room create(User owner, String name, String bio, int emblem, int capacity, int minPoint, int privacy)
    {
        LobbyData lobbyData = new LobbyData(-1, name, bio, emblem, capacity, minPoint, privacy, null, null);

        // add member
        Player player = ((Game)owner.getSession().getProperty("core")).player;
        SFSObject member = new SFSObject();
        member.putInt("id", player.id);
        member.putShort("pr", DefaultPermissionProfile.ADMINISTRATOR.getId());
        lobbyData.getMembers().addSFSObject(member);

        // insert to DB
        String query = "INSERT INTO lobbies (name, bio, emblem, capacity, min_point, privacy, members) VALUES ('"
                + name + "', '" + bio + "', " + emblem + ", " + capacity + ", " + minPoint + ", " + privacy + ", ?);";
        try {
            lobbyData.setId(Math.toIntExact((Long) ext.getParentZone().getDBManager().executeInsert(query, new Object[]{lobbyData.getMembersBytes()})));
        } catch (SQLException e) {  e.printStackTrace(); }

        getAllData().put(lobbyData.getId(), lobbyData);

        // create and join room
        Room lobby = createRoom(lobbyData);
        try {
            ext.getApi().joinRoom(owner, lobby);
        } catch (SFSJoinRoomException e) { e.printStackTrace(); }

        return lobby;
    }

    private Room createRoom(LobbyData lobbyData)
    {
        CreateRoomSettings.RoomExtensionSettings res = new CreateRoomSettings.RoomExtensionSettings("TowerExtension", "com.gerantech.towers.sfs.socials.LobbyRoom");
        CreateRoomSettings rs = new CreateRoomSettings();
        rs.setHidden(false);
        rs.setAllowOwnerOnlyInvitation(false);
        rs.setDynamic(true);
        rs.setGroupId("lobbies");
        rs.setName(lobbyData.getName());
        rs.setAutoRemoveMode(SFSRoomRemoveMode.NEVER_REMOVE);
        rs.setExtension(res);
        //rs.setMaxVariablesAllowed(7);
        rs.setMaxUsers(lobbyData.getCapacity());

        rs.setRoomProperties(new HashMap(){{put("data", lobbyData);}});

        Room lobby = null;
        try {
            lobby = ext.getApi().createRoom(ext.getParentZone(), rs, null);
        } catch (SFSCreateRoomException e) { e.printStackTrace(); }

        return lobby;
    }

    /**
     * Save room to file for server restore rooms after resetting
     * @param lobbyData (required)
     * @param name (set null if you want not save)
     * @param bio (set null if you want not save)
     * @param emblem (set -1 if you want not save)
     * @param capacity (set -1 if you want not save)
     * @param minPoint (set -1 if you want not save)
     * @param privacy (set -1 if you want not save)
     * @param members (set null if you want not save)
     * @param messages (set null if you want not save)
     */
    public void save(LobbyData lobbyData, String name, String bio, int emblem, int capacity, int minPoint, int privacy, byte[] members, byte[] messages)
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
        query += "WHERE id = " + lobbyData.getId() + ";";
        //ext.trace(query);

        List<Object> arguments = new ArrayList();
        if( members != null ) arguments.add(members);
        if( messages != null ) arguments.add(messages);
        try {
            ext.getParentZone().getDBManager().executeUpdate(query, arguments.toArray());
        } catch (SQLException e) { e.printStackTrace(); }

        getAllData().replace(lobbyData.getId(), lobbyData);
    }

    public Room getLobby(int memberId)
    {
        LobbyData data = getDataByMember(memberId);
        if( data != null )
            return getLobby(data);
        return null;
    }

    public Room getLobby(LobbyData data)
    {
        Room lobby = ext.getParentZone().getRoomByName(data.getName());
        if( lobby != null )
            return lobby;
        return createRoom(data);
    }

    public LobbyData getDataById(Integer id)
    {
        return getAllData().get(id);
    }
    public LobbyData getDataByMember(int memberId)
    {
        Map<Integer, LobbyData> lobbiesData = getAllData();
        for (Map.Entry<Integer, LobbyData> entry : lobbiesData.entrySet())
            if( getMemberIndex(entry.getValue(), memberId) > -1 )
                return entry.getValue();
        return null;
    }

    public int getMemberIndex(LobbyData lobbyData, int memberId)
    {
        ISFSArray members = lobbyData.getMembers();
        for(int i=0; i<members.size(); i++)
            if( members.getSFSObject(i).getInt("id").equals(memberId) ){
            ext.trace(lobbyData.getId(), i, members.getSFSObject(i).getInt("id"), memberId, members.size());
                return i;}
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
        ext.trace(lobbyId + " removed.");
    }


    public void join(Room room, User user)
    {
        if( user == null )
            return;
        Game game = ((Game) user.getSession().getProperty("core"));
        try {
            ext.getApi().joinRoom(user, room, null, false, null);
        } catch (SFSJoinRoomException e) { e.printStackTrace(); }

        // reset weekly battles
        try {
            ext.getParentZone().getDBManager().executeUpdate("UPDATE resources SET count= 0 WHERE type=1204 AND count != 0 AND player_id = " + game.player.id, new Object[]{});
        } catch (SQLException e) { e.printStackTrace(); }
        IMap<Integer, RankData> users = Hazelcast.getOrCreateHazelcastInstance(new Config("aaa")).getMap("users");
        game.player.resources.set(ResourceType.R14_BATTLES_WEEKLY, 0);
        game.player.resources.set(ResourceType.R18_STARS_WEEKLY, 0);
        RankData rd = new RankData(game.player.nickName,  game.player.get_point(), 0, 0);
        if( users.containsKey(game.player.id) )
            users.replace(game.player.id, rd);
        else
            users.put(game.player.id, rd);
    }

    public boolean addUser(LobbyData lobbyData, int userId)
    {
        LobbyData oldLobby =  getDataByMember(userId);
        if( oldLobby != null )
        {
            ext.trace(userId, "already joint in", oldLobby.getName());
            return false;
        }

        SFSObject member = new SFSObject();
        member.putInt("id", userId);
        member.putShort("pr", (lobbyData.getMembers().size()==0 ? DefaultPermissionProfile.ADMINISTRATOR : DefaultPermissionProfile.GUEST).getId());
        lobbyData.getMembers().addSFSObject(member);
        save(lobbyData, null, null, -1, -1, -1, -1, lobbyData.getMembersBytes(), null);
        return true;
    }



    /**
     * Remove user from room variables and save lobby. if lobby is empty then lobby removed.
     * @param lobbyData
     * @param memberId
     */
    public void removeUser(LobbyData lobbyData, int memberId)
    {
        int memberIndex = getMemberIndex(lobbyData, memberId);
        ISFSArray members = lobbyData.getMembers();
        if( memberIndex < 0 )
            return;

        Short permission = members.getSFSObject(memberIndex).getShort("pr");
        members.removeElementAt(memberIndex);

        if( members.size() == 0 )
        {
            remove(lobbyData.getId());
            if( ext.getParentZone().getRoomManager().containsRoom(lobbyData.getId() + "") )
                ext.getApi().removeRoom(ext.getParentZone().getRoomManager().getRoomByName(lobbyData.getId() + ""));
            return;
        }

        // move permission to oldest member
        if( permission == DefaultPermissionProfile.ADMINISTRATOR.getId() )
            members.getSFSObject(0).putShort("pr", DefaultPermissionProfile.ADMINISTRATOR.getId());

        lobbyData.setMembers(members);
        save(lobbyData, null, null, -1, -1, -1, -1, lobbyData.getMembersBytes(), null);
    }



    private void migrateToLobbies()
    {
        IMap<Integer, RankData> users = Hazelcast.getOrCreateHazelcastInstance(new Config("aaa")).getMap("users");
        DBRoomStorageConfig dbRoomStorageConfig = new DBRoomStorageConfig();
        dbRoomStorageConfig.storeInactiveRooms = true;
        dbRoomStorageConfig.tableName = "rooms";
        ext.getParentZone().initRoomPersistence(RoomStorageMode.DB_STORAGE, dbRoomStorageConfig);

        Map<Integer, ISFSObject> reservedPlayers = new HashMap();
        List<CreateRoomSettings> lobbies = null;
        try {
            lobbies = ext.getParentZone().getRoomPersistenceApi().loadAllRooms("lobbies");
        } catch (SFSStorageException e) { e.printStackTrace(); }

        for( CreateRoomSettings crs : lobbies )
        {
            ISFSArray members = getSettingsVariable(crs, "all").getSFSArrayValue();

            // remove inactive lobbies
            int activeness = 0;
            for(int i=0; i<members.size(); i++)
                activeness += (users.containsKey(members.getSFSObject(i).getInt("id")) ? users.get(members.getSFSObject(i).getInt("id")).weeklyBattles : 0);

            if( activeness <= 0 )
            {
                //ext.trace(crs.getName(), "is inactive.");
                continue;
            }


            // remove duplicate player
            ISFSArray mems = new SFSArray();
            for(int i=0; i<members.size(); i++)
            {
                if( reservedPlayers.containsKey(members.getSFSObject(i).getInt("id")) )
                {
                    ext.trace(members.getSFSObject(i).getInt("id"), "already joint in other lobby");
                }
                else
                {
                    if( members.getSFSObject(i).containsKey("na") ) members.getSFSObject(i).removeElement("na");
                    if( members.getSFSObject(i).containsKey("po") ) members.getSFSObject(i).removeElement("po");
                    mems.addSFSObject(members.getSFSObject(i));
                    reservedPlayers.put(members.getSFSObject(i).getInt("id"), members.getSFSObject(i));
                }
            }

            if( mems.size() < 2 )
            {
                ext.trace(crs.getName(), "is too small.");
                continue;
            }


            // create lobby data
            LobbyData ld = new LobbyData(-1, crs.getName(), getSettingsVariable(crs, "bio").getStringValue(), getSettingsVariable(crs, "pic").getIntValue(), crs.getMaxUsers(), getSettingsVariable(crs, "min").getIntValue(), getSettingsVariable(crs, "pri").getIntValue(), null, null);
            ld.setMembers(mems);
            ld.setMessages(getSettingsVariable(crs, "msg").getSFSArrayValue());

            String query = "INSERT INTO lobbies (name, bio, emblem, capacity, min_point, privacy, members, messages) VALUES ('"
                    + ld.getName() + "', '" + ld.getBio() + "', " + ld.getEmblem() + ", " + ld.getCapacity() + ", " + ld.getMinPoint() + ", " + ld.getPrivacy() + ", ?, ?);";
            //ext.trace(ld.getName());
            try {
                ext.getParentZone().getDBManager().executeInsert(query, new Object[]{ld.getMembersBytes(), ld.getMessagesBytes()});
            } catch (SQLException e) {  e.printStackTrace(); }
        }
    }

    public RoomVariable getSettingsVariable(CreateRoomSettings setting, String name)
    {
        List<RoomVariable> lobbyVariables = setting.getRoomVariables();
        for( RoomVariable lv : lobbyVariables )
            if( lv.getName().equals(name) )
                return lv;
        ext.trace(name , "not found  in", setting.getName());
        return null;
    }

    public ISFSArray searchInChats(String word)
    {
        ISFSObject message;
        ISFSArray ret = new SFSArray();
        Map<Integer, LobbyData> all = getAllData();
        LobbyData data;
        for (Map.Entry<Integer, LobbyData> entry : all.entrySet())
        {
            data = entry.getValue();
            for(int i = 0; i < data.getMessages().size(); i++)
            {
                message = data.getMessages().getSFSObject(i);
                if( message.getShort("m") == (short) MessageTypes.M0_TEXT && message.getUtfString("t").indexOf( word ) > -1 )
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
    public String resetActivenessOfLobbies()
    {
        String result = "Reset Activeness:";
       /* List<Room> lobbies = ext.getParentZone().getRoomListFromGroup("lobbies");
        int lobbiesLen = lobbies.size()-1;
        while ( lobbiesLen >= 0 )
        {
            setActivenessVariable(lobbies.get(lobbiesLen), 0);
            result += "\n Lobby " + lobbies.get(lobbiesLen).getName() + " activeness set to '0'";
            lobbiesLen --;
        }

        try {
            ext.getParentZone().getRoomPersistenceApi().saveAllRooms("lobbies");
        } catch (SFSStorageException e) { e.printStackTrace(); }
*/
        result += DBUtils.getInstance().resetWeeklyBattles();
        return result;
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
                ext.trace("vars of " + crs.getName() + " cleaned.");
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
