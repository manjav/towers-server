package com.gerantech.towers.sfs.administration;

import com.gerantech.towers.sfs.Commands;
import com.gerantech.towers.sfs.utils.BanSystem;
import com.gt.towers.Game;
import com.gt.towers.constants.MessageTypes;
import com.smartfoxserver.v2.db.IDBManager;
import com.smartfoxserver.v2.entities.Room;
import com.smartfoxserver.v2.entities.User;
import com.smartfoxserver.v2.entities.data.ISFSArray;
import com.smartfoxserver.v2.entities.data.ISFSObject;
import com.smartfoxserver.v2.entities.data.SFSObject;
import com.smartfoxserver.v2.extensions.BaseClientRequestHandler;

import java.sql.SQLException;
import java.time.Instant;
import java.util.List;

/**
 * @author ManJav
 *
 */
public class BanHandler extends BaseClientRequestHandler
{
	public void handleClientRequest(User sender, ISFSObject params)
    {
		Game game = ((Game)sender.getSession().getProperty("core"));
		/*if( !game.player.admin )
		{
			sendResponse(sender, params, MessageTypes.RESPONSE_NOT_ALLOWED);
			return;
		}*/

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
			sendResponse(sender, params, MessageTypes.RESPONSE_NOT_FOUND);
			return;
		}

		// get udid
		String udid = "";
		ISFSArray udids = null;
		query = "SELECT udid FROM devices WHERE player_id=" + params.getInt("id");
		try {
			udids = db.executeQuery(query, new Object[]{});
		} catch (SQLException e) { e.printStackTrace(); }
		if( udids != null && udids.size() > 0 )
			udid = udids.getSFSObject(0).getText("udid");

		long now = Instant.now().getEpochSecond();
		BanSystem.getInstance().warnOrBan(db, params.getInt("id"), udid, params.getInt("mode"), now, params.getInt("len"), params.getText("msg"));
		sendResponse(sender, params, MessageTypes.RESPONSE_SUCCEED);

		if( params.getInt("mode") == 2 )
		{
			User u = getParentExtension().getParentZone().getUserByName(params.getInt("id")+"");
			if( u != null )
			{
				List<Room> publics = getParentExtension().getParentZone().getRoomListFromGroup("publics");
				for (Room p : publics) {
					SFSObject msg = new SFSObject();
					msg.putShort("m", (short) MessageTypes.M18_COMMENT_BAN);
					msg.putUtfString("s", "KOOT");
					msg.putUtfString("o", players.getSFSObject(0).getUtfString("name"));
					msg.putShort("p", (short) -1);
					getApi().sendExtensionResponse(Commands.LOBBY_PUBLIC_MESSAGE, msg, p.getUserList(), p, false);
					p.getVariable("msg").getSFSArrayValue().addSFSObject(msg);

				}
				getApi().disconnectUser(u);
			}
		}
	}

	private void sendResponse(User sender, ISFSObject params, int responseId)
	{
		params.putInt("response", responseId);
		send(Commands.BAN, params, sender);
	}
}