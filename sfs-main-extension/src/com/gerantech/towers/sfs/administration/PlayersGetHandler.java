package com.gerantech.towers.sfs.administration;

import com.gt.Commands;
import com.gerantech.towers.sfs.utils.PasswordGenerator;
import com.gt.towers.Game;
import com.smartfoxserver.v2.entities.User;
import com.smartfoxserver.v2.entities.data.ISFSObject;
import com.smartfoxserver.v2.extensions.BaseClientRequestHandler;

import java.sql.SQLException;

/**
 * @author ManJav
 *
 */
public class PlayersGetHandler extends BaseClientRequestHandler
{
	public void handleClientRequest(User sender, ISFSObject params)
    {
		Game game = (Game) sender.getSession().getProperty("core");
		if( !game.player.admin )
			return;
		String query = "SELECT id, name, password, app_version, sessions_count,  DATE_FORMAT(create_at, '%y-%m-%d  %h:%i:%s') create_at, DATE_FORMAT(last_login, '%y-%m-%d  %h:%i:%s') last_login FROM players WHERE ";
    	if( params.containsKey("id") )
			query += "id=" + params.getInt("id");
		else if( params.containsKey("tag") )
			query += "id=" + PasswordGenerator.recoverPlayerId(params.getText("tag"));
		else if( params.containsKey("name") )
			query += "name LIKE '%" + params.getUtfString("name") + "%'";
    	else
    		return;

		query += " ORDER BY id DESC LIMIT 500;";
		trace(query);
			try {
			params.putSFSArray("players", getParentExtension().getParentZone().getDBManager().executeQuery(query , new Object[] {}));
		} catch (SQLException e) { e.printStackTrace(); }
		send(Commands.PLAYERS_GET, params, sender);
    }
}