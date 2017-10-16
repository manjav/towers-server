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
import com.smartfoxserver.v2.entities.SFSRoomRemoveMode;
import com.smartfoxserver.v2.entities.User;
import com.smartfoxserver.v2.entities.Zone;
import com.smartfoxserver.v2.entities.data.ISFSArray;
import com.smartfoxserver.v2.entities.data.SFSObject;
import com.smartfoxserver.v2.entities.variables.SFSRoomVariable;
import com.smartfoxserver.v2.exceptions.SFSException;
import com.smartfoxserver.v2.exceptions.SFSVariableException;
import com.smartfoxserver.v2.extensions.BaseServerEventHandler;
import com.smartfoxserver.v2.extensions.SFSExtension;
import com.smartfoxserver.v2.persistence.room.FileRoomStorageConfig;
import com.smartfoxserver.v2.persistence.room.RoomStorageMode;
import com.smartfoxserver.v2.persistence.room.SFSStorageException;
import com.smartfoxserver.v2.security.DefaultPermissionProfile;

import static sun.audio.AudioPlayer.player;

public class LobbyRoomServerEventsHandler extends BaseServerEventHandler
{

	private Room room;
	private LobbyRoom roomClass;

	public void handleServerEvent(ISFSEvent arg) throws SFSException
	{
		room = (Room)arg.getParameter(SFSEventParam.ROOM);
		roomClass = (LobbyRoom) room.getExtension();
		User user = (User)arg.getParameter(SFSEventParam.USER);
		//Zone zone = (Zone)arg.getParameter(SFSEventParam.ZONE);

		if( arg.getType().equals(SFSEventType.USER_JOIN_ROOM) )
			joinRoom( user );
		else if( arg.getType().equals(SFSEventType.USER_LEAVE_ROOM) )
			leaveRoom( user );
	}

	private void joinRoom(User user)
	{
		Player player = ((Game) user.getSession().getProperty("core")).player;
		if( !LobbyUtils.getInstance().addUser(room, player.id) )
			return;

		// broadcast join message
		if( room.getVariable("all").getSFSArrayValue().size() > 1 )
			roomClass.sendComment((short) MessageTypes.M10_COMMENT_JOINT, player.nickName, "", (short)-1);// mode = join
	}

	public void leaveRoom(User user)
	{
		Player player = ((Game) user.getSession().getProperty("core")).player;

		// broadcast leave message
		roomClass.sendComment((short) MessageTypes.M11_COMMENT_LEAVE, player.nickName, "", (short)-1);// mode = leave
		LobbyUtils.getInstance().removeUser(room, player.id);
	}
}