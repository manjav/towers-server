package com.gerantech.towers.sfs.socials.handlers;

import com.gerantech.towers.sfs.Commands;
import com.gerantech.towers.sfs.TowerExtension;
import com.gerantech.towers.sfs.handlers.BattleAutoJoinHandler;
import com.gt.towers.Game;
import com.gt.towers.Player;
import com.gt.towers.constants.MessageTypes;
import com.smartfoxserver.v2.entities.Room;
import com.smartfoxserver.v2.entities.User;
import com.smartfoxserver.v2.entities.data.ISFSObject;
import com.smartfoxserver.v2.extensions.BaseClientRequestHandler;

/**
 * Created by ManJav on 8/24/2017.
 */
public class PublicMessageHandler extends BaseClientRequestHandler
{
    public void handleClientRequest(User sender, ISFSObject params)
    {
        // Friendly battle
        if( params.getShort("m") == MessageTypes.M30_FRIENDLY_BATTLE )
        {
            Integer friendlyBattleId = params.getInt("bid");
            if (friendlyBattleId == -1)
            {
                Room room = BattleAutoJoinHandler.makeNewRoom(getApi(), getParentExtension().getParentZone(), sender, false, 0);
                room.setProperty("isFriendly", true);
                BattleAutoJoinHandler.join(getApi(), sender, room, "");//params.getText("bsu")
                params.putInt("bid", room.getId());
            }
            else if (friendlyBattleId > -1)
            {
                Room room = getParentExtension().getParentZone().getRoomById(friendlyBattleId);
                if( params.getShort("st") == 3 )
                    getApi().removeRoom(room);
                else if( params.getShort("st") == 1 )
                    BattleAutoJoinHandler.join(getApi(), sender, room, "");//params.getText("bsu")
            }
        }

            /*else if ( friendlyBattleId == -2 )
            {
                trace("remove");
                sender.getBuddyProperties().setVariable(new SFSBuddyVariable("fbId", room.getId()));
                 getParentExtension().getBuddyApi().setBuddyVariables(sender, sender.getBuddyProperties().getVariables(), true, true);
            }
        }*/

        send(Commands.LOBBY_PUBLIC_MESSAGE, params, getParentExtension().getParentRoom().getUserList());
    }
}