package com.gerantech.towers.sfs.socials.handlers;

import com.gerantech.towers.sfs.utils.PasswordGenerator;
import com.gt.BBGClientRequestHandler;
import com.gt.Commands;
import com.gt.towers.Game;
import com.gt.towers.constants.MessageTypes;
import com.gt.towers.constants.ResourceType;
import com.gt.towers.socials.Lobby;
import com.gt.utils.InboxUtils;
import com.gt.utils.OneSignalUtils;
import com.smartfoxserver.v2.api.ISFSBuddyApi;
import com.smartfoxserver.v2.buddylist.Buddy;
import com.smartfoxserver.v2.buddylist.BuddyList;
import com.smartfoxserver.v2.db.IDBManager;
import com.smartfoxserver.v2.entities.User;
import com.smartfoxserver.v2.entities.data.ISFSArray;
import com.smartfoxserver.v2.entities.data.ISFSObject;
import com.smartfoxserver.v2.exceptions.SFSBuddyListException;

import java.sql.SQLException;

/**
 * Created by Babak on 8/19/2017.
 */
public class BuddyAddRequestHandler extends BBGClientRequestHandler
{
    /*private static final int zOK = 0;
    private static final int zINVALID_INVITATION_CODE = -1;
    private static final int zALREADY_FRIEND = -2;
    private static final int zUSER_ADD_HIMSELF = -3;
    private static final int zANOTHER_USER_SAME_PHONE = -4;

    public static var RESPONSE_SENT:Int = 1;
    public static var RESPONSE_SUCCEED:Int = 0;
    public static var RESPONSE_NOT_ALLOWED:Int = -1;
    public static var RESPONSE_ALREADY_SENT:Int = -3;
    public static var RESPONSE_NOT_FOUND:Int = -4;
    public static var RESPONSE_UNKNOWN_ERROR:Int = -5;
    public static var RESPONSE_NOT_ENOUGH_REQS:Int = -6;
    public static var RESPONSE_MUST_WAIT:Int = -7;*/

    private ISFSArray sfsArray;
    private IDBManager dbManager;
    private ISFSBuddyApi buddyApi;
    public void handleClientRequest(User sender, ISFSObject params)
    {
        BuddyList buddies;
        buddyApi = getParentExtension().getBuddyApi();
        dbManager = getParentExtension().getParentZone().getDBManager();
        Game game = ((Game)sender.getSession().getProperty("core"));

        String invitationCode = params.getText("invitationCode");
        String inviteeUDID = params.containsKey("udid") ? params.getText("udid") : null;

        int inviteeId = game.player.id;
        String inviteeName = inviteeId + "";
        trace("Invitation Code", invitationCode);
        int inviterId = PasswordGenerator.recoverPlayerId(invitationCode);
        String inviterName = inviterId + "";
        boolean existsUDID = false;
        trace("inviteeId", inviteeId, "inviterId", inviterId);

        // Case 1:
        // User wants to add himself to get rewards
        if ( inviterId == inviteeId )
        {
            send(Commands.BUDDY_ADD, MessageTypes.RESPONSE_ALREADY_SENT, params, sender);
            return;
        }

        // Case 3:
        // Invalid invitation code
        if( !check(sender, params, MessageTypes.RESPONSE_NOT_ALLOWED, "SELECT name FROM players WHERE id="+ inviterId, true) )
            return;
        params.putText("inviter", sfsArray.getSFSObject(0).getText("name"));

        // Case 2:
        // Invitee player has been already added to inviter's friend list
        buddies = getParentExtension().getParentZone().getBuddyListManager().getBuddyList(inviteeName);
        if( buddies.containsBuddy(inviterName) )
        {
            send(Commands.BUDDY_ADD, -2, params, sender);
            return;
        }

        // Case 4:
        // Users can't get rewards when was friends with another user with the same UDID
        if( inviteeUDID != null )
        {
            try {
                existsUDID = dbManager.executeQuery("SELECT COUNT(*) FROM devices WHERE udid='" + inviteeUDID + "'", new Object[]{}).size() > 0;
                trace("existsUDID", existsUDID);
            } catch (SQLException e) { e.printStackTrace(); }
        }
       // if( !check(sender, params, ANOTHER_USER_SAME_PHONE, "SELECT COUNT(*) FROM devices WHERE player_id="+ inviteeId +" GROUP BY udid HAVING COUNT(*)>1" ) )
            //return;

        String msg = (game.player.nickName.equals("guest")?"یه نفر":game.player.nickName) + " باهات رفیق شد. ";
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
                game.player.resources.increase(ResourceType.R4_CURRENCY_HARD, Lobby.buddyInviteeReward);
                queryStr = "UPDATE resources SET count=" + game.player.get_hards() + " WHERE type=1003 AND player_id=" + inviteeId + ";";
                trace("add reward query:", queryStr);
                dbManager.executeUpdate(queryStr, new Object[] {});
                params.putInt("rewardType", ResourceType.R4_CURRENCY_HARD);
                params.putInt("rewardCount", Lobby.buddyInviteeReward);
            }

