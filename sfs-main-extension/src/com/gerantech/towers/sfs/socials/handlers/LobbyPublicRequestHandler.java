package com.gerantech.towers.sfs.socials.handlers;
import com.gerantech.towers.sfs.Commands;
import com.gerantech.towers.sfs.handlers.LoginEventHandler;
import com.gt.towers.Game;
import com.gt.towers.Player;
import com.smartfoxserver.v2.api.CreateRoomSettings;
import com.smartfoxserver.v2.entities.Room;
import com.smartfoxserver.v2.entities.SFSRoomRemoveMode;
import com.smartfoxserver.v2.entities.User;
import com.smartfoxserver.v2.entities.data.ISFSObject;
import com.smartfoxserver.v2.entities.data.SFSArray;
import com.smartfoxserver.v2.entities.variables.RoomVariable;
import com.smartfoxserver.v2.entities.variables.SFSRoomVariable;
import com.smartfoxserver.v2.entities.variables.SFSUserVariable;
import com.smartfoxserver.v2.entities.variables.UserVariable;
import com.smartfoxserver.v2.exceptions.SFSCreateRoomException;
import com.smartfoxserver.v2.exceptions.SFSJoinRoomException;
import com.smartfoxserver.v2.extensions.BaseClientRequestHandler;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class LobbyPublicRequestHandler extends BaseClientRequestHandler
{
	private Room theRoom;
    private static AtomicInteger roomId = new AtomicInteger();

	public void handleClientRequest(User sender, ISFSObject params)
    {
        int now = (int)Instant.now().getEpochSecond();
        trace(now, LoginEventHandler.UNTIL_MAINTENANCE);
        if( now < LoginEventHandler.UNTIL_MAINTENANCE )
        {
            params.putInt("umt", LoginEventHandler.UNTIL_MAINTENANCE - now);
            send(Commands.START_BATTLE, params, sender);
            return;
        }

        theRoom = findReady(sender);

        if( theRoom == null )
            theRoom = make(sender);

        join(sender, theRoom);
        send(Commands.LOBBY_PUBLIC, null, sender);
    }

    private Room findReady(User user)
    {
        // search public lobby with geo ip
        //MatchExpression exp = new MatchExpression('rank', NumberMatch.GREATER_THAN, 5).and('country', StringMatch.EQUALS, 'Italy')
        //List<User> matchingUsers = sfsApi.findUsers(zone.getUserList(), exp, 50);
        List<Room> rList = getParentExtension().getParentZone().getRoomListFromGroup("publics");
        for (Room r : rList)
            if ( !r.isFull() )
                return r;
        return null;
    }

    private Room make(User owner)
    {
        CreateRoomSettings.RoomExtensionSettings res = new CreateRoomSettings.RoomExtensionSettings("TowerExtension", "com.gerantech.towers.sfs.socials.BaseLobbyRoom");

        Game game = ((Game)owner.getSession().getProperty("core"));
        trace("---------=========<<<<  MAKE public lobby by ", owner.getName(), " >>>>==========---------");

       // Map<Object, Object> roomProperties = new HashMap<>();

        CreateRoomSettings rs = new CreateRoomSettings();
        rs.setGame(false);
        rs.setDynamic(true);
        rs.setAutoRemoveMode(SFSRoomRemoveMode.WHEN_EMPTY);
        //rs.setRoomProperties( roomProperties );
        rs.setName( "public_lobby_" + roomId.getAndIncrement() );
        rs.setMaxUsers(50);
        rs.setGroupId("publics");
        rs.setExtension(res);

        List<RoomVariable> listOfVars = new ArrayList<>();
        listOfVars.add( new SFSRoomVariable("msg", new SFSArray(),  false, true, false) );
        rs.setRoomVariables(listOfVars);

        try {
            return getApi().createRoom(getParentExtension().getParentZone(), rs, owner);
        } catch (SFSCreateRoomException e) {
            e.printStackTrace();
        }
        return null;
    }

    private void join(User user, Room theRoom)
    {
        Player player = ((Game)user.getSession().getProperty("core")).player;
        trace("---------=========<<<<  JOIN user:"+user.getName()+" the public lobby:"+theRoom.getName()+" >>>>==========---------");
        List<UserVariable> vars = new ArrayList();
        vars.add(new SFSUserVariable("name", player.nickName));
        //vars.add(new SFSUserVariable("point", player.get_point()));
        getApi().setUserVariables(user, vars, true, true);

        try
        {
            getApi().joinRoom(user, theRoom, null, false, null);
        }
        catch (SFSJoinRoomException e)
        {
            e.printStackTrace();
        }
    }
}