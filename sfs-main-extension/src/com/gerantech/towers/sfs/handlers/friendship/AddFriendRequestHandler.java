package com.gerantech.towers.sfs.handlers.friendship;

import com.gt.towers.Game;
import com.smartfoxserver.v2.db.IDBManager;
import com.smartfoxserver.v2.entities.User;
import com.smartfoxserver.v2.entities.data.ISFSArray;
import com.smartfoxserver.v2.entities.data.ISFSObject;
import com.smartfoxserver.v2.entities.data.SFSObject;
import com.smartfoxserver.v2.extensions.BaseClientRequestHandler;

import java.sql.SQLException;

/**
 * Created by Babak on 8/19/2017.
 */
public class AddFriendRequestHandler extends BaseClientRequestHandler {

    private int RESPONSE_ALREADY_EXISTS = -1;
    private int REPETITIVE_INVITATION_CODE = -2;
    private int USER_ADD_HIMSELF = -3;
    private int WRONGE_INVITATION_CODE = -4;
    private int ALREADY_FRIEND = -5;
    private int ALREADY_ADDED = -6;


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
            String str = "SELECT `invitee_id` FROM `friendship` WHERE `invitee_id`="+ inviteeId +" AND `inviter_reward`="+ 1;
            trace("\nQUERY: ",str);
            ISFSArray sfsArray = dbManager.executeQuery(str, new Object[] {});
            if(sfsArray.size() > 0)
            {
                sendResult(sender, params, RESPONSE_ALREADY_EXISTS);
                return;
            }

            str = "SELECT `invitation_code` FROM `friendship` WHERE `invitation_code`="+ invitationCode;
            trace("\nQUERY: ", str);
            sfsArray = dbManager.executeQuery(str, new Object[] {});
            if(sfsArray.size() > 0)
            {
                sendResult(sender, params, REPETITIVE_INVITATION_CODE);
                return;
            }

            if (game.player.id == inviteeId)
            {
                sendResult(sender, params, USER_ADD_HIMSELF);
                return;
            }
    /*
            if(!algorithmCheck(invitationCode))
            {
                sendResult(sender, params, WRONGE_INVITATION_CODE);
                return;
            }
    */
            str = "SELECT `invitee_id` FROM `friendship` WHERE `inviter_id`="+ game.player.id +" AND `invitee_id`="+ inviteeId;
            trace("QUERY: ", str);
            sfsArray = dbManager.executeQuery(str, new Object[]{});
            if(sfsArray.size() > 0)
            {
                sendResult(sender, params, ALREADY_ADDED);
                return;
            }

            str = "SELECT `invitee_id` FROM `friendship` WHERE `inviter_id`="+ inviteeId +" AND `invitee_id`="+ game.player.id;
            trace("QUERY: ", str);
            sfsArray = dbManager.executeQuery(str, new Object[]{});
            if(sfsArray.size() > 0)
            {
                sendResult(sender, params, ALREADY_FRIEND);
                return;
            }
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