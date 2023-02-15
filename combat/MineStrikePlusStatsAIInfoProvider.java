package real_combat.combat;

import com.fs.starfarer.api.combat.ShipAPI;

public interface MineStrikePlusStatsAIInfoProvider {
	float getFuseTime();
	float getMineRange(ShipAPI ship);
	//float getMineRange();
}
