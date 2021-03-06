package com.gerantech.towers.sfs.battle;

import com.gt.towers.Game;
import com.gt.towers.battle.fieldes.FieldData;
import com.gt.towers.constants.ExchangeType;
import com.gt.towers.constants.ResourceType;
import com.gt.towers.exchanges.ExchangeItem;
import com.gt.towers.others.Arena;
import com.gt.towers.utils.maps.IntIntMap;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by ManJav on 2/13/2018.
 */
public class Outcome
{
    public static int MIN_POINTS = 5;
    public static int COE_POINTS = 4;

    public static IntIntMap get(Game game, FieldData field, int star, float ratio, int now)
    {
        IntIntMap ret = new IntIntMap();
        if( game.player.inFriendlyBattle )
        {
            ret.set(ResourceType.R15_BATTLES_FRIENDLY, 1);
            return ret;
        }

        if( field.isOperation )
        {
            if( game.player.isBot() )
                return ret;

            int diffScore = star - game.player.operations.get(field.index);
            boolean newRecord = diffScore > 0;

            if( game.player.inTutorial() )
                return ret;

            if( newRecord )
            {
                // xp
                ret.set(ResourceType.R1_XP, diffScore + 1);

                // soft-currency
                ret.set(ResourceType.R3_CURRENCY_SOFT, 10 * diffScore + field.index * 2);
            }
        }
        else
        {
            // points
            int point = 0;
            if( ratio > 1 )
                point = (int) (MIN_POINTS + star * COE_POINTS + Math.round(Math.random() * 8 - 4));
            else if( ratio < 1 )
                point = (int) (-MIN_POINTS - 2 * COE_POINTS + Math.round(Math.random() * 8 - 4));

            // for novice
            if( point < 0 && game.player.resources.get(ResourceType.R2_POINT) < -point)
                point = 0;
            ret.set(ResourceType.R2_POINT, point);

            if( game.player.isBot() )
                return ret;

            int arena = game.player.get_arena(0);
            int dailyBattles = game.exchanger.items.exists(ExchangeType.C29_DAILY_BATTLES) ? game.exchanger.items.get(ExchangeType.C29_DAILY_BATTLES).numExchanges : 0;

            // battle stats
            ret.set(ResourceType.R12_BATTLES, 1);
            ret.set(ResourceType.R16_WIN_RATE, getWinStreak(game, arena, star));
            if( game.player.get_battleswins() > 3 )
                ret.set(ResourceType.R17_STARS, star);

            if( point > 0 )
            {
                // num wins
                ret.set(ResourceType.R13_BATTLES_WINS, 1);

                // soft-currency
                int soft = 2 * Math.max(0, star) + Math.min(arena * 2, Math.max(0, game.player.get_point() - game.player.get_softs()));
                if( dailyBattles > 10 )
                {
                    point = (int) (point * Math.pow(10f / dailyBattles, 0.2));
                    soft = (int) (soft * Math.pow(10f / dailyBattles, 0.8));
                }
                ret.set(ResourceType.R2_POINT, point );
                if( game.inBattleChallengMode > -1 )
                    return  ret;

                ret.set(ResourceType.R3_CURRENCY_SOFT, soft);

                // random book
                List<Integer> emptySlotsType = getEmptySlots(game, game.player.get_battleswins() == 1, now);
                if( game.player.get_battleswins() > 0 && emptySlotsType.size() > 0 )
                {
                    int randomEmptySlotIndex = game.player.get_battleswins() == 1 ? 3 : (int) Math.floor(Math.random() * emptySlotsType.size());
                    ExchangeItem emptySlot = game.exchanger.items.get(emptySlotsType.get(randomEmptySlotIndex));
                    game.exchanger.findRandomOutcome(emptySlot, now);
                    ret.set(emptySlot.outcome, emptySlot.type);
                }
            }
        }
        return ret;
    }

    private static int getWinStreak(Game game, int arena, int score)
    {
        int ret = score > 0 ? 1 : -1;
        if( arena == 0 )
            return ret;

        int winStreak = game.player.resources.get(ResourceType.R16_WIN_RATE);
        if( winStreak > 3 || winStreak < -3 )
            ret *= (int)Math.ceil(winStreak / 2);

        Arena a = game.arenas.get(arena);
        if( ret < 0 && winStreak < a.minWinStreak )
            ret = 0;
        return ret;
    }

    private static List<Integer> getEmptySlots(Game game, boolean forced, int now)
    {
        List<Integer> ret = new ArrayList<>();
        int[] keys = game.exchanger.items.keys();
        for ( int k : keys )
            if( game.exchanger.items.get(k).category == ExchangeType.C110_BATTLES && (forced || game.exchanger.items.get(k).getState(now) == ExchangeItem.CHEST_STATE_EMPTY) )
                ret.add(game.exchanger.items.get(k).type);
        return ret;
    }
}