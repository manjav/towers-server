package com.gerantech.towers.sfs.battle.factories;

import com.gerantech.towers.sfs.battle.BattleRoom;
import com.gt.towers.Game;
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
    private static int MIN_POINTS = 3;
    private static int COE_POINTS = 3;
    public static IntIntMap get(Game game, BattleRoom battleRoom, int star, float ratio, int now)
    {
        IntIntMap ret = new IntIntMap();
        if( battleRoom.battleField.friendlyMode > 0 )
        {
            ret.set(ResourceType.R15_BATTLES_FRIENDLY, 1);
            return ret;
        }
        int mode = battleRoom.getPropertyAsInt("mode");
        int league = game.player.get_arena(0);

        // points
        int point = 0;
        if( ratio > 1 )
        {
            if( league == 0 )
                point = 13;
            else
                point = ((int) (MIN_POINTS + star * COE_POINTS + Math.ceil(Math.random() * COE_POINTS - COE_POINTS * 0.5))) * mode;
        }
        else if( ratio < 1 && game.player.get_point() > 50 && game.player.get_battleswins() > 6  )
        {
            point = ((int) (-MIN_POINTS + star * COE_POINTS + Math.ceil(Math.random() * COE_POINTS - COE_POINTS * 0.5))) * mode;
        }

        if( game.player.isBot() )
            return ret;

        // battle stats
        ret.set(ResourceType.R12_BATTLES, 1);
        ret.set(ResourceType.R16_WIN_RATE, getWinRate(game, league, star, ratio));
        if( game.player.get_arena(0) > 0 )
            ret.set(ResourceType.R17_STARS, star);

        if( ratio > 1 )
        {
            // num wins
            ret.set(ResourceType.R13_BATTLES_WINS, 1);
            ret.set(ResourceType.R30_CHALLENGES, mode + 1);

            // soft-currency
            int soft = 2 * Math.max(0, star) + Math.min(league * 2, Math.max(0, game.player.get_point() - game.player.get_softs())) * mode;
            if( soft != 0 )
                ret.set(ResourceType.R3_CURRENCY_SOFT, soft);

            /*int dailyBattles = game.exchanger.items.exists(ExchangeType.C29_DAILY_BATTLES) ? game.exchanger.items.get(ExchangeType.C29_DAILY_BATTLES).numExchanges : 0;
            if( mode == 0 && dailyBattles > 10 )
            {
                point = (int) (point * Math.pow(10f / dailyBattles, 0.2));
                soft = (int) (soft * Math.pow(10f / dailyBattles, 0.8));
            }*/
            ret.set(ResourceType.R3_CURRENCY_SOFT, soft);

            // random book
            List<Integer> emptySlotsType = getEmptySlots(game, now);
            if( emptySlotsType.size() > 0 )
            {
                int randomEmptySlotIndex = game.player.get_battleswins() == 0 ? 0 : (int) Math.floor(Math.random() * emptySlotsType.size());
                ExchangeItem emptySlot = game.exchanger.items.get(emptySlotsType.get(randomEmptySlotIndex));
                game.exchanger.findRandomOutcome(emptySlot, now);
                ret.set(emptySlot.outcome, emptySlot.type);
            }
        }
        if( point != 0 )
            ret.set(ResourceType.R2_POINT, point);
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

    private static List<Integer> getEmptySlots(Game game, int now)
    {
        List<Integer> ret = new ArrayList<>();
        int[] keys = game.exchanger.items.keys();
        for (int k : keys )
            if( game.exchanger.items.get(k).category == ExchangeType.C110_BATTLES && game.exchanger.items.get(k).getState(now) == ExchangeItem.CHEST_STATE_EMPTY )
                ret.add(game.exchanger.items.get(k).type);
        return ret;
    }
}