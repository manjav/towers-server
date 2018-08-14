package com.gerantech.towers.sfs.socials.handlers;

import com.gerantech.towers.sfs.socials.LobbyRoom;
import com.gerantech.towers.sfs.socials.LobbyUtils;
import com.gt.towers.Game;
import com.gt.towers.Player;
import com.gt.towers.constants.MessageTypes;
import com.smartfoxserver.v2.core.ISFSEvent;
import com.smartfoxserver.v2.core.SFSEventParam;
import com.smartfoxserver.v2.core.SFSEventType;
import com.smartfoxserver.v2.entities.Room;
import com.smartfoxserver.v2.entities.User;
import com.smartfoxserver.v2.exceptions.SFSException;
import com.smartfoxserver.v2.extensions.BaseServerEventHandler;

public class LobbyRoomServerEventsHandler extends BaseServerEventHandler
{
	public void handleServerEvent(ISFSEvent arg) throws SFSException
	{
		Room lobby = (Room) arg.getParameter(SFSEventParam.ROOM);
		LobbyRoom lobbyClass = (LobbyRoom) lobby.getExtension();
		User user = (User)arg.getParameter(SFSEventParam.USER);
		Player player = ((Game) user.getSession().getProperty("core")).player;

		if( arg.getType().equals(SFSEventType.USER_JOIN_ROOM) )
		{
			if( !LobbyUtils.getInstance().addUser(lobbyClass.getData(), player.id) )
				return;

			// broadcast join message
			if( lobby.getVariable("all").getSFSArrayValue().size() > 1 )
				lobbyClass.sendComment((short) MessageTypes.M10_COMMENT_JOINT, player.nickName, "", (short)-1);
			// mode = join
		}
		else if( arg.getType().equals(SFSEventType.USER_LEAVE_ROOM) )
		{
			// broadcast leave message
			lobbyClass.sendComment((short) MessageTypes.M11_COMMENT_LEAVE, player.nickName, "", (short)-1);
			// mode = leave
			LobbyUtils.getInstance().removeUser(lobbyClass.getData(), player.id);
		}
	}
}