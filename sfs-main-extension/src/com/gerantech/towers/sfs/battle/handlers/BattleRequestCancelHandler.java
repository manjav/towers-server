package com.gerantech.towers.sfs.battle.handlers;

import com.gt.BBGRoom;
import com.gt.Commands;
import com.gt.towers.battle.BattleField;
import com.gt.utils.BattleUtils;
import com.smartfoxserver.v2.entities.User;
import com.smartfoxserver.v2.entities.data.ISFSObject;
import com.smartfoxserver.v2.extensions.BaseClientRequestHandler;

import java.util.Map;
import java.util.Set;

public class BattleRequestCancelHandler extends BaseClientRequestHandler
{
	public void handleClientRequest(User sender, ISFSObject params) {
        Set<Map.Entry<Integer, BBGRoom>> entries = BattleUtils.getInstance().rooms.entrySet();
        BBGRoom foundRoom = null;
        for (Map.Entry<Integer, BBGRoom> entry : entries)
        {
            if( entry.getValue().getOwner().equals(sender) )
            {
                if( entry.getValue().getPropertyAsInt("state") < BattleField.STATE_1_CREATED )
                {
                    foundRoom = entry.getValue();
                    break;
                }
            }
        }

        if( foundRoom != null )
            BattleUtils.getInstance().remove(foundRoom);
        send(Commands.BATTLE_CANCEL, null, sender);
    }
}