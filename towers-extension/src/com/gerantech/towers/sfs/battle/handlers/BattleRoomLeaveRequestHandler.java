package com.gerantech.towers.sfs.battle.handlers;

import com.gerantech.towers.sfs.battle.BattleRoom;
import com.smartfoxserver.v2.entities.User;
import com.smartfoxserver.v2.entities.data.ISFSObject;
import com.smartfoxserver.v2.extensions.BaseClientRequestHandler;

public class BattleRoomLeaveRequestHandler extends BaseClientRequestHandler
{

	public void handleClientRequest(User sender, ISFSObject params)
	{
try {
		BattleRoom roomClass = (BattleRoom) getParentExtension().getParentRoom().getExtension();
		roomClass.leave(sender, params.containsKey("retryMode"));
} catch (Exception | Error e) { e.printStackTrace(); }
	}
}