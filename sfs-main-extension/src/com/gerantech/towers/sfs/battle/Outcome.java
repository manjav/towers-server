package com.gerantech.towers.sfs.battle;

import com.gt.towers.Game;
import com.gt.towers.battle.fieldes.FieldData;
import com.gt.towers.constants.ExchangeType;
import com.gt.towers.constants.ResourceType;
import com.gt.towers.exchanges.ExchangeItem;
import com.gt.towers.utils.maps.IntIntMap;

/**
 * Created by ManJav on 2/13/2018.
 */
public class Outcome
{

    public static int MIN_POINTS = 6;
    public static int COE_POINTS = 4;
    public static int MAX_XP = 10;

    public static IntIntMap get(Game game, FieldData field, int score, float ratio)
    {
        IntIntMap ret = new IntIntMap();
        if ( game.player.inFriendlyBattle )
        {
            ret.set(ResourceType.BATTLES_COUNT_WEEKLY, 1);
            return ret;
        }

        if( field.isQuest )
        {
            if( game.player.isBot() )
                return ret;

            int diffScore = score - game.player.quests.get(field.index);
            boolean newRecord = diffScore > 0;

            if ( game.player.inTutorial() )
                return ret;

            if ( newRecord )
            {
                // xp
                ret.set(ResourceType.XP,  Math.max(0, MAX_XP * (score > 0 ? score : -3) / 3 ) );

                // keys
                ret.set(ResourceType.KEY, diffScore);

                // softs
                ret.set(ResourceType.CURRENCY_SOFT, 10 * diffScore + field.index * 2);
            }
        }
        else
        {
            // points
            int point = 0;
            if( ratio > 1 )
                point = (int) (MIN_POINTS + score * COE_POINTS + Math.round(Math.random() * 8 - 4));
            else if( ratio < 1 )
                point = (int) (-MIN_POINTS - 2 * COE_POINTS + Math.round(Math.random() * 8 - 4));

            // for new players
            if( point < 0 && game.player.resources.get(ResourceType.POINT) < -point)
                point = 0;
            ret.set(ResourceType.POINT, point );

            if( game.player.isBot() )
                return ret;

            int arena = game.player.get_arena(0);

            // softs
            if( point > 0 )
            {
                ret.set(ResourceType.CURRENCY_SOFT, 2 * Math.max(0, score) + Math.min(arena * 2, Math.max(0, game.player.get_point() - game.player.get_softs())));
                ret.set(ResourceType.BATTLES_WINS, 1);
            }

            // battle stats
            ret.set(ResourceType.BATTLES_COUNT, 1);
            ret.set(ResourceType.BATTLES_COUNT_WEEKLY, 1);
            ret.set(ResourceType.WIN_STREAK, getWinStreak(game, arena, score));

            // keys
            ExchangeItem keyItem = game.exchanger.items.get(ExchangeType.C41_KEYS);
            if( keyItem.numExchanges < game.loginData.maxKeysPerDay )
            {
                int numKeys = Math.min( game.loginData.maxKeysPerDay-keyItem.numExchanges, Math.max(0, score) );
                ret.set(ResourceType.KEY, numKeys);
                keyItem.numExchanges += numKeys;
            }
        }
        return ret;
    }

    private static int getWinStreak(Game game, int arena, int score)
    {
        int ret = score > 0 ? 1 : -1;
        if( arena == 0 )
            return ret;

        int winStreak = game.player.resources.get(ResourceType.WIN_STREAK);
        if( winStreak > 3 || winStreak < -3 )
            ret *= (int)Math.ceil(winStreak / 2);

        if( ret < 0 && winStreak < game.arenas.get(arena).minWinStreak )
            ret = 0;
        return ret;
    }
}