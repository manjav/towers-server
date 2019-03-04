package com.gt.data;

import java.io.Serializable;
public class RankData implements Serializable
{
	private static final long serialVersionUID = 1L;
	public String name;
	public int point;
	public RankData(String name, int point)
	{
	    super();
		this.name = name;
		this.point = point;
	}
}