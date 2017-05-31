package com.gerantech.towers.sfs.battle.handlers;

import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import com.gerantech.towers.sfs.battle.BattleRoom;
import com.smartfoxserver.v2.core.ISFSEvent;
import com.smartfoxserver.v2.core.SFSEventParam;
import com.smartfoxserver.v2.core.SFSEventType;
import com.smartfoxserver.v2.entities.Room;
import com.smartfoxserver.v2.entities.User;
import com.smartfoxserver.v2.entities.data.SFSObject;
import com.smartfoxserver.v2.exceptions.SFSException;
import com.smartfoxserver.v2.extensions.BaseServerEventHandler;

public class BattleRoomServerEventsHandler extends BaseServerEventHandler
{




	private BattleRoom roomClass;
	private Room room;

	public void handleServerEvent(ISFSEvent arg) throws SFSException
	{
		room = (Room)arg.getParameter(SFSEventParam.ROOM);
		roomClass = (BattleRoom) room.getExtension();
		//User user = (User)arg.getParameter(SFSEventParam.USER); 
		//Zone zone = (Zone)arg.getParameter(SFSEventParam.ZONE); 
		
		if(arg.getType().equals(SFSEventType.USER_JOIN_ROOM))
		{
			trace(room.getId(), room.getMaxUsers(), room.isFull());
			if(!room.isFull())
			{
		       	roomClass.autoJoinTimer = new Timer();
		       	roomClass.autoJoinTimer.scheduleAtFixedRate(new TimerTask()
	        	{
					@Override
					public void run()
					{
						room.setMaxUsers(1);
		            	sendStartBattleResponse();
					}
	    		}, 5000, 5000);
			}
			else
			{
            	sendStartBattleResponse();
	        }
		}
		else if(arg.getType().equals(SFSEventType.USER_LEAVE_ROOM) || arg.getType().equals(SFSEventType.ROOM_REMOVED))
		{
			roomClass.destroyGame();
		}
	}
	
	private void sendStartBattleResponse()
	{
		List<User> players = room.getPlayersList();
	    for (int i=0; i < players.size(); i++)
	    {
	        SFSObject sfsO = new SFSObject();
	        //sfsO.putLong("time", Instant.now().toEpochMilli());
	        sfsO.putInt("troopType", i);
	        sfsO.putInt("roomId", room.getId());
	    	send("startBattle", sfsO, players.get(i));
	    }
	
		roomClass.createGame();
	}
}
