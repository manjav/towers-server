package com.gerantech.towers.sfs.utils;

import com.gt.data.RankData;
import com.hazelcast.map.EntryBackupProcessor;
import com.hazelcast.map.EntryProcessor;

import java.io.Serializable;
import java.util.Map;

/**
 * Created by ManJav on 10/26/2017.
 */
public class ResetWeeklyEntryProcessor implements EntryProcessor<Integer, RankData>, Serializable
{
    @Override
    public Object process(Map.Entry<Integer, RankData> entry)
    {
        RankData value = entry.getValue();
 //       if( value.weeklyBattles > 0 || value.weeklyStars > 0 )
 //       {
                value.weeklyBattles = 0;
                value.weeklyStars = 0;
            entry.setValue(value);
//        System.out.print("id:" + entry.getKey() + " weekly-battles:" + value.weeklyBattles +"\n");
//        }
        return value;
    }

    @Override
    public EntryBackupProcessor<Integer, RankData> getBackupProcessor()
    {
        return null;
    }
}
