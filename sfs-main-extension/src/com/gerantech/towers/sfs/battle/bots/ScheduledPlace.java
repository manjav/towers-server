package com.gerantech.towers.sfs.battle.bots;

import com.gt.towers.buildings.Place;

import java.util.Timer;

/**
 * Created by ManJav on 1/25/2018.
 */
public class ScheduledPlace {
    public Place place;
    public double fightTime;
    public Timer timer;

    public ScheduledPlace(Place place)
    {
        this.place = place;
    }

    public void dispose()
    {
        if( timer != null )
        {
            timer.cancel();
            timer = null;
        }
    }
}
