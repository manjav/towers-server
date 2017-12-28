package com.gerantech.towers.sfs.utils;

import com.gerantech.towers.sfs.socials.handlers.LobbyDataHandler;
import com.gt.hazel.RankData;
import com.gt.towers.Game;
import com.gt.towers.Player;
import com.gt.towers.constants.ExchangeType;
import com.gt.towers.constants.ResourceType;
import com.gt.towers.utils.maps.IntIntMap;
import com.hazelcast.com.eclipsesource.json.JsonObject;
import com.hazelcast.config.Config;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.IMap;
import com.smartfoxserver.v2.SmartFoxServer;
import com.smartfoxserver.v2.db.IDBManager;
import com.smartfoxserver.v2.entities.Room;
import com.smartfoxserver.v2.entities.User;
import com.smartfoxserver.v2.entities.data.ISFSArray;
import com.smartfoxserver.v2.entities.data.SFSArray;
import com.smartfoxserver.v2.exceptions.SFSErrorCode;
import com.smartfoxserver.v2.exceptions.SFSException;
import com.smartfoxserver.v2.extensions.ISFSExtension;
import com.smartfoxserver.v2.extensions.SFSExtension;
import net.sf.json.JSONObject;

import java.sql.SQLException;
import java.util.Collection;

/**
 * Created by ManJav on 12/4/2017.
 */
public class DBUtils
{
    private final SFSExtension ext;
    private final IDBManager db;

    public DBUtils()
    {
        ext = (SFSExtension) SmartFoxServer.getInstance().getZoneManager().getZoneByName("towers").getExtension();
        db = ext.getParentZone().getDBManager();
    }
    public static DBUtils getInstance() { return new DBUtils(); }

    // _-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-   RESOURCES  -_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_
    public SFSArray getResources(int playerId) throws SFSException
    {
        try
        {
            SFSArray ret = (SFSArray) db.executeQuery("SELECT type, count, level FROM resources WHERE player_id="+playerId, new Object[] {});
            if(ret.size() == 0)
            {
                //trace("name", name, "id", id, "password", password);
                //Logger.throwLoginException(SFSErrorCode.GENERIC_ERROR, "Reterive data error!", "user resources nou found.");
            }
            return ret;
        }
        catch (SQLException e)
        {
          //  Logger.throwLoginException(SFSErrorCode.GENERIC_ERROR, "SQL Failed", e.toString());
        }
        return null;
    }

    public void updateResources(Player player, IntIntMap resources) throws SFSException, SQLException
    {
        int[] keys = resources.keys();
        int keyLen = keys.length;
        if(keyLen == 0) {
            return;
        }

        String query = "Update resources SET count = CASE ";
        boolean hasRankFields = false;

        keyLen = keys.length;
        for (int i = 0; i < keyLen; i++)
        {
            if( !hasRankFields )
                hasRankFields = keys[i]== ResourceType.POINT || keys[i]==ResourceType.BATTLES_COUNT_WEEKLY;

            query += "WHEN type = " + keys[i] + " AND player_id = " + player.id + " THEN " + player.resources.get(keys[i]) + " ";
        }
        query += "ELSE count END WHERE type IN (";

        for (int i = 0; i < keyLen; i++)
            query += keys[i] + (i < keyLen-1 ? "," : "");

        query += ") AND player_id IN (";

        for (int i = 0; i < keyLen; i++)
            query += player.id + (i < keyLen-1 ? "," : "");

        query += ");";

        db.executeUpdate(query, new Object[] {});

        // update hazelcast map
        if( hasRankFields )
        {
            IMap<Integer, RankData> users = Hazelcast.getOrCreateHazelcastInstance(new Config("aaa")).getMap("users");
            RankData rd = new RankData(player.id, player.nickName,  player.get_point(), player.resources.get(ResourceType.BATTLES_COUNT_WEEKLY));
            query += "\ralso changed hazel map for id:"+player.id+" => point:"+ player.get_point()+", weeklyBattles:"+player.resources.get(ResourceType.BATTLES_COUNT_WEEKLY);

            if( users.containsKey(player.id))
                users.replace(player.id, rd);
            else
                users.put(player.id, rd);
        }
        ext.trace(query);
    }

    public void insertResources(Player player, IntIntMap resources) throws SQLException
    {
        int[] keys = resources.keys();
        int keyLen = keys.length;
        if(keyLen == 0) {
            return ;
        }

        String query = "INSERT INTO resources (`player_id`, `type`, `count`, `level`) VALUES ";
        keyLen = keys.length;
        for (int i = 0; i < keyLen; i++)
        {
            query += "('" + player.id + "', '" + keys[i] + "', '" + player.resources.get(keys[i]) + "', '" + (ResourceType.isBuilding(keys[i])?1:0) + "')";
            query += i < keyLen - 1 ? ", " : ";";
        }

        db.executeInsert(query, new Object[] {});
        ext.trace(query);
    }

