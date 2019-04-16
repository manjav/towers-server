package com.gerantech.towers.sfs.handlers;

import com.gt.BBGClientRequestHandler;
import com.gt.Commands;
import com.gt.towers.Game;
import com.gt.towers.Player;
import com.gt.towers.constants.MessageTypes;
import com.gt.utils.DBUtils;
import com.smartfoxserver.v2.entities.User;
import com.smartfoxserver.v2.entities.data.ISFSObject;

/**
 * Created by ManJav on 17/11/29.
 */
public class ChangeDeckHandler extends BBGClientRequestHandler
{
    public void handleClientRequest(User sender, ISFSObject params)
    {
        trace(params.getDump());
        Player player = ((Game) sender.getSession().getProperty("core")).player;

        if( !player.cards.exists(params.getShort("type")) )
        {
            send(Commands.CHANGE_DECK, MessageTypes.RESPONSE_NOT_FOUND, params, sender);
            return;
        }

        if( player.decks.get(params.getShort("deckIndex")).existsValue(params.getShort("type")))
        {
            send(Commands.CHANGE_DECK, MessageTypes.RESPONSE_ALREADY_SENT, params, sender);
            return;
        }
        send(Commands.CHANGE_DECK, DBUtils.getInstance().updateDeck(player, params.getShort("deckIndex"), params.getShort("index"), params.getShort("type")), params, sender);
    }
}
