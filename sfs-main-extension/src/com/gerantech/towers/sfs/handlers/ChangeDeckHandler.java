package com.gerantech.towers.sfs.handlers;

import com.gt.towers.Game;
import com.smartfoxserver.v2.SmartFoxServer;
import com.smartfoxserver.v2.db.IDBManager;
import com.smartfoxserver.v2.entities.SFSZone;
import com.smartfoxserver.v2.entities.User;
import com.smartfoxserver.v2.entities.data.ISFSObject;
import com.smartfoxserver.v2.extensions.BaseClientRequestHandler;
import com.smartfoxserver.v2.extensions.SFSExtension;

import java.security.cert.Extension;
import java.sql.SQLException;

/**
 * Created by Babak on 17/11/29.
 */
public class ChangeDeckHandler extends BaseClientRequestHandler {

    public void handleClientRequest(User user, ISFSObject params) {
        int playerId = ((Game)user.getSession().getProperty("core")).player.id;
        int deckIndex = params.getShort("deckIndex");
        int index = params.getShort("index");
        int type = params.getShort("type");

        exec(getParentExtension().getParentZone().getDBManager(), playerId, deckIndex, index, type);
    }

    public static void exec(IDBManager dbManager, int playerId, int deckIndex, int index, int type)
    {
        try {
            String query = "UPDATE towers_db.decks SET decks.`type` = "+ type +" WHERE " +
                    "NOT EXISTS (SELECT 1 FROM (" +
                    "SELECT 1 FROM towers_db.decks WHERE decks.player_id = "+ playerId +" AND decks.deck_index = "+ deckIndex +" AND decks.`type` = "+ type +") as c1)" +
                    "AND decks.player_id = "+playerId+" AND decks.deck_index = "+ deckIndex +" AND decks.`index` = " + index;
            dbManager.executeUpdate(query, new Object[]{});
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
