package real_combat.weapons;

import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.impl.combat.RiftLanceEffect;
import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.util.vector.Vector2f;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.listeners.ApplyDamageResultAPI;
import com.fs.starfarer.api.util.IntervalUtil;
import com.fs.starfarer.api.util.Misc;
import real_combat.entity.RC_AnchoredEntity;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * 飞行过程中有烟和电弧从本体发散 园内 连接
 * 打中本体给给低伤害电弧到引擎武器 减机动性50%
 */
public class RC_StrongAcidCannonEffect extends BaseCombatLayeredRenderingPlugin implements OnHitEffectPlugin,OnFireEffectPlugin {
	private static String ID = "RC_StrongAcidCannonEffect";
	private final static float TIME_FOR_EXPLOSION = 1f;
	private final static Color FRINGE = new Color(100, 0, 25, 100);
	private final static Color CORE =  new Color(100,60,255,255);
	private final static float THICKNESS = 20f;
	protected IntervalUtil tracker = new IntervalUtil(0.3f, 0.4f);
	public RC_StrongAcidCannonEffect() {
	}
	
	public void onHit(DamagingProjectileAPI projectile, CombatEntityAPI target, Vector2f point, boolean shieldHit, ApplyDamageResultAPI damageResult, CombatEngineAPI engine) {
		//if (shieldHit) return;
		//if (projectile.isFading()) return;
		//if (!(target instanceof ShipAPI)) return;
		if(projectile.getSource()!=null)
		{
			if (projectile.getSource().getCustomData().get(ID)!=null) {
				projList = (List<DamagingProjectileAPI>) projectile.getSource().getCustomData().get(ID);
				projList.remove(projectile);
				projectile.getSource().setCustomData(ID,projList);
			}
		}
		RC_AnchoredEntity a = new RC_AnchoredEntity(target,point, projectile.getFacing());
		RC_StrongAcidCannonEffect effect = new RC_StrongAcidCannonEffect(new ElectromagnetismProj(a, target, projectile.getWeapon(), projectile, shieldHit));
		engine.addLayeredRenderingPlugin(effect);
	}

	@Override
	public void advance(float amount) {
		CombatEngineAPI engine = Global.getCombatEngine();
		if (!engine.isPaused()) {
			if (ep!=null) {
				tracker.advance(amount);
				ep.timeForExplosion -= amount;
				if (ep.target instanceof ShipAPI) {
					((ShipAPI) ep.target).getMutableStats().getMaxSpeed().modifyPercent(ID, 0.5F);
					((ShipAPI) ep.target).getMutableStats().getMaxTurnRate().modifyPercent(ID, 0.5F);
					if (tracker.intervalElapsed()) {
						dealDamage(((ShipAPI) ep.target), ep.projectile);
					}
				}
				for (int i = 0; i < 7; i++) {
					Vector2f partstartloc = MathUtils.getPointOnCircumference(ep.projectile.getLocation(), 20f*MathUtils.getRandomNumberInRange(0.5F, 2F), ep.anchoredEntity.getRelativeAngle()+MathUtils.getRandomNumberInRange(-45F, 45F));
					Vector2f partvec = ep.target.getVelocity();
					//Vector2f partvec = Vector2f.sub(partstartloc, ep.projectile.getLocation(), (Vector2f) null);
					partvec.scale(ep.timeForExplosion);
					float size = MathUtils.getRandomNumberInRange(40F, 60F);
					engine.addNegativeNebulaParticle(partstartloc, partvec, size*ep.timeForExplosion, 1.5f, 0.05f, 0f, MathUtils.getRandomNumberInRange(0.1f, 1f), RiftLanceEffect.getColorForDarkening(Color.GREEN));
					engine.addSwirlyNebulaParticle(partstartloc, partvec, size, 2f, 0.05f, 0f, MathUtils.getRandomNumberInRange(0.1f, 1f), Color.GREEN, false);
					/*
					float emp = ep.projectile.getEmpAmount();
					float dam = ep.projectile.getDamageAmount();
					if (((ShipAPI) ep.target).getEngineController() != null) {
						for (ShipEngineControllerAPI.ShipEngineAPI e : ((ShipAPI) ep.target).getEngineController().getShipEngines()) {
							EmpArcEntityAPI empArcEntity = engine.spawnEmpArcPierceShields(ep.projectile.getSource(), ep.anchoredEntity.getLocation(), ep.target, ep.target,
									DamageType.ENERGY,
									dam,
									emp, // emp
									100000f, // max range
									"tachyon_lance_emp_impact",
									THICKNESS, // thickness
									FRINGE,
									CORE
							);
						}
					}
					EmpArcEntityAPI empArcEntity = engine.spawnEmpArcPierceShields(ep.projectile.getSource(), ep.anchoredEntity.getLocation(), ep.target, ep.target,
							DamageType.ENERGY,
							dam,
							emp, // emp
							100000f, // max range
							"tachyon_lance_emp_impact",
							THICKNESS, // thickness
							FRINGE,
							CORE
					);
					 */
				}
			}
		}
	}

