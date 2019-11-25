package com.gt.utils;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import com.smartfoxserver.v2.entities.data.ISFSArray;

/**
 * Firebase Cloud Messaging Utility for sending messages using our FCM Token.
 */
public class FCMUtils extends UtilBase implements IPushUtils
{
    private Properties props = ConfigUtils.getInstance().load(ConfigUtils.DEFAULT);
    public static FCMUtils getInstance()
    {
        return (FCMUtils)UtilBase.get(FCMUtils.class);
    }

    public String getPushId(int playerId)
    {
        Integer [] players = {playerId};
        return getPushIds(players).get(0);
    }

    public List<String> getPushIds(Integer[] playerIds)
    {
        List<String> ret = new ArrayList<>();
        try {
            int len = playerIds.length;
            String queryStr= "SELECT fcm_token FROM pushtokens WHERE ";
            for ( int i=0; i < len; i++ )
            {
                queryStr += "player_id=" + playerIds[i];
                queryStr += ( i == len-1 ) ? (" LIMIT " + len) : " OR ";
            }
            ISFSArray sfsArray = ext.getParentZone().getDBManager().executeQuery(queryStr, new Object[]{});
            for ( int i=0; i<sfsArray.size(); i++ )
                ret.add(i, sfsArray.getSFSObject(i).getText("fcm_token"));

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return ret;
    }

    public void send(String message, String data, int playerId)
    {
        Integer [] players = {playerId};
        send(message, data, players);
    }

    public int send(String message, String data, Integer[] players )
    {
        int delivered = 0;
        List<String> pushIds = getPushIds(players);
        if( pushIds.size() == 0 )
        {
            // TODO: requires log as warn after 2203-logs merge.
            // getLogger().warn("Push Notification | Unable to find reciverIds.");
            return 0;
        }

        for( String pushId : pushIds )
        {
            String fcmServerKey = props.getProperty("fcmServerKey");
            try
            {
                URL url = new URL("https://fcm.googleapis.com/fcm/send");
                HttpURLConnection con = (HttpURLConnection)url.openConnection();
                con.setUseCaches(false);
                con.setDoOutput(true);
                con.setDoInput(true);

                con.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
                con.setRequestProperty("Authorization", "key=" + fcmServerKey);
                con.setRequestMethod("POST");

                String strJsonBody = "{"
                        +   "\"to\": \"" + pushId + "\", "
                        +   "\"notification\":"
                        +   "{ \"body\": \"" + message + "\""
                        +     "\"sound\": \"default\" }"
                        + "}";
                byte[] sendBytes = strJsonBody.getBytes("UTF-8");
                con.setFixedLengthStreamingMode(sendBytes.length);

                OutputStream outputStream = con.getOutputStream();
                outputStream.write(sendBytes);
                int httpResponse = con.getResponseCode();
                if(  httpResponse >= HttpURLConnection.HTTP_OK && httpResponse < HttpURLConnection.HTTP_BAD_REQUEST )
                    delivered += 1;
            }
            catch(Throwable t)
            {
                t.printStackTrace();
            }
        }
        return delivered;
    }
}