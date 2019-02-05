package com.gerantech.towers.sfs.battle.handlers;

import com.gerantech.towers.sfs.battle.BattleRoom;
import com.gt.towers.battle.BattleField;
import com.smartfoxserver.v2.entities.User;
import com.smartfoxserver.v2.entities.data.ISFSObject;
import com.smartfoxserver.v2.extensions.BaseClientRequestHandler;

public class BattleSummonRequestHandler extends BaseClientRequestHandler
{
	public void handleClientRequest(User sender, ISFSObject params)
	{
		BattleRoom battleRoom = (BattleRoom) getParentExtension().getParentRoom().getExtension();
		if( battleRoom.getState() < BattleField.STATE_1_CREATED || battleRoom.getState() > BattleField.STATE_2_STARTED )
			return;
		int side = battleRoom.getPlayerGroup(sender);
		battleRoom.summonUnit(side, params.getInt("t"), params.getDouble("x"), params.getDouble("y"));
	}
}