package com.gerantech.towers.sfs.battle.handlers;

import com.gerantech.towers.sfs.battle.BattleRoom;
import com.gerantech.towers.sfs.utils.BattleUtils;
import com.gerantech.towers.sfs.utils.RankingUtils;
import com.gt.hazel.RankData;
import com.gt.towers.Game;
import com.hazelcast.config.Config;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.IMap;
import com.hazelcast.util.RandomPicker;
import com.smartfoxserver.v2.buddylist.SFSBuddyVariable;
import com.smartfoxserver.v2.core.ISFSEvent;
import com.smartfoxserver.v2.core.SFSEventParam;
import com.smartfoxserver.v2.core.SFSEventType;
import com.smartfoxserver.v2.entities.Room;
import com.smartfoxserver.v2.entities.User;
import com.smartfoxserver.v2.entities.data.SFSArray;
import com.smartfoxserver.v2.entities.data.SFSObject;
import com.smartfoxserver.v2.exceptions.SFSBuddyListException;
import com.smartfoxserver.v2.exceptions.SFSException;
import com.smartfoxserver.v2.extensions.BaseServerEventHandler;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class BattleRoomServerEventsHandler extends BaseServerEventHandler
{
	private User user;
	private Room room;
	private BattleRoom roomClass;

	public void handleServerEvent(ISFSEvent arg) throws SFSException {
		user = (User) arg.getParameter(SFSEventParam.USER);
		if (arg.getType().equals(SFSEventType.USER_DISCONNECT))
			userDisconnected(arg);
		else if (arg.getType().equals(SFSEventType.USER_JOIN_ROOM))
			userJoined(arg);
	}

	private void userJoined(ISFSEvent arg)
	{
		room = (Room) arg.getParameter(SFSEventParam.ROOM);
		roomClass = ((BattleRoom) room.getExtension());

		if( user.isSpectator(room) )
		{
			sendBattleData( user );
			return;
		}

		if( !user.isNpc() )
			((Game) user.getSession().getProperty("core")).player.inFriendlyBattle = room.containsProperty("isFriendly");

		// Rejoin to previous room
		if( (Integer)room.getProperty("state") == BattleRoom.STATE_BATTLE_STARTED )
		{
			List<User> players = room.getPlayersList();
			for (int i=0; i < players.size(); i++)
			{
				if(players.get(i).equals(user))
				{
					sendBattleData( players.get(i) );
				}
				/*else if( !players.get(i).isNpc() )
				{
					SFSObject sfsO = new SFSObject();
					sfsO.putText("user", ((Game) user.getSession().getProperty("core")).player.nickName);
					send("rejoinBattle", sfsO, players.get(i));
				}*/
			}
			return;
		}

		// Wait to match making ( complete battle-room`s players )
		if( !room.isFull() )
		{
			int waitingPeak = room.containsProperty("isFriendly") ? 10000000 :  RandomPicker.getInt(4000, 8000 );
			//trace(room.getName(), waitingPeak, room.getPlayersList().size(), room.getOwner().getName());

			roomClass.autoJoinTimer = new Timer();
			roomClass.autoJoinTimer.schedule(new TimerTask() {

				@Override
				public void run() {
					cancel();
					roomClass.autoJoinTimer.cancel();
					room.setMaxUsers(1);
					sendStartBattleResponse(true);
				}
			}, waitingPeak);
		}
		else
		{
			sendStartBattleResponse(false);
		}
	}

	private void userDisconnected(ISFSEvent arg)
	{
		List<Room> joinedRooms = (List<Room>) arg.getParameter(SFSEventParam.JOINED_ROOMS);
		for(Room r:joinedRooms)
		{
			if( r.getGroupId() != "battles" || r.containsProperty("enabled"))
				continue;
			r.setProperty("enabled", false);

			int state = (Integer)r.getProperty("state");
			if( state < BattleRoom.STATE_CREATED )
			{
				BattleUtils.getInstance().removeRoom(r);
			}
			else
			{
				/*for(User u:r.getPlayersList())
				{
					if(!u.isNpc() && !u.equals(user))
					{
						SFSObject sfsO = SFSObject.newInstance();
						sfsO.putText("user", ((Game) user.getSession().getProperty("core")).player.nickName);
						send("leftBattle", sfsO, u);
					}
				}*/
			}
			r.removeProperty("enabled");
		}
	}

	private void sendStartBattleResponse(Boolean opponentNotFound)
	{
		boolean isQuest = (boolean) room.getProperty("isQuest");
		room.setProperty("startAt", (int) Instant.now().getEpochSecond());
		roomClass.createGame(getMapName(isQuest), opponentNotFound);

		List<User> players = room.getPlayersList();
		for (int i=0; i < players.size(); i++)
	    	sendBattleData(players.get(i));
	}

	private String getMapName(boolean isQuest)
	{
		int index = (Integer)room.getProperty("index");
		String mapName = "battle_" + index;
		if(isQuest)
			mapName = "quest_" + index;
		return mapName;
	}

	private void sendBattleData(User player)
	{
		if( player.isNpc() )
			return;


		SFSObject sfsO = new SFSObject();
		sfsO.putInt("troopType", roomClass.getPlayerGroup(player) );
		sfsO.putInt("startAt", (Integer)room.getProperty("startAt"));
		sfsO.putInt("roomId", room.getId());
		sfsO.putBool("isFriendly", room.containsProperty("isFriendly"));
		sfsO.putBool("hasExtraTime", room.containsProperty("hasExtraTime"));
		sfsO.putBool("singleMode", (boolean)room.getProperty("singleMode"));
		sfsO.putText("mapName", getMapName((boolean)room.getProperty("isQuest")));
		sfsO.putSFSArray("decks", (SFSArray)room.getProperty("decks"));

		boolean isSpectator = player.isSpectator(room);
		ArrayList<Game> registeredPlayers = (ArrayList)room.getProperty("registeredPlayers");
		int i = 0;
		for ( Game g : registeredPlayers )
		{
			SFSObject p = new SFSObject();
			p.putUtfString("name", g.player.nickName);
			p.putInt("point", g.player.get_point());
			if( isSpectator )
				sfsO.putSFSObject( i == 0 ? "allis" : "axis", p );
			else
				sfsO.putSFSObject( g.player.id == Integer.parseInt(player.getName()) ? "allis" : "axis", p );

			i ++;
		}

		send("startBattle", sfsO, player);

		if( !isSpectator )
		{
			player.getBuddyProperties().setState("Occupied");
			player.getBuddyProperties().setVariable(new SFSBuddyVariable("br", room.getId()));
			player.getBuddyProperties().setVariable(new SFSBuddyVariable("$point", player.getVariable("point").getIntValue()));

			try {
				getParentExtension().getBuddyApi().setBuddyVariables(player, player.getBuddyProperties().getVariables(), true, true);
			} catch (SFSBuddyListException e) { e.printStackTrace(); }
		}
	}
}
