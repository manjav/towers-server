package com.gerantech.towers.sfs.handlers;

import com.gt.Commands;
import com.gt.utils.DBUtils;
import com.gt.towers.Game;
import com.gt.towers.Player;
import com.gt.towers.battle.units.Card;
import com.gt.callbacks.MapChangeCallback;
import com.smartfoxserver.v2.entities.User;
import com.smartfoxserver.v2.entities.data.ISFSObject;
import com.smartfoxserver.v2.extensions.BaseClientRequestHandler;
import com.smartfoxserver.v2.extensions.ExtensionLogLevel;

/**
 * @author ManJav
 *
 */
public class CardUpgradeHandler extends BaseClientRequestHandler
{
	public CardUpgradeHandler() {}
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
			trace(ExtensionLogLevel.WARN, "card " + cardType + " can not upgrade to level " + card.level);
			send(Commands.CARD_UPGRADE, params, sender);
			return;
		}
		DBUtils.getInstance().upgradeBuilding(player, cardType, card.level);
		DBUtils.getInstance().updateResources(player, mapChangeCallback.updates);
		trace(ExtensionLogLevel.INFO, "card " + cardType + " upgraded to " + card.level );
		send(Commands.CARD_UPGRADE, params, sender);
    }
}