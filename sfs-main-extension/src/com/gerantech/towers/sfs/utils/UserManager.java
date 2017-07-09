package com.gerantech.towers.sfs.utils;

import java.sql.SQLException;

import com.gerantech.towers.sfs.TowerExtension;
import com.gt.hazel.RankData;
import com.gt.towers.Player;
import com.gt.towers.constants.ResourceType;
import com.hazelcast.core.IMap;
import com.smartfoxserver.v2.db.IDBManager;
import com.smartfoxserver.v2.entities.data.ISFSArray;
import com.smartfoxserver.v2.entities.data.SFSArray;
import com.smartfoxserver.v2.exceptions.SFSErrorCode;
import com.smartfoxserver.v2.exceptions.SFSException;
import com.smartfoxserver.v2.extensions.ISFSExtension;
import com.smartfoxserver.v2.extensions.SFSExtension;

public class UserManager {

	public static SFSArray getResources(ISFSExtension extension, int playerId) throws SFSException 
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
	
	public static SFSArray getQuests(ISFSExtension extension, int playerId) throws SFSException, SQLException 
	{
		IDBManager dbManager = extension.getParentZone().getDBManager();
    	return (SFSArray) dbManager.executeQuery("SELECT `index`,`score` FROM quests WHERE player_id="+playerId, new Object[] {});
	}

	public static ISFSArray getExchanges(SFSExtension extension, int playerId)throws SFSException, SQLException 
	{
		IDBManager dbManager = extension.getParentZone().getDBManager();
		return (SFSArray) dbManager.executeQuery("SELECT `type`,`num_exchanges`,`expired_at`,`outcome` FROM exchanges WHERE player_id="+playerId, new Object[] {});
	}
	public static void updateExchange(SFSExtension extension, int type, int playerId, int expireAt, int numExchanges, int outcome) throws SQLException
	{
		IDBManager dbManager = extension.getParentZone().getDBManager();
		dbManager.executeUpdate("UPDATE `exchanges` SET `expired_at`='" + expireAt + "', `num_exchanges`='" + numExchanges + "', `outcome`='" + outcome + "' WHERE `type`=" + type + " AND `player_id`=" + playerId + ";", new Object[] {});
	}
	
	public static void setQuestScore(ISFSExtension extension, Player player, int index, int score) throws SFSException, SQLException 
	{
		IDBManager dbManager = extension.getParentZone().getDBManager();

      	if( player.quests.exists( index ) )
      		dbManager.executeUpdate("UPDATE `quests` SET `score`='" + score + "' WHERE `index`=" + index + " AND `player_id`=" + player.id + ";", new Object[] {});
      	else
      		dbManager.executeInsert("INSERT INTO quests (`index`, `player_id`, `score`) VALUES ('" + index + "', '" + player.id + "', '" + score + "');", new Object[] {});
	}

	public static String updateResources(ISFSExtension extension, Player player, int[] keys) throws SFSException, SQLException 
	{
		if(keys.length == 0)
			return null;
		
		boolean hasRankFields = false;
		IDBManager dbManager = extension.getParentZone().getDBManager();
		String query = "Update resources SET count = CASE\r";
	
        int keyLen = keys.length;
        int r = 0;
        while( r < keyLen )
        {
        	hasRankFields = keys[r]==ResourceType.XP || keys[r]==ResourceType.POINT;
        	query += "WHEN type = " + keys[r] + " AND player_id = " + player.id + " THEN " + player.resources.get(keys[r]) + "\r";
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
        	query += player.id + (r < keyLen-1 ? "," : "");
        	r ++;
        }
        query += ");";
        
		dbManager.executeUpdate(query, new Object[] {});
        
        // update hazelcast map
		if( hasRankFields )
        {
        	IMap<Integer, RankData> users =  ((TowerExtension)extension).getHazelCast().getMap("users");
        	users.put(player.id, new RankData(player.id, player.nickName,  player.get_point(), player.get_xp()));
        }
		return query;
    }
	
	public static void upgradeBuilding(SFSExtension extension, Player player, int type, int level) throws SQLException
	{
		IDBManager dbManager = extension.getParentZone().getDBManager();
  		dbManager.executeUpdate("UPDATE `resources` SET `level`='" + level + "' WHERE `type`=" + type + " AND `player_id`=" + player.id + ";", new Object[] {});
	}
}