package com.gerantech.towers.sfs.handlers;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import com.smartfoxserver.v2.api.CreateRoomSettings;
import com.smartfoxserver.v2.entities.Room;
import com.smartfoxserver.v2.entities.User;
import com.smartfoxserver.v2.entities.data.ISFSObject;
import com.smartfoxserver.v2.entities.data.SFSObject;
import com.smartfoxserver.v2.exceptions.SFSCreateRoomException;
import com.smartfoxserver.v2.exceptions.SFSException;
import com.smartfoxserver.v2.exceptions.SFSJoinRoomException;
import com.smartfoxserver.v2.extensions.BaseClientRequestHandler;
import com.smartfoxserver.v2.extensions.ExtensionLogLevel;
 
public class BattleRequestsHandler extends BaseClientRequestHandler
{
    private final String version = "1.0.1";
 
    private final AtomicInteger roomId = new AtomicInteger();

	private ISFSObject params;
 
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
        	this.params = params;
            //if (params.getText("cmd").equals("joinMe"))
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
        Room theRoom = null;
 
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
            
            //SFSObject sfsO = new SFSObject();
            //sfsO.putSFSObject("param", params);
            user.setProperty("towers", params);
            
            if(theRoom.isFull())
            {
            	List<User> players = theRoom.getPlayersList();
                for (int i=0; i<players.size(); i++)
                {
                	User me = players.get(i);
                	User their = players.get(i==0?1:0);
                	send("startBattle", ((SFSObject)their.getProperty("towers")), me);
                }
            }
        }
        catch (SFSJoinRoomException e)
        {
            trace(ExtensionLogLevel.ERROR, e.toString());
        }
    }
 
    private Room makeNewRoom(User owner) throws SFSCreateRoomException
    {
        Room room = null;
 
        CreateRoomSettings rs = new CreateRoomSettings();
        rs.setGame(true);
        rs.setDynamic(true);
        rs.setName("BattleRoom_" + roomId.getAndIncrement());
        rs.setMaxUsers(2);
 
        room = getApi().createRoom(getParentExtension().getParentZone(), rs, owner);
        return room;
    }
}