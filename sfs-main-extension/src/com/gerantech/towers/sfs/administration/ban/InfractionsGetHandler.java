package com.gerantech.towers.sfs.administration.ban;

import com.gerantech.towers.sfs.Commands;
import com.gt.towers.Game;
import com.smartfoxserver.v2.entities.User;
import com.smartfoxserver.v2.entities.data.ISFSObject;
import com.smartfoxserver.v2.extensions.BaseClientRequestHandler;
import java.sql.SQLException;

/**
 * @author ManJav
 *
 */
public class InfractionsGetHandler extends BaseClientRequestHandler
{
	public void handleClientRequest(User sender, ISFSObject params)
    {
		Game game = ((Game)sender.getSession().getProperty("core"));
		if( !game.player.admin )
			return;

		String query = "SELECT players.name, infractions.id, infractions.reporter, infractions.offender, infractions.content, infractions.lobby, infractions.offend_at FROM players INNER JOIN infractions ON players.id = infractions.offender";
    	if( params.containsKey("id") )
			query += " WHERE infractions.offender=" + params.getInt("id");
		query += " ORDER BY infractions.offend_at DESC LIMIT 500;";

 		try {
			params.putSFSArray("data", getParentExtension().getParentZone().getDBManager().executeQuery(query, new Object[] {}));
		} catch (SQLException e) { e.printStackTrace(); }
		send(Commands.INFRACTIONS_GET, params, sender);
    }
}