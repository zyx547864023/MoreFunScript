package real_combat.combat;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.listeners.DamageDealtModifier;
import com.fs.starfarer.api.combat.listeners.WeaponRangeModifier;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.ui.newui._;
import org.lwjgl.util.vector.Vector2f;
import real_combat.hullmods.RealCombatHullmod;

public class RealCombatEveryFrameCombatPlugin implements EveryFrameCombatPlugin {

	public static final Map<WeaponAPI.WeaponSize, Float> ballisticRangeUp = new HashMap<>(3);
	public static final Map<WeaponAPI.WeaponSize, Float> ballisticDamageDown = new HashMap<>(3);
	public static final float engineRangeUp = 100f;
	public static final float missileRangeUp = 150f;

	static {
		ballisticRangeUp.put(WeaponAPI.WeaponSize.LARGE, 150f);
		ballisticRangeUp.put(WeaponAPI.WeaponSize.MEDIUM, 100f);
		ballisticRangeUp.put(WeaponAPI.WeaponSize.SMALL, 50f);

		ballisticDamageDown.put(WeaponAPI.WeaponSize.LARGE, -50f);
		ballisticDamageDown.put(WeaponAPI.WeaponSize.MEDIUM, -30f);
		ballisticDamageDown.put(WeaponAPI.WeaponSize.SMALL, -15f);
	}

	public void advance(float amount, List<InputEventAPI> events) {
		CombatEngineAPI engine = Global.getCombatEngine();
		List<ShipAPI> shipList = engine.getShips();
		for(ShipAPI ship:shipList) {
			if (ship.getListeners(DamageDealtMod.class).size() == 0) {
				ship.addListener(new DamageDealtMod());
			}
			if (ship.getListeners(WeaponRangeMod.class).size() == 0) {
				ship.addListener(new WeaponRangeMod());
			}
		}
	}

	public void renderInUICoords(ViewportAPI viewport) {
	}

	public void init(CombatEngineAPI engine) {
	}


	private static class DamageDealtMod implements DamageDealtModifier {

		@Override
		public String modifyDamageDealt(Object param, CombatEntityAPI target, DamageAPI damage, Vector2f point, boolean shieldHit) {
			WeaponAPI weapon = null;
			if (param instanceof DamagingProjectileAPI) {
				weapon = ((DamagingProjectileAPI) param).getWeapon();
			} else if (param instanceof BeamAPI) {
				weapon = ((BeamAPI) param).getWeapon();
			} else if (param instanceof MissileAPI) {
				weapon = ((MissileAPI) param).getWeapon();
				MissileAPI missile = ((MissileAPI) param);
			}
			String id = "real_combat_dam_mod";
			if (weapon == null) {
				return null;
			}
			if (weapon.getType()!= WeaponAPI.WeaponType.ENERGY&&weapon.getType()!= WeaponAPI.WeaponType.BALLISTIC&&weapon.getType()!= WeaponAPI.WeaponType.BUILT_IN) {
				return null;
			}
			if (weapon.getType()== WeaponAPI.WeaponType.ENERGY)
			{
				//Vector2f weaponlocation = weapon.getLocation();
				//double distance = Math.sqrt((weaponlocation.x - point.x)*(weaponlocation.x - point.x)+(weaponlocation.y - point.y)*(weaponlocation.y - point.y));
				//如果距离大于射程威力减小，距离小于射程威力增加，最大为2
				if (param instanceof BeamAPI) {
					double percent = ((BeamAPI) param).getLength()/weapon.getRange();
					if(percent<=0.3) {
						//100/0.8*(0.8-percent);
						damage.getModifier().modifyPercent(id, (float) (100/0.3*(0.3-percent)));
					}
					else{
						//-100/2*(percent)
						damage.getModifier().modifyPercent(id, (float) (-100*percent));
					}
				}
				if (param instanceof DamagingProjectileAPI) {
					Vector2f weaponlocation = ((DamagingProjectileAPI) param).getSpawnLocation();
					double distance = Math.sqrt((weaponlocation.x - point.x)*(weaponlocation.x - point.x)+(weaponlocation.y - point.y)*(weaponlocation.y - point.y));
					//如果距离在原版射程内80%才开始100%原版威力0-->2 0-->0.8是200-->100 0.8-->2是100-->0
					double percent = distance/weapon.getRange();
					if(percent<=0.4) {
						//100/0.8*(0.8-percent);
						damage.getModifier().modifyPercent(id, (float) (100/0.4*(0.4-percent)));
					}
					else{
						//-100/2*(percent)
						damage.getModifier().modifyPercent(id, (float) (-100*percent));
					}
				}
			}
			if (weapon.getType()== WeaponAPI.WeaponType.BALLISTIC)//||weapon.getType()==WeaponType.BUILT_IN
			{
				damage.getModifier().modifyPercent(id, ballisticDamageDown.get(weapon.getSize()));
			}
			return id;
		}
	}

