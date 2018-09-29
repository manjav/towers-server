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
		int id = battleRoom.deployUnit(side, params.getInt("t"), params.getDouble("x"), params.getDouble("y"));
		if( id >= 0 )
		{
			Unit unit = battleRoom.battleField.units.get(id);
			params.putInt("s", side);
			params.putInt("l", unit.card.level);
			params.putInt("id", id);
			send(Commands.BATTLE_DEPLOY_UNIT, params, getParentExtension().getParentRoom().getUserList());
		}

	}
}