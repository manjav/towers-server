package com.gt.utils;

import com.gt.data.RankData;
import com.gt.towers.Game;
import com.gt.towers.Player;
import com.gt.towers.constants.ExchangeType;
import com.gt.towers.constants.ResourceType;
import com.gt.towers.utils.maps.IntIntMap;
import com.smartfoxserver.v2.db.IDBManager;
import com.smartfoxserver.v2.entities.Room;
import com.smartfoxserver.v2.entities.User;
import com.smartfoxserver.v2.entities.data.ISFSArray;
import com.smartfoxserver.v2.entities.data.ISFSObject;
import com.smartfoxserver.v2.entities.data.SFSArray;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.sql.Connection;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;

/**
 * Created by ManJav on 12/4/2017.
 */
public class DBUtils extends UtilBase
{
    private final IDBManager db;
    private final Boolean DEBUG_MODE = true;
    public final String mainDB = ext.getParentZone().getDBManager().getConfig().connectionString.split("/")[3].split("\\?")[0];
    public final String inactiveDB = mainDB + "_inactive";
    public DBUtils()
    {
        super();
        db = this.ext.getParentZone().getDBManager();
    }
    public static DBUtils getInstance()
    {
        return (DBUtils)UtilBase.get(DBUtils.class);
    }
    public String cleanInactiveUsers(String pastHours) {
        String query = "";
        String ret = "";
        Instant start = Instant.now();
        Instant time = start.minusMillis(Long.parseUnsignedLong(pastHours)*3600000);
        Timestamp timestamp = Timestamp.from(time);
        try {
            Connection con = db.getConnection();
            try {
                con.setAutoCommit(false);
                Statement statement = con.createStatement();
                List<String> columns = new ArrayList<String>();
                // ---------- Table Backups ------------
                // accounts
                columns = new ArrayList<String>(Arrays.asList("id","player_id","type","social_id","name","image_url","timestamp"));
                backupTable("accounts", columns, statement);
                // banneds
                columns = new ArrayList<String>(Arrays.asList("id", "player_id", "udid", "imei", "message", "mode", "timestamp", "expire_at", "time"));
                backupTable("banneds", columns, statement);
                // bugs
                columns = new ArrayList<String>(Arrays.asList("id","player_id","email","description","status","report_at"));
                backupTable("bugs", columns, statement);
                // challenges
                columns = new ArrayList<String>(Arrays.asList("id","type","start_at","attendees"));
                backupTable("challenges", columns, statement);
                // devices
                columns = new ArrayList<String>(Arrays.asList("player_id","model","udid","imei"));
                backupTable("devices", columns, statement);
                // exchanges
                columns = new ArrayList<String>(Arrays.asList("id","type","player_id","num_exchanges","expired_at","outcome","reqs"));
                backupTable("exchanges", columns, statement);
                // friendship
                columns = new ArrayList<String>(Arrays.asList("id","inviter_id","invitee_id","invitation_code","has_reward"));
                backupTable("friendship", columns, statement);
                // inbox
                columns = new ArrayList<String>(Arrays.asList("id","type","text","sender","senderId","receiverId","data","read","utc"));
                backupTable("inbox", columns, statement);
                // infractions
                columns = new ArrayList<String>(Arrays.asList("id","reporter","offender","content","lobby","offend_at","report_at","proceed"));
                backupTable("infractions", columns, statement);
                // lobbies
                columns = new ArrayList<String>(Arrays.asList("id","name","bio","emblem","capacity","min_point","privacy","create_at","members","messages"));
                backupTable("lobbies", columns, statement);
                // operations
                columns = new ArrayList<String>(Arrays.asList("index","player_id","score","create_at"));
                backupTable("operations", columns, statement);
                // players
                columns = new ArrayList<String>(Arrays.asList("name", "password", "create_at", "app_version", "last_login", "sessions_count"));
                backupTable("players", columns, statement);
                // purchases
                columns = new ArrayList<String>(Arrays.asList("player_id","id","market","token","consumed","state","time","timestamp","old_res","new_res"));
                backupTable("purchases", columns, statement);
                // pushtokens
                columns = new ArrayList<String>(Arrays.asList("player_id","fcm_token","os_pid","os_token"));
                backupTable("pushtokens", columns, statement);
                // quests
                columns = new ArrayList<String>(Arrays.asList("id","player_id","type","key","step","timestamp"));
                backupTable("quests", columns, statement);
                // resources
                columns = new ArrayList<String>(Arrays.asList("type", "count", "level"));            
                backupTable("resources", columns, statement);
                // userprefs
                columns = new ArrayList<String>(Arrays.asList("player_id","k","v"));
                backupTable("userprefs", columns, statement);
                query = "DELETE FROM " + mainDB + ".players WHERE id != 10000 AND last_login < \"" + timestamp.toString() + "\"";
                traceQuery(query);
                statement.execute(query);
                con.commit();
                con.setAutoCommit(true);
                ret = "Cleaned inactive useres before " + timestamp.toString() + " ";
                trace(ret);
            } catch(SQLException e) {
                con.rollback();
                e.printStackTrace();
                ret = "Failed to clean users ";
            } finally {
                Instant fin = Instant.now();
                con.close();
                ret += "in " + Duration.between(start, fin).toMillis() + " milisecounds.\n";
            }
        } catch( SQLException e ) { e.printStackTrace(); }
        return ret;
    }

    private String getOnDuplicateKeyChanges(List<String> columns) {
        String ret = "ON DUPLICATE KEY UPDATE ";
        List<String> valueColumn = new ArrayList<String>();
        for ( String column : columns ) {
            valueColumn.add("`"+column+"`=VALUES(`"+column+"`)");
        }
        ret += String.join(",", valueColumn);
        return ret;
    }

