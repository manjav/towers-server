package com.gerantech.towers.sfs.socials;

import com.gerantech.towers.sfs.socials.handlers.LobbyRoomServerEventsHandler;
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
        FileRoomStorageConfig fileRoomStorageConfig = new FileRoomStorageConfig();
        zone.initRoomPersistence(RoomStorageMode.FILE_STORAGE, fileRoomStorageConfig);
        try {
            zone.getRoomPersistenceApi().saveRoom(room);
        } catch (SFSStorageException e) {
            e.printStackTrace();
        }
    }

    // remove room from db
    private void remove(Zone zone, Room room)
    {
        FileRoomStorageConfig fileRoomStorageConfig = new FileRoomStorageConfig();
        zone.initRoomPersistence(RoomStorageMode.FILE_STORAGE, fileRoomStorageConfig);
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

        all.removeElementAt(memberIndex);

        if( all.size() == 0 )
        {
            lobby.setAutoRemoveMode(SFSRoomRemoveMode.WHEN_EMPTY);
            remove(ext.getParentZone(), lobby);
            return;
        }

        setMembersVariable(lobby, all);
        save(lobby);
    }

    private void setMembersVariable (Room lobby, ISFSArray value)
    {
        try {
            SFSRoomVariable var = new SFSRoomVariable("all", value,  true, true, false);
            var.setHidden(true);
            lobby.setVariable( var );
        } catch (SFSVariableException e) {
            e.printStackTrace();
        }
    }

}
