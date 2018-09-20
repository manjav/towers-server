package com.gt.data;

import com.gt.towers.utils.lists.IntList;
import com.gt.towers.utils.maps.IntIntMap;
import com.smartfoxserver.v2.entities.data.ISFSArray;
import com.smartfoxserver.v2.entities.data.ISFSObject;
import com.smartfoxserver.v2.entities.data.SFSArray;
import com.smartfoxserver.v2.entities.data.SFSObject;

/**
 * Created by ManJav on 8/13/2018.
 */
public class SFSDataModel extends SFSObject
{

    public SFSDataModel()
    {
        super();
    }

    public static ISFSArray toSFSArray(IntIntMap map)
    {
        ISFSObject sfs;
        ISFSArray ret = new SFSArray();
        int[] keys = map.keys();
        int i = 0;
        while ( i < keys.length )
        {
            sfs = new SFSObject();
            sfs.putInt("key", keys[i]);
            sfs.putInt("value", map.get(keys[i]));
            ret.addSFSObject(sfs);
            i ++;
        }
        return ret;
    }

    public static ISFSArray toSFSArray(IntList list)
    {
        ISFSArray ret = new SFSArray();
        int i = 0;
        while ( i < list.size() )
        {
            ret.addInt(list.get(i));
            i ++;
        }
        return ret;
    }


    protected void setMap(String name, IntIntMap requirements)
    {
        putSFSArray(name, SFSDataModel.toSFSArray(requirements));
    }
}
