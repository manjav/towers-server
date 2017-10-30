package com.gerantech.towers.sfs.inbox;

import com.smartfoxserver.v2.SmartFoxServer;
import com.smartfoxserver.v2.entities.data.ISFSArray;
import com.smartfoxserver.v2.extensions.SFSExtension;

import java.sql.SQLException;
import java.time.Instant;

/**
 * Created by ManJav on 9/23/2017.
 */
public class InboxUtils
{
    private final SFSExtension ext;
    public InboxUtils()
    {
        ext = (SFSExtension) SmartFoxServer.getInstance().getZoneManager().getZoneByName("towers").getExtension();
    }
    public static InboxUtils getInstance()
    {
        return new InboxUtils();
    }

    public void send(int type, String text, String sender, int senderId, int receiverId, String data)
    {
        String query = "INSERT INTO `inbox`(`type`, `text`, `sender`, `senderId`, `receiverId`, `data`, `utc`) VALUES " +
                "(" + type + ",'" + text + "','" + sender + "'," + senderId + "," + receiverId + ",'" + data + "'," + Instant.now().getEpochSecond() + ")";
        try {
            ext.getParentZone().getDBManager().executeInsert(query, new Object[]{});
        } catch (SQLException e) {  e.printStackTrace(); }
    }

    public ISFSArray getAll(int receiverId)
    {
        String query = "SELECT * FROM `inbox` WHERE receiverId=" + receiverId + " ORDER BY utc DESC LIMIT 0,50";
        try {
            return ext.getParentZone().getDBManager().executeQuery(query, new Object[]{});
        } catch (SQLException e) {  e.printStackTrace(); }
        return null;
    }

    public void remove(int id)
    {
        try {
            ext.getParentZone().getDBManager().executeQuery("DELETE FROM `inbox` WHERE id=" + id, new Object[]{});
        } catch (SQLException e) {  e.printStackTrace(); }
    }

    public void open(int id)
    {
        try {
            ext.getParentZone().getDBManager().executeUpdate("UPDATE `inbox` SET `read`=1 WHERE id=" + id, new Object[]{});
        } catch (SQLException e) {  e.printStackTrace(); }
    }
}