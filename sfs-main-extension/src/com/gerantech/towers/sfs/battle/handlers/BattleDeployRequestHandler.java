package com.gerantech.towers.sfs.battle.handlers;

import com.gerantech.towers.sfs.Commands;
import com.gerantech.towers.sfs.battle.BattleRoom;
import com.gt.towers.battle.units.Unit;
import com.smartfoxserver.v2.entities.User;
import com.smartfoxserver.v2.entities.data.ISFSObject;
import com.smartfoxserver.v2.extensions.BaseClientRequestHandler;

public class BattleDeployRequestHandler extends BaseClientRequestHandler
{
	public void handleClientRequest(User sender, ISFSObject params)
	{
		BattleRoom battleRoom = (BattleRoom) getParentExtension().getParentRoom().getExtension();
		int side = battleRoom.getPlayerGroup(sender);
		battleRoom.deployUnit(side, params.getInt("t"), params.getDouble("x"), params.getDouble("y"));
	}
}