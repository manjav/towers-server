package com.gerantech.towers.sfs.handlers;

import com.gt.BBGClientRequestHandler;
import com.gt.Commands;
import com.gt.towers.Game;
import com.gt.towers.battle.units.Card;
import com.smartfoxserver.v2.entities.User;
import com.smartfoxserver.v2.entities.data.ISFSObject;

/**
 * @author ManJav
 *
 */
public class CardNewHandler extends BBGClientRequestHandler
{
	public CardNewHandler() {}
	public void handleClientRequest(User sender, ISFSObject params)
    {
		send(Commands.CARD_NEW, Card.addNew((Game)sender.getSession().getProperty("core"), params.getInt("c")), params, sender);
    }
}