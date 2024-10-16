package real_combat.weapons;

import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.listeners.ApplyDamageResultAPI;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lwjgl.util.vector.Vector2f;

import java.util.ArrayList;
import java.util.List;

public class RC_MirrorOnHitEffect implements OnHitEffectPlugin,EveryFrameWeaponEffectPlugin,OnFireEffectPlugin  {
	private final static String ID="RC_MirrorOnHitEffect";
	private final static String WEAPON = "mirror";
	private final static String RANGE = "RANGE";
	private final static String BACK = "BACK";
	private final static String GO = "GO";
	private final static String STATUS = "STATUS";
	private final static String POINT = "POINT";
	public void onHit(DamagingProjectileAPI projectile, CombatEntityAPI target,
					  Vector2f point, boolean shieldHit, ApplyDamageResultAPI damageResult, CombatEngineAPI engine) {
		if (projectile.isFading()) {
			return;
		}
		List<MirrorProj> projectileList = new ArrayList<>();
		if (engine.getCustomData().get(ID)!=null) {
			projectileList = (List<MirrorProj>) engine.getCustomData().get(ID);
		}
		float range = projectile.getWeapon().getRange();
		Vector2f newPoint = projectile.getSpawnLocation();
		MirrorProj old = null;
		for (MirrorProj m:projectileList) {
			if (m.projectile.equals(projectile)) {
				old = m;
				range = m.range;
				newPoint = m.point;
			}
		}
		CombatEntityAPI newprojectile = engine.spawnProjectile(projectile.getSource(), projectile.getWeapon(), projectile.getWeapon().getId(),
			point, VectorUtils.getAngle(point,projectile.getWeapon().getFirePoint(0)), new Vector2f(0,0));
		newprojectile.setCollisionClass(CollisionClass.NONE);
		range = range - MathUtils.getDistance(point,newPoint);
		if (old==null) {
			MirrorProj mirrorProj = new MirrorProj(1,0,(DamagingProjectileAPI) newprojectile,BACK,range,new Vector2f(point));
			projectileList.add(mirrorProj);
			engine.getCustomData().put(ID,projectileList);
		}
		else {
			old.status = BACK;
			old.range = range;
			old.point = new Vector2f(point);
			old.projectile = (DamagingProjectileAPI) newprojectile;
		}
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

		List<MirrorProj> projectileList = new ArrayList<>();
		if (engine.getCustomData().get(ID)!=null) {
			projectileList = (List<MirrorProj>) engine.getCustomData().get(ID);
		}
		List<MirrorProj> removeList = new ArrayList<>();
		for (MirrorProj p:projectileList) {
			/*
			if (!(p instanceof DamagingProjectileAPI)) {
				continue;
			}
			float range = p.range;
			if (p.getCustomData().get(ID+RANGE)!=null)
			{
				range = (Float) p.getCustomData().get(ID+RANGE);
			}
			if (p.getCustomData().get(ID+STATUS)==null||p.getCustomData().get(ID+POINT)==null) {
				return;
			}
			 */
			p.angle-=amount*1800;
			if (p.range<=MathUtils.getDistance(p.projectile.getLocation(),p.point)||p.projectile.isFading()) {
				p.alphaMult-=amount*2;
				if (p.alphaMult<0) {
					p.alphaMult=0;
					removeList.add(p);
					continue;
				}
				/**
				 * 增加一个飞并且慢慢渐变的尸体
				 */
			}
			if (BACK.equals(p.status)){
				if (MathUtils.getDistance(p.projectile.getLocation(),p.projectile.getWeapon().getFirePoint(0))<=10f) {
					p.projectile.getVelocity().set(MathUtils.getPoint(new Vector2f(0,0),p.projectile.getVelocity().length(),p.projectile.getWeapon().getCurrAngle()));
					p.projectile.setCollisionClass(CollisionClass.PROJECTILE_FF);
					p.status = GO;
					//p.setCustomData(ID+STATUS,GO);
					//计算剩余距离
					p.range = p.range - MathUtils.getDistance(p.projectile.getLocation(),p.point);
					p.projectile.setCustomData(ID+RANGE,p.range );
					p.projectile.setCustomData(ID+POINT,new Vector2f(p.projectile.getWeapon().getFirePoint(0)));
				}
				else {
					p.projectile.getVelocity().set(MathUtils.getPoint(new Vector2f(0,0),p.projectile.getVelocity().length(),VectorUtils.getAngle(p.projectile.getLocation(),p.projectile.getWeapon().getFirePoint(0))));
				}
			}
		}
		for (MirrorProj r:removeList) {
			projectileList.remove(r);
			engine.removeObject(r.projectile);
		}
		engine.getCustomData().put(ID,projectileList);
	}

	@Override
	public void onFire(DamagingProjectileAPI projectile, WeaponAPI weapon, CombatEngineAPI engine) {
		List<MirrorProj> projectileList = new ArrayList<>();
		if (engine.getCustomData().get(ID)!=null) {
			projectileList = (List<MirrorProj>) engine.getCustomData().get(ID);
		}
		MirrorProj mirrorProj = new MirrorProj(1,0,(DamagingProjectileAPI) projectile,BACK,weapon.getRange(),projectile.getSpawnLocation());
		projectileList.add(mirrorProj);
		engine.getCustomData().put(ID,projectileList);
	}

	public class MirrorProj{
		public float alphaMult;
		public float angle;
		public DamagingProjectileAPI projectile;
		public String status;
		public float range;
		public Vector2f point;
		public MirrorProj(float alphaMult,float angle,DamagingProjectileAPI projectile,String status,float range,Vector2f point)
		{
			this.alphaMult = alphaMult;
			this.angle = angle;
			this.projectile = projectile;
			this.status = status;
			this.range = range;
			this.point = point;

		}
	}
}
