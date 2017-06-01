package com.gerantech.towers.sfs.battle.handlers;

import com.gerantech.towers.sfs.battle.BattleRoom;
import com.smartfoxserver.v2.entities.User;
import com.smartfoxserver.v2.entities.data.ISFSObject;
import com.smartfoxserver.v2.extensions.BaseClientRequestHandler;

public class BattleRoomImproveRequestHandler extends BaseClientRequestHandler
{

	public void handleClientRequest(User sender, ISFSObject params)
	{
		BattleRoom roomClass = (BattleRoom) getParentExtension().getParentRoom().getExtension();
		roomClass.improveBuilding(sender, params);
	}
}