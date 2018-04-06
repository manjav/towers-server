package com.gerantech.towers.sfs.battle.bots;

import com.gt.towers.buildings.Place;

/**
 * Created by ManJav on 1/25/2018.
 */
public class ScheduledPlace
{
    public int target;
    public long fightTime;
    public Place place;

    public ScheduledPlace(Place place, int target, long fightTime)
    {
        this.target = target;
        this.fightTime = fightTime;
        this.place = place;
    }
}
