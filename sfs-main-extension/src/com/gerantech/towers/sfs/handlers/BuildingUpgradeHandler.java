package com.gerantech.towers.sfs.handlers;

import com.gerantech.towers.sfs.Commands;
import com.gerantech.towers.sfs.callbacks.MapChangeCallback;
import com.gerantech.towers.sfs.utils.UserManager;
import com.gt.towers.Game;
import com.gt.towers.Player;
import com.gt.towers.buildings.Building;
import com.gt.towers.utils.maps.IntIntMap;
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
	
	public BuildingUpgradeHandler() {}

	public void handleClientRequest(User sender, ISFSObject params)
    {
    	int buildingType = (int)params.getInt("type");
    	int confirmedHards = (int)params.getInt("confirmedHards");
		Player player = ((Game)sender.getSession().getProperty("core")).player;
		Building building = player.buildings.get(buildingType);
		
		trace(building.improveLevel, building.get_level(), building.type, building.get_upgradeRewards().keys()[0], building.get_upgradeRewards().values()[0]);
		
  		MapChangeCallback mapChangeCallback = new MapChangeCallback();
		player.resources.changeCallback = mapChangeCallback;
		boolean success = building.upgrade(confirmedHards);
		player.resources.changeCallback = null;

		if( !success )
		{
			trace(ExtensionLogLevel.WARN, "building " + buildingType + " can not upgrade to level " + building.get_level());
			return;
		}
		try {
			trace(UserManager.upgradeBuilding(getParentExtension(), player, buildingType, building.get_level()));
			trace(UserManager.updateResources(getParentExtension(), player, mapChangeCallback.updates));
		} catch (Exception e) {
			e.printStackTrace();
			return;
		}
		trace(ExtensionLogLevel.INFO, "building " + buildingType + " upgraded to " + building.get_level() );
		send(Commands.BUILDING_UPGRADE, params, sender);
    }
}