            // Inviter invited invitee before if query has result.
            queryStr = "SELECT inviter_id FROM friendship WHERE invitee_id="+ inviteeId + " AND inviter_id="+ inviterId + " OR invitee_id="+ inviterId + " AND inviter_id="+ inviteeId;
            trace("QUERY: ", queryStr);
            sfsArray = dbManager.executeQuery(queryStr, new Object[]{});

            // Inviter reward consumption if invitee is new player
            if( !existsUDID && sfsArray.size() == 0 )
            {
                queryStr = "UPDATE resources SET count=count+" + Lobby.buddyInviterReward + " WHERE type=1003 AND player_id=" + inviterId + ";";
                trace("add reward query:", queryStr);
                dbManager.executeUpdate(queryStr, new Object[] {});
                msg = (game.player.nickName.equals("guest")?"یه نفر":game.player.nickName) + " باهات رفیق شد و تو هم " + Lobby.buddyInviterReward + " تا جواهر جایزه گرفتی. ";
            }

            // create friendship if not exists
            queryStr = "SELECT invitee_id FROM friendship WHERE invitee_id=" + inviteeId + " AND inviter_id=" + inviterId;
            trace("QUERY: ", queryStr);
            sfsArray = dbManager.executeQuery(queryStr, new Object[]{});
            if( sfsArray.size() == 0 )
            {
                queryStr = "INSERT INTO friendship (inviter_id, invitee_id, invitation_code, has_reward) VALUES (" + inviterId + ", " + inviteeId + ", '" + invitationCode + "', 0)";
                trace("INSERT to DB:", queryStr);
                dbManager.executeInsert(queryStr, new Object[]{});
            }

            buddyApi.addBuddy(sender, inviterName, false, true, false);
            User inviterUser = getParentExtension().getParentZone().getUserManager().getUserByName(inviterName);
            if( inviterUser != null ) {
                buddyApi.addBuddy(inviterUser, inviteeName, false, true, false);
            }
        }
        catch (SQLException e) {
            params.putText("response", e.getErrorCode()+"");
            trace(e.getMessage());
        } catch (SFSBuddyListException e) {
            e.printStackTrace();
        }

        // Send friendship notification to inviter inbox
        InboxUtils.getInstance().send(MessageTypes.M50_URL, msg, inviteeId, inviterId, "towers://open?controls=tabs&dashTab=3&socialTab=2" );
        OneSignalUtils.getInstance().getInstance().send(msg, null, inviterId);
        send(Commands.BUDDY_ADD, MessageTypes.RESPONSE_SUCCEED, params, sender);
    }

    private boolean check ( User sender, ISFSObject params, int responseCode, String queryStr, boolean shouldExist )
    {
        trace("QUERY: ", queryStr);
        try {
            sfsArray = dbManager.executeQuery(queryStr, new Object[]{});
        } catch (SQLException e) {
            send(Commands.BUDDY_ADD, e.getErrorCode(), params, sender);
            e.printStackTrace();
            return false;
        }

        boolean failed = shouldExist ? sfsArray.size() == 0 : sfsArray.size() > 0;
        if( failed )
        {
            send(Commands.BUDDY_ADD, responseCode, params, sender);
            return false;
        }
        return true;
    }
}