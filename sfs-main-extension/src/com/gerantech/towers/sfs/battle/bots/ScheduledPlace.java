package com.gerantech.towers.sfs.battle.bots;

import com.gt.towers.buildings.Place;

import java.util.Timer;

/**
 * Created by ManJav on 1/25/2018.
 */
public class ScheduledPlace
{
    public int target;
    public long fightTime;
    public Place place;
    public Timer timer;

    public ScheduledPlace(Place place)
    {
        this.place = place;
    }

    public void dispose()
    {
        if( timer != null )
        {
            fightTime = -1;
            timer.cancel();
            timer = null;
        }
    }
}
