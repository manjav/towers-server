package com.gt.utils;

import com.smartfoxserver.v2.entities.data.ISFSArray;

import java.sql.SQLException;

/**
 * Created by ManJav on 9/23/2017.
 */
public class InboxUtils extends UtilBase
{
    public static InboxUtils getInstance()
    {
        return (InboxUtils)UtilBase.get(InboxUtils.class);
    }

    public int send(int type, String text, int senderId, int receiverId, String data)
    {
        int ret = 1;
        String query = "INSERT INTO messages(type, text, senderId, receiverId";
        if( data != null )
            query +=", data";
        query += ") VALUES (" + type + ",'" + text + "'," + senderId + "," + receiverId;
        if( data != null )
            query += ",'" + data + "'";
        query += ")";
        trace(query);

        int messageId = 0;
        try {
            messageId = Math.toIntExact((Long)ext.getParentZone().getDBManager().executeInsert(query, new Object[]{}));
        } catch (SQLException e) { e.printStackTrace(); }

        // send message to online users
        /*User receiver = ext.getParentZone().getUserManager().getUserByName(receiverId+"");
        if( receiver != null ) {
            SFSObject params = new SFSObject();
            SFSArray messages = new SFSArray();
            SFSObject msg = new SFSObject();
            msg.putInt("id", messageId);
            msg.putInt("read", 0);
            msg.putInt("type", type);
            msg.putUtfString("text", text);
            //msg.putUtfString("sender", sender);
            msg.putInt("senderId", senderId);
            msg.putInt("receiverId", receiverId);
            msg.putText("data", data);
            msg.putInt("utc", (int)Instant.now().getEpochSecond());
            messages.addSFSObject(msg);
            params.putSFSArray("data", messages);
            ext.send(Commands.INBOX_GET_RELATIONS, params, receiver);
            ret = 1;
        }*/
        return ret;
    }

    public ISFSArray getThreads(int playerId, int limit)
    {
        String query = "SELECT messages.text, messages.senderId, players.name as sender, messages.status, messages.timestamp FROM messages INNER JOIN players ON messages.senderId = players.id WHERE messages.id IN " +
                "( SELECT MAX(messages.id) FROM messages WHERE messages.receiverId=" + playerId + " OR messages.receiverId=1000 GROUP BY messages.senderId) " +
                "ORDER BY messages.id DESC LIMIT " + limit;
        ISFSArray inbox = null;
        try {
            inbox = ext.getParentZone().getDBManager().executeQuery(query, new Object[]{});
        } catch (SQLException e) {  e.printStackTrace(); }
        //trace(query);
        query = "SELECT messages.text, messages.receiverId, players.name as receiver, messages.status, messages.timestamp FROM messages INNER JOIN players ON messages.receiverId = players.id WHERE messages.id IN " +
                "( SELECT MAX(messages.id) FROM messages WHERE messages.senderId=" + playerId + " GROUP BY messages.receiverId ) " +
                "ORDER BY messages.id DESC;";
        ISFSArray outbox = null;
        try {
            outbox = ext.getParentZone().getDBManager().executeQuery(query, new Object[]{});
        } catch (SQLException e) {  e.printStackTrace(); }
        //trace(query);

        for( int i = 0; i < inbox.size(); i++ )
        {
            if( removeDuplicate(inbox, outbox, i) == 1 )
            {
                i--;
                continue;
            }
        }

        for( int j = 0; j < outbox.size(); j++ )
            inbox.addSFSObject(outbox.getSFSObject(j));

        return inbox;
    }

    private int removeDuplicate(ISFSArray inbox, ISFSArray outbox, int i)
    {
        for( int j = 0; j < outbox.size(); j++ )
        {
            if( inbox.getSFSObject(i).getInt("senderId").equals(outbox.getSFSObject(j).getInt("receiverId")) )
            {
                if( inbox.getSFSObject(i).getLong("timestamp") < outbox.getSFSObject(j).getLong("timestamp") )
                {
                    inbox.removeElementAt(i);
                    outbox.getSFSObject(j).putInt("status", 1);
                    return 1;
                }
                outbox.removeElementAt(j);
                return 0;
            }
        }
        return -1;
    }

    public ISFSArray getRelations(int id_0, int id_1, int limit)
    {
        String query = "SELECT id, text, senderId, data, status, timestamp FROM messages WHERE (senderId = " + id_0 + " AND receiverId = " + id_1 + ") OR (senderId = " + id_1 + " AND receiverId = " + id_0 + ")";
        if( id_1 == 10000 )
            query += " OR receiverId = 1000";
        query += " LIMIT " + limit;
        ISFSArray messages = null;
        try {
            messages = ext.getParentZone().getDBManager().executeQuery(query, new Object[]{});
        } catch (SQLException e) { e.printStackTrace(); }
        return messages;
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