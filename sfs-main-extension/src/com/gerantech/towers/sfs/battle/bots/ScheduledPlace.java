package com.gerantech.towers.sfs.battle.bots;

/**
 * Created by ManJav on 1/25/2018.
 */
public class ScheduledPlace
{
    public int target;
    public long fightTime;
    public int index;

    public ScheduledPlace(int index, int target, long fightTime)
    {
        this.index = index;
        this.target = target;
        this.fightTime = fightTime;
    }
}
