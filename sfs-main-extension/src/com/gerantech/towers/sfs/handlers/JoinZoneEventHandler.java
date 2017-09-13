package com.gerantech.towers.sfs.handlers;

import com.gt.towers.Game;
import com.gt.towers.Player;
import com.smartfoxserver.v2.SmartFoxServer;
import com.smartfoxserver.v2.api.CreateRoomSettings;
import com.smartfoxserver.v2.api.ISFSApi;
import com.smartfoxserver.v2.api.SFSApi;
import com.smartfoxserver.v2.buddylist.Buddy;
import com.smartfoxserver.v2.buddylist.BuddyList;
import com.smartfoxserver.v2.buddylist.SFSBuddyVariable;
import com.smartfoxserver.v2.core.ISFSEvent;
import com.smartfoxserver.v2.core.SFSEventParam;
import com.smartfoxserver.v2.core.SFSEventType;
import com.smartfoxserver.v2.entities.Room;
import com.smartfoxserver.v2.entities.User;
import com.smartfoxserver.v2.entities.Zone;
import com.smartfoxserver.v2.entities.data.ISFSArray;
import com.smartfoxserver.v2.entities.data.ISFSObject;
import com.smartfoxserver.v2.entities.variables.RoomVariable;
import com.smartfoxserver.v2.exceptions.SFSBuddyListException;
import com.smartfoxserver.v2.exceptions.SFSCreateRoomException;
import com.smartfoxserver.v2.exceptions.SFSException;
import com.smartfoxserver.v2.exceptions.SFSJoinRoomException;
import com.smartfoxserver.v2.extensions.BaseServerEventHandler;
import com.smartfoxserver.v2.persistence.room.FileRoomStorageConfig;
import com.smartfoxserver.v2.persistence.room.RoomStorageMode;
import com.smartfoxserver.v2.persistence.room.SFSStorageException;

import java.io.IOException;
import java.sql.SQLException;
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
		loadSavedLobbies(getParentExtension().getParentZone(), getApi());

		// Find last joined lobby room
		Room room = rejoinToLastLobbyRoom(user);

		// Init buddy data and link invitees to user
		initBuddy(user, room);
	}

	private Room rejoinToLastLobbyRoom(User user)
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
						return room;
					} catch (SFSJoinRoomException e) {
						e.printStackTrace();
						return null;
					}
				}
			}
		}
		return null;
	}

	// Save room to file for server rstore rooms after resetting
	public static void loadSavedLobbies(Zone zone, ISFSApi api)
	{
		if ( zone.getRoomListFromGroup("lobbies").size() > 0 )
			return;
		System.out.print ("lobbies.count: "+ zone.getRoomListFromGroup("lobbies").size() + "\n");

		FileRoomStorageConfig fileRoomStorageConfig = new FileRoomStorageConfig();
		zone.initRoomPersistence(RoomStorageMode.FILE_STORAGE, fileRoomStorageConfig);
		try {
			List<CreateRoomSettings> lobbies = zone.getRoomPersistenceApi().loadAllRooms("lobbies");
			for ( CreateRoomSettings crs : lobbies )
				if( hasUser(crs.getRoomVariables()) )
					api.createRoom(zone, crs, null);
		}
		catch (SFSStorageException e) {
			e.printStackTrace();
		} catch (SFSCreateRoomException e) {
			e.printStackTrace();
		}
	}

	private static boolean hasUser(List<RoomVariable> vars)
	{
		for (int i = 0; i < vars.size(); i++)
			if( vars.get(i).getName().equals("all") && vars.get(i).getSFSArrayValue().size() > 0)
				return true;
		return false;
	}

	private void initBuddy(User user, Room room) throws SFSBuddyListException {
		try {
			SmartFoxServer.getInstance().getAPIManager().getBuddyApi().initBuddyList(user, true);
			user.setProperty("hasBuddyList", true);
		} catch (IOException e) { e.printStackTrace(); }

		Game game = ((Game)user.getSession().getProperty("core"));
		user.getBuddyProperties().setNickName(game.player.nickName);
		user.getBuddyProperties().setVariable(new SFSBuddyVariable("$point", game.player.get_point()));
		getParentExtension().getBuddyApi().setBuddyVariables(user, user.getBuddyProperties().getVariables(), true, true);

		// add buddy that added user before
		if( room != null )
			user.getBuddyProperties().setVariable(new SFSBuddyVariable("$room", room.getName()));

		BuddyList buddies = getParentExtension().getParentZone().getBuddyListManager().getBuddyList(user.getName());
		try {
			ISFSArray result = getParentExtension().getParentZone().getDBManager().executeQuery("SELECT invitee_id FROM friendship WHERE inviter_id=" + game.player.id + " AND has_reward=" + 0, new Object[]{});
			for (int i=0; i<result.size(); i++) {
				String inviteeName = result.getSFSObject(i).getInt("invitee_id").toString();
				if ( !buddies.containsBuddy(inviteeName) )
					getParentExtension().getBuddyApi().addBuddy(user, inviteeName, false, true, false);
			}
		} catch (SQLException e) {
			e.printStackTrace();
		} catch (SFSBuddyListException e) {
			e.printStackTrace();
		}
	}
}