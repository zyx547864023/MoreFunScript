package real_combat.combat;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.EveryFrameCombatPlugin;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ViewportAPI;
import com.fs.starfarer.api.input.InputEventAPI;
import real_combat.ai.RC_SmartAI;

import java.util.List;

/**
 * AI
 */
public class RC_SmartAIEveryFrameCombatPlugin implements EveryFrameCombatPlugin {
	private CombatEngineAPI engine = Global.getCombatEngine();
	@Override
	public void processInputPreCoreControls(float amount, List<InputEventAPI> events) {

	}
	@Override
	public void advance(float amount, List<InputEventAPI> events) {
		if (engine==null) {return;}
		if (engine.isPaused()) {return;}
		for (ShipAPI s:engine.getShips()) {
			if (s.getOwner() == 0&&!s.isFighter()&&s!=Global.getCombatEngine().getPlayerShip() && s.isAlive()) {
				RC_SmartAI smartAI = new RC_SmartAI(s);
				smartAI.firingSynergy();
				smartAI.saveMissile();
				smartAI.safeV();
				smartAI.retreatCommand();
			}
		}
	}

	@Override
	public void renderInWorldCoords(ViewportAPI viewport) {

	}

	@Override
	public void renderInUICoords(ViewportAPI viewport) {
	}

	@Override
	public void init(CombatEngineAPI engine) {
	}
}
