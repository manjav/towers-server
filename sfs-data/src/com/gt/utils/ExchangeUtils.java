package com.gt.utils;

import com.gt.callbacks.MapChangeCallback;
import com.gt.towers.Game;
import com.gt.towers.constants.ExchangeType;
import com.gt.towers.constants.MessageTypes;
import com.gt.towers.exchanges.ExchangeItem;
import com.smartfoxserver.v2.entities.data.ISFSArray;
import com.smartfoxserver.v2.entities.data.SFSArray;
import com.smartfoxserver.v2.entities.data.SFSObject;
import com.smartfoxserver.v2.extensions.ExtensionLogLevel;

/**
 * Created by ManJav on 4/27/2018.
 */
public class ExchangeUtils extends UtilBase
{
    public static ExchangeUtils getInstance()
    {
        return (ExchangeUtils)UtilBase.get(ExchangeUtils.class);
    }
    public int process(Game game, int type, int now, int hardsConfimed)
    {
        return process(game, type, now, hardsConfimed, null);
    }
    public int process(Game game, int type, int now, int hardsConfimed, MapChangeCallback mapChangeCallback)
    {
        ExchangeItem item = game.exchanger.items.get(type);
        if( item == null )
        {
            trace(ExtensionLogLevel.ERROR, "Exchange item not found in exchanger.");
            return MessageTypes.RESPONSE_NOT_FOUND;
        }
        return process(game, item, now, hardsConfimed, mapChangeCallback);
    }

    public int process(Game game, ExchangeItem item, int now, int hardsConfimed)
    {
        return process(game, item, now, hardsConfimed, null);
    }
    public int process(Game game, ExchangeItem item, int now, int hardsConfimed, MapChangeCallback mapChangeCallback)
    {
		/*String log = "";
		int[] keys = game.player.resources.keys();
		for(int i = 0; i<keys.length; i++)
			log += (keys[i] + ": " +game.player.resources.get(keys[i]) +" , " );
		trace ( log );*/

		int deckLen = game.player.getSelectedDeck().keys().length;
        if( mapChangeCallback == null )
            mapChangeCallback = new MapChangeCallback();
        game.player.resources.changeCallback = mapChangeCallback;
        int response = -10;
        try {
            response = game.exchanger.exchange(item, now, hardsConfimed);
        } catch (Exception e) { e.printStackTrace(); }
        game.player.resources.changeCallback = null;
        if( response != MessageTypes.RESPONSE_SUCCEED )
            return response;

        // logs .....
		/*int[] inserts = mapChangeCallback.inserts.keys();
		for(int i = 0; i<inserts.length; i++)
			trace("inserts", inserts[i], mapChangeCallback.inserts.get(inserts[i]));
		int[] updates = mapChangeCallback.updates.keys();
		for(int o = 0; o<updates.length; o++)
			trace("updates", updates[o], mapChangeCallback.inserts.get(updates[o]));*/

        trace("Exchange => type:", item.type, " ,expiredAt:", item.expiredAt, " ,now:", now, " ,outcomes:", item.outcomes==null?"":item.outcomes.toString(), " ,hardsConfimed:", hardsConfimed, " ,response:", response, " ,numExchanges:", item.numExchanges, " ,outcome:", item.outcome);

        // Run db queries
        DBUtils dbUtils = DBUtils.getInstance();
        dbUtils.updateResources(game.player, mapChangeCallback.updates);
        dbUtils.insertResources(game.player, mapChangeCallback.inserts);
        if( item.isBook() || item.isIncreamental() || item.category == ExchangeType.C20_SPECIALS )
            dbUtils.updateExchange(game, item.type, item.expiredAt, item.numExchanges, item.outcomesStr, item.requirementsStr);

        // add new card into selected deck
        if( deckLen < game.player.getSelectedDeck().keys().length )
        {
            String query = "INSERT INTO decks (`player_id`, `deck_index`, `index`, `type`) VALUES (" + game.player.id + ", " + game.player.selectedDeckIndex + ", " + deckLen + ",  " + game.player.getSelectedDeck().get(deckLen) + ");";
            try {
            ext.getParentZone().getDBManager().executeInsert(query, new Object[]{});
            } catch (Exception e) {  e.printStackTrace(); return MessageTypes.RESPONSE_UNKNOWN_ERROR; }
        }
        return response;
    }

    /**
     * Return book rewards as params and clear change callback
     * @return
     */
    public ISFSArray getRewards(MapChangeCallback mapChangeCallback)
    {
        ISFSArray ret = new SFSArray();
        int[] outKeys = mapChangeCallback.all.keys();
        for (int i : outKeys)
        {
            if( mapChangeCallback.all.get(i) <= 0 )
                continue;

            SFSObject so = new SFSObject();
            so.putInt("t", i);
            so.putInt("c",  mapChangeCallback.all.get(i));
            ret.addSFSObject( so );
        }
        mapChangeCallback = null;
        return ret;
    }

    public static SFSObject toSFS(ExchangeItem item)
    {
        SFSObject ret = new SFSObject();
        ret.putInt("type", item.type);
        ret.putInt("outcome", item.outcome);
        ret.putInt("expiredAt", item.expiredAt);
        ret.putInt("numExchanges", item.numExchanges);

        SFSObject sfs;
        SFSArray elements = new SFSArray();
        int[] keys = item.outcomes.keys();
        int len = keys.length - 1;
        while ( len >= 0 )
        {
            sfs = new SFSObject();
            sfs.putInt("key", keys[len]);
            sfs.putInt("value", item.outcomes.get(keys[len]));
            elements.addSFSObject(sfs);
            len --;
        }
        ret.putSFSArray("outcomes", elements);

        elements = new SFSArray();
        keys = item.requirements.keys();
        len = keys.length - 1;
        while ( len >= 0 )
        {
            sfs = new SFSObject();
            sfs.putInt("key", keys[len]);
            sfs.putInt("value", item.requirements.get(keys[len]));
            elements.addSFSObject(sfs);
            len --;
        }
        ret.putSFSArray("requirements", elements);

        return ret;
    }
}