    public Boolean recoverFromInactives(int playerId) {
        List<String> columns = new ArrayList<String>();
        // players
        columns = new ArrayList<String>(Arrays.asList("name", "password", "create_at", "app_version", "last_login", "sessions_count"));
        String query = "INSERT INTO " + mainDB + ".players SELECT * FROM " + inactiveDB + ".players " + getOnDuplicateKeyChanges(columns);
        try { db.executeInsert(query, new Object[] {}); } catch(SQLException e) {e.printStackTrace(); }
        // exchanges
        columns = new ArrayList<String>(Arrays.asList("id","type","player_id","num_exchanges","expired_at","outcome","reqs"));
        recoverInactivePlayerFromTable("exchanges", columns, playerId);
        // quests
        columns = new ArrayList<String>(Arrays.asList("id","player_id","type","key","step","timestamp"));
        recoverInactivePlayerFromTable("quests", columns, playerId);
        // resources
        columns = new ArrayList<String>(Arrays.asList("type", "count", "level"));            
        recoverInactivePlayerFromTable("resources", columns, playerId);
        // userprefs
        columns = new ArrayList<String>(Arrays.asList("player_id","k","v"));
        recoverInactivePlayerFromTable("userprefs", columns, playerId);
        return false;
    }

    private void backupTable(String table, List<String> columns, Statement statement) throws SQLException {
        String query = "INSERT INTO " + inactiveDB + "." + table + " SELECT * FROM " + mainDB + "." + table + " " + getOnDuplicateKeyChanges(columns);
        traceQuery(query);
        statement.execute(query);
    }

    private void recoverInactivePlayerFromTable(String table, List<String> columns, int playerId)
    {
        String query =  "INSERT INTO " + mainDB + "." + table +
                        " SELECT " + inactiveDB + "." + table + ".* FROM " + inactiveDB +
                        "." + table + " INNER JOIN " + inactiveDB + ".players ON players.id=" + table + ".player_id " +
                        " WHERE player_id=" + playerId +
                        " " + getOnDuplicateKeyChanges(columns);
        traceQuery(query);
        try {
            db.executeInsert(query, new Object[]{});
        } catch(SQLException e) { e.printStackTrace(); }
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
        String query = "UPDATE resources SET count= CASE";
        for (int i = 0; i < keyLen; i++)
        {
            if( resources.get(keys[i]) == 0 || ResourceType.isBook(keys[i]) )
                continue;
            if( !hasRankFields )
                hasRankFields = keys[i] == ResourceType.R2_POINT;

            query += " WHEN type= " + keys[i] + " THEN " + player.resources.get(keys[i]);
        }
        query += " ELSE count END WHERE player_id= " + player.id;

        try {
            db.executeUpdate(query, new Object[] {});
        } catch (SQLException e) { e.printStackTrace(); }

        // update ranking table
        if( hasRankFields )
        {
            ConcurrentHashMap<Integer, RankData> users = RankingUtils.getInstance().getUsers();
            RankData rd = new RankData(player.nickName, player.get_point());
            query += "\n map for id:" + player.id + " => point:" + player.get_point();

            if( users.containsKey(player.id))
                users.replace(player.id, rd);
            else
                users.put(player.id, rd);
        }
    }

    public void insertResources(Player player, IntIntMap resources)
    {
        int[] keys = resources.keys();
        int keyLen = keys.length;
        List<Integer> res = new ArrayList<>();
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
        try{
        db.executeInsert(query, new Object[] {});
        } catch (SQLException e) { e.printStackTrace(); }
        //trace(query);
    }

    // _-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-   EXCHANGES  -_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_
    public ISFSArray getExchanges(int playerId, int now)
    {
        ISFSArray ret = null;
        try {
            ret = db.executeQuery("SELECT type, num_exchanges, expired_at, outcome, reqs FROM exchanges WHERE player_id=" + playerId + " OR player_id=10000", new Object[]{});
        } catch (SQLException e) { e.printStackTrace(); }
        return ret;
    }
    public void updateExchange(int type, int playerId, int expireAt, int numExchanges, String outcomesStr, String reqsStr)
    {
        String query = "SELECT _func_exchanges(" + type + "," + playerId + "," + numExchanges + "," + expireAt + ",'" + outcomesStr + "', '" + reqsStr + "')";
        try {
            db.executeQuery(query, new Object[] {});
        } catch (SQLException e) { e.printStackTrace(); }
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

    // _-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-   OTHERS  -_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_
    public void upgradeBuilding(Player player, int type, int level)
    {
        String query = "UPDATE `resources` SET `level`='" + level + "' WHERE `type`=" + type + " AND `player_id`=" + player.id + ";";
        try {
        db.executeUpdate(query, new Object[] {});
        } catch (Exception e) { e.printStackTrace(); }
    }

    public SFSArray getDecks(int playerId)
    {
        SFSArray decks = null;
        try {
            decks = (SFSArray) db.executeQuery("SELECT * FROM decks WHERE player_id=" + playerId, new Object[]{});
        } catch (Exception e) { e.printStackTrace(); }
        return decks;
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


    public ISFSArray getPrefs(int id)
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

        trace("Query succeeded.\n" + result);
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

    public ISFSObject getDevice(int id)
    {
        String query = "SELECT * FROM devices WHERE player_id=" + id;
        ISFSArray udids = null;
        try {
            udids = db.executeQuery(query, new Object[]{});
        } catch (SQLException e) { e.printStackTrace(); }
        if( udids != null && udids.size() > 0 )
            return udids.getSFSObject(0);
        return null;
    }
    public void traceQuery(String query)
    {
        if( DEBUG_MODE )
            System.out.println("SQLQuery: " + query);
    }
}