package com.gerantech.towers.sfs.socials;

import com.gerantech.towers.sfs.utils.DBUtils;
import com.gt.hazel.RankData;
import com.hazelcast.config.Config;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.IMap;
import com.smartfoxserver.v2.SmartFoxServer;
import com.smartfoxserver.v2.api.CreateRoomSettings;
import com.smartfoxserver.v2.entities.Room;
import com.smartfoxserver.v2.entities.SFSRoomRemoveMode;
import com.smartfoxserver.v2.entities.User;
import com.smartfoxserver.v2.entities.Zone;
import com.smartfoxserver.v2.entities.data.ISFSArray;
import com.smartfoxserver.v2.entities.data.ISFSObject;
import com.smartfoxserver.v2.entities.data.SFSArray;
import com.smartfoxserver.v2.entities.data.SFSObject;
import com.smartfoxserver.v2.entities.variables.RoomVariable;
import com.smartfoxserver.v2.entities.variables.SFSRoomVariable;
import com.smartfoxserver.v2.exceptions.SFSCreateRoomException;
import com.smartfoxserver.v2.exceptions.SFSJoinRoomException;
import com.smartfoxserver.v2.exceptions.SFSVariableException;
import com.smartfoxserver.v2.extensions.SFSExtension;
import com.smartfoxserver.v2.persistence.room.DBRoomStorageConfig;
import com.smartfoxserver.v2.persistence.room.RoomStorageMode;
import com.smartfoxserver.v2.persistence.room.SFSStorageException;
import com.smartfoxserver.v2.security.DefaultPermissionProfile;
import net.sf.json.JSONObject;

import java.util.*;

/**
 * Created by ManJav on 10/15/2017.
 */
public class LobbyUtils
{
    private final SFSExtension ext;
    public static int arenaDivider = 5;

    public LobbyUtils() {
        ext = (SFSExtension) SmartFoxServer.getInstance().getZoneManager().getZoneByName("towers").getExtension();
    }
    public static LobbyUtils getInstance()
    {
        return new LobbyUtils();
    }


    /**
     * Save room to file for server restore rooms after resetting
     */
    public void loadAllSettings()
    {
        if( ext.getParentZone().containsProperty("lobbiesData") )
            return;

        DBRoomStorageConfig dbRoomStorageConfig = new DBRoomStorageConfig();
        dbRoomStorageConfig.storeInactiveRooms = true;
        dbRoomStorageConfig.tableName = "rooms";
        ext.getParentZone().initRoomPersistence(RoomStorageMode.DB_STORAGE, dbRoomStorageConfig);

        List<CreateRoomSettings> lobbies = null;
        try {
            lobbies = ext.getParentZone().getRoomPersistenceApi().loadAllRooms("lobbies");
        } catch (SFSStorageException e) { e.printStackTrace(); }

        Map<String, CreateRoomSettings> lobbiesData = new HashMap();
        for( CreateRoomSettings crs : lobbies )
            lobbiesData.put(crs.getName(), crs);

        ext.getParentZone().setProperty("lobbiesData", lobbiesData);
        ext.trace("loaded lobbies data in " + (System.currentTimeMillis() - (long)ext.getParentZone().getProperty("startTime")) + " milliseconds.");
    }

    public Room create(User owner, String roomName, String bio, int maxUsers, int minPoints, int avatar, int privacyMode)
    {
        CreateRoomSettings.RoomExtensionSettings res = new CreateRoomSettings.RoomExtensionSettings("TowerExtension", "com.gerantech.towers.sfs.socials.LobbyRoom");
        CreateRoomSettings rs = new CreateRoomSettings();
        rs.setHidden(false);
        rs.setAllowOwnerOnlyInvitation(false);
        rs.setDynamic(true);
        rs.setGroupId("lobbies");
        rs.setName(roomName);
        rs.setAutoRemoveMode(SFSRoomRemoveMode.NEVER_REMOVE);
        rs.setExtension(res);
        rs.setMaxVariablesAllowed(7);
        rs.setMaxUsers(maxUsers);

        List<RoomVariable> listOfVars = new ArrayList();
        listOfVars.add( new SFSRoomVariable("act", 0,         false, true, false) );
        listOfVars.add( new SFSRoomVariable("bio", bio,             false, true, false) );
        listOfVars.add( new SFSRoomVariable("pic", avatar,          false, true, false) );
        listOfVars.add( new SFSRoomVariable("min", minPoints,       false, true, false) );
        listOfVars.add( new SFSRoomVariable("pri", privacyMode,     false, true, false) );
        listOfVars.add( new SFSRoomVariable("all", new SFSArray(),  false, true, false) );
        listOfVars.add( new SFSRoomVariable("msg", new SFSArray(),  false, true, false) );
        for (RoomVariable rv : listOfVars )
            rv.setHidden(true);
        rs.setRoomVariables(listOfVars);

        Room lobby = null;
        try {
            lobby = ext.getApi().createRoom(ext.getParentZone(), rs, owner);
        } catch (SFSCreateRoomException e) { e.printStackTrace(); }


        try {
            ext.getApi().joinRoom(owner, lobby);
        } catch (SFSJoinRoomException e) { e.printStackTrace(); }

        getAllSettings(ext.getParentZone()).put(rs.getName(), rs);
        save(lobby);
        return lobby;
    }


