package com.gerantech.towers.sfs.battle.factories;

import com.gerantech.towers.sfs.battle.BattleRoom;

public class HeadquarterEndCalculator extends EndCalculator
{
    public HeadquarterEndCalculator(BattleRoom roomClass)
    {
        super(roomClass);
    }

    @Override
    public boolean check()
    {
       // room.trace("check", scores[0] + scores[1]);
        if( scores[0] >= 3 || scores[1] >= 3 )
            return true;
        return false;
    }
}