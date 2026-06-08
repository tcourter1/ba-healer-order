package com.bahealerorder.healer.codes;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class CallCode
{
	private int callIndex;
	private List<HealerInstruction> healerInstructions = new ArrayList<>();
	private String note;

	public CallCode()
	{
	}

	public CallCode(int callIndex, List<HealerInstruction> healerInstructions, String note)
	{
		this.callIndex = Math.max(0, callIndex);
		this.healerInstructions = healerInstructions == null ? new ArrayList<>() : new ArrayList<>(healerInstructions);
		this.note = note;
	}

	public int getCallIndex()
	{
		return callIndex;
	}

	public void setCallIndex(int callIndex)
	{
		this.callIndex = Math.max(0, callIndex);
	}

	public List<HealerInstruction> getHealerInstructions()
	{
		return healerInstructions == null ? Collections.emptyList() : healerInstructions;
	}

	public void setHealerInstructions(List<HealerInstruction> healerInstructions)
	{
		this.healerInstructions = healerInstructions == null ? new ArrayList<>() : new ArrayList<>(healerInstructions);
	}

	public String getNote()
	{
		return note;
	}

	public void setNote(String note)
	{
		this.note = note;
	}

	public HealerInstruction getInstruction(int healerOrder)
	{
		int index = healerOrder - 1;

		if (index < 0 || index >= getHealerInstructions().size())
		{
			return null;
		}

		return getHealerInstructions().get(index);
	}
}
