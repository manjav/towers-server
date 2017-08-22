package com.gerantech.towers.sfs.handlers.friendship;

import com.gt.towers.Game;
import com.smartfoxserver.v2.db.IDBManager;
import com.smartfoxserver.v2.entities.User;
import com.smartfoxserver.v2.entities.data.ISFSArray;
import com.smartfoxserver.v2.entities.data.ISFSObject;
import com.smartfoxserver.v2.extensions.BaseClientRequestHandler;
import com.smartfoxserver.v2.extensions.ExtensionLogLevel;

import java.sql.SQLException;

/**
 * Created by Babak on 8/22/2017.
 */
public class RemoveFriendRequestHandler extends BaseClientRequestHandler
{
    public RemoveFriendRequestHandler() {}

    public void handleClientRequest(User sender, ISFSObject params)
    {
        try{
            IDBManager dbManager = getParentExtension().getParentZone().getDBManager();
            int inviterId = params.getInt("inviterId");
            int inviteeId = params.getInt("inviteeId");

            String str = "DELETE FROM `friendship` WHERE (`inviter_id` ="+ inviterId +" AND `invitee_id` ="+ inviteeId +") OR " +
                                                        "(`inviter_id` ="+ inviteeId +" AND `invitee_id` ="+ inviterId +" ) LIMIT 1;";
            dbManager.executeUpdate(str, new Object[]{});
         /*   if( res.size() == 0 )
                trace(ExtensionLogLevel.WARN, "friendship field not found." );*/
        } catch (SQLException e) {
            e.printStackTrace();
        }
        send("removeFriend", params, sender);
    }
}
