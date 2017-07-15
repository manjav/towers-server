package com.gerantech.towers.sfs.handlers;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import com.gerantech.towers.sfs.battle.BattleRoom;
import com.gt.towers.Game;
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
        try
        {
        	index = params.getInt("i");
        	isQuest = params.getBool("q");
        	//trace(((Game)sender.getSession().getProperty("core")).player.id);
            joinUser(sender);
        }
        catch(Exception err)
        {
            trace(ExtensionLogLevel.ERROR, err.toString());
        }
    }
 
	private void joinUser(User user) throws SFSException
    {
    	if( !isQuest )
            theRoom = findActiveRoom(user);

        if (theRoom == null)
            theRoom = makeNewRoom(user);
       
        try
        {
            getApi().joinRoom(user, theRoom);
        }
        catch (SFSJoinRoomException e)
        {
            trace(ExtensionLogLevel.ERROR, e.toString());
        }
    }

	@SuppressWarnings("unchecked")
	private Room findActiveRoom(User user) 
	{
		List<Room> rList = getParentExtension().getParentZone().getRoomList();
        for (Room room : rList)
        {
            if ( !room.isFull() || room.getGroupId()!="quests" )
            {
            	int roomState = (Integer)room.getProperty("state");
            	if(roomState == BattleRoom.STATE_WAITING )
            	{
            		return room;
             	}
            	else if( roomState == BattleRoom.STATE_BATTLE_STARTED )
            	{
            		if(((List<String>)room.getProperty("regidteredPlayersId")).contains(user.getName()))
                		return room;
            	}
            }
        }
        return null;
	}

	private Room makeNewRoom(User owner) throws SFSCreateRoomException
    {
    	RoomExtensionSettings res = new RoomExtensionSettings("TowerExtension", "com.gerantech.towers.sfs.battle.BattleRoom");
    	
    	if( !isQuest )
    		index = ((Game)owner.getSession().getProperty("core")).player.get_arena()*100+1;
    	
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