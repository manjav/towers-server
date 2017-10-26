package com.gerantech.towers.sfs.socials;

import com.gerantech.towers.sfs.utils.UserManager;
import com.smartfoxserver.v2.SmartFoxServer;
import com.smartfoxserver.v2.api.CreateRoomSettings;
import com.smartfoxserver.v2.entities.Room;
import com.smartfoxserver.v2.entities.SFSRoomRemoveMode;
import com.smartfoxserver.v2.entities.Zone;
import com.smartfoxserver.v2.entities.data.ISFSArray;
import com.smartfoxserver.v2.entities.data.SFSArray;
import com.smartfoxserver.v2.entities.data.SFSObject;
import com.smartfoxserver.v2.entities.variables.RoomVariable;
import com.smartfoxserver.v2.entities.variables.SFSRoomVariable;
import com.smartfoxserver.v2.exceptions.SFSCreateRoomException;
import com.smartfoxserver.v2.exceptions.SFSVariableException;
import com.smartfoxserver.v2.extensions.SFSExtension;
import com.smartfoxserver.v2.persistence.room.FileRoomStorageConfig;
import com.smartfoxserver.v2.persistence.room.RoomStorageMode;
import com.smartfoxserver.v2.persistence.room.SFSStorageException;
import com.smartfoxserver.v2.security.DefaultPermissionProfile;
import com.smartfoxserver.v2.util.CryptoUtils;

import javax.xml.bind.annotation.adapters.HexBinaryAdapter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by ManJav on 10/15/2017.
 */
public class LobbyUtils
{
    private final SFSExtension ext;
    public static int arenaDivider = 5;

    public LobbyUtils()
    {
        ext = (SFSExtension) SmartFoxServer.getInstance().getZoneManager().getZoneByName("towers").getExtension();
    }
    public static LobbyUtils getInstance()
    {
        return new LobbyUtils();
    }

    // Save room to file for server rstore rooms after resetting
    public void save(Room room)
    {
        save( ext.getParentZone(), room );
    }
    private void save(Zone zone, Room room)
    {
        try {
            zone.getRoomPersistenceApi().saveRoom(room);
        } catch (SFSStorageException e) {
            e.printStackTrace();
        }
    }

    // remove room from db
    private void remove(Zone zone, Room room)
    {
        try {
            zone.getRoomPersistenceApi().removeRoom(room.getName());
            ext.getApi().removeRoom(room);
        } catch (SFSStorageException e) {
            e.printStackTrace();
        }
        ext.trace("remove " + room.getName());
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
            remove(ext.getParentZone(), lobby);
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
        } catch (SFSVariableException e) {
            e.printStackTrace();
        }
    }

    public void setActivenessVariable (Room lobby, int value)
    {
        try {
            SFSRoomVariable var = new SFSRoomVariable("act", value,  true, true, false);
            var.setHidden(true);
            lobby.setVariable( var );
        } catch (SFSVariableException e) {
            e.printStackTrace();
        }

        if( value % 5 == 0 )
            save(lobby);
    }

    public String resetActivenessOfLobbies()
    {
        String result = "Reset Activeness:";
        Zone zone = ext.getParentZone();
        List<Room> lobbies = ext.getParentZone().getRoomListFromGroup("lobbies");
        int lobbiesLen = lobbies.size()-1;
        while ( lobbiesLen >= 0 )
        {
            setActivenessVariable(lobbies.get(lobbiesLen), 0);
            result += "\n Lobby " + lobbies.get(lobbiesLen).getName() + " activeness set to '0'";
            lobbiesLen --;
        }

        try {
            zone.getRoomPersistenceApi().saveAllRooms("lobbies");
        } catch (SFSStorageException e) {
            e.printStackTrace();
        }

        result += UserManager.resetWeeklyBattles(ext);
        return result;
    }



    public String cleanLobbyVars()
    {
        Zone zone = ext.getParentZone();
        String result = "Start lobby cleaning:";

        try {
            FileRoomStorageConfig fileRoomStorageConfig = new FileRoomStorageConfig();
            zone.initRoomPersistence(RoomStorageMode.FILE_STORAGE, fileRoomStorageConfig);
            List<CreateRoomSettings> lobbies = zone.getRoomPersistenceApi().loadAllRooms("lobbies");
            for ( CreateRoomSettings crs : lobbies )
            {
                crs.setMaxVariablesAllowed(6);
                List<RoomVariable> listOfVars = crs.getRoomVariables();

                listOfVars.add( new SFSRoomVariable("act", 0,         false, true, false) );
                listOfVars.add( new SFSRoomVariable("msg", new SFSArray(),  false, true, false) );
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
    }


    /*public String saveLobbies()
    {
        Zone zone = ext.getParentZone();
        List<Room> lobbies = ext.getParentZone().getRoomListFromGroup("lobbies");
*/
        /*try {
            zone.getRoomPersistenceApi().saveAllRooms("lobbies");
        } catch (SFSStorageException e) {
            e.printStackTrace();
        }*/

        /*int lobbiesLen = lobbies.size()-1;
        Room lobby;
        while ( lobbiesLen >= 0 )
        {
            lobby = lobbies.get(lobbiesLen);
            save(zone, lobby);
            lobbiesLen --;
        }
        return "Variables of " + lobbies.size() + " lobbies have been saved.";
    }
*/

}
