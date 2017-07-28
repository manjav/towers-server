package com.gerantech.towers.sfs.battle.handlers;

import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import com.gerantech.towers.sfs.battle.BattleRoom;
import com.gerantech.towers.sfs.utils.RankingTools;
import com.gt.hazel.RankData;
import com.gt.towers.Game;
import com.hazelcast.config.Config;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.IMap;
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
	private static boolean _ENABLED = true;
	
	private BattleRoom roomClass;
	private Room room;

	public void handleServerEvent(ISFSEvent arg) throws SFSException
	{
		if(!_ENABLED)
			return;
		//trace("HANDLESERVEREVENT______", arg);
		User user = (User)arg.getParameter(SFSEventParam.USER); 
		
		if(arg.getType().equals(SFSEventType.USER_DISCONNECT))
		{
			_ENABLED = false;
			@SuppressWarnings("unchecked")
			List<Room> joinedRooms = (List<Room>) arg.getParameter(SFSEventParam.JOINED_ROOMS);
		    for(Room r:joinedRooms)
		    {
		    	int state = (Integer)r.getProperty("state");
		    	if(state < BattleRoom.STATE_CREATED || state < BattleRoom.STATE_BATTLE_STARTED && r.getUserManager().getNPCCount() > 0)
		    		getApi().removeRoom(r);
		    	else
		    	{
				    for(User u:r.getPlayersList())
				    {
				    	if(!u.isNpc() && !u.equals(user))
				    	{
				    		SFSObject sfsO = SFSObject.newInstance();
							sfsO.putText("user", ((Game) user.getSession().getProperty("core")).player.nickName);
				    		send("leftBattle", sfsO, u);
				    	}
				    }
		    	}
		    }
			_ENABLED = true;
		    return;
		}
		
		room = (Room)arg.getParameter(SFSEventParam.ROOM);
		roomClass = (BattleRoom) room.getExtension();
		//Zone zone = (Zone)arg.getParameter(SFSEventParam.ZONE); 
		if(arg.getType().equals(SFSEventType.USER_JOIN_ROOM))
		{
			//trace(room.getId(), room.getProperty("state"), room.getMaxUsers(), room.isFull(), room.getCapacity(), room.getSize(), room.getProperties().keySet().toArray());
			
			// return to previous room
			if( (Integer)room.getProperty("state") == BattleRoom.STATE_BATTLE_STARTED )
			{
				List<User> players = roomClass.getPlayers();
				for (int i=0; i < players.size(); i++)
			    {
					SFSObject sfsO = new SFSObject();
			    	if(players.get(i).equals(user))
			    	{
				    	sendBattleData(sfsO, players, i, false);
			    	}
			    	else if( !players.get(i).isNpc() )
			    	{
			    		sfsO.putText("user", ((Game) user.getSession().getProperty("core")).player.nickName);
			    		send("rejoinBattle", sfsO, players.get(i));	
		    		}
			    }
				return;
			}
			
			// wait to match making ( complete battle-room`s players )
			if( !room.isFull() )
			{
		       	roomClass.autoJoinTimer = new Timer();
		       	roomClass.autoJoinTimer.scheduleAtFixedRate(new TimerTask()
	        	{
					@Override
					public void run()
					{

						Game game = (Game) room.getPlayersList().get(0).getSession().getProperty("core");
						// trace(game.player.id, game.player.nickName, game.player.get_point());

						IMap<Integer, RankData> users = RankingTools.fill(Hazelcast.getOrCreateHazelcastInstance(new Config("aaa")).getMap("users"), game);
						RankData opponent = RankingTools.getNearOpponent(users, game.player.get_point(), 20);
						try {
							User npcUser = getApi().createNPC(opponent.id+"", getParentExtension().getParentZone(), true);
							npcUser.setProperty("point", opponent.point);
							npcUser.setProperty("name", opponent.name);
							getApi().joinRoom(npcUser, room);
						} catch (Exception e) {
							trace(e.getMessage());
						}
						cancel();
						roomClass.autoJoinTimer.cancel();
						//sendStartBattleResponse();
					}
	    		}, 3333, 3333);
			}
			else
			{
				sendStartBattleResponse();
	        }
		}
//		else if(arg.getType().equals(SFSEventType.USER_LEAVE_ROOM) || arg.getType().equals(SFSEventType.ROOM_REMOVED)){}
	}

	private void sendStartBattleResponse()
	{
		boolean isQuest = (Boolean)room.getProperty("isQuest");
		boolean existsNpc = false;
		String mapName = getMapName(isQuest);

		List<User> players = roomClass.getPlayers();
	    for (int i=0; i < players.size(); i++)
	    {
	    	if( players.get(i).isNpc() )
	    		existsNpc = true;
	    	sendBattleData(new SFSObject(), players, i, isQuest);
	    }
	    
		roomClass.createGame(((Game)players.get(0).getSession().getProperty("core")), mapName, isQuest, existsNpc||isQuest);
	}

	private String getMapName(boolean isQuest)
	{
		int index = (Integer)room.getProperty("index");
		String mapName = "battle_" + index;
		if(isQuest)
			mapName = "quest_" + index;//((Game)players.get(0).getSession().getProperty("core")).player.get_questIndex();
		return mapName;
	}


	private void sendBattleData(SFSObject sfsO, List<User> players, int palyerIndex, boolean isQuest)
	{
		if(players.get(palyerIndex).isNpc())
			return;

		sfsO.putInt("startAt", (Integer)room.getProperty("startAt"));
		sfsO.putInt("troopType", palyerIndex);
		sfsO.putInt("roomId", room.getId());
		sfsO.putText("mapName", getMapName(isQuest));

		if( players.size() == 2 )
		{
			User opponent = players.get(palyerIndex==0?1:0);
			SFSObject sfsOpp = new SFSObject();
			sfsOpp.putText("name", (String)opponent.getProperty("name"));
			sfsOpp.putInt("point", (Integer)opponent.getProperty("point"));
			sfsO.putSFSObject("opponent", sfsOpp);
		}
		send("startBattle", sfsO, players.get(palyerIndex));
	}

}
