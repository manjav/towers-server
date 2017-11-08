package com.gerantech.towers.sfs.socials.handlers;

import com.gerantech.towers.sfs.Commands;
import com.gerantech.towers.sfs.utils.OneSignalUtils;
import com.gerantech.towers.sfs.utils.PasswordGenerator;
import com.gt.towers.Game;
import com.gt.towers.constants.ResourceType;
import com.smartfoxserver.v2.api.ISFSBuddyApi;
import com.smartfoxserver.v2.buddylist.Buddy;
import com.smartfoxserver.v2.buddylist.BuddyList;
import com.smartfoxserver.v2.db.IDBManager;
import com.smartfoxserver.v2.entities.User;
import com.smartfoxserver.v2.entities.data.ISFSArray;
import com.smartfoxserver.v2.entities.data.ISFSObject;
import com.smartfoxserver.v2.exceptions.SFSBuddyListException;
import com.smartfoxserver.v2.extensions.BaseClientRequestHandler;

import java.sql.SQLException;

/**
 * Created by Babak on 8/19/2017.
 */
public class BuddyAddRequestHandler extends BaseClientRequestHandler {

    private static final int OK = 0;
    private static final int INVALID_INVITATION_CODE = -1;
    private static final int ALREADY_FRIEND = -2;
    private static final int USER_ADD_HIMSELF = -3;
    private static final int ANOTHER_USER_SAME_PHONE = -4;

    private ISFSBuddyApi buddyApi;
    private IDBManager dbManager;
    private ISFSArray sfsArray;

    public void handleClientRequest(User sender, ISFSObject params)
    {
        dbManager = getParentExtension().getParentZone().getDBManager();
        buddyApi = getParentExtension().getBuddyApi();
        Game game = ((Game)sender.getSession().getProperty("core"));
        BuddyList buddies = null;

        String invitationCode = params.getText("invitationCode");
        int inviteeId = game.player.id;
        String inviteeName = inviteeId + "";
        trace("Invitation Code", invitationCode);
        int inviterId = PasswordGenerator.recoverPlayerId(invitationCode);
        String inviterName = inviterId + "";
        trace("inviteeId", inviteeId, "inviterId", inviterId);

        // Case 1:
        // User wants to add himself to get rewards
        if ( inviterId == inviteeId )
        {
            sendResult(sender, params, USER_ADD_HIMSELF );
            return;
        }

        // Case 3:
        // Invalid invitation code
        if( !check(sender, params, INVALID_INVITATION_CODE, "SELECT name FROM players WHERE id="+ inviterId, true) )
            return;
        params.putText("inviter", sfsArray.getSFSObject(0).getText("name"));

        // Case 2:
        // Invitee player has been already added to inviter's friend list
        buddies = getParentExtension().getParentZone().getBuddyListManager().getBuddyList(inviteeName);
        if( buddies.containsBuddy(inviterName) )
        {
            sendResult(sender, params, ALREADY_FRIEND );
            return;
        }

        // Case 4:
        // Users can't get rewards and be friends with another user with the same UDID
        if( !check(sender, params, ANOTHER_USER_SAME_PHONE, "SELECT COUNT(*) FROM devices WHERE player_id="+ inviterId +" OR player_id="+ inviteeId +" GROUP BY udid HAVING COUNT(*)>1" ) )
            return;

        String msg = game.player.nickName + " باهات رفیق شد. ";
        sendResult(sender, params, OK);

        for (Buddy b : buddies.getBuddies() )
            trace("b  :  ", b.getName());

        try
        {
            // Invitee already consumed first invitation reward if query has result.
            String queryStr = "SELECT invitee_id FROM friendship WHERE invitee_id="+ inviteeId + " OR inviter_id="+ inviteeId;
            trace("QUERY: ", queryStr);
            sfsArray = dbManager.executeQuery(queryStr, new Object[]{});
            if( sfsArray.size() == 0 )
            {
                // Invitee reward consumption
                game.player.resources.increase(ResourceType.CURRENCY_HARD, 10);
                queryStr = "UPDATE resources SET count=" + game.player.get_hards() + " WHERE type=1003 AND player_id=" + inviteeId + ";";
                trace("add reward query:", queryStr);
                dbManager.executeUpdate(queryStr, new Object[] {});
                params.putInt("rewardType", ResourceType.CURRENCY_HARD);
                params.putInt("rewardCount", 10);
            }

            // Inviter invited invitee before if query has result.
            queryStr = "SELECT inviter_id FROM friendship WHERE invitee_id="+ inviteeId + " AND inviter_id="+ inviterId + " OR invitee_id="+ inviterId + " AND inviter_id="+ inviteeId;
            trace("QUERY: ", queryStr);
            sfsArray = dbManager.executeQuery(queryStr, new Object[]{});
            if( sfsArray.size() == 0 )
            {
                // Inviter reward consumption
                queryStr = "UPDATE resources SET count=count+5 WHERE type=1003 AND player_id=" + inviterId + ";";
                trace("add reward query:", queryStr);
                dbManager.executeUpdate(queryStr, new Object[] {});
                msg = game.player.nickName + " باهات رفیق شد و تو هم 5 تا جواهر جایزه گرفتی. ";
            }

            // If nothing is wrong Insert to friendship
            queryStr = "INSERT INTO friendship (inviter_id, invitee_id, invitation_code, has_reward) VALUES (" + inviterId + ", " + inviteeId + ", '" + invitationCode + "', 0)";
            trace("INPUT string to DB:", queryStr);
            dbManager.executeInsert(queryStr, new Object[] {});

            buddyApi.addBuddy(sender, inviterName, false, true, false);
            User inviterUser = getParentExtension().getParentZone().getUserManager().getUserByName(inviterName);
            if( inviterUser != null )
                buddyApi.addBuddy(inviterUser, inviteeName, false, true, false);
        }
        catch (SQLException e) {
            params.putText("responseCode", e.getErrorCode()+"");
            trace(e.getMessage());
        } catch (SFSBuddyListException e) {
            e.printStackTrace();
        }
        OneSignalUtils.getInstance().getInstance().send(msg, null, inviterId);
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
            sendResult( sender, params, e.getErrorCode() );
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
        send( Commands.BUDDY_ADD, params, sender );
    }
}