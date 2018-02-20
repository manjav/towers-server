package com.gerantech.towers.sfs.handlers;

import com.gerantech.towers.sfs.socials.LobbyUtils;
import com.gt.towers.Game;
import com.gt.towers.Player;
import com.smartfoxserver.v2.SmartFoxServer;
import com.smartfoxserver.v2.api.CreateRoomSettings;
import com.smartfoxserver.v2.api.ISFSApi;
import com.smartfoxserver.v2.buddylist.BuddyList;
import com.smartfoxserver.v2.buddylist.SFSBuddyVariable;
import com.smartfoxserver.v2.core.ISFSEvent;
import com.smartfoxserver.v2.core.SFSEventParam;
import com.smartfoxserver.v2.entities.Room;
import com.smartfoxserver.v2.entities.User;
import com.smartfoxserver.v2.entities.Zone;
import com.smartfoxserver.v2.entities.data.ISFSArray;
import com.smartfoxserver.v2.entities.variables.RoomVariable;
import com.smartfoxserver.v2.exceptions.SFSBuddyListException;
import com.smartfoxserver.v2.exceptions.SFSCreateRoomException;
import com.smartfoxserver.v2.exceptions.SFSException;
import com.smartfoxserver.v2.exceptions.SFSJoinRoomException;
import com.smartfoxserver.v2.extensions.BaseServerEventHandler;
import com.smartfoxserver.v2.persistence.room.DBRoomStorageConfig;
import com.smartfoxserver.v2.persistence.room.RoomStorageMode;
import com.smartfoxserver.v2.persistence.room.SFSStorageException;

import java.io.IOException;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;

/**
 * @author ManJav
 */
public class JoinZoneEventHandler extends BaseServerEventHandler
{
	public void handleServerEvent(ISFSEvent event) throws SFSException
	{
		User user = (User) event.getParameter(SFSEventParam.USER);
		Game game = ((Game) user.getSession().getProperty("core"));
		if( game == null )
			return;
		Zone zone = getParentExtension().getParentZone();

		// Update player data
		String query = "UPDATE `players` SET `app_version`='" + game.appVersion + "', `sessions_count`='" + (game.sessionsCount+1) + "', `last_login`='" + Timestamp.from(Instant.now()) + "' WHERE `id`=" + game.player.id + ";";
		try {
			getParentExtension().getParentZone().getDBManager().executeUpdate(query, new Object[] {});
		} catch (SQLException e) { e.printStackTrace(); }

		// Reload saved rooms
		loadSavedLobbies(zone, getApi());

		// Find last joined lobby room
		Room room = rejoinToLastLobbyRoom(zone, user, game.player);

		// Init buddy data and link invitees to user
		initBuddy(user, room);

		if( LoginEventHandler.STARTING_STATE == 1 )
			LoginEventHandler.STARTING_STATE = 2;
	}

	/**
	 * Save room to file for server restore rooms after resetting
	 */
	private void loadSavedLobbies(Zone zone, ISFSApi api)
	{
		if ( zone.getRoomListFromGroup("lobbies").size() > 0 )
			return;

        DBRoomStorageConfig dbRoomStorageConfig = new DBRoomStorageConfig();
		dbRoomStorageConfig.storeInactiveRooms = true;
		dbRoomStorageConfig.tableName = "rooms";
		zone.initRoomPersistence(RoomStorageMode.DB_STORAGE, dbRoomStorageConfig);

		List<CreateRoomSettings> lobbies = null;
		try {
			lobbies = zone.getRoomPersistenceApi().loadAllRooms("lobbies");
		} catch (SFSStorageException e) { e.printStackTrace(); }

		for ( CreateRoomSettings crs : lobbies )
		{
			try {
				if( hasUser(crs.getRoomVariables()) )
					api.createRoom(zone, crs, null);
			} catch (SFSCreateRoomException e) { e.printStackTrace(); }

		}
	}

	private Room rejoinToLastLobbyRoom(Zone zone, User user, Player player)
	{
		Room room = LobbyUtils.getLobbyOfOfflineUser(zone, player.id);
		if( room != null ) {
			try {
				getApi().joinRoom(user, room);
				return room;
			} catch (SFSJoinRoomException e) { e.printStackTrace(); return null; }
		}
		return null;
	}

	private static boolean hasUser(List<RoomVariable> vars)
	{
		for (int i = 0; i < vars.size(); i++)
			if( vars.get(i).getName().equals("all") && vars.get(i).getSFSArrayValue().size() > 0 )
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
		ISFSArray result = null;
		try {
			result = getParentExtension().getParentZone().getDBManager().executeQuery("SELECT invitee_id FROM friendship WHERE inviter_id=" + game.player.id + " AND has_reward=" + 0, new Object[]{});
		} catch (SQLException e) { e.printStackTrace(); }

		for (int i=0; i<result.size(); i++)
		{
			String inviteeName = result.getSFSObject(i).getInt("invitee_id").toString();
			if ( !buddies.containsBuddy(inviteeName) )
				getParentExtension().getBuddyApi().addBuddy(user, inviteeName, false, true, false);
		}
	}
}