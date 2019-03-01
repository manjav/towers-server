package com.gerantech.towers.sfs.handlers;

import com.gt.utils.LobbyUtils;
import com.gt.towers.Game;
import com.gt.towers.Player;
import com.smartfoxserver.v2.SmartFoxServer;
import com.smartfoxserver.v2.buddylist.BuddyList;
import com.smartfoxserver.v2.buddylist.SFSBuddyVariable;
import com.smartfoxserver.v2.core.ISFSEvent;
import com.smartfoxserver.v2.core.SFSEventParam;
import com.smartfoxserver.v2.entities.Room;
import com.smartfoxserver.v2.entities.User;
import com.smartfoxserver.v2.entities.data.ISFSArray;
import com.smartfoxserver.v2.exceptions.SFSBuddyListException;
import com.smartfoxserver.v2.exceptions.SFSException;
import com.smartfoxserver.v2.exceptions.SFSJoinRoomException;
import com.smartfoxserver.v2.extensions.BaseServerEventHandler;

import java.io.IOException;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;

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

		// Update player data
		String query = "UPDATE `players` SET `app_version`='" + game.appVersion + "', `sessions_count`='" + (game.sessionsCount+1) + "', `last_login`='" + Timestamp.from(Instant.now()) + "' WHERE `id`=" + game.player.id + ";";
		try {
			getParentExtension().getParentZone().getDBManager().executeUpdate(query, new Object[] {});
		} catch (SQLException e) { e.printStackTrace(); }

		// Find last joined lobby room
		Room room = rejoinToLastLobbyRoom(user, game.player);

		// Init buddy data and link invitees to user
		initBuddy(user, room);
	}

	private Room rejoinToLastLobbyRoom(User user, Player player)
	{
		Room lobby = LobbyUtils.getInstance().getLobby(player.id);
		if( lobby == null )
			return null;
		try {
			getApi().joinRoom(user, lobby);
		} catch (SFSJoinRoomException e) { e.printStackTrace(); }
		return lobby;
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

		if( result == null )
			return;

		for (int i=0; i<result.size(); i++)
		{
			String inviteeName = result.getSFSObject(i).getInt("invitee_id").toString();
			if ( !buddies.containsBuddy(inviteeName) )
				getParentExtension().getBuddyApi().addBuddy(user, inviteeName, false, true, false);
		}
	}
}