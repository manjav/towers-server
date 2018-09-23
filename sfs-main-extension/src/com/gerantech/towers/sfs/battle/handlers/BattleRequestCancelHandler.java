package com.gerantech.towers.sfs.battle.handlers;
import com.gerantech.towers.sfs.Commands;
import com.gerantech.towers.sfs.battle.BattleRoom;
import com.smartfoxserver.v2.entities.Room;
import com.smartfoxserver.v2.entities.SFSRoomRemoveMode;
import com.smartfoxserver.v2.entities.User;
import com.smartfoxserver.v2.entities.data.ISFSObject;
import com.smartfoxserver.v2.extensions.BaseClientRequestHandler;

import java.util.List;

public class BattleRequestCancelHandler extends BaseClientRequestHandler
{
	public void handleClientRequest(User sender, ISFSObject params)
    {
        List<Room> battles = getParentExtension().getParentZone().getRoomListFromGroup("battles");
        int numBattles = battles.size()-1;
        while ( numBattles >= 0 )
        {
            //trace(battles.get(numBattles).getOwner().getId(), sender.getId(),  battles.get(numBattles).getProperty("state")); remove after deploy
            if( battles.get(numBattles).getOwner().equals(sender) )
            {
                if( (Integer) battles.get(numBattles).getProperty("state") <= BattleRoom.STATE_WAITING )
                {
                    battles.get(numBattles).setAutoRemoveMode(SFSRoomRemoveMode.WHEN_EMPTY);
                    getApi().leaveRoom(sender, battles.get(numBattles));
                    //getApi().removeRoom(battles.get(numBattles));
                    send(Commands.BATTLE_CANCEL, null, sender);
                    return;
                }
                break;
            }
            numBattles --;
        }

        // not found any related battle
        if( numBattles <= -1 )
            send(Commands.BATTLE_CANCEL, null, sender);

    }
}