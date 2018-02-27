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
    public static int MIN_POINTS = 10;
    public static int COE_POINTS = 5;
    public static int MAX_XP = 10;

    public static IntIntMap get(Game game, FieldData field, int score, int earnedKeys)
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
            if( score > 0 )
                point = (int) (MIN_POINTS + score * COE_POINTS + Math.round(Math.random() * 8 - 4));
            else if ( score < 0 )
                point = (int) (-MIN_POINTS + score * COE_POINTS + Math.round(Math.random() * 8 - 4));

            if( point < 0 && game.player.resources.get(ResourceType.POINT) < -point)
                point = 0;
            ret.set(ResourceType.POINT, point );

            if( game.player.isBot() )
                return ret;

            int arena = game.player.get_arena(0);

            // softs

            if( score > 0 )
                ret.set(ResourceType.CURRENCY_SOFT, 2 * Math.max(0, earnedKeys) + Math.min(arena * 3, Math.max(0, game.player.get_point() - game.player.get_softs())));

            // battle stats
            ret.set(ResourceType.BATTLES_COUNT, 1);
            ret.set(ResourceType.BATTLES_COUNT_WEEKLY, 1);

            // win streak
            int winStreak = game.player.resources.get(ResourceType.WIN_STREAK);
            if( score > 0 )
            {
                ret.set(ResourceType.BATTLES_WINS, 1);
                if( arena > 0 )
                    ret.set(ResourceType.WIN_STREAK, 1);
            }
            else if ( score < 0 )
            {
                if( arena > 0 && winStreak >= game.arenas.get(arena).minWinStreak )
                    ret.set(ResourceType.WIN_STREAK, Math.random() > 0.5 ? -2 : -3);
            }

            // keys
            ExchangeItem keyItem = game.exchanger.items.get(ExchangeType.S_41_KEYS);
            if( keyItem.numExchanges < game.loginData.maxKeysPerDay )
            {
				int numKeys = Math.min(game.loginData.maxKeysPerDay-keyItem.numExchanges, Math.max(0, earnedKeys));
                ret.set(ResourceType.KEY, numKeys);
                keyItem.numExchanges += numKeys;
            }
        }
        return ret;
    }
}