	private static class WeaponRangeMod implements WeaponRangeModifier {

		@Override
		public float getWeaponRangePercentMod(ShipAPI ship, WeaponAPI weapon) {
			boolean hasOverLoad = false;
			for(String hullMod : ship.getVariant().getHullMods()){
				if("safetyoverrides".equals(hullMod))
				{
					hasOverLoad = true;
				}
			}
			//原版是450,如果射程*2应该让超载在900才开始衰减
			if(hasOverLoad) {
				float range = weapon.getSpec().getMaxRange();
				float rangeOld = range;
				if (weapon.getType() == WeaponAPI.WeaponType.BALLISTIC) {
					//加强之后的射程
					range = range*(1+(ballisticRangeUp.get(weapon.getSize()) / 100f));
					return (ballisticRangeUp.get(weapon.getSize())) / 100f+countOldNewRange(range,(ballisticRangeUp.get(weapon.getSize())) / 100f)/rangeOld;
				} else if (weapon.getType() == WeaponAPI.WeaponType.MISSILE) {
					//加强之后的射程
					range = range*(1+(missileRangeUp/ 100f));
					return missileRangeUp / 100f+countOldNewRange(range, missileRangeUp / 100f)/rangeOld;
				} else if (weapon.getType() == WeaponAPI.WeaponType.ENERGY) {
					//加强之后的射程
					range = range*(1+(engineRangeUp/ 100f));
					return engineRangeUp / 100f+countOldNewRange(range, engineRangeUp / 100f)/rangeOld;
				}
			}
			else {
				if (weapon.getType() == WeaponAPI.WeaponType.BALLISTIC) {
					return (ballisticRangeUp.get(weapon.getSize())) / 100f;
				} else if (weapon.getType() == WeaponAPI.WeaponType.MISSILE) {
					return missileRangeUp / 100f;
				} else if (weapon.getType() == WeaponAPI.WeaponType.ENERGY) {
					return engineRangeUp / 100f;
				}
			}
			return 0f;
		}

		@Override
		public float getWeaponRangeMultMod(ShipAPI ship, WeaponAPI weapon) {
			return 1f;
		}

		@Override
		public float getWeaponRangeFlatMod(ShipAPI ship, WeaponAPI weapon) {
			return 0f;
		}
	}


	public void renderInWorldCoords(ViewportAPI viewport) {
		
	}

	public void processInputPreCoreControls(float amount, List<InputEventAPI> events) {
		
	}

	private static float countOldNewRange(float range, float rangeUp)
	{
		if(range>900) {
			//原版射程
			//int rangeOld = Math.round(450 + (range - 450) / 4);
			//改版射程
			int rangeNew = Math.round(450*(rangeUp+1) + (range - 450*(rangeUp+1))/ 4);
			//新的射程-450
			//实际应该增加的射程
			float rangeAdd = ((rangeNew - 450)*4)+450-range;
			return rangeAdd;
		}
		else if (range>=450&&900>=range){
			//原版射程
			//float rangeOld = Math.round(450 + (range - 450) / 4);
			//改版射程
			float rangeNew = range;
			//新的射程-450
			//实际应该增加的射程
			float rangeAdd = ((rangeNew - 450)*4)+450-range;
			return rangeAdd;
		}
		return 1;
	}
}
