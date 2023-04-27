package real_combat.combat;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.listeners.DamageDealtModifier;
import com.fs.starfarer.api.combat.listeners.WeaponRangeModifier;
import com.fs.starfarer.api.input.InputEventAPI;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 只用于加载渲染
 */
public class RC_WeaponsEveryFrameCombatPlugin implements EveryFrameCombatPlugin {
	private final static String IS_CATCH = "IS_CATCH";
	private CombatEngineAPI engine = Global.getCombatEngine();
	@Override
	public void processInputPreCoreControls(float amount, List<InputEventAPI> events) {

	}
	@Override
	public void advance(float amount, List<InputEventAPI> events) {
		if (engine==null) {return;}
		if (engine.isPaused()) {return;}
		/*
		if(engine.getCustomData().get(IS_CATCH)!=null)
		{
			List<ShipAPI> isCatch = (List<ShipAPI>)engine.getCustomData().get(IS_CATCH);
			for (ShipAPI s:isCatch)
			{
				int count = 0;
				List<ShipEngineControllerAPI.ShipEngineAPI> shipEngines = s.getEngineController().getShipEngines();
				for (int e = 0; e < shipEngines.size(); e++) {
					ShipEngineControllerAPI.ShipEngineAPI oe = shipEngines.get(e);
					if (!oe.isDisabled()) {
						oe.disable(true);
						count++;
					}
				}
				List<WeaponAPI> shipWeapons = s.getAllWeapons();
				for (int w = 0; w < shipWeapons.size(); w++) {
					WeaponAPI ow = shipWeapons.get(w);
					if (!ow.isDisabled()) {
						ow.disable(true);
						count++;
					}
				}
				s.setShipSystemDisabled(true);
				if (count != 0)
				{
					engine.addFloatingText(s.getLocation(), "占领成功", 50f, Color.GREEN, s, 5f, 10f);
				}
			}
		}
		 */
	}

	@Override
	public void renderInWorldCoords(ViewportAPI viewport) {

	}

	@Override
	public void renderInUICoords(ViewportAPI viewport) {
	}

	@Override
	public void init(CombatEngineAPI engine) {
		engine.addLayeredRenderingPlugin(new RC_RepairCombatLayeredRenderingPlugin());
	}
}
