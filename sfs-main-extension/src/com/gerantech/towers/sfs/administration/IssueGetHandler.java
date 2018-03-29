package com.gerantech.towers.sfs.administration;

import com.gerantech.towers.sfs.Commands;
import com.smartfoxserver.v2.entities.User;
import com.smartfoxserver.v2.entities.data.ISFSObject;
import com.smartfoxserver.v2.extensions.BaseClientRequestHandler;

import java.sql.SQLException;

/**
 * @author ManJav
 *
 */
public class IssueGetHandler extends BaseClientRequestHandler
{
	public void handleClientRequest(User sender, ISFSObject params)
    {
 		try {
			params.putSFSArray("issues", getParentExtension().getParentZone().getDBManager().executeQuery("SELECT bugs.id, bugs.player_id, bugs.description, bugs.status, UNIX_TIMESTAMP(bugs.report_at), players.name as sender FROM bugs INNER JOIN players ON bugs.player_id = players.id ORDER BY bugs.id DESC LIMIT 100;", new Object[] {}));
		} catch (SQLException e) { e.printStackTrace(); }
		send(Commands.ISSUE_GET, params, sender);
    }
}