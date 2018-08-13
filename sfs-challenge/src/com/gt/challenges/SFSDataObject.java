package com.gt.challenges;

import com.gt.towers.utils.maps.IntIntMap;
import com.smartfoxserver.v2.entities.data.ISFSArray;
import com.smartfoxserver.v2.entities.data.ISFSObject;
import com.smartfoxserver.v2.entities.data.SFSArray;
import com.smartfoxserver.v2.entities.data.SFSObject;

/**
 * Created by ManJav on 8/13/2018.
 */
public class SFSDataObject extends SFSObject
{

    public SFSDataObject()
    {
        super();
    }

    protected void setMap(String name, IntIntMap requirements)
    {
        ISFSObject sfs;
        ISFSArray ret = new SFSArray();
        int[] keys = requirements.keys();
        int i = 0;
        while ( i < keys.length )
        {
            sfs = new SFSObject();
            sfs.putInt("key", keys[i]);
            sfs.putInt("value", requirements.get(keys[i]));
            ret.addSFSObject(sfs);
            i ++;
        }
        putSFSArray(name, ret);
    }
}
