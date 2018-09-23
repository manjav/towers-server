package com.gerantech.towers.sfs.battle.handlers;

import com.gerantech.towers.sfs.Commands;
import com.gerantech.towers.sfs.battle.BattleRoom;
import com.smartfoxserver.v2.entities.User;
import com.smartfoxserver.v2.entities.data.ISFSObject;
import com.smartfoxserver.v2.extensions.BaseClientRequestHandler;

public class BattleDeployRequestHandler extends BaseClientRequestHandler
{
	public void handleClientRequest(User sender, ISFSObject params)
	{
		params.putInt("id", ((BattleRoom) getParentExtension().getParentRoom().getExtension()).deployUnit(params.getInt("t"), sender));
		send(Commands.BATTLE_DEPLOY_UNIT, params, sender);
	}
}