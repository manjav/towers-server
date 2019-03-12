package com.gerantech.towers.sfs.handlers;

import com.gt.Commands;
import com.gt.callbacks.MapChangeCallback;
import com.gt.utils.DBUtils;
import com.gt.towers.Game;
import com.gt.towers.Player;
import com.gt.towers.buildings.Building;
import com.smartfoxserver.v2.entities.User;
import com.smartfoxserver.v2.entities.data.ISFSObject;
import com.smartfoxserver.v2.extensions.BaseClientRequestHandler;
import com.smartfoxserver.v2.extensions.ExtensionLogLevel;

/**
 * @author ManJav
 *
 */
public class BuildingUpgradeHandler extends BaseClientRequestHandler
{
	public void handleClientRequest(User sender, ISFSObject params)
    {
    	int buildingType = params.getInt("type");
    	int confirmedHards = params.getInt("confirmedHards");
		Player player = ((Game)sender.getSession().getProperty("core")).player;
		Building building = player.buildings.get(buildingType);

		//trace(building.improveLevel, building.get_level(), building.type, building.get_upgradeRewards().keys()[0], building.get_upgradeRewards().values()[0]);

  		MapChangeCallback mapChangeCallback = new MapChangeCallback();
		player.resources.changeCallback = mapChangeCallback;
		boolean success = building.upgrade(confirmedHards);
		player.resources.changeCallback = null;
		params.putBool("success", success);
		if( !success )
		{
			trace(ExtensionLogLevel.WARN, "building " + buildingType + " can not upgrade to level " + building.get_level());
			send(Commands.BUILDING_UPGRADE, params, sender);
			return;
		}
		DBUtils dbUtils = DBUtils.getInstance();
		dbUtils.upgradeBuilding(player, buildingType, building.get_level());
		dbUtils.updateResources(player, mapChangeCallback.updates);
		trace(ExtensionLogLevel.INFO, "building " + buildingType + " upgraded to " + building.get_level() );
		send(Commands.BUILDING_UPGRADE, params, sender);
    }
}