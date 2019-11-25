package com.gt.utils;

import java.util.List;

/**
 * IPushUtils
 */
public interface IPushUtils
{
    public String getPushId(int playerId);
    public List<String> getPushIds(Integer[] playerIds);
    public void send(String message, String data, int playerId);
    public int send(String message, String data, Integer[] players );
}