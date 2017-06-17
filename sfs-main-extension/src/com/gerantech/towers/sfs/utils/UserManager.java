package com.gerantech.towers.sfs.utils;

import java.sql.SQLException;

import com.gt.towers.Player;
import com.gt.towers.utils.maps.Bundle;
import com.smartfoxserver.v2.db.IDBManager;
import com.smartfoxserver.v2.entities.data.SFSArray;
import com.smartfoxserver.v2.exceptions.SFSErrorCode;
import com.smartfoxserver.v2.exceptions.SFSException;
import com.smartfoxserver.v2.extensions.ISFSExtension;

public class UserManager {

	public static SFSArray getResources(ISFSExtension extension, long playerId) throws SFSException 
	{
		IDBManager dbManager = extension.getParentZone().getDBManager();
        try
        {
        	SFSArray ret = (SFSArray) dbManager.executeQuery("SELECT type, count, level FROM resources WHERE player_id="+playerId, new Object[] {});
	        if(ret.size() == 0)
	        {
	        	//trace("name", name, "id", id, "password", password);
	        	Logger.warn(SFSErrorCode.GENERIC_ERROR, "Reterive data error!", "user resources nou found.");
	        }
	        return ret;
        }
        catch (SQLException e)
		{
        	Logger.warn(SFSErrorCode.GENERIC_ERROR, "SQL Failed", e.toString());
		}
		return null;
	}
	
	public static SFSArray getQuests(ISFSExtension extension, long playerId) throws SFSException 
	{
		IDBManager dbManager = extension.getParentZone().getDBManager();
        try
        {
        	return (SFSArray) dbManager.executeQuery("SELECT `index`,`score` FROM quests WHERE player_id="+playerId, new Object[] {});
        }
        catch (SQLException e)
		{
        	Logger.warn(SFSErrorCode.GENERIC_ERROR, "SQL Failed", e.toString());
		}
		return null;
	}
	
	public static void setQuestScore(ISFSExtension extension, Player player, int index, int score) throws SFSException, SQLException 
	{
		IDBManager dbManager = extension.getParentZone().getDBManager();

      	if( player.get_quests().exists( index ) )
      		dbManager.executeUpdate("UPDATE `quests` SET `score`='" + score + "' WHERE `index`=" + index + " AND `player_id`=" + player.get_id() + ";", new Object[] {});
      	else
      		dbManager.executeInsert("INSERT INTO quests (`index`, `player_id`, `score`) VALUES ('" + index + "', '" + player.get_id() + "', '" + score + "');", new Object[] {});

	}

	public static void updateResources(ISFSExtension extension, Player player, Bundle outcomes, org.slf4j.Logger logger) throws SFSException, SQLException 
	{
		IDBManager dbManager = extension.getParentZone().getDBManager();
		
		String query = "Update resources SET count = CASE\r";
		
        int[] keys = outcomes.keys();
        int keyLen = keys.length;
        int r = 0;
        while( r < keyLen )
        {
        	query += "WHEN type = " + keys[r] + " AND player_id = " + player.get_id() + " THEN " + (player.get_resources().get(keys[r]) + outcomes.get(keys[r])) + "\r";
        	r ++;
        }
        query += "ELSE count END WHERE type IN (";
        
        r = 0;
        while( r < keyLen )
        {
        	query += keys[r] + (r < keyLen-1 ? "," : "");
        	r ++;
        }
        query += ") AND player_id IN (";
        
        r = 0;
        while( r < keyLen )
        {
        	query += player.get_id() + (r < keyLen-1 ? "," : "");
        	r ++;
        }
        query += ");";
        
		dbManager.executeUpdate(query, new Object[] {});
        
    }

}