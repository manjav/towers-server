package com.gerantech.towers.sfs.utils;

import com.gerantech.towers.sfs.handlers.RankRequestHandler;
import com.gt.hazel.RankData;
import com.gt.towers.Game;
import com.gt.towers.arenas.Arena;
import com.hazelcast.config.Config;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.IMap;
import com.hazelcast.query.EntryObject;
import com.hazelcast.query.PagingPredicate;
import com.hazelcast.query.Predicate;
import com.hazelcast.query.PredicateBuilder;
import com.hazelcast.util.RandomPicker;
import com.smartfoxserver.v2.db.IDBManager;
import com.smartfoxserver.v2.entities.data.ISFSArray;
import com.smartfoxserver.v2.entities.data.ISFSObject;
import com.smartfoxserver.v2.entities.data.SFSDataWrapper;
import com.smartfoxserver.v2.entities.data.SFSObject;
import com.smartfoxserver.v2.extensions.ISFSExtension;

import java.sql.SQLException;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * Created by ManJav on 7/22/2017.
 */
public class NPCTools
{

    public static IMap<Integer, RankData> fill(IMap<Integer, RankData> users, Game game, ISFSExtension extension)
    {
        if( users.size() > 10 )
        {
           /* for (RankData rd:users.values())
                if(rd.xp != -1)
                    game.tracer.log( rd.id + " ====> " + rd.xp);*/
            return users;
        }

        // insert real champions
        try {
            IDBManager dbManager = extension.getParentZone().getDBManager();
            Arena arena;
            int[] arenaKeys = game.arenas.keys();
            for( int a=0; a<arenaKeys.length; a++ )
            {
                arena = game.arenas.get(arenaKeys[a]);
                String query = "SELECT  players.id, players.name, resources.count FROM players INNER JOIN resources ON players.id = resources.player_id WHERE resources.type =1001 AND resources.count >= " + arena.min + " AND resources.count <= " + arena.max + " ORDER BY resources.count DESC LIMIT 0," + RankRequestHandler.PAGE_SIZE;
                ISFSArray players = dbManager.executeQuery(query, new Object[] {});

                for( int p=0; p<players.size(); p++ )
                {
                    ISFSObject pp = players.getSFSObject(p);
                    users.put(pp.getInt("id"), new RankData(pp.getInt("id"), pp.getUtfString("name"), pp.getInt("count"), 0));
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        // insert npcs
        String[] names = {"_mahmudMoh390", "&ManMoh891", "_mohreza891", "*sadeghHoseini390", "&zohreAzar891", "*alirezaSH_", "_javadRazi891", "@hosnaM390", "&zohreKing_", "*mohrezaMoh390", "@javadM891", "@hosnaM390", "&sadeghSH_", "*sadeghAzar891", "*pouriaJJ390", "_mahmudDD891", "*pouriaJJ390", "&mahmudShahi_", "*ManRazi891", "@mansourDavoudi_", "@maryamKing891", "*mahmudAzar996", "*babakDD390", "*ManRazi891", "*sohrabDavoudi996", "*mahmudAzar996", "*sohrabDavoudi996", "*mohrezaRainbow_", "&mahmudKing996", "*mohrezaRainbow_", "*alirezaKing891", "&ManJafari996", "_maryamSH891", "&ManJafari996", "&hosnaShahi891", "*sadeghShahi_", "@mahmudJafari_", "*sadeghShahi_", "_ManHoseini390", "&Man390", "&mansourJJ_", "&mansourSH891", "&Man390", "&mansourSH891", "*hosnaAzar891", "@mohrezaRazi891", "@raminShahi_", "&mohrezaSH390", "@raminShahi_", "&mohrezaSH390", "_mAliRainbow390", "&ManDavoudi891", "@pouria_", "*raminSadeghi390", "*maryamRazi_", "@mAliKing996", "*maryamRazi_", "*javadDD891", "*mohrezaSadeghi996", "*maryamRainbow996", "@hosnaDavoudi996", "&sohrabJJ390", "*raminJJ390", "&mohrezaHoseini390", "&mAliDD891", "_raminDavoudi996", "&sohrabJJ390", "&pouriaDD_", "_mAliJafari390", "@zohreShahi996", "@ManRainbow891", "@mansourDD390", "@mohrezaM891", "@mansourDD390", "&mansourAzar996", "@javadSadeghi996", "&mansourAzar996", "@javadSadeghi996", "&alirezaMoh891", "@hosnaJafari891", "*mAliSadeghi996", "@hosnaJafari891", "*maryamDD_", "@hosnaJafari891", "&babakJJ_", "*pouriaShahi996", "&javadShahi_", "&babakJJ_", "@zohreJafari390", "_mAliAzar_", "@alirezaJafari_", "@zohreRazi_", "_mAliAzar_", "@zohreRazi_", "*mAliSH996", "@hosnaHoseini_", "&alirezaDavoudi996", "@hosnaHoseini_", "_zohreMoh996", "@ManKing891", "_zohreMoh996", "@ManKing891", "_hosnaJJ996", "_hosnaMoh996", "@javad996", "&sohrabJafari390", "@javad996", "&ManM996", "@ManSH891", "*javadRainbow390", "@alirezaRainbow891", "*javadRainbow390", "*babakAzar996", "*alirezaJJ891", "*babakAzar996", "*sadeghDD390", "&pouriaSH_", "*sadeghDD390", "&pouriaSH_", "*sadeghDD390", "&hosnaKing_", "_mohrezaDD996", "@babakRainbow891", "@mansourSadeghi891", "_alirezaShahi891", "_mohrezaDD996", "_alirezaShahi891", "*zohreM_", "&sadegh996", "@maryamShahi390", "_zohreSadeghi_", "*zohreHoseini390", "@maryamShahi390", "&sadegh996", "@maryamShahi390", "*mansourRainbow996", "*sadeghKing390", "_sohrabMoh390", "*sadeghKing390", "_hosnaDD_", "&javadHoseini_", "_hosnaDD_", "*javadJafari891", "*mAliDavoudi390", "_javadSH891", "*mAliShahi996", "&sohrabM891", "*mansour891", "&sohrabM891", "_pouriaDavoudi996", "@mansourRazi_", "*babakKing996", "@mansourRazi_", "*sadeghMoh891", "_pouriaSadeghi390", "@mohrezaJJ996", "*babakKing996", "_raminRazi891", "&mAliJJ_", "*sohrabSH390", "&mahmudHoseini996", "&mAliJJ_", "_mahmudDavoudi891", "*sadeghM891", "_mahmudDavoudi891", "&sadeghRainbow891", "@pouriaRainbow_", "_raminKing390", "@maryam_", "@javadKing891", "*sohrabKing891", "@babakMoh996", "_ManAzar390", "@zohreRainbow390", "*hosnaSH390", "*raminM390", "&mahmudSadeghi891", "*raminM390", "&babakJafari390", "_mahmudRazi_", "&babakJafari390", "_raminMoh891", "*alirezaAzar390", "@alirezaM_", "@raminDD891", "@hosnaSadeghi891", "@raminDD891", "&mahmudJJ891", "_babakSH996"};

        int start = Integer.MAX_VALUE;
        int nameIndex = 0;
        Arena[] arenas = game.arenas.values();
        for (Arena a:arenas)
        {
            int len = (int) Math.floor(names.length/arenas.length);
            for ( int i=0;  i<len;   i++,start--,nameIndex++ )
                users.put(start , new RankData(start, names[nameIndex], RandomPicker.getInt(a.min, a.max), -1));
        }

        return users;
    }

    public static RankData getNearOpponent(IMap<Integer, RankData> users, int point, int range)
    {
        Collection<RankData> result = getResult(users, point, range);
        int num = (int) (Math.random() * result.size());
        for(RankData t: result)
            if (--num < 0)
                return t;
        throw new AssertionError();
    }
    private static Collection<RankData> getResult(IMap<Integer, RankData> users, int point, int range)
    {
        EntryObject eo = new PredicateBuilder().getEntryObject();
        Predicate sqlQuery = eo.get("point").between(point-range, point+range).and(eo.get("point").notEqual(point)).and(eo.get("xp").equal(-1));

        // a comparator which helps to sort in descending order of point field
        Comparator<Map.Entry> descendingComparator = new Comparator<Map.Entry>() {
            public int compare(Map.Entry e1, Map.Entry e2) {
                RankData s1 = (RankData) e1.getValue();
                RankData s2 = (RankData) e2.getValue();
                return s2.point - s1.point;
            }
        };
        PagingPredicate pagingPredicate = new PagingPredicate(sqlQuery, descendingComparator, 20);

        //PagingPredicate pagingPredicate = new PagingPredicate(sqlQuery, 20);

        Collection<RankData> result = users.values(pagingPredicate);

        if(result.size() == 0 && range < 1000)
            result = getResult(users, point, range * 2);

        return result;
    }


    public static void setXP(int id, int xp)
    {
        IMap<Integer, RankData> users = Hazelcast.getOrCreateHazelcastInstance(new Config("aaa")).getMap("users");
        RankData opponent = users.get(id);
        opponent.xp = xp;
        users.replace(opponent.id, opponent);
    }
}
