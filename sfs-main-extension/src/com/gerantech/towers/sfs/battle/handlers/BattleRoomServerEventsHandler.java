package com.gerantech.towers.sfs.battle.handlers;

import com.gerantech.towers.sfs.Commands;
import com.gerantech.towers.sfs.battle.BattleRoom;
import com.gerantech.towers.sfs.battle.BattleUtils;
import com.gt.data.SFSDataModel;
import com.gt.data.UnitData;
import com.gt.towers.Game;
import com.gt.towers.battle.BattleField;
import com.smartfoxserver.v2.SmartFoxServer;
import com.smartfoxserver.v2.buddylist.SFSBuddyVariable;
import com.smartfoxserver.v2.core.ISFSEvent;
import com.smartfoxserver.v2.core.SFSEventParam;
import com.smartfoxserver.v2.core.SFSEventType;
import com.smartfoxserver.v2.entities.Room;
import com.smartfoxserver.v2.entities.User;
import com.smartfoxserver.v2.entities.data.SFSObject;
import com.smartfoxserver.v2.exceptions.SFSBuddyListException;
import com.smartfoxserver.v2.extensions.BaseServerEventHandler;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class BattleRoomServerEventsHandler extends BaseServerEventHandler
{
	private User user;
	private Room room;
	private BattleRoom roomClass;

	public void handleServerEvent(ISFSEvent arg)
	{
		user = (User) arg.getParameter(SFSEventParam.USER);
		if( arg.getType().equals(SFSEventType.USER_DISCONNECT) )
			userDisconnected(arg);
		else if( arg.getType().equals(SFSEventType.USER_JOIN_ROOM) )
			userJoined(arg);
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

		// Rejoin to previous room
		if( (Integer)room.getProperty("state") == BattleField.STATE_2_STARTED )
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
					send("battleRejoin", sfsO, players.get(i));
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
			int delay = 3000;//Math.max(12000, player.get_arena(0) * 400 + 7000);
			//trace(room.getName(), waitingPeak, room.getPlayersList().size(), room.getOwner().getName());

			roomClass.autoJoinTimer = SmartFoxServer.getInstance().getTaskScheduler().schedule(new TimerTask() {
				@Override
				public void run() {
				cancel();
				roomClass.autoJoinTimer.cancel(true);
				room.setMaxUsers(1);
				sendStartBattleResponse(true);
				}
			}, delay, TimeUnit.MILLISECONDS);
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
			if( state < BattleField.STATE_1_CREATED )
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
						send("battleLeft", sfsO, u);
					}
				}*/
			}
			r.removeProperty("enabled");
		}
	}

	private void sendStartBattleResponse(Boolean opponentNotFound)
	{
		room.setProperty("startAt", (int) Instant.now().getEpochSecond());
		roomClass.createGame((int) room.getProperty("index"), opponentNotFound);

		List<User> players = room.getPlayersList();
		for (int i=0; i < players.size(); i++)
	    	sendBattleData(players.get(i), false);
	}

	private void sendBattleData(User player, boolean containBuildings)
	{
		if( player.isNpc() )
			return;

		Game game = (Game) player.getSession().getProperty("core");
		SFSObject sfsO = new SFSObject();
		sfsO.putInt("side", roomClass.getPlayerGroup(player) );
		sfsO.putInt("index", (int) room.getProperty("index"));
		sfsO.putInt("startAt", (int)room.getProperty("startAt"));
		sfsO.putInt("roomId", room.getId());
		sfsO.putText("map", roomClass.battleField.field.mapLayout);
		sfsO.putText("type", (String) room.getProperty("type"));
		sfsO.putBool("isFriendly", room.containsProperty("isFriendly"));
		sfsO.putBool("hasExtraTime", room.containsProperty("hasExtraTime"));
		sfsO.putBool("singleMode", (boolean)room.getProperty("singleMode"));
		sfsO.putSFSArray("units", UnitData.toSFSArray(roomClass.battleField.units));

		boolean isSpectator = player.isSpectator(room);
		ArrayList<Game> registeredPlayers = (ArrayList)room.getProperty("registeredPlayers");
		int i = 0;
		for ( Game g : registeredPlayers )
		{
			SFSObject p = new SFSObject();
			p.putUtfString("name", g.player.nickName);
			p.putInt("xp", g.player.get_xp());
			p.putInt("point", g.player.get_point());
			p.putIntArray("deck", Arrays.stream(roomClass.battleField.decks.get(i).keys()).boxed().collect(Collectors.toList()));
			p.putInt("score", roomClass.endCalculator.scores[i]);

			if( isSpectator )
				sfsO.putSFSObject( i == 0 ? "allis" : "axis", p );
			else
				sfsO.putSFSObject( g.player.id == game.player.id ? "allis" : "axis", p );

			i ++;
		}

		send(Commands.BATTLE_START, sfsO, player);

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
