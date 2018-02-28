package com.gerantech.towers.sfs.handlers;

import com.gerantech.towers.sfs.socials.LobbyUtils;
import com.gerantech.towers.sfs.utils.PasswordGenerator;
import com.smartfoxserver.v2.db.IDBManager;
import com.smartfoxserver.v2.entities.Room;
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
		IDBManager dbManager = getParentExtension().getParentZone().getDBManager();
		int playerId = params.getInt("id");

		//  -=-=-=-=-=-=-=-=-  add resources data  -=-=-=-=-=-=-=-=-
		String query = "SELECT type, count FROM resources WHERE player_id=" + playerId + " AND (type=1001 OR type=1201 OR type=1202" ;
		if( params.containsKey("am") )
			query += " OR type=1000 OR type=1002 OR type=1003 OR type=1004 OR type=1203 OR type=1204 OR type=1211)" ;
		else
			query += ")" ;

		ISFSArray featuresArray = null;
		try {
			featuresArray = dbManager.executeQuery(query, new Object[]{});
		} catch (SQLException e) { trace(e.getMessage()); }

		//  -=-=-=-=-=-=-=-=-  add quests data  -=-=-=-=-=-=-=-=-
		ISFSArray questsArray = null;
		try {
			questsArray = dbManager.executeQuery("SELECT `index` FROM quests WHERE player_id=" + playerId + " AND score>0 ORDER BY `index` DESC LIMIT 0, 1", new Object[]{});
		} catch (SQLException e) { trace(e.getMessage()); }
		SFSObject q = new SFSObject();
		q.putInt("type", 5000);
		q.putInt("count", questsArray.getSFSObject(0).getInt("index")+1);
		featuresArray.addSFSObject( q );

		params.putSFSArray("features", featuresArray );

		/*//  -=-=-=-=-=-=-=-=-  add buildings data  -=-=-=-=-=-=-=-=-
		ISFSArray buildingArray = null;
		try {
			buildingArray = dbManager.executeQuery("SELECT type, level FROM resources WHERE player_id=" + playerId + " AND (type<1000 AND type>0) LIMIT 100", new Object[]{});
		} catch (SQLException e) { trace(e.getMessage()); }
		params.putSFSArray("buildings", buildingArray );*/

		//  -=-=-=-=-=-=-=-=-  add lobby data  -=-=-=-=-=-=-=-=-
		Room lobby = null;
		if( params.containsKey("lp") )
			lobby = LobbyUtils.getInstance().getLobbyOfOfflineUser(playerId);
		if( lobby != null )
		{
			params.putText("ln", lobby.getName());
			params.putInt("lp", lobby.getVariable("pic").getIntValue());
		}

		//  -=-=-=-=-=-=-=-=-  add player tag  -=-=-=-=-=-=-=-=-
		params.putText("tag", PasswordGenerator.getInvitationCode(playerId));

		//  -=-=-=-=-=-=-=-=-  add player deck  -=-=-=-=-=-=-=-=-
		try {
			params.putSFSArray("decks", dbManager.executeQuery("SELECT decks.`type`, resources.`level` FROM decks INNER JOIN resources ON decks.player_id = resources.player_id AND decks.`type` = resources.`type` WHERE decks.player_id = "+ playerId +" AND decks.deck_index = 0", new Object[]{}));
		} catch (SQLException e) { trace(e.getMessage()); }


		//trace(params.getDump());
		send("profile", params, sender);
    }
}