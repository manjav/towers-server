package com.gt.data;

import com.gt.towers.battle.units.Unit;
import com.gt.towers.utils.maps.IntUnitMap;
import com.smartfoxserver.v2.entities.data.SFSArray;
import com.smartfoxserver.v2.entities.data.SFSObject;

import java.util.Iterator;
import java.util.Map;

public class UnitData
{
    public double x;
    public double y;
    public double health;

    public UnitData(double x, double y, double health)
    {
        this.x = x;
        this.y = y;
        this.health = health;
    }

    public static SFSObject toSFS(Unit unit)
    {
        SFSObject ret = new SFSObject();
        ret.putInt("i", unit.id);
        ret.putInt("s", unit.side);
        ret.putInt("t", unit.card.type);
        ret.putInt("l", unit.card.level);
        ret.putDouble("h", unit.health);
        ret.putDouble("x", unit.x);
        ret.putDouble("y", unit.y);
        return ret;
    }


    public static SFSArray toSFSArray(IntUnitMap unitMap)
    {
        SFSArray ret = new SFSArray();
        Iterator<Map.Entry<Object, Unit>> iterator = unitMap._map.entrySet().iterator();
        while( iterator.hasNext() )
            ret.addSFSObject(toSFS(iterator.next().getValue()));
        return ret;
    }
}
