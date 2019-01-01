package com.gerantech.towers.sfs.quests;

import com.gerantech.towers.sfs.utils.ExchangeManager;
import com.gt.data.SFSDataModel;
import com.gt.towers.Game;
import com.gt.towers.Player;
import com.gt.towers.constants.ExchangeType;
import com.gt.towers.constants.MessageTypes;
import com.gt.towers.constants.ResourceType;
import com.gt.towers.exchanges.ExchangeItem;
import com.gt.towers.others.Quest;
import com.smartfoxserver.v2.SmartFoxServer;
import com.smartfoxserver.v2.entities.data.ISFSArray;
import com.smartfoxserver.v2.entities.data.ISFSObject;
import com.smartfoxserver.v2.entities.data.SFSArray;
import com.smartfoxserver.v2.entities.data.SFSObject;
import com.smartfoxserver.v2.extensions.SFSExtension;
import haxe.root.Array;

import java.sql.SQLException;

/**
 * Created by ManJav on 8/27/2018.
 */
public class QuestsUtils
{
    private final SFSExtension ext;

    public QuestsUtils() { ext = (SFSExtension) SmartFoxServer.getInstance().getZoneManager().getZoneByName("towers").getExtension(); }
    public static QuestsUtils getInstance() { return new QuestsUtils(); }

    public void insertNewQuests(Player player)
    {
        Quest.fill(player);

        if( player.quests.length == 0 )
            return;

        String query = "INSERT INTO quests (player_id, `type`, `key`, step) VALUES ";
        Quest q;
        for(int i = 0; i < player.quests.length; i++ )
        {
            q = player.quests.__get(i);
            query += "(" + player.id + ", " + q.type + ", " + q.key + ", " + q.nextStep + ")";
            query += (i < player.quests.length - 1 ? ", " : ";");
        }
        int idFrom = 0;
        try {
            idFrom = Math.toIntExact((long) ext.getParentZone().getDBManager().executeInsert(query, new Object[]{}));
        } catch (SQLException e) {e.printStackTrace();}

        // insert ids
        for(int i = 0; i < player.quests.length; i++ )
            player.quests.__get(i).id = idFrom + i;
    }

    public ISFSArray getAll(int playerId)
    {
        String query = "SELECT id, `type`, `key`, step FROM quests WHERE player_id = " + playerId + " ORDER BY timestamp";
        ISFSArray quests = new SFSArray();
        try {
            quests = ext.getParentZone().getDBManager().executeQuery(query, new Object[]{});
        } catch (SQLException e) {e.printStackTrace();}
        return quests;
    }

    public void updateAll(Player player, ISFSArray quests)
    {

        ISFSObject q;
        Quest quest;
        player.quests = new Array();
        for(int i = 0; i < quests.size(); i++ )
        {
            q = quests.getSFSObject(i);
            quest = new Quest(player, q.getInt("type"), q.getInt("key"), q.getInt("step"));
            quest.id = q.getInt("id");
            player.quests.push(quest);
        }
    }

    public int collectReward(Game game, int questId)
    {
        int questIndex = game.player.getQuestIndexById(questId);
        if( questIndex == -1 )
            return MessageTypes.RESPONSE_NOT_FOUND;

        Quest quest = game.player.quests.__get(questIndex);
        quest.current = Quest.getCurrent(game.player, quest.type, quest.key);

        // exchange
        int response = ExchangeManager.getInstance().process(game, Quest.getExchangeItem(quest.type, quest.nextStep), 0, 0);
        if( response != MessageTypes.RESPONSE_SUCCEED )
            return response;
        game.player.quests.remove(quest);
        Quest.fill(game.player);
        quest = game.player.quests.__get(game.player.quests.length - 1);
        quest.id = questId;

        // update DB
        String query = "UPDATE quests SET `type`=" + quest.type + ", `key`=" + quest.key + ", step=" + quest.nextStep + " WHERE `id`=" + quest.id;
        ext.trace(query);
        try {
            ext.getParentZone().getDBManager().executeUpdate(query, new Object[]{});
        } catch (SQLException e) {e.printStackTrace();}

        return response;
    }

    public static ISFSObject toSFS(Quest quest)
    {
        SFSObject ret = new SFSObject();
        ret.putInt("id", quest.id);
        ret.putInt("key", quest.key);
        ret.putInt("type", quest.type);
        ret.putInt("target", quest.target);
        ret.putInt("current", quest.current);
        ret.putInt("nextStep", quest.nextStep);
        ret.putSFSArray("rewards", SFSDataModel.toSFSArray(quest.rewards));
        return ret;
    }

    public static ISFSArray toSFS(Array<Quest> quests)
    {
        ISFSArray ret = new SFSArray();
        for( int i = 0; i < quests.length; i++ )
            ret.addSFSObject( QuestsUtils.toSFS(quests.__get(i)));
        return ret;
    }

}
