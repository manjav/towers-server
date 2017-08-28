package com.gerantech.towers.sfs.socials.handlers;

import com.gerantech.towers.sfs.socials.LobbyRoom;
import com.gt.towers.Game;
import com.gt.towers.Player;
import com.smartfoxserver.v2.core.ISFSEvent;
import com.smartfoxserver.v2.core.SFSEventParam;
import com.smartfoxserver.v2.core.SFSEventType;
import com.smartfoxserver.v2.entities.Room;
import com.smartfoxserver.v2.entities.User;
import com.smartfoxserver.v2.entities.Zone;
import com.smartfoxserver.v2.entities.data.ISFSArray;
import com.smartfoxserver.v2.entities.data.SFSObject;
import com.smartfoxserver.v2.exceptions.SFSException;
import com.smartfoxserver.v2.extensions.BaseServerEventHandler;
import com.smartfoxserver.v2.persistence.room.FileRoomStorageConfig;
import com.smartfoxserver.v2.persistence.room.RoomStorageMode;
import com.smartfoxserver.v2.persistence.room.SFSStorageException;
import com.smartfoxserver.v2.security.DefaultPermissionProfile;

public class LobbyRoomServerEventsHandler extends BaseServerEventHandler
{

	private Room room;
	private LobbyRoom roomClass;

	public void handleServerEvent(ISFSEvent arg) throws SFSException
	{
		trace("LOBBY_ROOM_HANDLE_SERVEREVENT______", arg);
		room = (Room)arg.getParameter(SFSEventParam.ROOM);
		roomClass = (LobbyRoom) room.getExtension();
		User user = (User)arg.getParameter(SFSEventParam.USER);
		//Zone zone = (Zone)arg.getParameter(SFSEventParam.ZONE);

		if( arg.getType().equals(SFSEventType.USER_JOIN_ROOM) )
		{
			joinRoom(user);
		}
		else if( arg.getType().equals(SFSEventType.USER_LEAVE_ROOM) )
		{
			leaveRoom(user);
		}
	}

	private void joinRoom(User user)
	{
		Player player = ((Game) user.getSession().getProperty("core")).player;
		ISFSArray all = room.getVariable("all").getSFSArrayValue();
		int allSize = all.size();
		for(int i=0; i<allSize; i++)
            if ( all.getSFSObject(i).getInt("id").equals(player.id) )
				return;

		SFSObject member = new SFSObject();
		member.putInt("id", player.id);
		member.putShort("pr", (allSize==0 ? DefaultPermissionProfile.ADMINISTRATOR : DefaultPermissionProfile.STANDARD).getId());
		all.addSFSObject(member);

		save(getParentExtension().getParentZone(), room);
	}

	private void leaveRoom(User user)
	{
        int id = Integer.parseInt(user.getName());
		int memberIndex = -1;
		ISFSArray all = room.getVariable("all").getSFSArrayValue();
		int allSize = all.size();
		for(int i=0; i<allSize; i++)
		{
			if (all.getSFSObject(i).getInt("id").equals(id))
			{
				memberIndex = i;
				break;
			}
		}

		if( memberIndex < 0 )
			return;

		all.removeElementAt(memberIndex);
		save(getParentExtension().getParentZone(), room);
	}

	// Save room to file for server rstore rooms after resetting
	public static void save(Zone zone, Room room)
	{
		FileRoomStorageConfig fileRoomStorageConfig = new FileRoomStorageConfig();
		zone.initRoomPersistence(RoomStorageMode.FILE_STORAGE, fileRoomStorageConfig);
		try {
			zone.getRoomPersistenceApi().saveRoom(room);
		} catch (SFSStorageException e) {
			e.printStackTrace();
		}
	}
}