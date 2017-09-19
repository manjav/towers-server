package com.gerantech.towers.sfs.handlers;
import com.gerantech.towers.sfs.battle.BattleRoom;
import com.gt.towers.Game;
import com.gt.towers.Player;
import com.hazelcast.util.RandomPicker;
import com.smartfoxserver.v2.api.CreateRoomSettings;
import com.smartfoxserver.v2.api.CreateRoomSettings.RoomExtensionSettings;
import com.smartfoxserver.v2.api.ISFSApi;
import com.smartfoxserver.v2.entities.Room;
import com.smartfoxserver.v2.entities.SFSRoomRemoveMode;
import com.smartfoxserver.v2.entities.User;
import com.smartfoxserver.v2.entities.Zone;
import com.smartfoxserver.v2.entities.data.ISFSObject;
import com.smartfoxserver.v2.entities.variables.SFSUserVariable;
import com.smartfoxserver.v2.entities.variables.UserVariable;
import com.smartfoxserver.v2.exceptions.SFSCreateRoomException;
import com.smartfoxserver.v2.exceptions.SFSException;
import com.smartfoxserver.v2.exceptions.SFSJoinRoomException;
import com.smartfoxserver.v2.extensions.BaseClientRequestHandler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class BattleAutoJoinHandler extends BaseClientRequestHandler
{

    private static AtomicInteger roomId = new AtomicInteger();
    private static int arenaDivider = 5;
    private int index;
	private Boolean isQuest;
	private Room theRoom;

	public void handleClientRequest(User sender, ISFSObject params)
    {
        try
        {
            index = params.getInt("i");
            isQuest = params.getBool("q");
            if( params.containsKey("su") )
            {

                index -= 100000;
                theRoom = getParentExtension().getParentZone().getRoomById(index);
                if( theRoom != null )
                    join(getApi(), sender, theRoom, params.getText("su"));
                return;
            }

        	//trace(((Game)sender.getSession().getProperty("core")).player.id);
            joinUser(sender);
        }
        catch(Exception err)
        {
            err.printStackTrace();
        }
    }
 
	private void joinUser(User user) throws SFSException
    {
        if( !isQuest )
        {
            int joinedRoomId = (Integer) user.getSession().getProperty("joinedRoomId");
            if( joinedRoomId > -1 )
                theRoom = getParentExtension().getParentZone().getRoomById(joinedRoomId);
            else
                theRoom = findWaitingBattlsRoom(user);
        }

        if (theRoom == null)
            theRoom = makeNewRoom(getApi(), getParentExtension().getParentZone(), user, isQuest, index);

        join(getApi(), user, theRoom, "");
    }

    private Room findWaitingBattlsRoom(User user)
    {
        //MatchExpression exp = new MatchExpression('rank', NumberMatch.GREATER_THAN, 5).and('country', StringMatch.EQUALS, 'Italy')
        //List<User> matchingUsers = sfsApi.findUsers(zone.getUserList(), exp, 50);
        int arenaIndex =  Math.min(arenaDivider, ((Game)user.getSession().getProperty("core")).player.get_arena(0));
        List<Room> rList = getParentExtension().getParentZone().getRoomListFromGroup("battles");
        for (Room room : rList)
            if ( !room.isFull() && !theRoom.containsProperty("isFriendly") && (Integer)room.getProperty("state") == BattleRoom.STATE_WAITING && ((int)room.getProperty("arena")) == arenaIndex)
                return room;
        return null;
    }

    public static void join(ISFSApi api, User user, Room theRoom, String spectatedUser)
    {
        Player player = ((Game)user.getSession().getProperty("core")).player;
        List<UserVariable> vars = new ArrayList<>();
        vars.add(new SFSUserVariable("name", player.nickName));
        vars.add(new SFSUserVariable("point", player.get_point()));
        vars.add(new SFSUserVariable("spectatedUser", spectatedUser));
        api.setUserVariables(user, vars, true, true);

        try
        {
            api.joinRoom(user, theRoom, null, spectatedUser!="", null);
            // trace("joined to battle or quest room.");
        }
        catch (SFSJoinRoomException e)
        {
            e.printStackTrace();
        }
    }

	public static Room makeNewRoom(ISFSApi api, Zone zone, User owner, boolean isQuest, int index)
    {
        RoomExtensionSettings res = new RoomExtensionSettings("TowerExtension", "com.gerantech.towers.sfs.battle.BattleRoom");

        Game game = ((Game)owner.getSession().getProperty("core"));
        int arenaIndex = game.player.get_arena(0);
        String[] fields = game.arenas.get(arenaIndex).fields.keys();
    	if( !isQuest )
    		index = game.arenas.get(arenaIndex).fields.get(fields[RandomPicker.getInt(0, fields.length)]).index;

        Map<Object, Object> roomProperties = new HashMap<>();
        roomProperties.put("isQuest", isQuest);
        roomProperties.put("index", index);
        roomProperties.put("arena", Math.min(arenaDivider, arenaIndex));// ===> is temp

        CreateRoomSettings rs = new CreateRoomSettings();
        rs.setGame(true);
        rs.setMaxSpectators(10);
        rs.setDynamic(true);
        rs.setAutoRemoveMode(SFSRoomRemoveMode.WHEN_EMPTY);
		rs.setRoomProperties( roomProperties );
        rs.setName((isQuest?"room_quest_":"room_battle_") + roomId.getAndIncrement());
        rs.setMaxUsers(isQuest?1:2);
        rs.setGroupId(isQuest?"quests":"battles");
        rs.setExtension(res);

        try {
            return api.createRoom(zone, rs, owner);
        } catch (SFSCreateRoomException e) {
            e.printStackTrace();
        }
        return null;
    }
}