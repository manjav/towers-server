package com.gerantech.towers.sfs.handlers;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import com.gerantech.towers.sfs.battle.BattleRoom;
import com.gt.towers.Game;
import com.gt.towers.Player;
import com.smartfoxserver.v2.api.CreateRoomSettings;
import com.smartfoxserver.v2.api.CreateRoomSettings.RoomExtensionSettings;
import com.smartfoxserver.v2.entities.Room;
import com.smartfoxserver.v2.entities.SFSRoomRemoveMode;
import com.smartfoxserver.v2.entities.User;
import com.smartfoxserver.v2.entities.data.ISFSObject;
import com.smartfoxserver.v2.exceptions.SFSCreateRoomException;
import com.smartfoxserver.v2.exceptions.SFSException;
import com.smartfoxserver.v2.exceptions.SFSJoinRoomException;
import com.smartfoxserver.v2.extensions.BaseClientRequestHandler;
import com.smartfoxserver.v2.extensions.ExtensionLogLevel;
 
public class BattleAutoJoinHandler extends BaseClientRequestHandler
{
    private final String version = "1.0.1";
 
    private static AtomicInteger roomId = new AtomicInteger();

	private int index;
	private Boolean isQuest;
	private Room theRoom;

 
    public void init()
    {
        trace("Java AutoJoiner: " + version);
    }
 
	/* (non-Javadoc)
	 * @see com.smartfoxserver.v2.extensions.IClientRequestHandler#handleClientRequest(com.smartfoxserver.v2.entities.User, com.smartfoxserver.v2.entities.data.ISFSObject)
	 */
	public void handleClientRequest(User sender, ISFSObject params)
    {
        trace(params.getDump());
        try
        {
        	index = params.getInt("i");
        	isQuest = params.getBool("q");
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
            theRoom = makeNewRoom(user);

        Player player = ((Game)user.getSession().getProperty("core")).player;
        user.setProperty("name", player.nickName);
        user.setProperty("point", player.get_point());
        try
        {
            trace("joinRoom");
            getApi().joinRoom(user, theRoom);
        }
        catch (SFSJoinRoomException e)
        {
            trace(ExtensionLogLevel.ERROR, e.toString());
        }
    }

    private Room findWaitingBattlsRoom(User user)
	{
		List<Room> rList = getParentExtension().getParentZone().getRoomListFromGroup("battles");
        for (Room room : rList)
            if ( !room.isFull() && (Integer)room.getProperty("state") == BattleRoom.STATE_WAITING )
              		return room;
        return null;
	}

	private Room makeNewRoom(User owner) throws SFSCreateRoomException
    {
        trace("makeNewRoom");
        RoomExtensionSettings res = new RoomExtensionSettings("TowerExtension", "com.gerantech.towers.sfs.battle.BattleRoom");

    	if( !isQuest )
    		index = ((Game)owner.getSession().getProperty("core")).player.get_arena(0)*100+(int)Math.ceil(Math.random()*2);

        Map<Object, Object> roomProperties = new HashMap<Object, Object>();
        roomProperties.put("isQuest", isQuest);
        roomProperties.put("index", index);
        roomProperties.put("startAt", (int)Instant.now().getEpochSecond());

        CreateRoomSettings rs = new CreateRoomSettings();
        rs.setGame(true);
        rs.setDynamic(true);
        rs.setAutoRemoveMode(SFSRoomRemoveMode.WHEN_EMPTY);
		rs.setRoomProperties( roomProperties );
        rs.setName((isQuest?"room_quest_":"room_battle_") + roomId.getAndIncrement());
        rs.setMaxUsers(isQuest?1:2);
        rs.setGroupId(isQuest?"quests":"battles");
        rs.setExtension(res);

        return getApi().createRoom(getParentExtension().getParentZone(), rs, owner);
    }
}