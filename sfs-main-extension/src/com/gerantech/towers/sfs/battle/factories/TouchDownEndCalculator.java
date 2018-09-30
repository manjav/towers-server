package com.gerantech.towers.sfs.battle.factories;

import com.gerantech.towers.sfs.battle.BattleRoom;
import com.gt.towers.battle.BattleField;
import com.gt.towers.battle.units.Unit;
import java.util.Iterator;
import java.util.Map;

public class TouchDownEndCalculator extends EndCalculator
{
    public TouchDownEndCalculator(BattleRoom roomClass)
    {
        super(roomClass);
    }

    @Override
    public boolean check()
    {
        Unit unit = checkUnitPassed();
        if( unit == null )
            return false;
        roomClass.trace("unit passed " + unit.id);
        roomClass.battleField.units.clear();
        scores[unit.side] ++;

        for (int g : scores)
            if( g > 3 )
                return true;

        return false;
    }

    private Unit checkUnitPassed()
    {
        Unit u;
        Iterator<Map.Entry<Object, Unit>> iterator = roomClass.battleField.units._map.entrySet().iterator();
        while( iterator.hasNext() ) {
           u = iterator.next().getValue();
           if( (u.side == 0 && u.y > BattleField.HEIGHT) || (u.side == 1 && u.y < 0) )
               return u;
        }
        return null;
    }
}
