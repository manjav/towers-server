package com.gerantech.towers.sfs.socials.handlers;

import com.smartfoxserver.v2.db.IDBManager;
import com.smartfoxserver.v2.entities.User;
import com.smartfoxserver.v2.entities.data.ISFSObject;
import com.smartfoxserver.v2.extensions.BaseClientRequestHandler;

import java.sql.SQLException;

/**
 * Created by ManJav on 8/24/2017.
 */
public class FriendsRemoveRequestHandler extends BaseClientRequestHandler
{
    public FriendsRemoveRequestHandler() {}

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
