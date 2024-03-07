package net.celsiusqc.totw_modded;

import net.fabricmc.api.ModInitializer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// Very important comment
public class TowersOfTheWildModded implements ModInitializer {
	public static final String MOD_ID = "totw_modded";
	public static final Logger LOGGER = LoggerFactory.getLogger("totw_modded");

	@Override
	public void onInitialize() {
		LOGGER.info("Hello Fabric world!");
	}
}