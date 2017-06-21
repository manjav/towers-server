package com.gerantech.towers.sfs.utils;

import java.sql.SQLException;

import com.gt.towers.Player;
import com.smartfoxserver.v2.db.IDBManager;
import com.smartfoxserver.v2.entities.data.ISFSArray;
import com.smartfoxserver.v2.entities.data.SFSArray;
import com.smartfoxserver.v2.exceptions.SFSErrorCode;
import com.smartfoxserver.v2.exceptions.SFSException;
import com.smartfoxserver.v2.extensions.ISFSExtension;
import com.smartfoxserver.v2.extensions.SFSExtension;

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
	
	public static SFSArray getQuests(ISFSExtension extension, long playerId) throws SFSException, SQLException 
	{
		IDBManager dbManager = extension.getParentZone().getDBManager();
    	return (SFSArray) dbManager.executeQuery("SELECT `index`,`score` FROM quests WHERE player_id="+playerId, new Object[] {});
	}

	public static ISFSArray getExchanges(SFSExtension extension, long playerId)throws SFSException, SQLException 
	{
		IDBManager dbManager = extension.getParentZone().getDBManager();
		SFSArray exchanges = (SFSArray) dbManager.executeQuery("SELECT `type`,`num_exchanges`,`expired_at`,`outcome` FROM exchanges WHERE player_id="+playerId, new Object[] {});

//		ISFSObject element;
//		long now = Instant.now().toEpochMilli();
//		for(int i=0; i<exchanges.size(); i++)
//		{
//			element = exchanges.getSFSObject(i);
//			
//			// bonus items :
//			if( ShopItemType.getCategory(element.getInt("type")) == ShopItemType.S_20_BUILDING )
//			{
//				if( element.getLong("expired_at") < now )
//				{
//					element.putLong("expired_at", now+Exchanger.S_21_BUILDING_TIME );
//					element.putInt("outcome", 0 );
//					element.putInt("num_exchanges", 0 );
//		      		dbManager.executeUpdate("UPDATE `exchanges` SET `expired_at`='" + new java.sql.Timestamp(now+Exchanger.S_21_BUILDING_TIME) + "', `num_exchanges`='" + 0 + "' WHERE `type`=" + element.getInt("type") + " AND `player_id`=" + playerId + ";", new Object[] {});
//				}
//			}
//		}
		return exchanges;
	}
	public static void updateExchange(SFSExtension extension, int type, int playerId, long expireAt, int numExchanges, int outcome) throws SQLException
	{
		IDBManager dbManager = extension.getParentZone().getDBManager();
		dbManager.executeUpdate("UPDATE `exchanges` SET `expired_at`='" + new java.sql.Timestamp(expireAt) + "', `num_exchanges`='" + numExchanges + "', `outcome`='" + outcome + "' WHERE `type`=" + type + " AND `player_id`=" + playerId + ";", new Object[] {});
	}
	
	
	public static void setQuestScore(ISFSExtension extension, Player player, int index, int score) throws SFSException, SQLException 
	{
		IDBManager dbManager = extension.getParentZone().getDBManager();

      	if( player.get_quests().exists( index ) )
      		dbManager.executeUpdate("UPDATE `quests` SET `score`='" + score + "' WHERE `index`=" + index + " AND `player_id`=" + player.get_id() + ";", new Object[] {});
      	else
      		dbManager.executeInsert("INSERT INTO quests (`index`, `player_id`, `score`) VALUES ('" + index + "', '" + player.get_id() + "', '" + score + "');", new Object[] {});
	}

	public static String updateResources(ISFSExtension extension, Player player, int[] keys) throws SFSException, SQLException 
	{
		IDBManager dbManager = extension.getParentZone().getDBManager();
		
		String query = "Update resources SET count = CASE\r";
		
        int keyLen = keys.length;
        int r = 0;
        while( r < keyLen )
        {
        	query += "WHEN type = " + keys[r] + " AND player_id = " + player.get_id() + " THEN " + player.get_resources().get(keys[r]) + "\r";
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
        return query;
    }
	
	public static void upgradeBuilding(SFSExtension extension, Player player, int type, int level) throws SQLException
	{
		IDBManager dbManager = extension.getParentZone().getDBManager();
  		dbManager.executeUpdate("UPDATE `resources` SET `level`='" + level + "' WHERE `type`=" + type + " AND `player_id`=" + player.get_id() + ";", new Object[] {});
	}




}