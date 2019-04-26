package com.gt.utils;

import com.gt.Commands;
import com.gt.data.LobbySFS;
import com.gt.towers.Game;
import com.gt.towers.Player;
import com.gt.towers.battle.BattleField;
import com.smartfoxserver.v2.api.CreateRoomSettings;
import com.smartfoxserver.v2.entities.Room;
import com.smartfoxserver.v2.entities.SFSRoomRemoveMode;
import com.smartfoxserver.v2.entities.User;
import com.smartfoxserver.v2.entities.data.ISFSArray;
import com.smartfoxserver.v2.entities.data.ISFSObject;
import com.smartfoxserver.v2.entities.variables.SFSUserVariable;
import com.smartfoxserver.v2.entities.variables.UserVariable;
import com.smartfoxserver.v2.exceptions.SFSCreateRoomException;
import com.smartfoxserver.v2.exceptions.SFSJoinRoomException;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by ManJav on 9/23/2017.
 */
public class BattleUtils extends UtilBase
{
    public static BattleUtils getInstance()
    {
        return (BattleUtils)UtilBase.get(BattleUtils.class);
    }
    private AtomicInteger roomId = new AtomicInteger();
    public ConcurrentHashMap<Integer, String> maps = new ConcurrentHashMap();
    public void join(User user, Room room, String spectatedUser)
    {
        //user.getSession().setProperty("challengeType", challengeType);
        Player player = ((Game)user.getSession().getProperty("core")).player;
        trace("---------=========<<<<  JOIN user:" + user.getName() + " room:" + room.getName() + " spectatedUser:" + spectatedUser + " >>>>==========---------");
        List<UserVariable> vars = new ArrayList();
        vars.add(new SFSUserVariable("name", player.nickName));
        vars.add(new SFSUserVariable("point", player.get_point()));
        vars.add(new SFSUserVariable("spectatedUser", spectatedUser));
        ext.getApi().setUserVariables(user, vars, true, true);

        try
        {
            ext.getApi().joinRoom(user, room, null, spectatedUser!="", null);
        }
        catch (SFSJoinRoomException e) { e.printStackTrace(); }
    }

    public Room make(User owner, int mode, int type, int friendlyMode)
    {
        // temp solution
        long now = Instant.now().getEpochSecond();
        List<Room> rList = ext.getParentZone().getRoomListFromGroup("battles");
        for (Room r : rList)
        {
            // trace(">>>>>>>", r.containsProperty("startAt"), now );
            if ( r.containsProperty("startAt") && now - (Integer)r.getProperty("startAt") > 400 )
            {
                trace("WAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAY!!!    BATTLE KHARAB SHOOOOOD!!!!");
                removeRoom(r);
                trace("** battle removed", r.getName(), now-(Integer)r.getProperty("startAt"));
            }
        }

        int league = ((Game)owner.getSession().getProperty("core")).player.get_arena(0);
        boolean singleMode = league == 0;

        Map<Object, Object> roomProperties = new HashMap();
        roomProperties.put("mode", mode);
        roomProperties.put("type", type);
        roomProperties.put("league", league);// ===> is temp
        roomProperties.put("friendlyMode", friendlyMode);
        roomProperties.put("state", BattleField.STATE_0_WAITING);
        trace("---------=========<<<<  MAKE owner:", owner.getName(), "mode:", mode, "type:", type, "friendlyMode:", friendlyMode, " >>>>==========---------");

        CreateRoomSettings rs = new CreateRoomSettings();
        rs.setExtension(new CreateRoomSettings.RoomExtensionSettings("TowerExtension", "com.gerantech.towers.sfs.battle.BattleRoom"));
        rs.setAutoRemoveMode( singleMode ? SFSRoomRemoveMode.WHEN_EMPTY : SFSRoomRemoveMode.NEVER_REMOVE );
        rs.setName( "m" + mode + "_t" + type + "_f" + friendlyMode + "_" + roomId.getAndIncrement() );
        rs.setRoomProperties( roomProperties );
        rs.setMaxUsers(singleMode ? 1 : 2);
        rs.setGroupId("battles");
        rs.setMaxSpectators(50);
        rs.setDynamic(true);
        rs.setGame(true);

        try {
            return ext.getApi().createRoom(ext.getParentZone(), rs, owner);
        } catch (SFSCreateRoomException e) { e.printStackTrace(); }
        return null;
    }

    public Room findActiveBattleRoom(int playerId)
    {
        List<Room> battles = ext.getParentZone().getRoomListFromGroup("battles");
        for (Room room : battles)
        {
            if( (int) room.getProperty("state") == BattleField.STATE_2_STARTED )
            {
                ArrayList<Game> registeredPlayers = (ArrayList)room.getProperty("registeredPlayers");
                if( registeredPlayers != null )
                    for (Game g : registeredPlayers)
                        if( g.player.id == playerId )
                            return room;
            }
        }
        return null;
    }

    /**
     * Kick all users and remove room
     * @param room
     */
    public void removeRoom(Room room)
    {
        List<User> users = room.getUserList();
        if( users.size() > 0 )
        {
            room.setAutoRemoveMode(SFSRoomRemoveMode.WHEN_EMPTY);
            for( User u : users )
            {
                ext.getApi().leaveRoom(u, room);
                /*if ( u.isNpc() )
                {
                    RankingUtils.getInstance().setWeeklyBattles(Integer.parseInt(u.getName()), -1);
                    ext.getApi().disconnect(u.getSession());
                }*/
            }
        }
        else
        {
            ext.getApi().removeRoom(room, false, false);
        }
    }

    public void removeReferences(Room room)
    {
        List<Room> lobbies = ext.getParentZone().getRoomListFromGroup("lobbies");
        int msgIndex = -1;
        for (int i = 0; i < lobbies.size(); i++)
        {
            Room lobby = lobbies.get(i);
            if( lobby.containsProperty(room.getName()) )
            {
                ISFSArray messageQueue = ((LobbySFS)lobby.getProperty("data")).getMessages();
                int msgSize = messageQueue.size();
                for (int j = msgSize-1; j >= 0; j--)
                {
                    ISFSObject msg = messageQueue.getSFSObject(j);
                    Short battleState = msg.getShort("st");
                    if( msg.containsKey("bid") && msg.getInt("bid") == room.getId() && battleState < 2 )
                    {
                        msg.putShort("st", (short) (battleState == 0 ? 3 : 2));
                        if( battleState == 0 )
                            msgIndex = j;
                        if( lobby.getUserList().size() > 0 )
                            ext.getApi().sendExtensionResponse(Commands.LOBBY_PUBLIC_MESSAGE, msg, lobby.getUserList(), lobby, false);
                    }
                }
                //trace(room.getName(), lobby.getName());
                if( msgIndex > -1 )
                    messageQueue.removeElementAt(msgIndex);
            }
        }
    }
}