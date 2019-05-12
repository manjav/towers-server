package com.gerantech.towers.sfs.socials.handlers;

import com.gerantech.towers.sfs.socials.LobbyRoom;
import com.gt.utils.LobbyUtils;
import com.gt.towers.Game;
import com.gt.towers.Player;
import com.gt.towers.constants.MessageTypes;
import com.smartfoxserver.v2.core.ISFSEvent;
import com.smartfoxserver.v2.core.SFSEventParam;
import com.smartfoxserver.v2.core.SFSEventType;
import com.smartfoxserver.v2.entities.Room;
import com.smartfoxserver.v2.entities.User;
import com.smartfoxserver.v2.extensions.BaseServerEventHandler;

import java.util.LinkedList;

public class LobbyRoomServerEventsHandler extends BaseServerEventHandler
{
	public void handleServerEvent(ISFSEvent arg)
	{
		if( arg.getType().equals(SFSEventType.USER_DISCONNECT) )
		{
			LinkedList<Room> joinedRooms = (LinkedList<Room>) arg.getParameter(SFSEventParam.JOINED_ROOMS);
			for ( Room r : joinedRooms )
			{
				if( r.getGroupId() == "lobbies" )
					LobbyUtils.getInstance().removeEmptyRoom(r);

			}
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
				lobbyClass.sendComment( MessageTypes.M10_COMMENT_JOINT, player, "", -1);
		}
		else if( arg.getType().equals(SFSEventType.USER_LEAVE_ROOM) )// mode = leave
		{
			// broadcast leave message
			lobbyClass.sendComment( MessageTypes.M11_COMMENT_LEAVE, player, "", -1);
			LobbyUtils.getInstance().removeUser(lobbyClass.getData(), player.id);
			LobbyUtils.getInstance().removeEmptyRoom(lobby);

		}
	}
}