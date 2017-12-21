package com.gerantech.towers.sfs.handlers;

import com.gerantech.towers.sfs.utils.PasswordGenerator;
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

		params.putText("tag", PasswordGenerator.getInvitationCode(playerId));
		IDBManager dbManager = getParentExtension().getParentZone().getDBManager();
  		try {
			String query = "SELECT type, count FROM resources WHERE player_id=" + playerId + " AND (type=1001 OR type=1201 OR type=1202" ;
			if( params.containsKey("am") )
				query += " OR type=1000 OR type=1002 OR type=1003 OR type=1004 OR type=1203 OR type=1204 OR type=1211)" ;
			else
				query += ")" ;

			ISFSArray sfsArray = dbManager.executeQuery(query, new Object[]{});

			query = "SELECT `index` FROM quests WHERE player_id=" + playerId + " AND score>0 ORDER BY `index` DESC LIMIT 0, 1";
			ISFSArray sfsArray2 = dbManager.executeQuery(query, new Object[]{});
			SFSObject q = new SFSObject();
			q.putInt("type", 5000);
			q.putInt("count", sfsArray2.getSFSObject(0).getInt("index")+1);
			sfsArray.addSFSObject( q );
			params.putSFSArray("features", sfsArray );

			query = "SELECT decks.`type`, resources.`level` FROM decks INNER JOIN resources ON decks.player_id = resources.player_id AND decks.`type` = resources.`type` WHERE decks.player_id = "+ playerId +" AND decks.deck_index = 0";
			params.putSFSArray("decks", dbManager.executeQuery(query, new Object[]{}));

		} catch (SQLException e) { e.printStackTrace(); }
		send("profile", params, sender);
    }
}