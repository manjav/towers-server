package com.gerantech.towers.sfs.inbox;

import com.gerantech.towers.sfs.Commands;
import com.smartfoxserver.v2.SmartFoxServer;
import com.smartfoxserver.v2.entities.User;
import com.smartfoxserver.v2.entities.data.ISFSArray;
import com.smartfoxserver.v2.entities.data.SFSArray;
import com.smartfoxserver.v2.entities.data.SFSObject;
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

    public int send(int type, String text, String sender, int senderId, int receiverId, String data)
    {
        int ret = 0;
        String query = "INSERT INTO `inbox`(`type`, `text`, `sender`, `senderId`, `receiverId`, `data`, `utc`) VALUES " +
                "(" + type + ",'" + text + "','" + sender + "'," + senderId + "," + receiverId + ",'" + data + "'," + Instant.now().getEpochSecond() + ")";
        int messageId = 0;
        try {
            messageId = Math.toIntExact((Long)ext.getParentZone().getDBManager().executeInsert(query, new Object[]{}));
        } catch (SQLException e) {  e.printStackTrace(); }

        // send message to online users
        User receiver = ext.getParentZone().getUserManager().getUserByName(receiverId+"");
        if( receiver != null ) {
            SFSObject params = new SFSObject();
            SFSArray mssages = new SFSArray();
            SFSObject msg = new SFSObject();
            msg.putInt("id", messageId);
            msg.putShort("read", (short)0);
            msg.putShort("type", (short)type);
            msg.putUtfString("text", text);
            msg.putUtfString("sender", sender);
            msg.putInt("senderId", senderId);
            msg.putInt("receiverId", receiverId);
            msg.putText("data", data);
            msg.putInt("utc", (int)Instant.now().getEpochSecond());
            mssages.addSFSObject(msg);
            params.putSFSArray("data", mssages);
            ext.send(Commands.INBOX_GET, params, receiver);
            ret = 1;
        }
        return ret;
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