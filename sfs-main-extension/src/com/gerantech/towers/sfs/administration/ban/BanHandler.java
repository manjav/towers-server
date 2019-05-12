package com.gerantech.towers.sfs.administration.ban;

import com.gt.BBGClientRequestHandler;
import com.gt.Commands;
import com.gt.towers.Game;
import com.gt.towers.constants.MessageTypes;
import com.gt.utils.BanUtils;
import com.gt.utils.DBUtils;
import com.smartfoxserver.v2.db.IDBManager;
import com.smartfoxserver.v2.entities.Room;
import com.smartfoxserver.v2.entities.User;
import com.smartfoxserver.v2.entities.data.ISFSArray;
import com.smartfoxserver.v2.entities.data.ISFSObject;
import com.smartfoxserver.v2.entities.data.SFSObject;

import java.sql.SQLException;
import java.time.Instant;
import java.util.List;

/**
 * @author ManJav
 */
public class BanHandler extends BBGClientRequestHandler
{
	public void handleClientRequest(User sender, ISFSObject params)
    {
		Game game = ((Game)sender.getSession().getProperty("core"));
		if( !game.player.admin )
			return;
		// get name
		ISFSArray players = null;
		IDBManager db = getParentExtension().getParentZone().getDBManager();
		String query = "SELECT name FROM players WHERE id=" + params.getInt("id");
		try {
			players = db.executeQuery(query, new Object[]{});
		} catch (SQLException e) { e.printStackTrace(); }
		if( players == null || players.size() == 0 )
		{
			send(Commands.BAN, MessageTypes.RESPONSE_NOT_FOUND, params, sender);
			return;
		}

		// get udid
		String udid = DBUtils.getInstance().getUDID(params.getInt("id"));

		long now = Instant.now().getEpochSecond();
		BanUtils.getInstance().warnOrBan(db, params.getInt("id"), udid, params.getInt("mode"), now, params.getInt("len"), params.getText("msg"));
		send(Commands.BAN, MessageTypes.RESPONSE_SUCCEED, params, sender);

		if( params.getInt("mode") >= 2 )
		{
			User u = getParentExtension().getParentZone().getUserByName(params.getInt("id")+"");
			if( u != null )
			{
				List<Room> publics = getParentExtension().getParentZone().getRoomListFromGroup("publics");
				for (Room p : publics) {
					SFSObject msg = new SFSObject();
					msg.putInt("m",  MessageTypes.M18_COMMENT_BAN);
					msg.putUtfString("s", "KOOT");
					msg.putUtfString("o", players.getSFSObject(0).getUtfString("name"));
					msg.putInt("p",  -1);
					getApi().sendExtensionResponse(Commands.LOBBY_PUBLIC_MESSAGE, msg, p.getUserList(), p, false);
					p.getVariable("msg").getSFSArrayValue().addSFSObject(msg);

				}
				getApi().disconnectUser(u);
			}
		}
	}
}