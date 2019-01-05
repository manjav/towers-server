package com.gerantech.towers.sfs.battle.factories;

import com.gt.towers.Game;
import com.gt.towers.battle.BattleField;
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
    public static int COE_POINTS = 8;

    public static IntIntMap get(Game game, BattleField battleField, int score, float ratio, int now)
    {
        IntIntMap ret = new IntIntMap();
        if( battleField.friendlyMode )
        {
            ret.set(ResourceType.R15_BATTLES_FRIENDLY, 1);
            ret.set(ResourceType.R14_BATTLES_WEEKLY, 1);
            return ret;
        }

        if( battleField.field.isOperation() )
        {
            if( game.player.isBot() )
                return ret;

            int diffScore = score - game.player.operations.get(battleField.field.index);
            boolean newRecord = diffScore > 0;

            if( game.player.inTutorial() )
                return ret;

            if( newRecord )
            {
                // xp
                ret.set(ResourceType.R1_XP, diffScore + 1);

                // soft-currency
                ret.set(ResourceType.R3_CURRENCY_SOFT, 10 * diffScore + battleField.field.index * 2);
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
            ret.set(ResourceType.R16_WIN_RATE, getWinStreak(game, arena, score));

            if( point > 0 )
            {
                // soft-currency
                ret.set(ResourceType.R3_CURRENCY_SOFT, 2 * Math.max(0, score) + Math.min(arena * 2, Math.max(0, game.player.get_point() - game.player.get_softs())));

                // num wins
                ret.set(ResourceType.R13_BATTLES_WINS, 1);

                // random book
                List<Integer> emptySlotsType = getEmptySlots(game, game.player.get_battleswins() == 1);
                if( emptySlotsType.size() > 0 )
                {
                    int randomEmptySlotIndex = game.player.get_battleswins() == 0 ? 3 : (int) Math.floor(Math.random() * emptySlotsType.size());
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

    private static List<Integer> getEmptySlots(Game game, boolean forced)
    {
        int now = (int) Instant.now().getEpochSecond();
        List<Integer> ret = new ArrayList<>();

        for (ExchangeItem ei : game.exchanger.items.values() )
            if( ei.category == ExchangeType.C110_BATTLES && (forced || ei.getState(now) == ExchangeItem.CHEST_STATE_EMPTY) )
                ret.add(ei.type);
        return ret;
    }
}