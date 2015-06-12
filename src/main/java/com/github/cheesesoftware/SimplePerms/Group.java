package com.github.cheesesoftware.SimplePerms;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class Group
{
	private int id;
	private String name;
	private HashMap<String, String> permissions = new HashMap<String, String>();
	private ArrayList<Group> parents;
	private String prefix;
	private String suffix;

	public Group(int id, String name, HashMap<String, String> permissions, String prefix, String suffix)
	{
		this.id = id;
		this.name = name;
		this.permissions = permissions;
		this.prefix = prefix;
		this.suffix = suffix;
	}

	public int getId()
	{
		return this.id;
	}

	public String getName()
	{
		return this.name;
	}
	
	public ArrayList<Group> getParents()
	{
		return this.parents;
	}
	
	public String getPrefix()
	{
		return prefix;
	}
	
	public String getSuffix()
	{
		return suffix;
	}

	public HashMap<String, String> getPermissions()
	{
		HashMap<String, String> temp = new HashMap<String, String>(permissions);
		temp.putAll(permissions);
		for (Group parent : this.parents)
		{
			temp.putAll(parent.getPermissions_());
		}
		return temp;
	}
	
	public HashMap<String, String> getPermissions_()
	{
		HashMap<String, String> temp = new HashMap<String, String>(permissions);
		for (Group parent : this.parents)
		{
			temp.putAll(parent.getPermissions());
		}
		return temp;
	}
	
	public String getRawPermissions()
	{
		String raw = getRawOwnPermissions();
		for(Group g : parents)
			raw += g.getRawPermissions();
		return raw;
	}
	
	public String getRawOwnPermissions()
	{
		String raw = "";
		for(Map.Entry<String, String> e : permissions.entrySet())
		{
			raw += e.getKey() + (e.getValue().isEmpty() ? "" : ":" + e.getValue()) + ";";
		}
		return raw;
	}
	
	public String getRawOwnParents()
	{
		String raw = "";
		for(Group g : parents)
			raw += g.getId() + ";";
		return raw;
	}
	
	public void setParents(ArrayList<Group> parents)
	{
		this.parents = parents;
	}
}
