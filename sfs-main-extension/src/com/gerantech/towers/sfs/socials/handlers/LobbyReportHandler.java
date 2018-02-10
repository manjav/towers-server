package com.gerantech.towers.sfs.socials.handlers;


import com.gerantech.towers.sfs.Commands;
import com.gt.towers.Game;
import com.gt.towers.Player;
import com.smartfoxserver.v2.db.IDBManager;
import com.smartfoxserver.v2.entities.User;
import com.smartfoxserver.v2.entities.data.ISFSArray;
import com.smartfoxserver.v2.entities.data.ISFSObject;
import com.smartfoxserver.v2.extensions.BaseClientRequestHandler;

import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;

/**
 * Created by ManJav on 2/7/2018.
 */
public class LobbyReportHandler extends BaseClientRequestHandler
{
    public void handleClientRequest(User sender, ISFSObject params)
    {
        Player reporter = ((Game) sender.getSession().getProperty("core")).player;
        String lobby = getParentExtension().getParentRoom().getName();
        IDBManager db = getParentExtension().getParentZone().getDBManager();
        ISFSArray infractions = null;

        // check report exists on db
        try {
            infractions = db.executeQuery("SELECT * From infractions WHERE content = '" + params.getUtfString("t") + "' AND lobby = '" + lobby + "' AND offender = " + params.getInt("i"), new Object[]{});
        } catch (SQLException e) { e.printStackTrace(); }
        if( infractions != null &&  infractions.size() > 0 )
        {
            sendResponse(sender, params, infractions.getSFSObject(0).getInt("reporter") == reporter.id ? 2 : 1);
            return;
        }

        // check reporter is verbose
        try {
            infractions = db.executeQuery("SELECT * From infractions WHERE report_at > '" + new Timestamp(Instant.now().toEpochMilli()-86400000).toString() + "' AND reporter = " + reporter.id, new Object[]{});
        } catch (SQLException e) { e.printStackTrace(); }
        if( infractions != null &&  infractions.size() > 5 )
        {
            sendResponse(sender, params, 3);
            send(Commands.LOBBY_REPORT, params, sender);
            return;
        }

        // insert report
        Timestamp reportAt = new Timestamp(Instant.now().toEpochMilli());
        Timestamp offendAt = new Timestamp(params.getInt("u")*1000l);
        try {
            db.executeInsert( "INSERT INTO `infractions` (`reporter`, `offender`, `content`, `lobby`, `offend_at`, `report_at`) VALUES ('" + reporter.id + "', '" + params.getInt("i") + "', '" + params.getUtfString("t") + "', '" + lobby + "', '" + offendAt.toString() + "', '" + reportAt.toString() + "');", new Object[]{});
        } catch (SQLException e) { e.printStackTrace(); }
        sendResponse(sender, params, 0);
    }

    private void sendResponse(User sender, ISFSObject params, int response)
    {
        params.putInt("response", response);
        params.removeElement("t");
        send(Commands.LOBBY_REPORT, params, sender);
    }
}