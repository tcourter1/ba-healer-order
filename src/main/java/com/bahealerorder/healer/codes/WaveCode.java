package com.bahealerorder.healer.codes;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WaveCode
{
	private String id;
	private String name;
	private int wave;
	private boolean builtIn;
	private HealerCodeOverstock overstock = HealerCodeOverstock.REGULAR;
	private boolean alchHorn;
	private String restockingInstructions;
	private String additionalNotes;
	private String sourceText;
	private Map<Integer, Integer> expectedTimesSeconds = new HashMap<>();
	private List<CallCode> calls = new ArrayList<>();

	public WaveCode()
	{
	}

	public WaveCode(String id, String name, int wave, boolean builtIn, List<CallCode> calls)
	{
		this.id = id;
		this.name = name;
		this.wave = wave;
		this.builtIn = builtIn;
		this.calls = calls == null ? new ArrayList<>() : new ArrayList<>(calls);
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

	public HealerCodeOverstock getOverstock()
	{
		return HealerCodeOverstock.valueOrRegular(overstock);
	}

	public void setOverstock(HealerCodeOverstock overstock)
	{
		this.overstock = HealerCodeOverstock.valueOrRegular(overstock);
	}

	public boolean isAlchHorn()
	{
		return alchHorn;
	}

	public void setAlchHorn(boolean alchHorn)
	{
		this.alchHorn = alchHorn;
	}

	public String getRestockingInstructions()
	{
		return restockingInstructions;
	}

	public void setRestockingInstructions(String restockingInstructions)
	{
		this.restockingInstructions = restockingInstructions;
	}

	public String getAdditionalNotes()
	{
		return additionalNotes;
	}

	public void setAdditionalNotes(String additionalNotes)
	{
		this.additionalNotes = additionalNotes;
	}

	String getStoredSourceText()
	{
		return sourceText;
	}

	void clearStoredSourceText()
	{
		this.sourceText = null;
	}

	public Map<Integer, Integer> getExpectedTimesSeconds()
	{
		if (expectedTimesSeconds == null)
		{
			expectedTimesSeconds = new HashMap<>();
		}

		return expectedTimesSeconds;
	}

	public void setExpectedTimesSeconds(Map<Integer, Integer> expectedTimesSeconds)
	{
		this.expectedTimesSeconds = expectedTimesSeconds == null ? new HashMap<>() : new HashMap<>(expectedTimesSeconds);
	}

	public Integer getExpectedTimeSeconds(int healerOrder)
	{
		return getExpectedTimesSeconds().get(healerOrder);
	}

	public void setExpectedTimeSeconds(int healerOrder, Integer seconds)
	{
		if (seconds == null)
		{
			getExpectedTimesSeconds().remove(healerOrder);
			return;
		}

		getExpectedTimesSeconds().put(healerOrder, seconds);
	}

	public String getSourceText()
	{
		return HealerCodeFormatter.format(this);
	}

	public List<CallCode> getCalls()
	{
		return calls == null ? Collections.emptyList() : calls;
	}

	public void setCalls(List<CallCode> calls)
	{
		this.calls = calls == null ? new ArrayList<>() : new ArrayList<>(calls);
	}

	public CallCode getCall(int callIndex)
	{
		for (CallCode call : getCalls())
		{
			if (call.getCallIndex() == callIndex)
			{
				return call;
			}
		}

		return null;
	}

	@Override
	public String toString()
	{
		return name == null ? id : name;
	}
}
