package com.gerantech.towers.sfs.socials.handlers;

import com.gerantech.towers.sfs.handlers.BattleAutoJoinHandler;
import com.gt.towers.constants.MessageTypes;
import com.smartfoxserver.v2.entities.Room;
import com.smartfoxserver.v2.entities.User;
import com.smartfoxserver.v2.entities.data.ISFSObject;
import com.smartfoxserver.v2.extensions.BaseClientRequestHandler;

import com.gt.towers.constants.FriendlyBattleStates;

/**
 * Created by Babak on 9/4/2017.
 */
public class BuddyFriendlyBattleRequestHandler extends BaseClientRequestHandler
{
    public static final int STATE_REQUEST = 0;
    public static final int STATE_REQUEST_STARTED = 1;
    public static final int STATE_REQUEST_END = 2;
    public static final int STATE_REQUEST_CANCELED = 3;

    private static final int STATE_IN_GAME = 1;
    private static final int STATE_NOT_IN_GAME = 0;


    public void handleClientRequest(User sender, ISFSObject params)
    {
        Integer friendlyBattleId = params.getInt("bid");
        if (params.getShort("st") == STATE_REQUEST)
        {
            Room room = BattleAutoJoinHandler.makeNewRoom(getApi(), getParentExtension().getParentZone(), sender, false, 0);
            room.setProperty("isFriendly", true);
            BattleAutoJoinHandler.join(getApi(), sender, room, "");//params.getText("bsu")
            params.putInt("bid", room.getId());

            if(params.getShort("in") == STATE_IN_GAME)
            {
                // send pop up massage to join the game
                params.putInt("sendPopup", 1);
            }
            else if(params.getShort("in") == STATE_NOT_IN_GAME )
            {
                // push notification to join the game
            }
        }
        else if(params.getShort("st") == STATE_REQUEST_STARTED)
        {
            Room room = getParentExtension().getParentZone().getRoomById(friendlyBattleId);
            room.setProperty("isFriendly", true);
            BattleAutoJoinHandler.join(getApi(), sender, room, "");
            params.putInt("bid", room.getId());
        }
        else if(params.getShort("st") == STATE_REQUEST_END)
        {
            Room room = getParentExtension().getParentZone().getRoomById(friendlyBattleId);
            getApi().removeRoom(room);
        }
        else if(params.getShort("st") == STATE_REQUEST_CANCELED)
        {
            Room room = getParentExtension().getParentZone().getRoomById(friendlyBattleId);
            getApi().removeRoom(room);
        }
    }
}
