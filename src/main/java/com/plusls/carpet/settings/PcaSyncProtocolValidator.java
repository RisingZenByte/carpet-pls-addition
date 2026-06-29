package com.plusls.carpet.settings;

import carpet.api.settings.CarpetRule;
import carpet.api.settings.Validator;
import com.plusls.carpet.network.PcaSyncProtocol;
import net.minecraft.commands.CommandSourceStack;
import org.jetbrains.annotations.Nullable;

public class PcaSyncProtocolValidator extends Validator<Boolean>
{
	@Override
	public Boolean validate(@Nullable CommandSourceStack source, CarpetRule<Boolean> changingRule, Boolean newValue, String userInput)
	{
		if (newValue)
		{
			PcaSyncProtocol.enablePcaSyncProtocolGlobal();
		}
		else
		{
			PcaSyncProtocol.disablePcaSyncProtocolGlobal();
		}
		return newValue;
	}
}
