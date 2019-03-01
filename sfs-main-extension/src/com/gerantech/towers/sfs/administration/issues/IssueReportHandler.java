package com.gerantech.towers.sfs.administration.issues;

import com.gt.Commands;
import com.gt.towers.Game;
import com.smartfoxserver.v2.db.IDBManager;
import com.smartfoxserver.v2.entities.User;
import com.smartfoxserver.v2.entities.data.ISFSObject;
import com.smartfoxserver.v2.extensions.BaseClientRequestHandler;

import java.sql.SQLException;

/**
 * @author ManJav
 *
 */
public class IssueReportHandler extends BaseClientRequestHandler
{
	public void handleClientRequest(User sender, ISFSObject params)
    {
		params.putBool("succeed", true);
		Game game = ((Game)sender.getSession().getProperty("core"));

		String email = params.getText("email");
		String description = params.getUtfString("description");
		//trace(game.player.id, email, description);

		IDBManager dbManager = getParentExtension().getParentZone().getDBManager();
  		try {
			dbManager.executeInsert("INSERT INTO bugs (`player_id`, `email`, `description`) VALUES ('" + game.player.id + "', '" + email + "', '" + description + "');", new Object[] {});
		} catch (SQLException e) {
			params.putBool("succeed", false);
			params.putText("errorCode", e.getErrorCode()+"");
			trace(e.getMessage());
		}
		send(Commands.ISSUE_REPORT, params, sender);
    }
}