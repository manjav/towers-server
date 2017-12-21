package com.gerantech.towers.sfs.handlers;

import com.smartfoxserver.v2.api.CreateRoomSettings;
import com.smartfoxserver.v2.entities.Room;
import com.smartfoxserver.v2.entities.User;
import com.smartfoxserver.v2.entities.Zone;
import com.smartfoxserver.v2.entities.data.ISFSObject;
import com.smartfoxserver.v2.exceptions.SFSCreateRoomException;
import com.smartfoxserver.v2.extensions.BaseClientRequestHandler;
import com.smartfoxserver.v2.persistence.room.SFSStorageException;

import java.util.List;

/**
 * @author ManJav
 *
 */
public class ResetLobbiesHandler extends BaseClientRequestHandler
{
	public void handleClientRequest(User sender, ISFSObject params)
    {
		Zone zone = getParentExtension().getParentZone();
		/*List<Room> lobbies = zone.getRoomListFromGroup("lobbies");
		for ( Room lobby:lobbies )
			getApi().removeRoom(lobby, false, false);
		JoinZoneEventHandler.loadSavedLobbies(zone, getApi());*/

		Room room = zone.getRoomById(params.getInt("id"));
		List<CreateRoomSettings> lobbies = null;
		try {
			lobbies = zone.getRoomPersistenceApi().loadAllRooms("lobbies");
		} catch (SFSStorageException e) {
			e.printStackTrace();
		}
		for ( CreateRoomSettings crs : lobbies ){
			if( crs.getName().equals(room.getName()) ) {
				getApi().removeRoom(room);
				try {
					getApi().createRoom(zone, crs, null);
				} catch (SFSCreateRoomException e) {
					e.printStackTrace();
				}
				break;
			}
		}

    }
}