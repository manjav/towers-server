package com.gerantech.towers.sfs.utils;

import com.gt.data.RankData;;
import com.gt.towers.Game;
import com.gt.towers.Player;
import com.gt.towers.constants.ExchangeType;
import com.gt.towers.constants.ResourceType;
import com.gt.towers.utils.maps.IntIntMap;
import com.hazelcast.config.Config;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.IMap;
import com.smartfoxserver.v2.SmartFoxServer;
import com.smartfoxserver.v2.db.IDBManager;
import com.smartfoxserver.v2.entities.User;
import com.smartfoxserver.v2.entities.data.ISFSArray;
import com.smartfoxserver.v2.entities.data.SFSArray;
import com.smartfoxserver.v2.exceptions.SFSException;
import com.smartfoxserver.v2.extensions.SFSExtension;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

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

    public void updateResources(Player player, IntIntMap resources)
    {
        int[] keys = resources.keys();
        int keyLen = keys.length;
        List<Integer> res = new ArrayList();
        for (int i = 0; i < keyLen; i++)
            if( !ResourceType.isBook(keys[i]) )
                res.add(keys[i]);

        keyLen = res.size();
        if( keyLen == 0 )
            return;

        String query = "Update resources SET count = CASE ";
        boolean hasRankFields = false;

        for (int i = 0; i < keyLen; i++)
        {
            if( !hasRankFields )
                hasRankFields = res.get(i) == ResourceType.POINT || res.get(i) == ResourceType.BATTLES_COUNT_WEEKLY;

            query += "WHEN type = " + res.get(i) + " AND player_id = " + player.id + " THEN " + player.resources.get(res.get(i)) + " ";
        }
        query += "ELSE count END WHERE type IN (";

        for (int i = 0; i < keyLen; i++)
            query += res.get(i) + (i < keyLen-1 ? "," : "");

        query += ") AND player_id IN (";

        for (int i = 0; i < keyLen; i++)
            query += player.id + (i < keyLen-1 ? "," : "");

        query += ");";

        try {
            db.executeUpdate(query, new Object[] {});
        } catch (SQLException e) { e.printStackTrace(); }

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
        List<Integer> res = new ArrayList();
        for (int i = 0; i < keyLen; i++)
            if( !ResourceType.isBook(keys[i]) )
                res.add(keys[i]);

        keyLen = res.size();
        if( keyLen == 0 )
            return;

        String query = "INSERT INTO resources (`player_id`, `type`, `count`, `level`) VALUES ";
        for (int i = 0; i < keyLen; i++)
        {
            if( ResourceType.isBook(res.get(i)) )
                continue;
            query += "('" + player.id + "', '" + res.get(i) + "', '" + player.resources.get(res.get(i)) + "', '" + (ResourceType.isBuilding(res.get(i))?player.buildings.get(res.get(i)).get_level():0) + "')";
            query += i < keyLen - 1 ? ", " : ";";
        }
        if( query == "INSERT INTO resources (`player_id`, `type`, `count`, `level`) VALUES " )
            return;
        db.executeInsert(query, new Object[] {});
        ext.trace(query);
    }

    // _-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-   EXCHANGES  -_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_
    public ISFSArray getExchanges(int playerId)
    {
        ISFSArray ret = null;
        try {
            ret = db.executeQuery("SELECT type, num_exchanges, expired_at, outcome, reqs FROM exchanges WHERE player_id=" + playerId + " OR player_id=10000", new Object[]{});
        } catch (SQLException e) { e.printStackTrace(); }
        return ret;
    }
    public void updateExchange(int type, int playerId, int expireAt, int numExchanges, String outcomesStr, String reqsStr) throws Exception
    {
        String query = "SELECT _func_exchanges(" + type + "," + playerId + "," + numExchanges + "," + expireAt + ",'" + outcomesStr + "', '" + reqsStr + "')";
        db.executeQuery(query, new Object[] {});

        /*
        DROP FUNCTION IF EXISTS _func_exchanges;

        DELIMITER $$
        CREATE FUNCTION _func_exchanges (_type INT(5), _playerId INT(11), _numExchanges INT(4), _expiredAt INT(11), _outcomeStr VARCHAR(32)) RETURNS INT
        BEGIN
            DECLARE var_resp INT DEFAULT 0;

            IF EXISTS (SELECT * FROM exchanges WHERE type=_type AND player_id=_playerId) THEN
                UPDATE exchanges SET num_exchanges=_numExchanges, expired_at=_expiredAt, outcome=_outcomeStr WHERE type=_type AND player_id=_playerId;
                SET var_resp = 1;
            ELSE
                INSERT INTO exchanges (type, player_id, num_exchanges, expired_at, outcome) VALUES (_type, _playerId, _numExchanges, _expiredAt, _outcomeStr);
                SET var_resp = 2;
            END IF;

            RETURN var_resp;

        END $$
        DELIMITER ;
        */
    }

    // _-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-   OPERATIONS  -_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_
    public ISFSArray getOperations(int playerId)
    {
        ISFSArray ret = null;
        try {
            ret = db.executeQuery("SELECT `index`,`score` FROM operations WHERE player_id=" + playerId, new Object[]{});
        } catch (SQLException e) { e.printStackTrace(); }
        return ret;
    }
    public void setOperationScore(Player player, int index, int score) throws SQLException
    {
        if( player.operations.exists( index ) )
            db.executeUpdate("UPDATE `operations` SET `score`='" + score + "' WHERE `index`=" + index + " AND `player_id`=" + player.id + ";", new Object[] {});
        else
            db.executeInsert("INSERT INTO operations (`index`, `player_id`, `score`) VALUES ('" + index + "', '" + player.id + "', '" + score + "');", new Object[] {});
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

    public ISFSArray getFriends(int playerId)throws SQLException
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


    public ISFSArray getPrefs(int id, int appVersion)
    {
        ISFSArray ret = null;
        try {
            ret = db.executeQuery("SELECT k,v FROM userprefs WHERE player_id=" + id, new Object[]{});
        } catch (SQLException e) { e.printStackTrace(); }

        if( appVersion >= 2500 )
        {
            for( int i=0; i < ret.size(); i ++ )
            {
                if( ret.getSFSObject(i).getText("k").equals("101") )
                {
                    if( ret.getSFSObject(i).getUtfString("v").equals("111") )
                        ret.getSFSObject(i).putUtfString("v", "141");
                    else if( ret.getSFSObject(i).getUtfString("v").equals("113") )
                        ret.getSFSObject(i).putUtfString("v", "151");
                    else if( ret.getSFSObject(i).getUtfString("v").equals("115") || ret.getSFSObject(i).getUtfString("v").equals("116") || ret.getSFSObject(i).getUtfString("v").equals("118") )
                        ret.getSFSObject(i).putUtfString("v", "182");
                }
            }
        }

        return ret;
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
            ((Game)u.getSession().getProperty("core")).exchanger.items.get(ExchangeType.C41_KEYS).numExchanges = 0;
            result += u.getName() + " key limit reset to '0'.\n";
        }

        return "Query succeeded.\n" + result;
    }

    public String resetWeeklyBattles()
    {
        String result = "\n";
        try {
            db.executeUpdate("UPDATE `resources` SET `count`= 0 WHERE `type`=1204 AND `count` != 0;", new Object[]{});
        } catch (SQLException e) { return "Query failed";}

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