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
			params.putSFSArray("issues", getParentExtension().getParentZone().getDBManager().executeQuery("SELECT id, player_id, description, status, UNIX_TIMESTAMP(report_at) FROM bugs ORDER BY id DESC LIMIT 100;", new Object[] {}));
		} catch (SQLException e) { e.printStackTrace(); }
		send(Commands.ISSUE_GET, params, sender);
    }
}