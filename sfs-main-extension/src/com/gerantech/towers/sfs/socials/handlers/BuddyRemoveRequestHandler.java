package com.gerantech.towers.sfs.socials.handlers;

import com.gt.Commands;
import com.smartfoxserver.v2.entities.User;
import com.smartfoxserver.v2.entities.data.ISFSObject;
import com.smartfoxserver.v2.extensions.BaseClientRequestHandler;

import java.sql.SQLException;

/**
 * Created by ManJav on 8/24/2017.
 */
public class BuddyRemoveRequestHandler extends BaseClientRequestHandler
{
    public void handleClientRequest(User sender, ISFSObject params)
    {
        try{
            String buddyId = params.getText("buddyId");
            getParentExtension().getBuddyApi().removeBuddy(sender, buddyId, true, true);
            User buddyUser = getParentExtension().getParentZone().getUserManager().getUserByName(buddyId);
            if( buddyUser != null )
                getParentExtension().getBuddyApi().removeBuddy(buddyUser, sender.getName(), true, true);

            String str = "UPDATE `friendship` SET `has_reward`=1 WHERE (`inviter_id` ="+ sender.getName() +" AND `invitee_id` ="+ buddyId +") OR " + "(`inviter_id` ="+ buddyId +" AND `invitee_id` ="+ sender.getName() +" );";
            getParentExtension().getParentZone().getDBManager().executeUpdate(str, new Object[]{});
        } catch (SQLException e) {
            e.printStackTrace();
        }
        send( Commands.BUDDY_REMOVE, params, sender);
    }
}
