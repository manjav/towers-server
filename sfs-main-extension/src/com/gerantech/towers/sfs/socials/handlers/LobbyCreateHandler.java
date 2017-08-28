package com.gerantech.towers.sfs.socials.handlers;

import com.gt.towers.Game;
import com.gt.towers.Player;
import com.smartfoxserver.v2.api.CreateRoomSettings;
import com.smartfoxserver.v2.entities.Room;
import com.smartfoxserver.v2.entities.SFSRoomRemoveMode;
import com.smartfoxserver.v2.entities.User;
import com.smartfoxserver.v2.entities.data.ISFSObject;
import com.smartfoxserver.v2.entities.data.SFSArray;
import com.smartfoxserver.v2.entities.data.SFSObject;
import com.smartfoxserver.v2.entities.variables.RoomVariable;
import com.smartfoxserver.v2.entities.variables.SFSRoomVariable;
import com.smartfoxserver.v2.exceptions.SFSCreateRoomException;
import com.smartfoxserver.v2.exceptions.SFSJoinRoomException;
import com.smartfoxserver.v2.extensions.BaseClientRequestHandler;
import com.smartfoxserver.v2.persistence.room.FileRoomStorageConfig;
import com.smartfoxserver.v2.persistence.room.RoomStorageMode;
import com.smartfoxserver.v2.persistence.room.SFSStorageException;
import com.smartfoxserver.v2.security.DefaultPermissionProfile;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by ManJav on 8/24/2017.
 */
public class LobbyCreateHandler extends BaseClientRequestHandler
{
    public LobbyCreateHandler() {}

    public void handleClientRequest(User sender, ISFSObject params)
    {
        String roomName = params.getUtfString("name");
        String bio = params.getUtfString("bio");
        int maxUsers = params.getInt("max");
        int minPoint = params.getInt("min");
        int avatar = params.getInt("pic");
        Room room = createRoom(sender, roomName, bio, maxUsers, minPoint, avatar);
        if( room == null )
            return;

        LobbyRoomServerEventsHandler.save(getParentExtension().getParentZone(), room);
        send("lobbyCreate", params, sender);
    }

    private Room createRoom(User owner, String roomName, String bio, int maxUsers, int minPoints, int avatar)
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
        rs.setMaxUsers(maxUsers);

        List<RoomVariable> listOfVars = new ArrayList<>();
        listOfVars.add( new SFSRoomVariable("bio", bio,             false, true, false) );
        listOfVars.add( new SFSRoomVariable("pic", avatar,          false, true, false) );
        listOfVars.add( new SFSRoomVariable("min", minPoints,       false, true, false) );
        listOfVars.add( new SFSRoomVariable("all", new SFSArray(),  false, true, false) );
        rs.setRoomVariables(listOfVars);

        try {
            Room r = getApi().createRoom(getParentExtension().getParentZone(), rs, owner);
            getApi().joinRoom(owner, r);
            return r;
        } catch (SFSCreateRoomException e) {
            e.printStackTrace();
        } catch (SFSJoinRoomException e) {
            e.printStackTrace();
        }
        return null;
    }
}
