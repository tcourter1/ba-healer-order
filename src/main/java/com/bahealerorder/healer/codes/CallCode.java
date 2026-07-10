package com.bahealerorder.healer.codes;

import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@Getter
@Setter
public class CallCode
{
	private int callIndex;
	private List<HealerInstruction> healerInstructions = new ArrayList<>();
	private String note;

	public CallCode(int callIndex, List<HealerInstruction> healerInstructions, String note)
	{
		this.callIndex = Math.max(0, callIndex);
		this.healerInstructions = healerInstructions == null ? new ArrayList<>() : new ArrayList<>(healerInstructions);
		this.note = note;
	}

	public void setCallIndex(int callIndex)
	{
		this.callIndex = Math.max(0, callIndex);
	}

	public HealerInstruction getInstruction(int healerOrder)
	{
		int index = healerOrder - 1;

		if (index < 0 || index >= getHealerInstructions().size()) return null;

		return getHealerInstructions().get(index);
	}
}
