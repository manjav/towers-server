package com.gerantech.towers.sfs.battle.handlers;

import java.time.Instant;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import com.gerantech.towers.sfs.battle.BattleRoom;
import com.gt.towers.Game;
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
			//trace(room.getId(), room.getMaxUsers(), room.isFull());
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
	    		}, 3333, 3333);
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
		Boolean isQuest = (Boolean)room.getProperty("isQuest");
	
		int index = (Integer)room.getProperty("index");
		String mapName = "battle_" + index;//"+(Math.random()>0.5?1:0);
		if(isQuest)
			mapName = "quest_" + index;//((Game)players.get(0).getSession().getProperty("core")).player.get_questIndex();
		
		List<User> players = room.getPlayersList();
	    for (int i=0; i < players.size(); i++)
	    {
	        SFSObject sfsO = new SFSObject();
	        sfsO.putInt("startAt", (int)Instant.now().getEpochSecond());
	        sfsO.putInt("troopType", i);
	        sfsO.putInt("roomId", room.getId());
	        sfsO.putText("mapName", mapName);
	    	send("startBattle", sfsO, players.get(i));
	    }
	    
		roomClass.createGame(((Game)players.get(0).getSession().getProperty("core")), mapName, isQuest);
	}
}
