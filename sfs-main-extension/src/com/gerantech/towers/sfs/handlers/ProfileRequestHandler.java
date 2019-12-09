package com.gerantech.towers.sfs.handlers;

import com.gt.Commands;
import com.gt.utils.DBUtils;
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
		String query = "SELECT type, count, level FROM " + DBUtils.getInstance().liveDB + ".resources WHERE player_id = " + playerId + (params.containsKey("am") ? ";" : " AND (type<13);");
		ISFSArray resources = null;
		boolean isOld = false;
		try {
			resources = dbManager.executeQuery(query, new Object[]{});
			isOld = resources.size() < 1;
			if( isOld )
			{
				query = "SELECT type, count, level FROM resources WHERE player_id = " + playerId + (params.containsKey("am") ? ";" : " AND (type<13);");
				resources = dbManager.executeQuery(query, new Object[]{});
			}
		} catch (SQLException e) { e.printStackTrace(); }
		params.putSFSArray("resources", resources);
		String liveDB = isOld ? "" : (DBUtils.getInstance().liveDB + ".");

		//  -=-=-=-=-=-=-=-=-  add player data  -=-=-=-=-=-=-=-=-
		if( params.containsKey("pd") )
		{
			query = "SELECT * FROM " + liveDB + "players WHERE id=" + playerId + " Limit 1;";
			ISFSArray players = null;
			try {
				players = dbManager.executeQuery(query, new Object[]{});
			} catch (SQLException e) { e.printStackTrace(); }
			params.putSFSObject("pd", players.getSFSObject(0));
		}
		//  -=-=-=-=-=-=-=-=-  add player tag  -=-=-=-=-=-=-=-=-
		params.putText("tag", PasswordGenerator.getInvitationCode(playerId));

		//trace(params.getDump());
		send(Commands.PROFILE, params, sender);
    }
}