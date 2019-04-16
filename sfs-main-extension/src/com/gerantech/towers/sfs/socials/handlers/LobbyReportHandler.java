package com.gerantech.towers.sfs.socials.handlers;

import com.gt.Commands;
import com.gt.towers.Game;
import com.gt.towers.Player;
import com.gt.towers.constants.MessageTypes;
import com.smartfoxserver.v2.db.IDBManager;
import com.smartfoxserver.v2.entities.User;
import com.smartfoxserver.v2.entities.data.ISFSArray;
import com.smartfoxserver.v2.entities.data.ISFSObject;
import com.smartfoxserver.v2.extensions.BaseClientRequestHandler;

import java.sql.SQLException;
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
        String query = "SELECT * From infractions WHERE content = '" + params.getUtfString("t") + "' AND lobby = '" + lobby + "' AND offender = " + params.getInt("i");
        // trace(query);
        try {
            infractions = db.executeQuery(query, new Object[]{});
        } catch (SQLException e) { e.printStackTrace(); }
        if( infractions != null &&  infractions.size() > 0 )
        {
            send(sender, params, infractions.getSFSObject(0).getInt("reporter") == reporter.id ? 2 : 1);
            return;
        }

        // check reporter is verbose
        //trace(query);
        query = "SELECT * From infractions WHERE report_at > FROM_UNIXTIME(" + (Instant.now().getEpochSecond() - 86400)+ ") AND reporter = " + reporter.id;
        try {
            infractions = db.executeQuery(query, new Object[]{});
        } catch (SQLException e) { e.printStackTrace(); }
        if( infractions != null &&  infractions.size() > 4 )
        {
            send(sender, params, 3);
            return;
        }

        // insert report
        query = "INSERT INTO `infractions` (`reporter`, `offender`, `content`, `lobby`, `offend_at`, `report_at`) VALUES ('" + reporter.id + "', '" + params.getInt("i") + "', '" + params.getUtfString("t") + "', '" + lobby + "', FROM_UNIXTIME(" + params.getInt("u") + "), FROM_UNIXTIME(" + Instant.now().getEpochSecond() + "));";
        //trace(query);
        try {
            db.executeInsert(query , new Object[]{});
        } catch (SQLException e) { e.printStackTrace(); }
        send(sender, params, MessageTypes.RESPONSE_SUCCEED);
    }

    protected void send(User sender, ISFSObject params, int response)
    {
        params.putInt("response", response);
        params.removeElement("t");
        params.removeElement("u");
        super.send(Commands.LOBBY_REPORT, params, sender);
    }
}