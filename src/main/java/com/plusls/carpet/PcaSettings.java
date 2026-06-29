package com.plusls.carpet;

import carpet.api.settings.Rule;
import carpet.api.settings.RuleCategory;
import com.plusls.carpet.settings.PcaSyncProtocolValidator;

public class PcaSettings
{
	public static final String PCA = "PCA";
	public static final String PROTOCOL = "protocal";

	@Rule(
			categories = {PCA, PROTOCOL, RuleCategory.CLIENT},
			validators = PcaSyncProtocolValidator.class
	)
	public static boolean pcaSyncProtocol = false;

	@Rule(
			categories = {PCA, PROTOCOL, RuleCategory.CLIENT}
	)
	public static PcaSyncPlayerEntityOption pcaSyncPlayerEntity = PcaSyncPlayerEntityOption.OPS;

	public enum PcaSyncPlayerEntityOption
	{
		NOBODY,
		BOT,
		OPS,
		OPS_AND_SELF,
		EVERYONE
	}
}
