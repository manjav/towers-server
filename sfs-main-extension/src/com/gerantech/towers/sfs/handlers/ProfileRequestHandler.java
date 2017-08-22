package com.gerantech.towers.sfs.handlers;

import com.smartfoxserver.v2.db.IDBManager;
import com.smartfoxserver.v2.entities.User;
import com.smartfoxserver.v2.entities.data.ISFSArray;
import com.smartfoxserver.v2.entities.data.ISFSObject;
import com.smartfoxserver.v2.entities.data.SFSObject;
import com.smartfoxserver.v2.extensions.BaseClientRequestHandler;

import java.sql.SQLException;

/**
 * @author ManJav
 *
 */
public class ProfileRequestHandler extends BaseClientRequestHandler
{
	public ProfileRequestHandler() {}
	public void handleClientRequest(User sender, ISFSObject params)
    {
		int playerId = params.getInt("id");

		IDBManager dbManager = getParentExtension().getParentZone().getDBManager();
  		try {
			String query = "SELECT type, count FROM resources WHERE player_id=" + playerId + " AND (type=1001 OR type=1201 OR type=1202) LIMIT 3";
			ISFSArray sfsArray = dbManager.executeQuery(query, new Object[]{});

			query = "SELECT `index` FROM quests WHERE player_id=" + playerId + " AND score>0 ORDER BY `index` DESC LIMIT 0, 1";
			ISFSArray sfsArray2 = dbManager.executeQuery(query, new Object[]{});
			SFSObject q = new SFSObject();
			q.putInt("type", 5000);
			q.putInt("count", sfsArray2.getSFSObject(0).getInt("index"));
			sfsArray.addSFSObject( q );

			params.putSFSArray("features", sfsArray );
		} catch (SQLException e) {
			trace(e.getMessage());
		}
		//trace(params.getDump());
		send("profile", params, sender);
    }
}