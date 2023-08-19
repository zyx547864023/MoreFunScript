package real_combat.weapons;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.listeners.ApplyDamageResultAPI;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lwjgl.util.vector.Vector2f;
import org.w3c.dom.ranges.Range;
import real_combat.entity.RC_AnchoredEntity;

import java.util.ArrayList;
import java.util.List;

public class RC_MirrorOnHitEffect implements OnHitEffectPlugin,EveryFrameWeaponEffectPlugin {
	private final static String ID="RC_MirrorOnHitEffect";
	private final static String WEAPON = "mirror";
	private final static String RANGE = "RANGE";
	private final static String BACK = "BACK";
	private final static String GO = "GO";
	private final static String STATUS = "STATUS";
	private final static String POINT = "POINT";
	public void onHit(DamagingProjectileAPI projectile, CombatEntityAPI target,
					  Vector2f point, boolean shieldHit, ApplyDamageResultAPI damageResult, CombatEngineAPI engine) {
		List<CombatEntityAPI> projectileList = new ArrayList<>();
		if (engine.getCustomData().get(ID)!=null) {
			projectileList = (List<CombatEntityAPI>) engine.getCustomData().get(ID);
		}
		CombatEntityAPI newprojectile = engine.spawnProjectile(projectile.getSource(), projectile.getWeapon(), projectile.getWeapon().getId(),
			point, VectorUtils.getAngle(point,projectile.getWeapon().getLocation()), projectile.getSource().getVelocity());
		newprojectile.setCollisionClass(CollisionClass.NONE);
		newprojectile.setCustomData(ID+STATUS,BACK);
		newprojectile.setCustomData(ID+POINT,new Vector2f(point));
		float range = projectile.getWeapon().getRange();
		if (projectile.getCustomData().get(ID+RANGE)!=null)
		{
			range = (Float) projectile.getCustomData().get(ID+RANGE);
			if (projectile.getCustomData().get(ID+POINT)==null) {
				range = range - MathUtils.getDistance(point,projectile.getSpawnLocation());
			}
			else{
				range = range - MathUtils.getDistance(point,(Vector2f) projectile.getCustomData().get(ID+POINT));
			}
		}
		newprojectile.setCustomData(ID+RANGE,range);
		projectileList.add(newprojectile);
		engine.getCustomData().put(ID,projectileList);
	}

	@Override
	public void advance(float amount, CombatEngineAPI engine, WeaponAPI weapon) {
		if (engine.isPaused()) {
			return;
		}
		if (!weapon.getShip().isAlive()||weapon.getShip().getFluxTracker().isOverloaded()) {
			return;
		}
		if (weapon.isDisabled()) {
			return;
		}

		List<CombatEntityAPI> projectileList = new ArrayList<>();
		if (engine.getCustomData().get(ID)!=null) {
			projectileList = (List<CombatEntityAPI>) engine.getCustomData().get(ID);
		}
		List<CombatEntityAPI> removeList = new ArrayList<>();
		for (CombatEntityAPI p:projectileList) {
			if (!(p instanceof DamagingProjectileAPI)) {
				continue;
			}
			float range = 0;
			if (p.getCustomData().get(ID+RANGE)!=null)
			{
				range = (Float) p.getCustomData().get(ID+RANGE);
			}
			if (p.getCustomData().get(ID+STATUS)==null||p.getCustomData().get(ID+POINT)==null) {
				return;
			}
			if (range<=MathUtils.getDistance(p.getLocation(),(Vector2f) p.getCustomData().get(ID+POINT))) {
				removeList.add(p);
				continue;
			}

			if (BACK.equals(p.getCustomData().get(ID+STATUS).toString())){
				p.getVelocity().set(MathUtils.getPoint(new Vector2f(0,0),p.getVelocity().length(),VectorUtils.getAngle(p.getLocation(),((DamagingProjectileAPI) p).getWeapon().getLocation())));
				if (MathUtils.getDistance(p.getLocation(),((DamagingProjectileAPI) p).getWeapon().getLocation())<p.getCollisionRadius()) {
					p.getVelocity().set(MathUtils.getPoint(new Vector2f(),p.getVelocity().length(),((DamagingProjectileAPI) p).getWeapon().getCurrAngle()));
					p.setCollisionClass(CollisionClass.PROJECTILE_FF);
					p.setCustomData(ID+STATUS,GO);
					//计算剩余距离
					range = range - MathUtils.getDistance(p.getLocation(),(Vector2f) p.getCustomData().get(ID+POINT));
					p.setCustomData(ID+RANGE,range);
					p.setCustomData(ID+POINT,new Vector2f(((DamagingProjectileAPI) p).getWeapon().getLocation()));
				}
			}
		}
		for (CombatEntityAPI r:removeList) {
			projectileList.remove(r);
			engine.removeObject(r);
		}
		engine.getCustomData().put(ID,projectileList);
	}
}
