package com.github.cheesesoftware.PowerfulPerms.common;

public class PMR
{
	private boolean success = true;
	private String response = "";
	
	public PMR(String response)
	{
		this.response = response;
	}
	
	public PMR(boolean success, String response)
	{
		this.success = success;
		this.response = response;
		
	}
	
	public boolean isSucceeded()
	{
		return this.success;
	}
	
	public String getResponse()
	{
		return (this.success ? "Success! " : "Failure: ") + this.response;
	}
}
