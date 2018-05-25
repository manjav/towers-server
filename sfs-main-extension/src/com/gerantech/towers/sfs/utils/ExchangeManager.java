package com.gerantech.towers.sfs.utils;

import com.gerantech.towers.sfs.callbacks.MapChangeCallback;
import com.gt.towers.Game;
import com.gt.towers.constants.ExchangeType;
import com.gt.towers.exchanges.ExchangeItem;
import com.smartfoxserver.v2.SmartFoxServer;
import com.smartfoxserver.v2.entities.data.SFSArray;
import com.smartfoxserver.v2.entities.data.SFSObject;
import com.smartfoxserver.v2.extensions.ExtensionLogLevel;
import com.smartfoxserver.v2.extensions.SFSExtension;

/**
 * Created by ManJav on 4/27/2018.
 */
public class ExchangeManager
{
    private final SFSExtension ext;
    public MapChangeCallback mapChangeCallback;

    public ExchangeManager() {
        ext = (SFSExtension) SmartFoxServer.getInstance().getZoneManager().getZoneByName("towers").getExtension();
    }
    public static ExchangeManager getInstance() { return new ExchangeManager(); }

    public boolean process(Game game, int type, int now, int hardsConfimed)
    {
        ExchangeItem item = game.exchanger.items.get(type);
        if( item == null )
        {
            ext.trace(ExtensionLogLevel.ERROR, "Exchange item not found in exchanger.");
            return false;
        }
        return process(game, item, now, hardsConfimed);
    }

    public boolean process(Game game, ExchangeItem item, int now, int hardsConfimed)
    {
		/*String log = "";
		int[] keys = game.player.resources.keys();
		for(int i = 0; i<keys.length; i++)
			log += (keys[i] + ": " +game.player.resources.get(keys[i]) +" , " );
		trace ( log );*/

        mapChangeCallback = new MapChangeCallback();
        game.player.resources.changeCallback = mapChangeCallback;
        Boolean succeed = false;
        try {
            succeed = game.exchanger.exchange(item, now, hardsConfimed);
        } catch (Exception e) { e.printStackTrace(); }
        game.player.resources.changeCallback = null;
        if( !succeed )
            return false;

        // logs .....
		/*int[] inserts = mapChangeCallback.inserts.keys();
		for(int i = 0; i<inserts.length; i++)
			trace("inserts", inserts[i], mapChangeCallback.inserts.get(inserts[i]));
		int[] updates = mapChangeCallback.updates.keys();
		for(int o = 0; o<updates.length; o++)
			trace("updates", updates[o], mapChangeCallback.inserts.get(updates[o]));*/

        ext.trace("Exchange => type:", item.type, " ,expiredAt:", item.expiredAt, " ,now:", now, " ,outcomes:", item.outcomes==null?"":item.outcomes.toString(), " ,hardsConfimed:", hardsConfimed, " ,succeed:", succeed, " ,numExchanges:", item.numExchanges, " ,outcome:", item.outcome);

        // Run db queries
        DBUtils dbUtils = DBUtils.getInstance();
        try
        {
            dbUtils.updateResources(game.player, mapChangeCallback.updates);
            dbUtils.insertResources(game.player, mapChangeCallback.inserts);
            if( item.isBook() || item.isIncreamental() || item.category == ExchangeType.C20_SPECIALS )
                dbUtils.updateExchange(item.type, game.player.id, item.expiredAt, item.numExchanges, item.outcomesStr);
        }
        catch (Exception e) {  e.printStackTrace(); return false; }
        return true;
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
