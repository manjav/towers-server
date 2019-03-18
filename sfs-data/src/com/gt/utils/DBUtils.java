package com.gt.utils;

import com.gt.data.RankData;
import com.gt.towers.Game;
import com.gt.towers.LoginData;
import com.gt.towers.Player;
import com.gt.towers.constants.ExchangeType;
import com.gt.towers.constants.MessageTypes;
import com.gt.towers.constants.ResourceType;
import com.gt.towers.utils.maps.IntIntMap;
import com.smartfoxserver.v2.db.IDBManager;
import com.smartfoxserver.v2.entities.Room;
import com.smartfoxserver.v2.entities.User;
import com.smartfoxserver.v2.entities.data.ISFSArray;
import com.smartfoxserver.v2.entities.data.ISFSObject;
import com.smartfoxserver.v2.entities.data.SFSArray;
import com.smartfoxserver.v2.entities.data.SFSObject;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by ManJav on 12/4/2017.
 */
public class DBUtils extends UtilBase
{
    private final IDBManager db;
    public DBUtils()
    {
        super();
        db = this.ext.getParentZone().getDBManager();
    }
    public static DBUtils getInstance()
    {
        return (DBUtils)UtilBase.get(DBUtils.class);
    }

    // _-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-   RESOURCES  -_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_
    public SFSArray getResources(int playerId)
    {
        SFSArray ret = new SFSArray();
        try {
            ret = (SFSArray) db.executeQuery("SELECT id, type, count, level FROM resources WHERE player_id = " + playerId, new Object[] {});
        } catch (SQLException e) { e.printStackTrace(); }
        return ret;
    }

    public void updateResources(Player player, IntIntMap resources)
    {
        int[] keys = resources.keys();
        int keyLen = keys.length;
        if( keyLen <= 0 )
            return;

        boolean hasRankFields = false;
        String log = "";
        for (int i = 0; i < keyLen; i++)
        {
            if( resources.get(keys[i]) == 0 || ResourceType.isBook(keys[i]) )
                continue;

            try {
                log += "(" + keys[i] + "," + player.resourceIds.get(keys[i]) + "," + player.resources.get(keys[i]) + ")  ";
                db.executeUpdate("UPDATE resources SET count = " + player.resources.get(keys[i]) + " WHERE id = " + player.resourceIds.get(keys[i]), new Object[] {});
            } catch (SQLException e) { e.printStackTrace(); }

            if( !hasRankFields )
                hasRankFields = keys[i] == ResourceType.R2_POINT;
        }

        // update ranking table
        if( hasRankFields )
        {
            ConcurrentHashMap<Integer, RankData> users = RankingUtils.getInstance().getUsers();
            RankData rd = new RankData(player.nickName, player.get_point());
//            log += " map for id:" + player.id + " => point:" + player.get_point();
            if( users.containsKey(player.id) )
                users.replace(player.id, rd);
            else
                users.put(player.id, rd);
        }
        trace(log);
    }

