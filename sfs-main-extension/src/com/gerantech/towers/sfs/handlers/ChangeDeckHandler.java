package com.gerantech.towers.sfs.handlers;

import com.gerantech.towers.sfs.Commands;
import com.gt.towers.Game;
import com.gt.towers.Player;
import com.smartfoxserver.v2.entities.User;
import com.smartfoxserver.v2.entities.data.ISFSObject;
import com.smartfoxserver.v2.extensions.BaseClientRequestHandler;

import java.sql.SQLException;

/**
 * Created by Babak on 17/11/29.
 */
public class ChangeDeckHandler extends BaseClientRequestHandler
{
    public void handleClientRequest(User sender, ISFSObject params)
    {
        trace(params.getDump());
        Player player = ((Game) sender.getSession().getProperty("core")).player;

        int response = 0;
        if( !player.buildings.exists(params.getShort("type")) )
            response = -2;

        if( response >= 0 )
            response = change(player, params.getShort("deckIndex"), params.getShort("index"), params.getShort("type")) ? 0 :-1;

        params.putInt("response", response);
        send(Commands.CHANGE_DECK, params, sender);
    }

    public boolean change( Player player, int deckIndex, int index, int type)
    {
        player.decks.get(deckIndex).set(index, type);
        try {
            String query = "UPDATE towers_db.decks SET decks.`type` = "+ type +" WHERE " +
                    "NOT EXISTS (SELECT 1 FROM (" +
                    "SELECT 1 FROM towers_db.decks WHERE decks.player_id = "+ player.id +" AND decks.deck_index = "+ deckIndex +" AND decks.`type` = "+ type +") as c1)" +
                    "AND decks.player_id = "+ player.id +" AND decks.deck_index = "+ deckIndex +" AND decks.`index` = " + index;

            trace(query);
            getParentExtension().getParentZone().getDBManager().executeUpdate(query, new Object[]{});
            return true;
        }
        catch (SQLException e) { e.printStackTrace(); }
        return false;
    }
}
