package com.gerantech.towers.sfs.socials.handlers;

import com.gerantech.towers.sfs.socials.LobbyRoom;
import com.gt.towers.Game;
import com.gt.towers.Player;
import com.gt.towers.constants.MessageTypes;
import com.gt.utils.LobbyUtils;
import com.smartfoxserver.v2.SmartFoxServer;
import com.smartfoxserver.v2.core.ISFSEvent;
import com.smartfoxserver.v2.core.SFSEventParam;
import com.smartfoxserver.v2.core.SFSEventType;
import com.smartfoxserver.v2.entities.Room;
import com.smartfoxserver.v2.entities.User;
import com.smartfoxserver.v2.extensions.BaseServerEventHandler;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

public class LobbyRoomServerEventsHandler extends BaseServerEventHandler
{
	public void handleServerEvent(ISFSEvent arg)
	{
		if( arg.getType().equals(SFSEventType.USER_DISCONNECT) )
		{
			List<Room> empties = new ArrayList<>();
			LinkedList<?> joinedRooms = (LinkedList<?>) arg.getParameter(SFSEventParam.JOINED_ROOMS);
			for( Object o : joinedRooms )
			{
				Room r = (Room)o;
				if( r.getGroupId() == "lobbies" )
					empties.add(r);
			}
			SmartFoxServer.getInstance().getTaskScheduler().schedule(new TimerTask() {
				@Override
				public void run() {
					cancel();
					for ( Room l : empties )
						LobbyUtils.getInstance().removeEmptyRoom(l);
				}
			}, 10, TimeUnit.MILLISECONDS);
			return;
		}

		Room lobby = (Room) arg.getParameter(SFSEventParam.ROOM);
		LobbyRoom lobbyClass = (LobbyRoom) lobby.getExtension();
		User user = (User)arg.getParameter(SFSEventParam.USER);
		Player player = ((Game) user.getSession().getProperty("core")).player;

		if( arg.getType().equals(SFSEventType.USER_JOIN_ROOM) )// mode = join
		{
			if( !LobbyUtils.getInstance().addUser(lobbyClass.getData(), player.id) )
				return;

			// broadcast join message
			if( lobbyClass.getData().getMembers().size() > 1 )
				lobbyClass.sendComment((short) MessageTypes.M10_COMMENT_JOINT, player, "", (short)-1);
		}
		else if( arg.getType().equals(SFSEventType.USER_LEAVE_ROOM) )// mode = leave
		{
			// broadcast leave message
			lobbyClass.sendComment((short) MessageTypes.M11_COMMENT_LEAVE, player, "", (short)-1);
			LobbyUtils.getInstance().removeUser(lobbyClass.getData(), player.id);
			LobbyUtils.getInstance().removeEmptyRoom(lobby);

		}
	}
}