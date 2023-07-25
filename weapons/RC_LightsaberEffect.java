package real_combat.weapons;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.BoundsAPI.SegmentAPI;
import com.fs.starfarer.api.combat.WeaponAPI.WeaponType;
import com.fs.starfarer.api.impl.combat.RiftCascadeMineExplosion;
import com.fs.starfarer.api.loading.MissileSpecAPI;
import com.fs.starfarer.api.util.IntervalUtil;
import com.fs.starfarer.api.util.Misc;
import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.util.vector.Vector2f;
import real_combat.util.RC_Util;

import java.awt.*;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.util.List;

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
			beam.getDamage().setDamage(beam.getDamage().getBaseDamage()+difference*amount);
		}
	}
}





