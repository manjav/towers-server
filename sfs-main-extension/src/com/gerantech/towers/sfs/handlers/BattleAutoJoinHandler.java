package com.gerantech.towers.sfs.handlers;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import com.smartfoxserver.v2.api.CreateRoomSettings;
import com.smartfoxserver.v2.api.CreateRoomSettings.RoomExtensionSettings;
import com.smartfoxserver.v2.entities.Room;
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
        List<Room> rList = getParentExtension().getParentZone().getRoomList();

        for (Room room : rList)
        {
            if (room.isFull())
                continue;
            else
            {
                theRoom = room;
                break;
            }
        }
 
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

	private Room makeNewRoom(User owner) throws SFSCreateRoomException
    {
    	RoomExtensionSettings res = new RoomExtensionSettings("TowerExtension", "com.gerantech.towers.sfs.battle.BattleRoom");
    	
        Map<Object, Object> roomProperties = new HashMap<Object, Object>();
        roomProperties.put("isQuest", isQuest);
        roomProperties.put("index", index);
    	
        CreateRoomSettings rs = new CreateRoomSettings();
        rs.setGame(true);
        rs.setDynamic(true);
		rs.setRoomProperties( roomProperties );
        rs.setName((isQuest?"room_quest_":"room_battle_") + roomId.getAndIncrement());
        rs.setMaxUsers(isQuest?1:2);
        rs.setGroupId(isQuest?"quests":"battles");
        rs.setExtension(res);
        
        return getApi().createRoom(getParentExtension().getParentZone(), rs, owner);
    }
}