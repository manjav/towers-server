package com.gerantech.towers.sfs.utils;

import com.smartfoxserver.v2.entities.data.ISFSArray;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import com.smartfoxserver.v2.db.IDBManager;
import com.smartfoxserver.v2.extensions.ISFSExtension;
import org.apache.http.util.TextUtils;

import java.sql.SQLException;

/**
 * Created by ManJav on 8/20/2017.
 */
public class OneSignalUtils
{
    public static void addPlayerId(ISFSExtension extension, int playerId, String oneSignalId)
    {
        IDBManager dbManager = extension.getParentZone().getDBManager();
        try{
            String str = "INSERT INTO pushtokens (player_id, push_id) VALUES ("+playerId+", '"+oneSignalId+"') ON DUPLICATE KEY UPDATE push_id = VALUES(push_id)";
            dbManager.executeInsert(str, new Object[]{});
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static String getPushId(ISFSExtension extension, int playerId)
    {
        int [] players = {playerId};
        return getPushIds(extension, players).get(0);
    }
    public static List<String> getPushIds(ISFSExtension extension, int[] playerIds)
    {
        List<String> ret = new ArrayList<>();
        IDBManager dbManager = extension.getParentZone().getDBManager();
        try{

            int len = playerIds.length;
            String queryStr= "SELECT push_id FROM pushtokens WHERE ";
            for ( int i=0; i < len; i++ )
            {
                queryStr += "player_id=" + playerIds[i];
                queryStr += ( i == len-1 ) ? (" LIMIT " + len) : " OR ";
            }
            ISFSArray sfsArray = dbManager.executeQuery(queryStr, new Object[]{});
            for ( int i=0; i<sfsArray.size(); i++ )
                ret.add(i, sfsArray.getSFSObject(i).getText("push_id"));

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return ret;
    }

    public static void send(ISFSExtension extension, String message, String data, int playerId )
    {
        int [] players = {playerId};
        send( extension, message, data, players );
    }

    public static void send(ISFSExtension extension, String message, String data, int[] players )
    {
        List<String> pushIds = getPushIds(extension, players);//["6392d91a-b206-4b7b-a620-cd68e32c3a76","76ece62b-bcfe-468c-8a78-839aeaa8c5fa","8e0f21fa-9a5a-4ae7-a9a6-ca1f24294b86"]
        String pushIdsStr = "[\"" + String.join("\", \"", pushIds) + "\"]";

        try
        {
            if(data == null)
                data = "";//{"foo": "bar"}

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
                    +   "\"app_id\": \"83cdb330-900e-4494-82a8-068b5a358c18\","
                    +   "\"include_player_ids\": " + pushIdsStr
                    +   "\"data\": ," + data
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
    }
}
