package com.gerantech.towers.sfs.handlers;

import com.gt.Commands;
import com.gt.utils.LobbyUtils;
import com.gerantech.towers.sfs.utils.PasswordGenerator;
import com.gt.data.LobbySFS;
import com.smartfoxserver.v2.db.IDBManager;
import com.smartfoxserver.v2.entities.User;
import com.smartfoxserver.v2.entities.data.ISFSArray;
import com.smartfoxserver.v2.entities.data.ISFSObject;
import com.smartfoxserver.v2.extensions.BaseClientRequestHandler;

import java.sql.SQLException;

/**
 * @author ManJav
 *
 */
public class ProfileRequestHandler extends BaseClientRequestHandler
{
	public void handleClientRequest(User sender, ISFSObject params)
    {
		IDBManager dbManager = getParentExtension().getParentZone().getDBManager();
		int playerId = params.getInt("id");

		//  -=-=-=-=-=-=-=-=-  add resources data  -=-=-=-=-=-=-=-=-
		String query = "SELECT type, count, level FROM resources WHERE player_id = " + playerId + (params.containsKey("am") ? ";" : " AND (type<13);") ;
		try {
			params.putSFSArray("resources", dbManager.executeQuery(query, new Object[]{}));
		} catch (SQLException e) { e.printStackTrace(); }

		//  -=-=-=-=-=-=-=-=-  add player data  -=-=-=-=-=-=-=-=-
		if( params.containsKey("pd") )
		{
			query = "SELECT * FROM players WHERE id=" + playerId + " Limit 1;";
			ISFSArray dataArray = null;
			try {
				dataArray = dbManager.executeQuery(query, new Object[]{});
			} catch (SQLException e) { e.printStackTrace(); }
			params.putSFSObject("pd", dataArray.getSFSObject(0));
		}

		//  -=-=-=-=-=-=-=-=-  add lobby data  -=-=-=-=-=-=-=-=-
		LobbySFS lobbyData = null;
		if( params.containsKey("lp") )
			lobbyData = LobbyUtils.getInstance().getDataByMember(playerId);
		if( lobbyData != null )
		{
			params.putText("ln", lobbyData.getName());
			params.putInt("lp", lobbyData.getEmblem());
		}

		//  -=-=-=-=-=-=-=-=-  add player tag  -=-=-=-=-=-=-=-=-
		params.putText("tag", PasswordGenerator.getInvitationCode(playerId));

		//  -=-=-=-=-=-=-=-=-  add player deck  -=-=-=-=-=-=-=-=-
		try {
			params.putSFSArray("decks", dbManager.executeQuery("SELECT decks.`type`, resources.`level` FROM decks INNER JOIN resources ON decks.player_id = resources.player_id AND decks.`type` = resources.`type` WHERE decks.player_id = "+ playerId +" AND decks.deck_index = 0", new Object[]{}));
		} catch (SQLException e) { trace(e.getMessage()); }




		//trace(params.getDump());
		send(Commands.PROFILE, params, sender);
    }
}