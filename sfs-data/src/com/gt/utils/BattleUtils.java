package com.gt.utils;

import com.gt.Commands;
import com.gt.data.LobbySFS;
import com.gt.towers.Game;
import com.gt.towers.Player;
import com.gt.towers.battle.FieldProvider;
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
    public void join(User user, Room theRoom, String spectatedUser, int challengeType)
    {
        user.getSession().setProperty("challengeType", challengeType);
        Player player = ((Game)user.getSession().getProperty("core")).player;
        trace("---------=========<<<<  JOIN user:"+user.getName()+" theRoom:"+theRoom.getName()+" spectatedUser:"+spectatedUser+" >>>>==========---------");
        List<UserVariable> vars = new ArrayList();
        vars.add(new SFSUserVariable("name", player.nickName));
        vars.add(new SFSUserVariable("point", player.get_point()));
        vars.add(new SFSUserVariable("spectatedUser", spectatedUser));
        ext.getApi().setUserVariables(user, vars, true, true);

        try
        {
            ext.getApi().joinRoom(user, theRoom, null, spectatedUser!="", null);
        }
        catch (SFSJoinRoomException e) { e.printStackTrace(); }
    }

    public Room make(User owner, boolean isOperation, int index, int friendlyMode, boolean hasExtraTime)
    {
        CreateRoomSettings.RoomExtensionSettings res = new CreateRoomSettings.RoomExtensionSettings("TowerExtension", "com.gerantech.towers.sfs.battle.BattleRoom");

        Game game = ((Game)owner.getSession().getProperty("core"));

        Map<Object, Object> roomProperties = new HashMap<>();

        int arena = 0;
        if( !isOperation )
        {
            arena = game.arenas.get(game.player.get_arena(game.player.get_point())).index;
            int tutorMaps = 3;
            boolean tutorMode = game.player.get_battleswins() < tutorMaps;
            List<String> fields = FieldProvider.battles.getKeyRange(arena * 100 + (arena == 0 && !tutorMode ? tutorMaps : 0), (arena + 1) * 100);
            index = tutorMode ? (game.player.get_battleswins() + 1) : FieldProvider.battles.get(fields.get((int) Math.floor(Math.random() * fields.size()))).index;

            //Double arenaIndex =  Math.min(BattleUtils.arenaDivider, Math.floor(arena.index/2)*2);
            roomProperties.put("arena", arena);// ===> is temp
        }
        trace("---------=========<<<<  MAKE owner:", owner.getName(), "index:", index, "isOperation:", isOperation, "friendlyMode:", friendlyMode, " >>>>==========---------");

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


        boolean singleMode = isOperation || arena == 0;
        roomProperties.put("isOperation", isOperation);
        roomProperties.put("index", index);
        if( hasExtraTime )
            roomProperties.put("hasExtraTime", true);
        if( friendlyMode > 0 )
            roomProperties.put("isFriendly", true);

        String pref = isOperation ? "o_" : "b_";
        if( friendlyMode > 0 )
            pref = friendlyMode == 1 ? "fl_" : "fb_";

        CreateRoomSettings rs = new CreateRoomSettings();
        rs.setGame(true);
        rs.setMaxSpectators(50);
        rs.setDynamic(true);
        rs.setAutoRemoveMode( singleMode ? SFSRoomRemoveMode.WHEN_EMPTY : SFSRoomRemoveMode.NEVER_REMOVE );
        rs.setRoomProperties( roomProperties );
        rs.setName( pref + index+ "__" + roomId.getAndIncrement() );
        rs.setMaxUsers(singleMode ? 1 : 2);
        rs.setGroupId(isOperation?"operations":"battles");
        rs.setExtension(res);

        try {
            return ext.getApi().createRoom(ext.getParentZone(), rs, owner);
        } catch (SFSCreateRoomException e) {
            e.printStackTrace();
        }
        return null;
    }

    public Room findActiveBattleRoom(int playerId)
    {
        List<Room> battles = ext.getParentZone().getRoomListFromGroup("battles");
        for (Room room : battles)
        {
            if( (int) room.getProperty("state") == 2 )//BattleRoom.STATE_BATTLE_STARTED
            {
                ArrayList<Game> registeredPlayers = (ArrayList)room.getProperty("registeredPlayers");
                if( registeredPlayers != null )
                    for (Game g : registeredPlayers)
                        if ( g.player.id == playerId )
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