package real_combat.combat;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.listeners.DamageDealtModifier;
import com.fs.starfarer.api.combat.listeners.WeaponRangeModifier;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.fleet.FleetMemberType;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.loading.VariantSource;
import com.fs.starfarer.api.mission.FleetSide;
import org.json.JSONException;
import org.json.JSONObject;
import org.lwjgl.util.vector.Vector2f;
import real_combat.shipsystems.scripts.RC_TransAmSystem;

public class RealCombatEveryFrameCombatPlugin implements EveryFrameCombatPlugin {
	public static final String ID = "RealCombatEveryFrameCombatPlugin";
	public static final String SAFETYOVERRIDES = "safetyoverrides";
	public static final String HIGH_SCATTER_AMP = "high_scatter_amp";
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
		/*
		try {
			JSONObject json = Global.getSettings().loadJSON("mod_info2.json","real_combat");
			Global.getLogger(this.getClass()).info(json.getJSONObject("version").get("major"));
		} catch (IOException e) {
			throw new RuntimeException(e);
		} catch (JSONException e) {
			throw new RuntimeException(e);
		}
		 */
		CombatEngineAPI engine = Global.getCombatEngine();
		//士气系统
		engine.maintainStatusForPlayerShip("RealCombatEveryFrameCombatPlugin", Global.getSettings().getSpriteName("ui","icon_tactical_cr_bonus"), "士气 100%", "增益", false);
		List<ShipAPI> shipList = engine.getShips();
		for(ShipAPI ship:shipList) {
			if (ship.getListeners(DamageDealtMod.class).size() == 0) {
				ship.addListener(new DamageDealtMod());
			}
			if (ship.getListeners(WeaponRangeMod.class).size() == 0) {
				ship.addListener(new WeaponRangeMod());
			}
			/*
			List<WeaponAPI> newShipWeapons = ship.getAllWeapons();
			for (int w = 0; w < newShipWeapons.size(); w++) {
				WeaponAPI nw = newShipWeapons.get(w);
				nw.getRange();
			}
			 */
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
					if(percent<=0.5) {
						//100/0.8*(0.8-percent);
						damage.getModifier().modifyPercent(ID, (float) (100/0.5*(0.5-percent)));
					}
					else{
						//-100/2*(percent)
						damage.getModifier().modifyPercent(ID, (float) (-100*percent));
					}
				}
				if (param instanceof DamagingProjectileAPI) {
					Vector2f weaponlocation = ((DamagingProjectileAPI) param).getSpawnLocation();
					double distance = Math.sqrt((weaponlocation.x - point.x)*(weaponlocation.x - point.x)+(weaponlocation.y - point.y)*(weaponlocation.y - point.y));
					//如果距离在原版射程内80%才开始100%原版威力0-->2 0-->0.8是200-->100 0.8-->2是100-->0
					double percent = distance/weapon.getRange();
					if(percent<=0.5) {
						//100/0.8*(0.8-percent);
						damage.getModifier().modifyPercent(ID, (float) (100/0.5*(0.5-percent)));
					}
					else{
						//-100/2*(percent)
						damage.getModifier().modifyPercent(ID, (float) (-100*percent));
					}
				}
			}
			if (weapon.getType()== WeaponAPI.WeaponType.BALLISTIC)//||weapon.getType()==WeaponType.BUILT_IN
			{
				damage.getModifier().modifyPercent(ID, ballisticDamageDown.get(weapon.getSize()));
			}
			return ID;
		}
	}

	private static class WeaponRangeMod implements WeaponRangeModifier {

		@Override
		public float getWeaponRangePercentMod(ShipAPI ship, WeaponAPI weapon) {
			boolean hasOverLoad = false;
			boolean hashHighScatterAmp = false;
			for(String hullMod : ship.getVariant().getHullMods()){
				if(SAFETYOVERRIDES.equals(hullMod))
				{
					hasOverLoad = true;
				}
				else if(HIGH_SCATTER_AMP.equals(hullMod)){
					hashHighScatterAmp = true;
				}
			}
			float range = weapon.getSpec().getMaxRange();
			float rangeOld = range;
			//原版是200,如果射程*2应该让硬光在400才开始衰减
			if(hashHighScatterAmp&&weapon.isBeam()){
				return 0;
			}
			if(hasOverLoad) {
				if (weapon.getType() == WeaponAPI.WeaponType.BALLISTIC) {
					//加强之后的射程
					range = range*(1+(ballisticRangeUp.get(weapon.getSize()) / 100f));
					return (ballisticRangeUp.get(weapon.getSize())) / 100f + countOldNewRange(range,(ballisticRangeUp.get(weapon.getSize())) / 100f,450f,4f)/rangeOld;
				} else if (weapon.getType() == WeaponAPI.WeaponType.MISSILE) {
					//加强之后的射程
					range = range*(1+(missileRangeUp/ 100f));
					return missileRangeUp / 100f + countOldNewRange(range, missileRangeUp / 100f,450f,4f)/rangeOld;
				} else if (weapon.getType() == WeaponAPI.WeaponType.ENERGY) {
					//加强之后的射程
					range = range*(1+(engineRangeUp/ 100f));
					return engineRangeUp / 100f + countOldNewRange(range, engineRangeUp / 100f,450f,4f)/rangeOld;
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
			boolean hasOverLoad = false;
			boolean hashHighScatterAmp = false;
			for(String hullMod : ship.getVariant().getHullMods()){
				if(SAFETYOVERRIDES.equals(hullMod))
				{
					hasOverLoad = true;
				}
				if(HIGH_SCATTER_AMP.equals(hullMod)){
					hashHighScatterAmp = true;
				}
			}
			float range = weapon.getSpec().getMaxRange();
			float rangeOld = range;
			//加强之后的射程
			range = range * (1 + (engineRangeUp / 100f));
			//原版是200,如果射程*2应该让硬光在400才开始衰减
			if(hashHighScatterAmp&&weapon.isBeam()){
				if(hasOverLoad) {
					//先计算出来在最新效果下应该有的射程
					float getNewRange = getNewRange(getNewRange(range,(engineRangeUp / 100f),200f,2f),(engineRangeUp / 100f),450f,4f);
					//倒推在原版方程下初始射程
					float oldOriginalRange = getOldOriginalRange(getNewRange,450f,4f);
					//实际硬光减了
					float oldRangleCut = rangeOld - getOldRange(rangeOld, 200f, 2f);
					//初始射程-range
					return oldOriginalRange-rangeOld+oldRangleCut;
				}
				else {
					//实际硬光减了
					float oldRangleCut = rangeOld - getOldRange(rangeOld, 200f, 2f);
					//应该减多少
					float newRangleCut = range - getNewRange(range, (engineRangeUp / 100f), 200f, 2f);
					//硬光应该减多少
					return -newRangleCut + oldRangleCut + rangeOld*(engineRangeUp / 100f);
				}
			}
			return 0f;
		}
	}


	public void renderInWorldCoords(ViewportAPI viewport) {
		
	}

	public void processInputPreCoreControls(float amount, List<InputEventAPI> events) {
		
	}

	private static float countOldNewRange(float range, float rangeUp,float rangeCut,float rangeDivide)
	{
		if(range>rangeCut*(rangeUp+1f)) {
			//原版射程
			//int rangeOld = Math.round(450 + (range - 450) / 4);
			//改版射程
			int rangeNew = Math.round(rangeCut*(rangeUp+1) + (range - rangeCut*(rangeUp+1))/ rangeDivide);
			//新的射程-450
			//实际应该增加的射程
			float rangeAdd = ((rangeNew - rangeCut)*rangeDivide)+rangeCut-range;
			return rangeAdd;
		}
		else if (range>=rangeCut&&rangeCut*(rangeUp+1)>=range){
			//原版射程
			//float rangeOld = Math.round(450 + (range - 450) / 4);
			//改版射程
			float rangeNew = range;
			//新的射程-450
			//实际应该增加的射程
			float rangeAdd = ((rangeNew - rangeCut)*rangeDivide)+rangeCut-range;
			return rangeAdd;
		}
		return 0;
	}

	//新版射程
	private static float getNewRange(float range, float rangeUp,float rangeCut,float rangeDivide)
	{
		if(range>rangeCut*(rangeUp+1f)) {
			//改版射程
			int rangeNew = Math.round(rangeCut*(rangeUp+1) + (range - rangeCut*(rangeUp+1))/ rangeDivide);
			return rangeNew;
		}
		return range;
	}

	//旧版射程
	private static float getOldRange(float range,float rangeCut,float rangeDivide)
	{
		if(range>rangeCut) {
			//改版射程
			int rangeNew = Math.round(rangeCut + (range - rangeCut)/ rangeDivide);
			return rangeNew;
		}
		return range;
	}

	//获取原版初始射程
	//新版本最终射程
	//
	private static float getOldOriginalRange(float newFinalRange,float rangeCut,float rangeDivide)
	{
		if (newFinalRange>rangeCut) {
			float range = (newFinalRange - rangeCut) * rangeDivide + rangeCut;
			return range;
		}
		return newFinalRange;
	}

	public static void  main(String[] args) throws Exception {
		float range = 1200;
		float rangeOld = 600;
		//按照新版计算方式最终的射程
		float getNewRange = getNewRange(getNewRange(range,1,200,2),1,450,4);
		System.out.println("新版最终射程:"+getNewRange);
		//倒推在原版方程下初始射程
		float oldOriginalRange = getOldOriginalRange(getOldOriginalRange(getNewRange,450,4),200,2);
		System.out.println("倒退原始方程下初始射程:"+oldOriginalRange);
		//初始射程-range
		//当前射程如果超过安超射程还要做计算
		System.out.println(1 + (oldOriginalRange-range) / rangeOld);
	}
}
