package com.gerantech.towers.sfs.battle.handlers;

import com.gerantech.towers.sfs.battle.BattleRoom;
import com.smartfoxserver.v2.entities.User;
import com.smartfoxserver.v2.entities.data.ISFSObject;
import com.smartfoxserver.v2.extensions.BaseClientRequestHandler;

public class BattleRoomFightRequestHandler extends BaseClientRequestHandler
{
	public void handleClientRequest(User sender, ISFSObject params)
	{
try{
		BattleRoom roomClass = (BattleRoom) getParentExtension().getParentRoom().getExtension();
		roomClass.fight(params.getSFSArray("s"), params.getInt("d"), false, 0.5);
} catch (Exception | Error e) { e.printStackTrace(); }
	}
}