package com.gerantech.towers.sfs.handlers.friendship;

import com.gerantech.towers.sfs.utils.PasswordGenerator;
import com.gt.towers.Game;
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

    private int RESPONSE_ALREADY_EXISTS = -1;
    private int REPETITIVE_INVITATION_CODE = -2;
    private int USER_ADD_HIMSELF = -3;
    private int INVALID_INVITATION_CODE = -4;
    private int ALREADY_FRIEND = -5;
    private int ALREADY_ADDED = -6;
    private int ANOTHER_USER_SAME_PHONE = -7;


    public AddFriendRequestHandler() {}

    public void handleClientRequest(User sender, ISFSObject params)
    {
        params.putBool("succeed", true);
        Game game = ((Game)sender.getSession().getProperty("core"));

        int inviteeId = params.getInt("inviteeId");
        String invitationCode = params.getText("invitationCode");
        int inviterReward = 0;
        int inviteeReward = 0;

        IDBManager dbManager = getParentExtension().getParentZone().getDBManager();

        try {
            // Case 1:
            // Invitee player already has been rewarded for being invited
            String str = "SELECT `invitee_id` FROM `friendship` WHERE `invitee_id`="+ inviteeId +" AND `inviter_reward`="+ 1;
            trace("\nQUERY: ",str);
            ISFSArray sfsArray = dbManager.executeQuery(str, new Object[] {});
            if(sfsArray.size() > 0)
            {
                sendResult(sender, params, RESPONSE_ALREADY_EXISTS);
                return;
            }

            // Case 2:
            // Invitation code is used
            /*
            str = "SELECT `invitation_code` FROM `friendship` WHERE `invitation_code`="+ invitationCode;
            trace("\nQUERY: ", str);
            sfsArray = dbManager.executeQuery(str, new Object[] {});
            if(sfsArray.size() > 0)
            {
                sendResult(sender, params, REPETITIVE_INVITATION_CODE);
                return;
            }
            */

            // Case 3:
            // User wants to add himself to get rewards
            if (game.player.id == inviteeId)
            {
                sendResult(sender, params, USER_ADD_HIMSELF);
                return;
            }

            // Case 4:
            // Invitation code is invalid
            if(PasswordGenerator.getInvitationCode(game.player.id) != invitationCode)
            {
                sendResult(sender, params, INVALID_INVITATION_CODE);
                return;
            }

            // Case 5:
            // Invitee player has been already added to inviter's friend list
            str = "SELECT `invitee_id` FROM `friendship` WHERE `inviter_id`="+ game.player.id +" AND `invitee_id`="+ inviteeId;
            trace("QUERY: ", str);
            sfsArray = dbManager.executeQuery(str, new Object[]{});
            if(sfsArray.size() > 0)
            {
                sendResult(sender, params, ALREADY_ADDED);
                return;
            }

            // Case 6:
            // Users can't get rewards and be friends with another user with the same UDID
            str = "SELECT COUNT( * ) FROM `devices` WHERE `player_id` = "+ game.player.id +" OR `player_id` = "+ inviteeId +"GROUP BY udid\n" + "HAVING COUNT( * ) >1";
            trace("QUERY: ", str);
            sfsArray = dbManager.executeQuery(str, new Object[]{});
            if(sfsArray.size() > 0)
            {
                sendResult(sender, params, ANOTHER_USER_SAME_PHONE);
                return;
            }

            // Case 7:
            // The inviter has been invited by the invitee player before
            str = "SELECT `invitee_id` FROM `friendship` WHERE `inviter_id`="+ inviteeId +" AND `invitee_id`="+ game.player.id;
            trace("QUERY: ", str);
            sfsArray = dbManager.executeQuery(str, new Object[]{});
            if(sfsArray.size() > 0)
            {
                sendResult(sender, params, ALREADY_FRIEND);
                return;
            }

            // If nothing is wrong Insert to friendship
            str = "INSERT INTO `friendship` (`inviter_id`, `invitee_id`, `invitation_code`, `inviter_reward`, `invitee_reward`) VALUES ("+ game.player.id +", "+ inviteeId +", '"+ invitationCode +"', "+ inviterReward +", "+ inviteeReward +")";
            trace("\nINPUT string to DB:",str);
            dbManager.executeInsert(str, new Object[] {});

        } catch (SQLException e) {
            params.putBool("succeed", false);
            params.putText("errorCode", e.getErrorCode()+"");
            trace(e.getMessage());
        }
    }

    private void sendResult(User sender, ISFSObject params, int responseCode)
    {
        params.putInt("responseCode", responseCode);
        trace(params.getDump());
        send("addFriend", params, sender );
    }
}