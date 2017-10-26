package com.gerantech.towers.sfs.utils;

import com.gt.hazel.RankData;
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
    public Object process(Map.Entry<Integer, RankData> entry) {
        RankData value = entry.getValue();
        if( value.xp > -1 ) {
            value.xp = 0;
            entry.setValue(value);
            System.out.print("id:" + value.id + " xp:" + value.xp +"\n");
        }
        return value;
    }

    @Override
    public EntryBackupProcessor<Integer, RankData> getBackupProcessor() {
        return null;
    }
}
