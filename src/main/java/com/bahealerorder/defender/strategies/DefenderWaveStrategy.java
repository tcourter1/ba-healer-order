package com.bahealerorder.defender.strategies;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class DefenderWaveStrategy
{
	private String id;
	private String name;
	private int wave;
	private boolean builtIn;
	private String notes;
	private int numberOfLogs;
	private List<DefenderMarker> markers = new ArrayList<>();

	public DefenderWaveStrategy()
	{
	}

	public DefenderWaveStrategy(
			String id,
			String name,
			int wave,
			boolean builtIn,
			String notes,
			int numberOfLogs,
			List<DefenderMarker> markers)
	{
		this.id = id;
		this.name = name;
		this.wave = wave;
		this.builtIn = builtIn;
		this.notes = notes;
		this.numberOfLogs = numberOfLogs;
		this.markers = markers == null ? new ArrayList<>() : new ArrayList<>(markers);
	}

	public String getId()
	{
		return id;
	}

	public void setId(String id)
	{
		this.id = id;
	}

	public String getName()
	{
		return name;
	}

	public void setName(String name)
	{
		this.name = name;
	}

	public int getWave()
	{
		return wave;
	}

	public void setWave(int wave)
	{
		this.wave = wave;
	}

	public boolean isBuiltIn()
	{
		return builtIn;
	}

	public void setBuiltIn(boolean builtIn)
	{
		this.builtIn = builtIn;
	}

	public String getNotes()
	{
		return notes;
	}

	public void setNotes(String notes)
	{
		this.notes = notes;
	}

	public int getNumberOfLogs()
	{
		return Math.max(0, Math.min(4, numberOfLogs));
	}

	public void setNumberOfLogs(int numberOfLogs)
	{
		this.numberOfLogs = Math.max(0, Math.min(4, numberOfLogs));
	}

	public List<DefenderMarker> getMarkers()
	{
		return markers == null ? Collections.emptyList() : markers;
	}

	public void setMarkers(List<DefenderMarker> markers)
	{
		this.markers = markers == null ? new ArrayList<>() : new ArrayList<>(markers);
	}

	@Override
	public String toString()
	{
		return name == null ? id : name;
	}
}
