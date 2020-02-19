package com.gerantech.towers.sfs.administration;

import com.smartfoxserver.v2.api.CreateRoomSettings;
import com.smartfoxserver.v2.entities.Room;
import com.smartfoxserver.v2.entities.SFSRoomRemoveMode;
import com.smartfoxserver.v2.entities.User;
import com.smartfoxserver.v2.entities.data.ISFSObject;
import com.smartfoxserver.v2.exceptions.SFSCreateRoomException;
import com.smartfoxserver.v2.exceptions.SFSJoinRoomException;
import com.smartfoxserver.v2.extensions.BaseClientRequestHandler;

public class JoinSpectatorHandler extends BaseClientRequestHandler
{
	public void handleClientRequest(User sender, ISFSObject params)
    {
        Room spectatingRoom = getParentExtension().getParentZone().getRoomByName(params.getText("t"));
        if (spectatingRoom == null)
            spectatingRoom = make(sender, params.getText("t"));
        join(sender, spectatingRoom);
        params.putInt("roomId", spectatingRoom.getId());
        send("spectateBattles", params, sender);
    }

    public void join(User user, Room spectatingRoom)
    {
        try
        {
            getApi().joinRoom(user, spectatingRoom, null, true, null);
        }
        catch (SFSJoinRoomException e)
        {
            e.printStackTrace();
        }
    }

    public Room make(User owner, String type)
    {
        CreateRoomSettings.RoomExtensionSettings res = new CreateRoomSettings.RoomExtensionSettings("TowerExtension", "com.gerantech.towers.sfs.administration.SpectateRoom");
        CreateRoomSettings rs = new CreateRoomSettings();
        rs.setGame(true);
        rs.setDynamic(true);
        rs.setMaxSpectators(10);
        rs.setMaxUsers(10);
        rs.setAutoRemoveMode(SFSRoomRemoveMode.WHEN_EMPTY);
        rs.setName( type );
        rs.setGroupId("admin");
        rs.setExtension(res);

        try {
            return getApi().createRoom(getParentExtension().getParentZone(), rs, owner);
        } catch (SFSCreateRoomException e) {
            e.printStackTrace();
        }
        return null;
    }
}