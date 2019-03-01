package com.gt.utils;

import com.smartfoxserver.v2.SmartFoxServer;
import com.smartfoxserver.v2.db.IDBManager;
import com.smartfoxserver.v2.entities.data.ISFSArray;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

/**
 * Created by ManJav on 8/20/2017.
 */
public class OneSignalUtils extends UtilBase
{
    public static OneSignalUtils getInstance()
    {
        return (OneSignalUtils)UtilBase.get(OneSignalUtils.class);
    }

    public String getPushId(int playerId)
    {
        Integer [] players = {playerId};
        return getPushIds(players).get(0);
    }
    public List<String> getPushIds(Integer[] playerIds)
    {
        List<String> ret = new ArrayList<>();
        IDBManager dbManager = SmartFoxServer.getInstance().getZoneManager().getZoneByName("towers").getExtension().getParentZone().getDBManager();
        try{

            int len = playerIds.length;
            String queryStr= "SELECT os_pid FROM pushtokens WHERE ";
            for ( int i=0; i < len; i++ )
            {
                queryStr += "player_id=" + playerIds[i];
                queryStr += ( i == len-1 ) ? (" LIMIT " + len) : " OR ";
            }
            ISFSArray sfsArray = dbManager.executeQuery(queryStr, new Object[]{});
            for ( int i=0; i<sfsArray.size(); i++ )
                ret.add(i, sfsArray.getSFSObject(i).getText("os_pid"));

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return ret;
    }

    public void send(String message, String data, int playerId)
    {
        Integer [] players = {playerId};
        send(message, data, players );
    }

    public int send(String message, String data, Integer[] players )
    {
        List<String> pushIds = getPushIds(players);//["6392d91a-b206-4b7b-a620-cd68e32c3a76","76ece62b-bcfe-468c-8a78-839aeaa8c5fa","8e0f21fa-9a5a-4ae7-a9a6-ca1f24294b86"]
        if( pushIds.size() == 0 )
        {
            System.out.println( "receivers id not found." );
            return -1;
        }

        String pushIdsStr = "[\"" + String.join("\", \"", pushIds) + "\"]";

        try
        {
            if(data == null)
                data = "{}";//{"foo": "bar"}

            String jsonResponse;

            URL url = new URL("https://onesignal.com/api/v1/notifications");
            HttpURLConnection con = (HttpURLConnection)url.openConnection();
            con.setUseCaches(false);
            con.setDoOutput(true);
            con.setDoInput(true);

            con.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
            con.setRequestProperty("Authorization", "NzM4YjM3MmItZDUyMy00MzA3LWJhZGEtNGUwNGEyZTU2Njg");
            con.setRequestMethod("POST");

            String strJsonBody = "{"
                    +   "\"app_id\": \"83cdb330-900e-4494-82a8-068b5a358c18\", "
                    +   "\"include_player_ids\": " + pushIdsStr + ", "
                    +   "\"data\": " + data + ", "
                    +   "\"contents\": {\"en\": \"" + message + "\"}"
                    + "}";


            System.out.println("strJsonBody:\n" + strJsonBody);

            byte[] sendBytes = strJsonBody.getBytes("UTF-8");
            con.setFixedLengthStreamingMode(sendBytes.length);

            OutputStream outputStream = con.getOutputStream();
            outputStream.write(sendBytes);

            int httpResponse = con.getResponseCode();
            System.out.println("httpResponse: " + httpResponse);

            if (  httpResponse >= HttpURLConnection.HTTP_OK && httpResponse < HttpURLConnection.HTTP_BAD_REQUEST )
            {
                Scanner scanner = new Scanner(con.getInputStream(), "UTF-8");
                jsonResponse = scanner.useDelimiter("\\A").hasNext() ? scanner.next() : "";
                scanner.close();
            }
            else
            {
                Scanner scanner = new Scanner(con.getErrorStream(), "UTF-8");
                jsonResponse = scanner.useDelimiter("\\A").hasNext() ? scanner.next() : "";
                scanner.close();
            }
            System.out.println("jsonResponse:\n" + jsonResponse);

        } catch(Throwable t)
        {
            t.printStackTrace();
        }
        return 0;
    }
}