    public void insertResources(Player player, IntIntMap resources)
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
            query += "('" + player.id + "', '" + res.get(i) + "', '" + player.resources.get(res.get(i)) + "', '" + (ResourceType.isCard(res.get(i))?player.cards.get(res.get(i)).level:0) + "')";
            query += i < keyLen - 1 ? ", " : ";";
        }

        int id = 0;
        try{
            id = Math.toIntExact((long) db.executeInsert(query, new Object[] {}));
        } catch (SQLException e) { e.printStackTrace(); }
        for( int i = 0; i < keyLen; i++ )
            player.resourceIds.put(res.get(i), id + i);
        trace(query);
    }

    // _-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-   EXCHANGES  -_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_
    public ISFSArray getExchanges(int playerId)
    {
        ISFSArray ret = null;
        try {
            ret = db.executeQuery("SELECT id, type, num_exchanges, expired_at, outcome, reqs FROM exchanges WHERE player_id=" + playerId + " OR player_id=10000", new Object[]{});
        } catch (SQLException e) { e.printStackTrace(); }
        return ret;
    }
    public void updateExchange(Game game, int type, int expireAt, int numExchanges, String outcomesStr, String reqsStr)
    {
        String query;
        if( game.exchanger.dbItems.exists(type) )
        {
            query = "UPDATE exchanges SET num_exchanges=" + numExchanges + ", expired_at=" + expireAt + ", outcome='" + outcomesStr + "', reqs='" + reqsStr + "' WHERE id=" + game.exchanger.dbItems.get(type);
            try {
                db.executeUpdate(query, new Object[]{});
            } catch (SQLException e) { e.printStackTrace();}
            query += "   t:" + type + " p:" + game.player.id;
        }
        else
        {
            query = "INSERT INTO exchanges (type, player_id, num_exchanges, expired_at, outcome, reqs) VALUES (" + type + ", " + game.player.id + ", " + numExchanges + ", " + expireAt + ", '" + outcomesStr + "', '" + reqsStr + "');";
            long id = 0;
            try {
                id = (long) db.executeInsert(query, new Object[]{});
            } catch (SQLException e) { e.printStackTrace();}
            game.exchanger.dbItems.set(type, Math.toIntExact(id));
        }
        trace(query);
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
    public void setOperationScore(Player player, int index, int score)
    {
        try {
            if( player.operations.exists( index ) )
                db.executeUpdate("UPDATE `operations` SET `score`='" + score + "' WHERE `index`=" + index + " AND `player_id`=" + player.id + ";", new Object[] {});
            else
                db.executeInsert("INSERT INTO operations (`index`, `player_id`, `score`) VALUES ('" + index + "', '" + player.id + "', '" + score + "');", new Object[] {});
        } catch (Exception e) { e.printStackTrace(); }
    }

    // _-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-   DECKS  -_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_
    public ISFSArray createDeck(LoginData loginData, int playerId)
    {
        SFSArray decks = new SFSArray();
        //for (int di=0; di<loginData.deckSize; di++)
        for (int i=0; i<loginData.deck.length; i++)
        {
            ISFSObject so = new SFSObject();
            so.putInt("index", i);
            so.putInt("deck_index", 0);
            so.putInt("type", (int) loginData.deck.__get(i));
            decks.addSFSObject(so);
        }


        String query = "INSERT INTO decks (`player_id`, `deck_index`, `index`, `type`) VALUES ";
        for(int i=0; i<decks.size(); i++)
        {
            query += "(" + playerId + ", " + decks.getSFSObject(i).getInt("deck_index") + ", " + decks.getSFSObject(i).getInt("index") + ",  " + decks.getSFSObject(i).getInt("type") + ")" ;
            query += i<decks.size()-1 ? ", " : ";";
        }
        trace(query);
        try {
            db.executeInsert(query, new Object[] {});
        } catch (SQLException e) { e.printStackTrace(); }
        return decks;
    }

    public ISFSArray getDecks(int playerId)
    {
        ISFSArray ret = null;
        try {
            ret = db.executeQuery("SELECT * FROM decks WHERE player_id = " + playerId, new Object[] {});
        } catch (SQLException e) { e.printStackTrace(); }
        return ret;
    }

    public int updateDeck(Player player, int deckIndex, int index, int type)
    {
        player.decks.get(deckIndex).set(index, type);
        try {
            String query = "UPDATE decks SET decks.`type` = "+ type +" WHERE " +
                    "NOT EXISTS (SELECT 1 FROM (" +
                    "SELECT 1 FROM decks WHERE decks.player_id = "+ player.id +" AND decks.deck_index = "+ deckIndex +" AND decks.`type` = "+ type +") as c1)" +
                    "AND decks.player_id = "+ player.id +" AND decks.deck_index = "+ deckIndex +" AND decks.`index` = " + index;

            trace(query);
            db.executeUpdate(query, new Object[]{});
            return MessageTypes.RESPONSE_SUCCEED;
        }
        catch (SQLException e) { e.printStackTrace(); }
        return -1;
    }

    // _-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-   OTHERS  -_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_
    public void upgradeBuilding(Player player, int type, int level)
    {
        String query = "UPDATE `resources` SET `level`='" + level + "' WHERE `type`=" + type + " AND `player_id`=" + player.id + ";";
        try {
        db.executeUpdate(query, new Object[] {});
        } catch (Exception e) { e.printStackTrace(); }
    }

    public ISFSArray getFriends(int playerId)
    {
        String query = "SELECT players.id, players.name, resources.count FROM players INNER JOIN friendship ON players.id=friendship.invitee_id OR players.id=friendship.inviter_id INNER JOIN resources ON resources.type=1001 AND players.id=resources.player_id WHERE players.id!=" + playerId + " AND friendship.inviter_id=" + playerId + " OR friendship.invitee_id=" + playerId + " ORDER BY resources.count DESC LIMIT 0,100";
        ISFSArray result = null;
        try {
            result = db.executeQuery(query, new Object[]{});
        } catch (Exception e) { e.printStackTrace(); }
        return result;
    }


    public ISFSArray getPrefs(int id, int appVersion)
    {
        ISFSArray ret = null;
        try {
            ret = db.executeQuery("SELECT k,v FROM userprefs WHERE player_id=" + id, new Object[]{});
        } catch (SQLException e) { e.printStackTrace(); }

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

        return ret;
    }

    public String resetDailyBattles()
    {
        String result = "";
        try {
            db.executeUpdate("UPDATE `exchanges` SET `num_exchanges`= 0 WHERE `type`=29 AND `num_exchanges` != 0;", new Object[] {});
        } catch (SQLException e) { return "Query failed"; }

        // reset disconnected in-battle players
        List<Room> battles = ext.getParentZone().getRoomManager().getRoomListFromGroup("battles");
        for( Room r : battles )
        {
            List<Game> registeredPlayers = (List<Game>) r.getProperty("registeredPlayers");
            for( Game game : registeredPlayers )
                result += resetDailyBattlesOfUsers(game,  " in game " + r.getName());
        }

        // reset connected players
        Collection<User> users = ext.getParentZone().getUserList();
        for( User u : users )
            result += resetDailyBattlesOfUsers((Game)u.getSession().getProperty("core"), "");

        return "Query succeeded.\n" + result;
    }

    private String resetDailyBattlesOfUsers(Game game, String comment)
    {
        if( game.exchanger.items.exists(ExchangeType.C29_DAILY_BATTLES) && game.exchanger.items.get(ExchangeType.C29_DAILY_BATTLES).numExchanges > 0 )
        {
            game.exchanger.items.get(ExchangeType.C29_DAILY_BATTLES).numExchanges = 0;
            return game.player.id + " daily battles reset to '0'" + comment + ".\n";
        }
        return "";
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

    public String getUDID(int id)
    {
        String query = "SELECT udid FROM devices WHERE player_id=" + id;
        ISFSArray udids = null;
        try {
            udids = db.executeQuery(query, new Object[]{});
        } catch (SQLException e) { e.printStackTrace(); }
        if( udids != null && udids.size() > 0 )
            return udids.getSFSObject(0).getText("udid");
        return null;
    }
}