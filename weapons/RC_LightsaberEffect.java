package real_combat.weapons;

import com.fs.starfarer.api.combat.*;
import org.lwjgl.util.vector.Vector2f;

/**
	速度相差越大 额外伤害越高
 */
public class RC_LightsaberEffect implements BeamEffectPlugin {
	public RC_LightsaberEffect() { }
	public void advance(float amount, CombatEngineAPI engine, BeamAPI beam) {
		if (beam.getDamageTarget() != null) {
			CombatEntityAPI target = beam.getDamageTarget();
			ShipAPI ship = beam.getSource();
			Vector2f shipV = ship.getVelocity();
			Vector2f targetV = target.getVelocity();
			float difference = Vector2f.sub(shipV, targetV, null).length();
			float dv = Math.abs(ship.getAngularVelocity() - target.getAngularVelocity());
			beam.getDamage().setDamage(beam.getDamage().getBaseDamage()+(difference+dv)*amount);
		}
	}
}