	@Override
	public boolean isExpired() {
		if (ep.timeForExplosion <=0 ) {
			if (ep.target instanceof ShipAPI) {
				((ShipAPI) ep.target).getMutableStats().getMaxSpeed().unmodifyPercent(ID);
				((ShipAPI) ep.target).getMutableStats().getMaxTurnRate().unmodifyPercent(ID);
			}
			return true;
		}
		return false;
	}

	protected ElectromagnetismProj ep;
	public RC_StrongAcidCannonEffect(ElectromagnetismProj ep) {
		this.ep = ep;
	}

	private List<DamagingProjectileAPI> projList = new ArrayList<DamagingProjectileAPI>();
	@Override
	public void onFire(DamagingProjectileAPI projectile, WeaponAPI weapon, CombatEngineAPI engine) {
		if(projectile.getSource()!=null)
		{
			if (projectile.getSource().getCustomData().get(ID)!=null) {
				projList = (List<DamagingProjectileAPI>) projectile.getSource().getCustomData().get(ID);
			}
			projList.add(projectile);
			projectile.getSource().setCustomData(ID,projList);
		}
	}

	public class ElectromagnetismProj{
		public RC_AnchoredEntity anchoredEntity;
		public CombatEntityAPI target;
		public WeaponAPI weapon;
		public DamagingProjectileAPI projectile;
		public Boolean shieldHit;
		public float distance = 1.5F;
		public float timeForExplosion = TIME_FOR_EXPLOSION;
		public ElectromagnetismProj(RC_AnchoredEntity anchoredEntity,CombatEntityAPI target,WeaponAPI weapon,DamagingProjectileAPI projectile,Boolean shieldHit)
		{
			this.anchoredEntity = anchoredEntity;
			this.target = target;
			this.weapon = weapon;
			this.projectile = projectile;
			this.shieldHit = shieldHit;
		}
	}

	public static int NUM_TICKS = 22;
	public static float TOTAL_DAMAGE = 500;

	protected void dealDamage(ShipAPI target,DamagingProjectileAPI proj) {
		CombatEngineAPI engine = Global.getCombatEngine();

		Vector2f point = new Vector2f(entity.getLocation());

		// maximum armor in a cell is 1/15th of the ship's stated armor rating

		ArmorGridAPI grid = target.getArmorGrid();
		int[] cell = grid.getCellAtLocation(point);
		if (cell == null) return;

		int gridWidth = grid.getGrid().length;
		int gridHeight = grid.getGrid()[0].length;

		float damageTypeMult = getDamageTypeMult(proj.getSource(), target);

		float damagePerTick = (float) TOTAL_DAMAGE / (float) NUM_TICKS;
		float damageDealt = 0f;
		for (int i = -2; i <= 2; i++) {
			for (int j = -2; j <= 2; j++) {
				if ((i == 2 || i == -2) && (j == 2 || j == -2)) continue; // skip corners

				int cx = cell[0] + i;
				int cy = cell[1] + j;

				if (cx < 0 || cx >= gridWidth || cy < 0 || cy >= gridHeight) continue;

				float damMult = 1/30f;
				if (i == 0 && j == 0) {
					damMult = 1/15f;
				} else if (i <= 1 && i >= -1 && j <= 1 && j >= -1) { // S hits
					damMult = 1/15f;
				} else { // T hits
					damMult = 1/30f;
				}

				float armorInCell = grid.getArmorValue(cx, cy);
				float damage = damagePerTick * damMult * damageTypeMult;
				damage = Math.min(damage, armorInCell);
				if (damage <= 0) continue;

				target.getArmorGrid().setArmorValue(cx, cy, Math.max(0, armorInCell - damage));
				damageDealt += damage;
			}
		}

		if (damageDealt > 0) {
			if (Misc.shouldShowDamageFloaty(proj.getSource(), target)) {
				engine.addFloatingDamageText(point, damageDealt, Misc.FLOATY_ARMOR_DAMAGE_COLOR, target, proj.getSource());
			}
			target.syncWithArmorGridState();
		}

	}

	public static float getDamageTypeMult(ShipAPI source, ShipAPI target) {
		if (source == null || target == null) return 1f;

		float damageTypeMult = target.getMutableStats().getArmorDamageTakenMult().getModifiedValue();
		switch (target.getHullSize()) {
			case CAPITAL_SHIP:
				damageTypeMult *= source.getMutableStats().getDamageToCapital().getModifiedValue();
				break;
			case CRUISER:
				damageTypeMult *= source.getMutableStats().getDamageToCruisers().getModifiedValue();
				break;
			case DESTROYER:
				damageTypeMult *= source.getMutableStats().getDamageToDestroyers().getModifiedValue();
				break;
			case FRIGATE:
				damageTypeMult *= source.getMutableStats().getDamageToFrigates().getModifiedValue();
				break;
			case FIGHTER:
				damageTypeMult *= source.getMutableStats().getDamageToFighters().getModifiedValue();
				break;
		}
		return damageTypeMult;
	}
}




