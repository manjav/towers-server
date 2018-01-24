package com.gerantech.towers.sfs.battle.bots;

/**
 * Created by ManJav on 1/25/2018.
 */
public enum BotActions
{
    INIT(-100),

    NO_CHANCE(-1),

    FIGHT(1),

    TRANSFORM(10),

    START_STICKER(21);


    public final int value;

    BotActions(int value) {
        this.value = value;
    }
}