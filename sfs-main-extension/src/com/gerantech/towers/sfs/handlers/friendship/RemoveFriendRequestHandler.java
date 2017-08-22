package com.gerantech.towers.sfs.handlers.friendship;

import com.gt.towers.Game;
import com.smartfoxserver.v2.db.IDBManager;
import com.smartfoxserver.v2.entities.User;
import com.smartfoxserver.v2.entities.data.ISFSArray;
import com.smartfoxserver.v2.entities.data.ISFSObject;
import com.smartfoxserver.v2.extensions.BaseClientRequestHandler;

import java.sql.SQLException;

/**
 * Created by Babak on 8/22/2017.
 */
public class RemoveFriendRequestHandler extends BaseClientRequestHandler {

    private IDBManager dbManager;

    public RemoveFriendRequestHandler() {}

    public void handleClientRequest(User sender, ISFSObject params)
    {
        try{
            dbManager = getParentExtension().getParentZone().getDBManager();
            int inviteId = params.getInt("inviterId");
            int inviteeId = params.getInt("inviteeId");

            String str = "DELETE FROM `friendship` WHERE (`inviter_id` ="+ inviteId +" AND `invitee_id` ="+ inviteeId +") OR " +
                                                        "(`inviter_id` ="+ inviteeId +" AND `invitee_id` ="+ inviteId +" ) LIMIT 1;";
            dbManager.executeQuery(str, new Object[]{});
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
