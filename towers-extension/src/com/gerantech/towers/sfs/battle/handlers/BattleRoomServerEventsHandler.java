package com.gerantech.towers.sfs.battle.handlers;

import com.gerantech.towers.sfs.battle.BattleRoom;
import com.gt.utils.BattleUtils;
import com.gt.towers.Game;
import com.gt.towers.Player;
import com.gt.towers.buildings.Building;
import com.smartfoxserver.v2.SmartFoxServer;
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
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

public class BattleRoomServerEventsHandler extends BaseServerEventHandler
{
	private User user;
	private Room room;
	private BattleRoom roomClass;

	public void handleServerEvent(ISFSEvent arg) throws SFSException
	{
try {
		user = (User) arg.getParameter(SFSEventParam.USER);
		if (arg.getType().equals(SFSEventType.USER_DISCONNECT))
			userDisconnected(arg);
		else if (arg.getType().equals(SFSEventType.USER_JOIN_ROOM))
			userJoined(arg);
} catch (Exception | Error e) { e.printStackTrace(); }
	}

	private void userJoined(ISFSEvent arg)
	{
		room = (Room) arg.getParameter(SFSEventParam.ROOM);
		roomClass = ((BattleRoom) room.getExtension());

		if( user.isSpectator(room) )
		{
			sendBattleData(user, true);
			return;
		}

		Player player = ((Game) user.getSession().getProperty("core")).player;
		if( !user.isNpc() )
			player.inFriendlyBattle = room.containsProperty("isFriendly");

		// Rejoin to previous room
		if( (Integer)room.getProperty("state") == BattleRoom.STATE_BATTLE_STARTED )
		{
			List<User> players = room.getPlayersList();
			for (int i=0; i < players.size(); i++)
			{
				if( players.get(i).equals(user) )
				{
					sendBattleData(players.get(i), true);
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
		if( room.isFull() )
		{
			sendStartBattleResponse(false);
			return;
		}

		// Wait to match making ( complete battle-room`s players )
		if( !room.containsProperty("isFriendly") )
		{
			int delay = Math.max(12000, player.get_arena(0) * 400 + 7000);
			//trace(room.getName(), waitingPeak, room.getPlayersList().size(), room.getOwner().getName());

			roomClass.autoJoinTimer = SmartFoxServer.getInstance().getTaskScheduler().schedule(new TimerTask() {
				@Override
				public void run() {
try {
					cancel();
					roomClass.autoJoinTimer.cancel(true);
					room.setMaxUsers(1);
					sendStartBattleResponse(true);
} catch (Exception | Error e) { e.printStackTrace(); }
				}
			}, delay, TimeUnit.MILLISECONDS);
		}
	}

	private void userDisconnected(ISFSEvent arg)
	{
		List<?> joinedRooms = (List<?>) arg.getParameter(SFSEventParam.JOINED_ROOMS);
		for(Object o:joinedRooms)
		{
			Room r = (Room) o;
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
		room.setProperty("startAt", (int) Instant.now().getEpochSecond());
		roomClass.createGame(getMapName((boolean) room.getProperty("isOperation")), opponentNotFound);

		List<User> players = room.getPlayersList();
		for (int i=0; i < players.size(); i++)
	    	sendBattleData(players.get(i), false);
	}

	private String getMapName(boolean isOperation)
	{
		int index = (Integer)room.getProperty("index");
		String mapName = "battle_" + index;
		if( isOperation )
			mapName = "operation_" + index;
		return mapName;
	}

	private void sendBattleData(User player, boolean containBuildings)
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
		sfsO.putText("mapName", getMapName((boolean)room.getProperty("isOperation")));

		boolean isSpectator = player.isSpectator(room);
		ArrayList<?> registeredPlayers = (ArrayList<?>) room.getProperty("registeredPlayers");
		int i = 0;
		for (Object o : registeredPlayers)
		{
			Player pl = ((Game) o).player;
			SFSObject p = new SFSObject();
			p.putUtfString("name", pl.nickName);
			p.putInt("point", pl.get_point());
			if( isSpectator )
				sfsO.putSFSObject( i == 0 ? "allis" : "axis", p );
			else
				sfsO.putSFSObject( pl.id == Integer.parseInt(player.getName()) ? "allis" : "axis", p );

			i ++;
		}

		// send buildings data
		if( containBuildings )
		{
			SFSArray buildingData = new SFSArray();
			for (int j = 0; j < roomClass.battleField.places.size(); j++)
			{
				Building b = roomClass.battleField.places.get(j).building;
				SFSObject bo = new SFSObject();
				bo.putInt("i", j);
				bo.putInt("t", b.type);
				bo.putInt("tt", b.troopType);
				bo.putInt("l", b.get_level());
				bo.putInt("p", b.get_population());
				buildingData.addSFSObject(bo);
			}
			sfsO.putSFSArray("buildings", buildingData);
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
