package com.gerantech.towers.sfs.administration.issues;

import com.smartfoxserver.v2.entities.User;
import com.smartfoxserver.v2.entities.data.ISFSObject;
import com.smartfoxserver.v2.extensions.BaseClientRequestHandler;

import java.sql.SQLException;

/**
 * @author ManJav
 *
 */
public class IssueTrackHandler extends BaseClientRequestHandler
{
	public void handleClientRequest(User sender, ISFSObject params)
    {
		try {
			getParentExtension().getParentZone().getDBManager().executeUpdate("UPDATE `bugs` SET `status`=" + params.getInt("status") + " WHERE id=" + params.getInt("id"), new Object[]{});
		} catch (SQLException e) {  e.printStackTrace(); }
    }
}