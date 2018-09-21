package com.gerantech.towers.sfs.handlers;

import com.gerantech.towers.sfs.Commands;
import com.gt.towers.Game;
import com.gt.towers.Player;
import com.gt.towers.constants.MessageTypes;
import com.smartfoxserver.v2.entities.User;
import com.smartfoxserver.v2.entities.data.ISFSObject;
import com.smartfoxserver.v2.extensions.BaseClientRequestHandler;

import java.sql.SQLException;

/**
 * Created by ManJav on 17/11/29.
 */
public class ChangeDeckHandler extends BaseClientRequestHandler
{
    public void handleClientRequest(User sender, ISFSObject params)
    {
        trace(params.getDump());
        Player player = ((Game) sender.getSession().getProperty("core")).player;

        if( !player.cards.exists(params.getShort("type")) )
        {
            sendResponse(MessageTypes.RESPONSE_NOT_FOUND, params, sender);
            return;
        }

        if( player.decks.get(params.getShort("deckIndex")).existsValue(params.getShort("type")))
        {
            sendResponse(MessageTypes.RESPONSE_ALREADY_SENT, params, sender);
            return;
        }
        sendResponse(change(player, params.getShort("deckIndex"), params.getShort("index"), params.getShort("type")), params, sender);
    }

    public int change(Player player, int deckIndex, int index, int type)
    {
        player.decks.get(deckIndex).set(index, type);
        try {
            String query = "UPDATE decks SET decks.`type` = "+ type +" WHERE " +
                    "NOT EXISTS (SELECT 1 FROM (" +
                    "SELECT 1 FROM decks WHERE decks.player_id = "+ player.id +" AND decks.deck_index = "+ deckIndex +" AND decks.`type` = "+ type +") as c1)" +
                    "AND decks.player_id = "+ player.id +" AND decks.deck_index = "+ deckIndex +" AND decks.`index` = " + index;

            trace(query);
            getParentExtension().getParentZone().getDBManager().executeUpdate(query, new Object[]{});
            return MessageTypes.RESPONSE_SUCCEED;
        }
        catch (SQLException e) { e.printStackTrace(); }
        return -1;
    }

    private void sendResponse(int response, ISFSObject params, User sender)
    {
        params.putInt("response", response);
        send(Commands.CHANGE_DECK, params, sender);
    }
}
