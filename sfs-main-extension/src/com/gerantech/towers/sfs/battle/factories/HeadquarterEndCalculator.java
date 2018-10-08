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
       // roomClass.trace("check", scores[0] + scores[1]);
        return( scores[0] + scores[1] > 0 );
    }
}