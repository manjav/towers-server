package com.gerantech.towers.sfs.callbacks;

import com.gt.towers.events.ValueChangeCallback;
import com.gt.towers.utils.maps.IntIntMap;
import haxe.root.Array;

/**
 * Created by ManJav on 8/14/2017.
 */

public class MapChangeCallback implements ValueChangeCallback
{
    public IntIntMap inserts = new IntIntMap() ;
    public IntIntMap updates = new IntIntMap();

    @Override
    public void insert(int key, int oldValue, int newValue) {
        if( !inserts.exists(key) )
            inserts.set(key, 0);
    }

    @Override
    public void update(int key, int oldValue, int newValue) {
        if( !updates.exists(key) )
            updates.set(key, 0);
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