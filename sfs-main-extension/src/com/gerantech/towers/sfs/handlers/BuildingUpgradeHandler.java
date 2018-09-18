package com.gerantech.towers.sfs.handlers;

import com.gerantech.towers.sfs.Commands;
import com.gerantech.towers.sfs.callbacks.MapChangeCallback;
import com.gerantech.towers.sfs.utils.DBUtils;
import com.gt.towers.Game;
import com.gt.towers.Player;
import com.gt.towers.battle.units.Card;
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
    	int cardType = params.getInt("type");
    	int confirmedHards = params.getInt("confirmedHards");
		Player player = ((Game)sender.getSession().getProperty("core")).player;
		Card card = player.cards.get(cardType);

		trace(card.level, card.type, card.get_upgradeRewards().keys()[0], card.get_upgradeRewards().values()[0]);

  		MapChangeCallback mapChangeCallback = new MapChangeCallback();
		player.resources.changeCallback = mapChangeCallback;
		boolean success = card.upgrade(confirmedHards);
		player.resources.changeCallback = null;
		params.putBool("success", success);
		if( !success )
		{
			trace(ExtensionLogLevel.WARN, "building " + cardType + " can not upgrade to level " + card.level);
			send(Commands.BUILDING_UPGRADE, params, sender);
			return;
		}
		DBUtils dbUtils = DBUtils.getInstance();
		try {
			dbUtils.upgradeBuilding(player, cardType, card.level);
			dbUtils.updateResources(player, mapChangeCallback.updates);
		} catch (Exception e) {
			e.printStackTrace();
			return;
		}
		trace(ExtensionLogLevel.INFO, "building " + cardType + " upgraded to " + card.level );
		send(Commands.BUILDING_UPGRADE, params, sender);
    }
}