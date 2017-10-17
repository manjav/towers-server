package com.gerantech.towers.sfs.socials.handlers;

import com.gerantech.towers.sfs.callbacks.MapChangeCallback;
import com.gerantech.towers.sfs.utils.UserManager;
import com.gt.towers.Game;
import com.smartfoxserver.v2.api.CreateRoomSettings;
import com.smartfoxserver.v2.entities.Room;
import com.smartfoxserver.v2.entities.SFSRoomRemoveMode;
import com.smartfoxserver.v2.entities.User;
import com.smartfoxserver.v2.entities.data.ISFSObject;
import com.smartfoxserver.v2.entities.data.SFSArray;
import com.smartfoxserver.v2.entities.variables.RoomVariable;
import com.smartfoxserver.v2.entities.variables.SFSRoomVariable;
import com.smartfoxserver.v2.exceptions.SFSCreateRoomException;
import com.smartfoxserver.v2.exceptions.SFSJoinRoomException;
import com.smartfoxserver.v2.extensions.BaseClientRequestHandler;
import com.gerantech.towers.sfs.socials.LobbyUtils;


import java.util.ArrayList;
import java.util.List;

/**
 * Created by ManJav on 8/24/2017.
 */
public class LobbyCreateHandler extends BaseClientRequestHandler
{
    public static final int RESPONSE_OK = 0;
    public static final int RESPONSE_ROOM_EXISTS = -1;
    public static final int RESPONSE_INTERNAL_ERROR = -2;
    public static final int RESPONSE_UNKOWN_ERROR = -10;

    public void handleClientRequest(User sender, ISFSObject params)
    {
        Game game = (Game)(sender.getSession().getProperty("core"));

        MapChangeCallback mapChangeCallback = new MapChangeCallback();
        game.player.resources.changeCallback = mapChangeCallback;
        Boolean succeed = game.lobby.create();
        game.player.resources.changeCallback = null;

        if(!succeed)
        {
            game.tracer.log("lobby create failed: RESPONSE_INTERNAL_ERROR");
            params.putInt("response", RESPONSE_INTERNAL_ERROR);
            return;
        }
        String roomName = params.getUtfString("name");
        String bio = params.getUtfString("bio");
        int maxUsers = params.getInt("max");
        int minPoint = params.getInt("min");
        int avatar = params.getInt("pic");

        if( getParentExtension().getParentZone().getRoomByName(roomName) != null )
        {
            params.putInt("response", RESPONSE_ROOM_EXISTS);
            send("lobbyCreate", params, sender);
            return;
        }

        Room room = null;
        try {
            trace(UserManager.updateResources(getParentExtension(), game.player, mapChangeCallback.updates));
            room = createRoom(sender, roomName, bio, maxUsers, minPoint, avatar);
        } catch (Exception e) {
            send("lobbyCreate", params, sender);
            e.printStackTrace();
            params.putInt("response", RESPONSE_UNKOWN_ERROR);
            send("lobbyCreate", params, sender);
            return;
        }

        params.putInt("response", RESPONSE_OK);
        LobbyUtils.getInstance().save(room);
        send("lobbyCreate", params, sender);
    }

    private Room createRoom(User owner, String roomName, String bio, int maxUsers, int minPoints, int avatar) throws SFSCreateRoomException, SFSJoinRoomException
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
        rs.setMaxVariablesAllowed(6);
        rs.setMaxUsers(maxUsers);

        List<RoomVariable> listOfVars = new ArrayList<>();
        listOfVars.add( new SFSRoomVariable("act", 0,         false, true, false) );
        listOfVars.add( new SFSRoomVariable("bio", bio,             false, true, false) );
        listOfVars.add( new SFSRoomVariable("pic", avatar,          false, true, false) );
        listOfVars.add( new SFSRoomVariable("min", minPoints,       false, true, false) );
        listOfVars.add( new SFSRoomVariable("all", new SFSArray(),  false, true, false) );
        listOfVars.add( new SFSRoomVariable("msg", new SFSArray(),  false, true, false) );
        for (RoomVariable rv : listOfVars )
            rv.setHidden(true);
        rs.setRoomVariables(listOfVars);

        Room r = getApi().createRoom(getParentExtension().getParentZone(), rs, owner);
        getApi().joinRoom(owner, r);
        return r;
    }
}
