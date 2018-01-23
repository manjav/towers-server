package com.gerantech.towers.sfs.socials.handlers;

import com.gt.towers.Game;
import com.gt.towers.Player;
import com.smartfoxserver.v2.SmartFoxServer;
import com.smartfoxserver.v2.db.IDBManager;
import com.smartfoxserver.v2.entities.User;
import com.smartfoxserver.v2.entities.data.ISFSArray;
import com.smartfoxserver.v2.entities.data.ISFSObject;
import com.smartfoxserver.v2.extensions.BaseClientRequestHandler;
import com.smartfoxserver.v2.extensions.SFSExtension;

import java.sql.SQLException;

public class DonateRequestHandler extends BaseClientRequestHandler
{
    private final SFSExtension ext;
    private final IDBManager dbManager;

    public DonateRequestHandler()
    {
        ext = (SFSExtension) SmartFoxServer.getInstance().getExtensionManager().getExtensions();
        dbManager = ext.getParentZone().getDBManager();
    }

    public void handleClientRequest(User sender, ISFSObject params)
    {
        Player player = ((Game) sender.getSession().getProperty("core")).player;

        int cardType = params.getInt("cardType");
        int requesterId = params.getInt("requesterId");

        // get card from player
        updateCardNum(player.id, cardType, 1);

        // give card to requester
        updateCardNum(requesterId, cardType, -1);

    }

    private void updateCardNum(int playerId, int cardType, int value) {
        try {
            String query = "SELECT resources.count FROM resources WHERE resources.player_id = "+ playerId +" AND resources.`type` = "+ cardType;
            ISFSArray res = dbManager.executeQuery(query, new Object[]{});
            if (res.size() > 0 && res.getInt(0) > 0)
            {
                query = "UPDATE resources SET resources.count = "+ (res.getInt(0) + value) +" WHERE resources.player_id = "+ playerId +" AND resources.`type` = "+ cardType;
                dbManager.executeQuery(query, new Object[]{});
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

}
