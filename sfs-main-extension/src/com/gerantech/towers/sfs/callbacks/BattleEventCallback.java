package com.gerantech.towers.sfs.callbacks;

import com.gerantech.towers.sfs.battle.BattleRoom;
import com.gt.towers.battle.fieldes.FieldData;
import com.gt.towers.battle.units.Unit;
import com.gt.towers.events.BattleEvent;
import com.gt.towers.events.EventCallback;
import haxe.root.Array;

import java.util.Iterator;
import java.util.Map;

public class BattleEventCallback implements EventCallback
{
    private final BattleRoom battleRoom;
    public BattleEventCallback(BattleRoom battleRoom)
    {
        this.battleRoom = battleRoom;
        Iterator<Map.Entry<Object, Unit>> iterator = battleRoom.battleField.units._map.entrySet().iterator();
        while( iterator.hasNext() )
        {
            iterator.next().getValue().eventCallback = this;
        }
    }

    @Override
    public void dispatch(int id, String type, Object data)
    {
        if( type == BattleEvent.DISPOSE )
        {
            //battleRoom.trace(battleRoom.battleField.map.type, battleRoom.battleField.map.type.equals(FieldData.TYPE_HEADQUARTER));
            if( battleRoom.battleField.map.type.equals(FieldData.TYPE_HEADQUARTER) )
            {
                if( battleRoom.battleField.units.get(id).card.type == 201 )
                {
                    //battleRoom.trace(id, type, battleRoom.battleField.map.type, battleRoom.battleField.units.get(id).card.type);
                    battleRoom.endCalculator.scores[id] = 0;
                    battleRoom.endCalculator.scores[id == 0 ? 1 : 0] = 1;
                }
            }
        }
    }

    @Override
    public boolean __hx_deleteField(String s) {
        return false;
    }

    @Override
    public Object __hx_lookupField(String s, boolean b, boolean b1) {
        return null;
    }

    @Override
    public double __hx_lookupField_f(String s, boolean b) {
        return 0;
    }

    @Override
    public Object __hx_lookupSetField(String s, Object o) {
        return null;
    }

    @Override
    public double __hx_lookupSetField_f(String s, double v) {
        return 0;
    }

    @Override
    public double __hx_setField_f(String s, double v, boolean b) {
        return 0;
    }

    @Override
    public Object __hx_setField(String s, Object o, boolean b) {
        return null;
    }

    @Override
    public Object __hx_getField(String s, boolean b, boolean b1, boolean b2) {
        return null;
    }

    @Override
    public double __hx_getField_f(String s, boolean b, boolean b1) {
        return 0;
    }

    @Override
    public Object __hx_invokeField(String s, Array array) {
        return null;
    }

    @Override
    public void __hx_getFields(Array<String> array) {

    }
}
