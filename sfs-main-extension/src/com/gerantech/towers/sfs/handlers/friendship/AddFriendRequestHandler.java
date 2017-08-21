package com.gerantech.towers.sfs.handlers.friendship;

import com.gerantech.towers.sfs.utils.OneSignalUtils;
import com.gerantech.towers.sfs.utils.PasswordGenerator;
import com.gerantech.towers.sfs.utils.UserManager;
import com.gt.towers.Game;
import com.gt.towers.constants.ResourceType;
import com.smartfoxserver.v2.db.IDBManager;
import com.smartfoxserver.v2.entities.User;
import com.smartfoxserver.v2.entities.data.ISFSArray;
import com.smartfoxserver.v2.entities.data.ISFSObject;
import com.smartfoxserver.v2.extensions.BaseClientRequestHandler;

import java.sql.SQLException;

/**
 * Created by Babak on 8/19/2017.
 */
public class AddFriendRequestHandler extends BaseClientRequestHandler {

    private static final int OK = 0;
    private static final int INVALID_INVITATION_CODE = -1;
    private static final int ALREADY_FRIEND = -2;
    private static final int USER_ADD_HIMSELF = -3;
    private static final int ANOTHER_USER_SAME_PHONE = -4;

    private IDBManager dbManager;
    private ISFSArray sfsArray;

    public AddFriendRequestHandler() {}

    public void handleClientRequest(User sender, ISFSObject params)
    {
        dbManager = getParentExtension().getParentZone().getDBManager();
        Game game = ((Game)sender.getSession().getProperty("core"));

        String invitationCode = params.getText("invitationCode");
        int inviteeId = game.player.id;
        trace("Invitation Code", invitationCode);
        int inviterId = PasswordGenerator.recoverPlayerId(invitationCode);
        trace("inviteeId", inviteeId, "inviterId", inviterId);

        // Case 1:
        // User wants to add himself to get rewards
        if ( inviterId == inviteeId )
        {
            sendResult(sender, params, USER_ADD_HIMSELF );
            return;
        }

        // Case 2:
        // Invalid invitation code
        if( !check(sender, params, INVALID_INVITATION_CODE, "SELECT name FROM players WHERE id="+ inviterId, true) )
            return;
        String inviterName = sfsArray.getSFSObject(0).getText("name");

        // Case 3:
        // Invitee player has been already added to inviter's friend list
        if( !check(sender, params, ALREADY_FRIEND, "SELECT invitee_id FROM friendship WHERE inviter_id="+inviterId+" AND invitee_id="+inviteeId+" OR inviter_id="+inviteeId+" AND invitee_id="+ inviterId ) )
            return;

        // Case 4:
        // Users can't get rewards and be friends with another user with the same UDID
        if( !check(sender, params, ANOTHER_USER_SAME_PHONE, "SELECT COUNT(*) FROM devices WHERE player_id="+ inviterId +" OR player_id="+ inviteeId +" GROUP BY udid HAVING COUNT(*)>1" ) )
            return;

        try
        {
            // Invitee player already consumed first invitation reward.
            String queryStr = "SELECT invitee_id FROM friendship WHERE invitee_id="+ inviteeId + " OR inviter_id="+ inviteeId;
            trace("QUERY: ", queryStr);
            sfsArray = dbManager.executeQuery(queryStr, new Object[]{});
            if( sfsArray.size() == 0 )
            {
                // Invitee reward consumption
                game.player.resources.increase(ResourceType.CURRENCY_HARD, 10);
                queryStr = "UPDATE resources SET count=" + game.player.get_hards() + " WHERE type=1003 AND player_id=" + game.player.id + ";";
                trace("add reward query:", queryStr);
                dbManager.executeUpdate(queryStr, new Object[] {});
                params.putInt("rewardType", ResourceType.CURRENCY_HARD);
                params.putInt("rewardCount", 10);
            }

            // If nothing is wrong Insert to friendship
            queryStr = "INSERT INTO friendship (inviter_id, invitee_id, invitation_code, has_reward) VALUES (" + inviterId + ", " + inviteeId + ", '" + invitationCode + "', 0)";
            trace("INPUT string to DB:", queryStr);
            dbManager.executeInsert(queryStr, new Object[] {});
            params.putText("inviter", inviterName);
        }
        catch (SQLException e)
        {
            params.putText("responseCode", e.getErrorCode()+"");
            trace(e.getMessage());
        }
        sendResult(sender, params, OK);
        String msg = game.player.nickName + "باهات رفیق شد.";
        OneSignalUtils.send(getParentExtension(), msg, null, inviterId);
    }

    private boolean check ( User sender, ISFSObject params, int responseCode, String queryStr )
    {
        return check(sender, params, responseCode, queryStr, false);
    }
    private boolean check ( User sender, ISFSObject params, int responseCode, String queryStr, boolean shouldExist )
    {
        trace("QUERY: ", queryStr);
        try {
            sfsArray = dbManager.executeQuery(queryStr, new Object[]{});
        } catch (SQLException e) {
            params.putText("responseCode", e.getErrorCode()+"");
            e.printStackTrace();
            return false;
        }

        boolean failed = shouldExist ? sfsArray.size() == 0 : sfsArray.size() > 0;
        if( failed )
        {
            sendResult( sender, params, responseCode );
            return false;
        }
        return true;
    }

    private void sendResult(User sender, ISFSObject params, int responseCode)
    {
        params.putInt("responseCode", responseCode);
        //trace(params.getDump());
        send("addFriend", params, sender );
    }
}