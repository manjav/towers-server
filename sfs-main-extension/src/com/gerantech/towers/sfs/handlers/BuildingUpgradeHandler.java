package com.gerantech.towers.sfs.handlers;

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
		Player player = ((Game)sender.getSession().getProperty("core")).player;
		Building building = player.buildings.get(buildingType);
		
		trace(building.improveLevel, building.level, building.type, building.get_upgradeRewards().keys()[0], building.get_upgradeRewards().values()[0]);
		
        IntIntMap reqs = building.get_upgradeRequirements();
        for(int r:building.get_upgradeRewards().keys())
        	reqs.set(r, 0);//add rewards to reqs

		trace(buildingType, building.level);
		if(!building.upgrade(0))
		{
			trace(ExtensionLogLevel.WARN, "building " + buildingType + "can not upgrade.");
			return;
		}
		trace(buildingType, building.level);
		try {
			UserManager.upgradeBuilding(getParentExtension(), player, buildingType, building.level);
			trace(UserManager.updateResources(getParentExtension(), player, reqs.keys()));
		} catch (Exception e) {
			trace(e.getMessage());
		}
    }
}