package com.gerantech.towers.sfs.callbacks;

import com.gerantech.towers.sfs.battle.BattleRoom;
import com.gt.towers.battle.units.Unit;
import com.gt.towers.events.EventCallback;
import com.gt.towers.events.UnitEvent;
import haxe.root.Array;

public class BattleEventCallback implements EventCallback
{
    private final BattleRoom battleRoom;

    public BattleEventCallback(BattleRoom battleRoom)
    {
        this.battleRoom = battleRoom;
    }

    @Override
    public void dispatch(int id, String type, Object data)
    {
        if( type == UnitEvent.ATTACK )
        {
            battleRoom.sendAttackResponse(id, (Integer) data);
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