    // _-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-   EXCHANGES  -_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_
    public ISFSArray getExchanges(int playerId)throws SFSException, SQLException
    {
        return db.executeQuery("SELECT `type`,`num_exchanges`,`expired_at`,`outcome` FROM exchanges WHERE player_id="+playerId, new Object[] {});
    }
    public void updateExchange(int type, int playerId, int expireAt, int numExchanges, int outcome) throws Exception
    {
        db.executeUpdate("UPDATE `exchanges` SET `expired_at`='" + expireAt + "', `num_exchanges`='" + numExchanges + "', `outcome`='" + outcome + "' WHERE `type`=" + type + " AND `player_id`=" + playerId + ";", new Object[] {});
    }

    // _-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-   QUESTS  -_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_
    public SFSArray getQuests(int playerId) throws SFSException, SQLException
    {
        return (SFSArray) db.executeQuery("SELECT `index`,`score` FROM quests WHERE player_id="+playerId, new Object[] {});
    }
    public void setQuestScore(Player player, int index, int score) throws SFSException, SQLException
    {
        if( player.quests.exists( index ) )
            db.executeUpdate("UPDATE `quests` SET `score`='" + score + "' WHERE `index`=" + index + " AND `player_id`=" + player.id + ";", new Object[] {});
        else
            db.executeInsert("INSERT INTO quests (`index`, `player_id`, `score`) VALUES ('" + index + "', '" + player.id + "', '" + score + "');", new Object[] {});
    }

    // _-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-   OTHERS  -_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_
    public void upgradeBuilding(Player player, int type, int level) throws SQLException
    {
        String query = "UPDATE `resources` SET `level`='" + level + "' WHERE `type`=" + type + " AND `player_id`=" + player.id + ";";
        db.executeUpdate(query, new Object[] {});
    }

    public SFSArray getDecks(int playerId) throws SQLException
    {
        return (SFSArray) db.executeQuery("SELECT * FROM decks WHERE player_id="+playerId, new Object[] {});
    }

    public ISFSArray getFriends(int playerId)throws SFSException, SQLException
    {
        String query = "SELECT players.id, players.name, resources.count FROM players INNER JOIN friendship ON players.id=friendship.invitee_id OR players.id=friendship.inviter_id INNER JOIN resources ON resources.type=1001 AND players.id=resources.player_id WHERE players.id!=" + playerId + " AND friendship.inviter_id=" + playerId + " OR friendship.invitee_id=" + playerId + " ORDER BY resources.count DESC LIMIT 0,100";
        ISFSArray result = db.executeQuery(query , new Object[] {});
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



    public String resetKeyExchanges()
    {
        String result = "";
        try {
            db.executeUpdate("UPDATE `exchanges` SET `num_exchanges`= 0 WHERE `type`=41 AND `num_exchanges` != 0;", new Object[] {});
        }
        catch (SQLException e)
        {
            //e.printStackTrace();
            return "Query failed";
        }

        Collection<User> users = ext.getParentZone().getUserList();
        for (User u : users)
        {
            ((Game)u.getSession().getProperty("core")).exchanger.items.get(ExchangeType.S_41_KEYS).numExchanges = 0;
            result += u.getName() + " key limit reset to '0'.\n";
        }

        return "Query succeeded.\n" + result;
    }

    public String resetWeeklyBattles()
    {
        String result = "\n";
        try {
            db.executeUpdate("UPDATE `resources` SET `count`= 0 WHERE `type`=1204 AND `count` != 0;", new Object[] {});
        }
        catch (SQLException e)
        {
            //e.printStackTrace();
            return "Query failed";
        }

        // update online users
        Collection<User> users = ext.getParentZone().getUserList();
        for (User u : users)
        {
            ((Game)u.getSession().getProperty("core")).player.resources.set(ResourceType.BATTLES_COUNT_WEEKLY, 0);
            result += u.getName() + " weekly battle reset to '0'.\n";
        }

        // update hazelcast
        IMap<Integer, RankData> usersMap = Hazelcast.getOrCreateHazelcastInstance(new Config("aaa")).getMap("users");
        ResetWeeklyEntryProcessor entryProcessor = new ResetWeeklyEntryProcessor();
        usersMap.executeOnEntries( entryProcessor );

        return "Query succeeded.\n" + result;
    }

    public String getPlayerNameById(int id)
    {
        try {
            String querystr = "SELECT name from players WHERE id = "+ id +" LIMIT 1";
            ISFSArray sfsArray = db.executeQuery( querystr, new Object[]{} );
            return sfsArray.getSFSObject(0).getUtfString("name");
        } catch (SQLException e) {
            e.printStackTrace();
            return "Player";
        }
    }
}