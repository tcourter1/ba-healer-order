package com.bahealerorder.healer.codes;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
public class WaveCode
{
	@Getter
	@Setter
	private String id;
	@Getter
	@Setter
	private String name;
	@Getter
	private int wave;
	@Getter
	@Setter
	private boolean builtIn;
	private HealerCodeOverstock overstock = HealerCodeOverstock.REGULAR;
	@Getter
	@Setter
	private boolean alchHorn;
	@Getter
	@Setter
	private String customOverstockInstructions;
	@Getter
	private Integer expectedWaveEndSeconds;
	@Getter
	@Setter
	private String restockingInstructions;
	@Getter
	@Setter
	private String additionalNotes;
	private String sourceText;
	@Getter
	@Setter
	private Map<Integer, Integer> expectedTimesSeconds = new HashMap<>();
	@Getter
	@Setter
	private List<CallCode> calls = new ArrayList<>();

	public WaveCode(String id, String name, int wave, boolean builtIn, List<CallCode> calls)
	{
		this.id = id;
		this.name = name;
		this.wave = wave;
		this.builtIn = builtIn;
		this.calls = calls == null ? new ArrayList<>() : new ArrayList<>(calls);
	}

	public HealerCodeOverstock getOverstock()
	{
		return HealerCodeOverstock.valueOrRegular(overstock);
	}

	public void setOverstock(HealerCodeOverstock overstock)
	{
		this.overstock = HealerCodeOverstock.valueOrRegular(overstock);
	}

	public void setExpectedWaveEndSeconds(Integer expectedWaveEndSeconds)
	{
		this.expectedWaveEndSeconds = expectedWaveEndSeconds == null ? null : Math.max(0, expectedWaveEndSeconds);
	}

	String getStoredSourceText()
	{
		return sourceText;
	}

	void clearStoredSourceText()
	{
		this.sourceText = null;
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

	public CallCode getCall(int callIndex)
	{
		for (CallCode call : getCalls())
		{
			if (call.getCallIndex() == callIndex) return call;
		}

		return null;
	}

	@Override
	public String toString()
	{
		return name == null ? id : name;
	}
}
