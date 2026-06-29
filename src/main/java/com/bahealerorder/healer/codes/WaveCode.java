package com.bahealerorder.healer.codes;

import com.bahealerorder.common.strategies.WaveStrategy;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class WaveCode implements WaveStrategy
{
	private String id;
	private String name;
	private int wave;
	private boolean builtIn;
	private String sourceText;
	private List<CallCode> calls = new ArrayList<>();

	public WaveCode()
	{
	}

	public WaveCode(String id, String name, int wave, boolean builtIn, String sourceText, List<CallCode> calls)
	{
		this.id = id;
		this.name = name;
		this.wave = wave;
		this.builtIn = builtIn;
		this.sourceText = sourceText;
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

	public String getSourceText()
	{
		return sourceText;
	}

	public void setSourceText(String sourceText)
	{
		this.sourceText = sourceText;
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
