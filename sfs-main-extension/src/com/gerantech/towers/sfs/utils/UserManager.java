package com.gerantech.towers.sfs.utils;

import java.sql.SQLException;

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
	
	public static void addQuest(ISFSExtension extension, long playerId, int index, int score) throws SFSException 
	{
		IDBManager dbManager = extension.getParentZone().getDBManager();
        try
        {
    		String query = "INSERT INTO quests (`index`, `player_id`, `score`) VALUES ('" + index + "', '" + playerId + "', '" + score + "');";
    		dbManager.executeInsert(query, new Object[] {});
    	}
        catch (SQLException e)
		{
        	Logger.warn(SFSErrorCode.GENERIC_ERROR, "SQL Failed", e.toString());
		}
	}

}