    /**
     * Save room to file for server restore rooms after resetting
     * @param lobby
     */
    public void save(Room lobby)
    {
        save( ext.getParentZone(), lobby );
    }
    private void save(Zone zone, Room lobby)
    {
        try {
            zone.getRoomPersistenceApi().saveRoom(lobby);
        } catch (SFSStorageException e) { e.printStackTrace(); }

        // update room variables
        CreateRoomSettings settings = getSettings(zone, lobby.getName());
        settings.setRoomVariables(lobby.getVariables());
        getAllSettings(zone).put(lobby.getName(), settings);
    }

    public Room getLobby(int id)
    {
        User user = ext.getParentZone().getUserByName(id + "");
        if( user == null )
            return  null;
        List<Room> rooms = user.getJoinedRooms();
        for (Room r:rooms)
            if( r.getGroupId().equals("lobbies" ))
                return r;
        return null;
    }

    public Room getLobby(CreateRoomSettings setting, Zone zone)
    {
        Room lobby = ext.getParentZone().getRoomByName(setting.getName());
        if( lobby != null )
            return lobby;

        //if( !hasMember(createLobbySetting.getRoomVariables()) )
        //    return null;
        try {
            lobby = ext.getApi().createRoom(zone, setting, null);
        } catch (SFSCreateRoomException e) { e.printStackTrace(); }

        return lobby;
    }


    public Map<String, CreateRoomSettings> getAllSettings(Zone zone)
    {
        return (Map<String, CreateRoomSettings>) zone.getProperty("lobbiesData");
    }
    public CreateRoomSettings getSettings(Zone zone, String roomName)
    {
        return getAllSettings(zone).get(roomName);
    }
    public CreateRoomSettings getSettings(Zone zone, int memberId)
    {
        ISFSArray all;
        ISFSObject member;
        Map<String, CreateRoomSettings> lobbiesData = getAllSettings(zone);
        for (Map.Entry<String, CreateRoomSettings> entry : lobbiesData.entrySet())
        {
            all = getSettingsVariable(entry.getValue(), "all").getSFSArrayValue();
            for(int i=0; i<all.size(); i++)
            {
                member = all.getSFSObject(i);
                if( member.getInt("id").equals(memberId) )
                    return  entry.getValue();
            }
        }
        return null;
    }

    public RoomVariable getSettingsVariable(CreateRoomSettings setting, String name)
    {
        List<RoomVariable> lobbyVariables = setting.getRoomVariables();
        for( RoomVariable lv : lobbyVariables )
        if( lv.getName().equals(name) )
            return lv;
        return null;
    }

    public JSONObject getLobbyNameById(String lobbyId)
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
    }

    private void remove(Room lobby)
    {
        ext.getApi().removeRoom(lobby);
        remove(lobby.getName());
    }

    // remove room from db
    private void remove(String lobbyName)
    {
        getAllSettings(ext.getParentZone()).remove(lobbyName);
        try {
            ext.getParentZone().getRoomPersistenceApi().removeRoom(lobbyName);
        } catch (SFSStorageException e) { e.printStackTrace(); }
        ext.trace(lobbyName + " removed.");
    }

    public boolean addUser(Room lobby, int userId)
    {
        ISFSArray all = lobby.getVariable("all").getSFSArrayValue();
        int allSize = all.size();
        for(int i=0; i<allSize; i++)
            if ( all.getSFSObject(i).getInt("id").equals(userId) )
                return false;

        SFSObject member = new SFSObject();
        member.putInt("id", userId);
        member.putShort("pr", (allSize==0 ? DefaultPermissionProfile.ADMINISTRATOR : DefaultPermissionProfile.STANDARD).getId());
        all.addSFSObject(member);
        setMembersVariable(lobby, all);
        save(lobby);
        return true;
    }

    /**
     * Remove user from room variables and save lobby. if lobby is empty then lobby removed.
     * @param lobby
     * @param userId
     */
    public void removeUser(Room lobby, int userId)
    {
        int memberIndex = -1;
        ISFSArray all = lobby.getVariable("all").getSFSArrayValue();
        int allSize = all.size();
        for (int i = 0; i < allSize; i++) {
            if (all.getSFSObject(i).getInt("id").equals(userId)) {
                memberIndex = i;
                break;
            }
        }
        if( memberIndex < 0 )
            return;

        Short permission = all.getSFSObject(memberIndex).getShort("pr");
        all.removeElementAt(memberIndex);

        if( all.size() == 0 )
        {
            lobby.setAutoRemoveMode(SFSRoomRemoveMode.WHEN_EMPTY);
            remove(lobby);
            return;
        }

        // move permission to oldest member
        if( permission == DefaultPermissionProfile.ADMINISTRATOR.getId() )
            all.getSFSObject(0).putShort("pr", DefaultPermissionProfile.ADMINISTRATOR.getId());

        setMembersVariable(lobby, all);
        save(lobby);
    }

    public void setMembersVariable (Room lobby, ISFSArray value)
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

    public String resetActivenessOfLobbies()
    {
        String result = "Reset Activeness:";
        List<Room> lobbies = ext.getParentZone().getRoomListFromGroup("lobbies");
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

        result += DBUtils.getInstance().resetWeeklyBattles();
        return result;
    }

    public String saveAll()
    {
        DBRoomStorageConfig dbRoomStorageConfig = new DBRoomStorageConfig();
        dbRoomStorageConfig.storeInactiveRooms = true;
        dbRoomStorageConfig.tableName = "rooms";
        ext.getParentZone().initRoomPersistence(RoomStorageMode.DB_STORAGE, dbRoomStorageConfig);

        String result = "Start lobby saving:";
        List<Room> lobbies = ext.getParentZone().getRoomManager().getRoomListFromGroup("lobbies");
        for ( Room l:lobbies )
        {
            ext.trace("trying to save", l.getName());
            save(l);
        }
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
