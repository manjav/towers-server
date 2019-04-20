package com.gerantech.towers.sfs.handlers;

import com.gt.BBGClientRequestHandler;
import com.gt.Commands;
import com.gt.towers.Game;
import com.gt.towers.battle.units.Card;
import com.gt.towers.constants.MessageTypes;
import com.gt.towers.utils.maps.IntIntMap;
import com.gt.utils.DBUtils;
import com.smartfoxserver.v2.entities.User;
import com.smartfoxserver.v2.entities.data.ISFSObject;

/**
 * @author ManJav
 */
public class CardNewHandler extends BBGClientRequestHandler
{
	public CardNewHandler() {}
	public void handleClientRequest(User sender, ISFSObject params)
    {
    	Game game = (Game)sender.getSession().getProperty("core");
    	int response = Card.addNew(game, params.getInt("c"));
    	if( response == MessageTypes.RESPONSE_SUCCEED )
			DBUtils.getInstance().insertResources(game.player, new IntIntMap(params.getInt("c") + ":1"));
		send(Commands.CARD_NEW, response, params, sender);
    }
}