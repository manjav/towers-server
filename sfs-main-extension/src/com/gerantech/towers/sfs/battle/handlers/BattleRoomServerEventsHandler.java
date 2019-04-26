package com.gerantech.towers.sfs.battle.handlers;

import com.gt.Commands;
import com.gerantech.towers.sfs.battle.BattleRoom;
import com.gt.towers.constants.MessageTypes;
import com.gt.towers.exchanges.ExchangeItem;
import com.gt.towers.socials.Challenge;
import com.gt.towers.utils.maps.IntIntMap;
import com.gt.utils.BattleUtils;
import com.gt.data.UnitData;
import com.gt.towers.Game;
import com.gt.towers.battle.BattleField;
import com.gt.utils.ExchangeUtils;
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
import java.util.Iterator;
import java.util.List;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

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
		if( ((int)room.getProperty("friendlyMode")) == 0 )
		{
			int delay = 5000;//Math.max(12000, player.get_arena(0) * 400 + 7000);
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
		for( Room r:joinedRooms )
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
		roomClass.createGame(opponentNotFound);

		List<User> players = room.getPlayersList();
		for (int i=0; i < players.size(); i++)
	    	sendBattleData(players.get(i), false);
	}

	private void sendBattleData(User user, boolean containBuildings)
	{
		if( user.isNpc() )
			return;

		Game game = (Game) user.getSession().getProperty("core");
		if( game.player.isBot() )
			return;

		SFSObject params = new SFSObject();
		IntIntMap cost = Challenge.getRunRequiements((int) room.getProperty("mode"));
		ExchangeItem exItem = Challenge.getExchangeItem((int) room.getProperty("mode"), cost, game.player.get_arena(0));
		int response = ExchangeUtils.getInstance().process(game, exItem, (int) room.getProperty("startAt"),0);
		if( response != MessageTypes.RESPONSE_SUCCEED )
		{
			params.putInt("response", response);
			send(Commands.BATTLE_START, params, user);
			return;
		}

		params.putInt("side", roomClass.getPlayerGroup(user));
		params.putInt("startAt", roomClass.battleField.startAt);
		params.putInt("roomId", room.getId());
		params.putDouble("now", roomClass.battleField.now);
		params.putText("map", roomClass.battleField.field.mapData);
		params.putInt("mode", (int) room.getProperty("mode"));
		params.putInt("friendlyMode", roomClass.battleField.friendlyMode);
		params.putBool("singleMode", (boolean)room.getProperty("singleMode"));
		params.putSFSArray("units", UnitData.toSFSArray(roomClass.battleField.units));
		boolean isSpectator = user.isSpectator(room);
		ArrayList<Game> registeredPlayers = (ArrayList)room.getProperty("registeredPlayers");
		int i = 0;
		for( Game g : registeredPlayers )
		{
			SFSObject p = new SFSObject();
			p.putInt("xp", g.player.get_xp());
			p.putInt("point", g.player.get_point());
			p.putUtfString("name", g.player.nickName);
			if( game.appVersion >= 1700 )
			{
				String deck = "";
				Iterator<Object> iter = roomClass.battleField.decks.get(i)._queue.iterator();
				while( iter.hasNext() )
				{
					int type = (int) iter.next();
					deck += (type + ":" + roomClass.battleField.decks.get(i).get(type).level + (iter.hasNext() ? "," : ""));
				}
				p.putText("deck", deck);
			}
			else
			{
				p.putIntArray("deck", (List<Integer>)(List<?>) roomClass.battleField.decks.get(i)._queue);
			}
			p.putInt("score", roomClass.endCalculator.scores[i]);
			params.putSFSObject( i == 0 ? "p0" : "p1", p );

			i ++;
		}

		send(Commands.BATTLE_START, params, user);

		if( !isSpectator )
		{
			user.getBuddyProperties().setState("Occupied");
			user.getBuddyProperties().setVariable(new SFSBuddyVariable("br", room.getId()));
			user.getBuddyProperties().setVariable(new SFSBuddyVariable("$point", user.getVariable("point").getIntValue()));

			try {
				getParentExtension().getBuddyApi().setBuddyVariables(user, user.getBuddyProperties().getVariables(), true, true);
			} catch (SFSBuddyListException e) { e.printStackTrace(); }
		}
	}
}
