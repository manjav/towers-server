package com.gerantech.towers.sfs.handlers;

import com.gerantech.towers.sfs.socials.handlers.LobbyRoomServerEventsHandler;
import com.gt.towers.Game;
import com.gt.towers.Player;
import com.smartfoxserver.v2.api.CreateRoomSettings;
import com.smartfoxserver.v2.api.ISFSApi;
import com.smartfoxserver.v2.core.ISFSEvent;
import com.smartfoxserver.v2.core.SFSEventParam;
import com.smartfoxserver.v2.entities.Room;
import com.smartfoxserver.v2.entities.User;
import com.smartfoxserver.v2.entities.Zone;
import com.smartfoxserver.v2.entities.data.ISFSArray;
import com.smartfoxserver.v2.entities.data.ISFSObject;
import com.smartfoxserver.v2.exceptions.SFSCreateRoomException;
import com.smartfoxserver.v2.exceptions.SFSException;
import com.smartfoxserver.v2.exceptions.SFSJoinRoomException;
import com.smartfoxserver.v2.extensions.BaseServerEventHandler;
import com.smartfoxserver.v2.persistence.room.FileRoomStorageConfig;
import com.smartfoxserver.v2.persistence.room.RoomStorageMode;
import com.smartfoxserver.v2.persistence.room.SFSStorageException;

import java.util.List;

/**
 * @author ManJav
 */
public class JoinZoneEventHandler extends BaseServerEventHandler
{
	public void handleServerEvent(ISFSEvent event) throws SFSException
	{
		User user = (User) event.getParameter(SFSEventParam.USER);

		// Reload saved rooms
		loadSavedLobbies();

		// Find last joined lobby room
		rejoinToLastLobbyRoom(user);
	}

	private boolean rejoinToLastLobbyRoom(User user)
	{
		Player player = ((Game) user.getSession().getProperty("core")).player;
		int id = Integer.parseInt(user.getName());
		ISFSObject member;
		List<Room> lobbies = getParentExtension().getParentZone().getRoomListFromGroup("lobbies");
		for (Room room : lobbies)
		{
			ISFSArray all = room.getVariable("all").getSFSArrayValue();
			for(int i=0; i<all.size(); i++)
			{
				member = all.getSFSObject(i);
				if( member.getInt("id").equals(player.id) )
				{
					try {
						getApi().joinRoom(user, room);
					} catch (SFSJoinRoomException e) {
						e.printStackTrace();
						return false;
					}
					return true;
				}
			}
		}
		return false;
	}

	// Save room to file for server rstore rooms after resetting
	public void loadSavedLobbies()
	{
		Zone zone = getParentExtension().getParentZone();
		System.out.print ("lobbies.count: "+ zone.getRoomListFromGroup("lobbies").size() + "\n");
		if ( zone.getRoomListFromGroup("lobbies").size() > 0 )
			return;

		FileRoomStorageConfig fileRoomStorageConfig = new FileRoomStorageConfig();
		zone.initRoomPersistence(RoomStorageMode.FILE_STORAGE, fileRoomStorageConfig);
		try {
			List<CreateRoomSettings> lobbies = zone.getRoomPersistenceApi().loadAllRooms("lobbies");
			for ( CreateRoomSettings crs : lobbies )
				getApi().createRoom(zone, crs, null);
		}
		catch (SFSStorageException e) {
			e.printStackTrace();
		} catch (SFSCreateRoomException e) {
			e.printStackTrace();
		}
	}


}