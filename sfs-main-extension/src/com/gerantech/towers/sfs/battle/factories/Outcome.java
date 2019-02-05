package com.gerantech.towers.sfs.battle.factories;

import com.gerantech.towers.sfs.battle.BattleRoom;
import com.gt.towers.Game;
import com.gt.towers.constants.ExchangeType;
import com.gt.towers.constants.ResourceType;
import com.gt.towers.exchanges.ExchangeItem;
import com.gt.towers.others.Arena;
import com.gt.towers.utils.maps.IntIntMap;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by ManJav on 2/13/2018.
 */
public class Outcome
{
    public static int MIN_POINTS = 5;
    public static int COE_POINTS = 6;
    public static IntIntMap get(Game game, BattleRoom battleRoom, int star, float ratio, int now)
    {
        IntIntMap ret = new IntIntMap();
        if( battleRoom.battleField.friendlyMode )
        {
            ret.set(ResourceType.R15_BATTLES_FRIENDLY, 1);
            ret.set(ResourceType.R14_BATTLES_WEEKLY, 1);
            return ret;
        }

        if( battleRoom.battleField.field.isOperation() )
        {
            if( game.player.isBot() )
                return ret;

            int diffScore = star - game.player.operations.get(battleRoom.battleField.field.index);
            boolean newRecord = diffScore > 0;

            if( game.player.inTutorial() )
                return ret;

            if( newRecord )
            {
                // xp
                ret.set(ResourceType.R1_XP, diffScore + 1);

                // soft-currency
                ret.set(ResourceType.R3_CURRENCY_SOFT, 10 * diffScore + battleRoom.battleField.field.index * 2);
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
            ret.set(ResourceType.R2_POINT, point );

            if( game.player.isBot() )
                return ret;

            int arena = game.player.get_arena(0);

            // battle stats
            ret.set(ResourceType.R12_BATTLES, 1);
            ret.set(ResourceType.R14_BATTLES_WEEKLY, 1);
            ret.set(ResourceType.R16_WIN_RATE, getWinRate(game, arena, star, ratio));
            if( game.player.get_arena(0) > 0 )
            {
                ret.set(ResourceType.R17_STARS, star);
                ret.set(ResourceType.R18_STARS_WEEKLY, star);
            }

            if( point > 0 )
            {
                // soft-currency
                ret.set(ResourceType.R3_CURRENCY_SOFT, 2 * Math.max(0, star) + Math.min(arena * 2, Math.max(0, game.player.get_point() - game.player.get_softs())));

                // num wins
                ret.set(ResourceType.R13_BATTLES_WINS, 1);

                // random book
                List<Integer> emptySlotsType = getEmptySlots(game);
                if( emptySlotsType.size() > 0 )
                {
                    int randomEmptySlotIndex = game.player.get_battleswins() == 0 ? 0 : (int) Math.floor(Math.random() * emptySlotsType.size());
                    ExchangeItem emptySlot = game.exchanger.items.get(emptySlotsType.get(randomEmptySlotIndex));
                    game.exchanger.findRandomOutcome(emptySlot, now);
                    ret.set(emptySlot.outcome, emptySlot.type);
                }
            }
        }
        return ret;
    }

    private static int getWinRate(Game game, int arena, int star, float ratio)
    {
        int ret = star > 0 ? 1 : -1;
        if( arena == 0 )
            return ret;

        int winRate = game.player.getResource(ResourceType.R16_WIN_RATE);
        //if( winRate > 3 || winRate < -3 )
        //    ret *= (int) Math.abs(winRate * 0.5);
        if( ratio > 1 )
            ret *= star;
        Arena a = game.arenas.get(arena);
        if( ret < 0 && winRate < a.minWinStreak )
            ret = 0;
        return ret;
    }

    private static List<Integer> getEmptySlots(Game game)
    {
        int now = (int) Instant.now().getEpochSecond();
        List<Integer> ret = new ArrayList<>();

        int[] keys = game.exchanger.items.keys();
        for (int k : keys )
            if( game.exchanger.items.get(k).category == ExchangeType.C110_BATTLES && game.exchanger.items.get(k).getState(now) == ExchangeItem.CHEST_STATE_EMPTY )
                ret.add(game.exchanger.items.get(k).type);
        return ret;
    }
}