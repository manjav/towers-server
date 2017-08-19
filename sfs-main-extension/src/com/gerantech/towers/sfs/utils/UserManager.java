package com.gerantech.towers.sfs.utils;

import com.gt.hazel.RankData;
import com.gt.towers.Player;
import com.gt.towers.constants.ResourceType;
import com.gt.towers.utils.maps.IntIntMap;
import com.hazelcast.config.Config;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.IMap;
import com.smartfoxserver.v2.db.IDBManager;
import com.smartfoxserver.v2.entities.data.ISFSArray;
import com.smartfoxserver.v2.entities.data.ISFSObject;
import com.smartfoxserver.v2.entities.data.SFSArray;
import com.smartfoxserver.v2.exceptions.SFSErrorCode;
import com.smartfoxserver.v2.exceptions.SFSException;
import com.smartfoxserver.v2.extensions.ISFSExtension;
import com.smartfoxserver.v2.extensions.SFSExtension;

import java.sql.SQLException;

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
	        	Logger.throwLoginException(SFSErrorCode.GENERIC_ERROR, "Reterive data error!", "user resources nou found.");
	        }
	        return ret;
        }
        catch (SQLException e)
		{
        	Logger.throwLoginException(SFSErrorCode.GENERIC_ERROR, "SQL Failed", e.toString());
		}
		return null;
	}
	
	public static SFSArray getQuests(ISFSExtension extension, int playerId) throws SFSException, SQLException 
	{
		IDBManager dbManager = extension.getParentZone().getDBManager();
    	return (SFSArray) dbManager.executeQuery("SELECT `index`,`score` FROM quests WHERE player_id="+playerId, new Object[] {});
	}

	public static ISFSArray getExchanges(ISFSExtension extension, int playerId)throws SFSException, SQLException
	{
		IDBManager dbManager = extension.getParentZone().getDBManager();
		return dbManager.executeQuery("SELECT `type`,`num_exchanges`,`expired_at`,`outcome` FROM exchanges WHERE player_id="+playerId, new Object[] {});
	}

	public static ISFSArray getFriends(SFSExtension extension, int playerId)throws SFSException, SQLException
	{
		IDBManager dbManager = extension.getParentZone().getDBManager();
		String query = "SELECT players.id, players.name, resources.count FROM players INNER JOIN friendship ON players.id=friendship.invitee_id OR players.id=friendship.inviter_id INNER JOIN resources ON resources.type=1001 AND players.id=resources.player_id WHERE players.id!=" + playerId + " AND friendship.inviter_id=" + playerId + " OR friendship.invitee_id=" + playerId + " ORDER BY resources.count DESC LIMIT 0,100";
		ISFSArray result = dbManager.executeQuery(query , new Object[] {});
		/*ISFSArray ret = new SFSArray();
		ISFSObject f;
		for ( int i=0; i<result.size(); i++)
		{
			f = result.getSFSObject(i);
			f.putUtfString("n", f.getUtfString("name")); f.removeElement("name");
			f.putInt("i", f.getInt("id")); f.removeElement("id");
			f.putInt("p", f.getInt("count")); f.removeElement("count");
			ret.addSFSObject(f);
		}*/
		return result;
	}


	public static String updateExchange(ISFSExtension extension, int type, int playerId, int expireAt, int numExchanges, int outcome) throws Exception
	{
		String query = "UPDATE `exchanges` SET `expired_at`='" + expireAt + "', `num_exchanges`='" + numExchanges + "', `outcome`='" + outcome + "' WHERE `type`=" + type + " AND `player_id`=" + playerId + ";";
		IDBManager dbManager = extension.getParentZone().getDBManager();
		dbManager.executeUpdate(query, new Object[] {});
		return query;
	}
	
	public static void setQuestScore(ISFSExtension extension, Player player, int index, int score) throws SFSException, SQLException 
	{
		IDBManager dbManager = extension.getParentZone().getDBManager();

      	if( player.quests.exists( index ) )
      		dbManager.executeUpdate("UPDATE `quests` SET `score`='" + score + "' WHERE `index`=" + index + " AND `player_id`=" + player.id + ";", new Object[] {});
      	else
      		dbManager.executeInsert("INSERT INTO quests (`index`, `player_id`, `score`) VALUES ('" + index + "', '" + player.id + "', '" + score + "');", new Object[] {});
	}

	public static String updateResources(ISFSExtension extension, Player player, IntIntMap resources) throws SFSException, SQLException
	{
		int[] keys = resources.keys();
		int keyLen = keys.length;
		if(keyLen == 0) {
			return "";
		}

		IDBManager dbManager = extension.getParentZone().getDBManager();
		String query = "Update resources SET count = CASE ";
		boolean hasRankFields = false;

        keyLen = keys.length;
		for (int i = 0; i < keyLen; i++)
        {
        	if( !hasRankFields )
        		hasRankFields = keys[i]==ResourceType.XP || keys[i]==ResourceType.POINT;

        	query += "WHEN type = " + keys[i] + " AND player_id = " + player.id + " THEN " + player.resources.get(keys[i]) + " ";
        }
        query += "ELSE count END WHERE type IN (";

		for (int i = 0; i < keyLen; i++)
        	query += keys[i] + (i < keyLen-1 ? "," : "");

        query += ") AND player_id IN (";

		for (int i = 0; i < keyLen; i++)
        	query += player.id + (i < keyLen-1 ? "," : "");

        query += ");";

		dbManager.executeUpdate(query, new Object[] {});

        // update hazelcast map
		if( hasRankFields )
        {
			IMap<Integer, RankData> users = Hazelcast.getOrCreateHazelcastInstance(new Config("aaa")).getMap("users");
        	RankData rd = new RankData(player.id, player.nickName,  player.get_point(), player.get_xp());
        	query += "\rid:"+player.id+", nickName:"+player.nickName+", point:"+ player.get_point()+", xp:"+player.get_xp();

        	if( users.containsKey(player.id))
        		users.replace(player.id, rd);
        	else
        		users.put(player.id, rd);
        }
		return query;
    }
	public static String insertResources(ISFSExtension extension, Player player, IntIntMap resources) throws SQLException
	{
		int[] keys = resources.keys();
		int keyLen = keys.length;
		if(keyLen == 0) {
			return "";
		}

		IDBManager dbManager = extension.getParentZone().getDBManager();
		String query = "INSERT INTO resources (`player_id`, `type`, `count`, `level`) VALUES ";

		keyLen = keys.length;
		for (int i = 0; i < keyLen; i++)
		{
			query += "('" + player.id + "', '" + keys[i] + "', '" + resources.get(keys[i]) + "', '" + (ResourceType.isBuilding(keys[i])?1:0) + "')";
			query += i < keyLen - 1 ? ", " : ";";
		}

		dbManager.executeInsert(query, new Object[] {});
		return query;
	}


	public static void upgradeBuilding(SFSExtension extension, Player player, int type, int level) throws SQLException
	{
		IDBManager dbManager = extension.getParentZone().getDBManager();
  		dbManager.executeUpdate("UPDATE `resources` SET `level`='" + level + "' WHERE `type`=" + type + " AND `player_id`=" + player.id + ";", new Object[] {});
	}

}