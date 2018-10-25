package com.gerantech.towers.sfs.handlers;

import com.gerantech.towers.sfs.Commands;
import com.gerantech.towers.sfs.socials.LobbyUtils;
import com.gerantech.towers.sfs.utils.PasswordGenerator;
import com.gt.data.LobbyData;
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
		LobbyData lobbyData = null;
		if( params.containsKey("lp") )
			lobbyData = LobbyUtils.getInstance().getDataByMember(playerId);
		if( lobbyData != null )
		{
			params.putText("ln", lobbyData.getName());
			params.putInt("lp", lobbyData.getEmblem());
		}

		//  -=-=-=-=-=-=-=-=-  add player tag  -=-=-=-=-=-=-=-=-
		params.putText("tag", PasswordGenerator.getInvitationCode(playerId));

		//trace(params.getDump());
		send(Commands.PROFILE, params, sender);
    }
}