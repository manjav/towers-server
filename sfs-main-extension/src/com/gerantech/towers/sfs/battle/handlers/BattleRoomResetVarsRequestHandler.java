package com.gerantech.towers.sfs.battle.handlers;

import com.gerantech.towers.sfs.Commands;
import com.gerantech.towers.sfs.battle.BattleRoom;
import com.gt.towers.buildings.Building;
import com.smartfoxserver.v2.entities.User;
import com.smartfoxserver.v2.entities.data.ISFSObject;
import com.smartfoxserver.v2.entities.data.SFSArray;
import com.smartfoxserver.v2.entities.data.SFSObject;
import com.smartfoxserver.v2.extensions.BaseClientRequestHandler;

public class BattleRoomResetVarsRequestHandler extends BaseClientRequestHandler
{
	public void handleClientRequest(User sender, ISFSObject params)
	{
		BattleRoom roomClass = (BattleRoom) getParentExtension().getParentRoom().getExtension();
		SFSArray buildingData = new SFSArray();
		for (int i = 0; i < roomClass.battleField.places.size(); i++)
		{
			Building b = roomClass.battleField.places.get(i).building;
			SFSObject bo = new SFSObject();
			bo.putInt("i", i);
			bo.putInt("t", b.type);
			bo.putInt("tt", b.troopType);
			bo.putInt("l", b.get_level());
			bo.putInt("p", b.get_population());
			buildingData.addSFSObject(bo);
		}
		params.putSFSArray("buildings", buildingData);
		send(Commands.RESET_ALL, params, sender);
	}
}