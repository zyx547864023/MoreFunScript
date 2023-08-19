package real_combat.weapons.bak;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.listeners.ApplyDamageResultAPI;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lazywizard.lazylib.combat.entities.AnchoredEntity;
import org.lwjgl.util.vector.Vector2f;

import java.util.*;

public class MonsterBallOnHitEffectBack implements OnHitEffectPlugin {
	private final String ID="monster_ball_shooter_sec";
	private CombatEntityAPI target = null;
	private boolean isPluginExist = false;

	private Map<CombatEntityAPI,CombatEntityAPI> speedMap =new HashMap();
	private Map<CombatEntityAPI, Float> targetFacingMap =new HashMap();
	private Map<CombatEntityAPI, Float> landingFacingMap =new HashMap();
	private Map<CombatEntityAPI, Float> landingDistanceMap =new HashMap();
	private Map<CombatEntityAPI, Float> angleMap =new HashMap();
	private List<AnchoredEntity> anchoredEntityList = new ArrayList<>();
	public void onHit(DamagingProjectileAPI projectile, CombatEntityAPI target,
					  Vector2f point, boolean shieldHit, ApplyDamageResultAPI damageResult, CombatEngineAPI engine) {
		int nowMarines = 9999;
		int addMarines = 0;
		//如果不是模拟训练
		if(!(engine.isSimulation()||engine.isMission()))
		{
			//每次命中都要消耗陆战队 如果是玩家的舰队
			if(projectile.getOwner()==0)
			{
				nowMarines = Global.getSector().getPlayerFleet().getCargo().getMarines();
				if(nowMarines>=20) {
					nowMarines -= 20;
					addMarines = 20;
				}
				else if(nowMarines>0){
					addMarines = nowMarines;
					nowMarines = 0;
				}
				else
				{
					addMarines = 0;
				}
				Global.getSector().getPlayerFleet().getCargo().removeMarines(addMarines);
			}
		}
		if (!shieldHit) {
			//将注入的陆战队扣除
			if(target instanceof ShipAPI) {
				//登陆舱
				/*
				CombatEntityAPI landing = engine.spawnProjectile(
						projectile.getSource(),
						null,
						ID,
						point,
						projectile.getFacing(),
						target.getVelocity()
				);

				 */
				//相对位置
				Vector2f newLocation = Vector2f.sub(point,target.getLocation(),(Vector2f)null);
				AnchoredEntity a = new AnchoredEntity(target,point);
				float landingToTarget = VectorUtils.getAngle(newLocation,target.getLocation());
				float landingFace = projectile.getFacing();
				float relativeAngle = MathUtils.getShortestRotation(landingToTarget,landingFace);
				//这里存相对角度
				a.setFacing(relativeAngle);
				if(engine.getCustomData().get("anchoredEntityList")!=null)
				{
					anchoredEntityList = (List<AnchoredEntity>) engine.getCustomData().get("anchoredEntityList");
				}
				anchoredEntityList.add(a);
				engine.getCustomData().put("anchoredEntityList",anchoredEntityList);
				/*
				speedMap.put(landing,target);
				targetFacingMap.put(target,target.getFacing());
				landingFacingMap.put(landing,landing.getFacing());
				landingDistanceMap.put(landing,MathUtils.getDistance(target.getLocation(),landing.getLocation()));
				angleMap.put(landing,VectorUtils.getAngle(target.getLocation(),landing.getLocation()));

				List<Map<CombatEntityAPI,CombatEntityAPI>> speedMapList = new ArrayList<>();
				if(engine.getCustomData().get("speedMapList")!=null)
				{
					speedMapList = (List<Map<CombatEntityAPI,CombatEntityAPI>>) engine.getCustomData().get("speedMapList");
				}
				speedMapList.add(speedMap);
				engine.getCustomData().put("speedMapList",speedMapList);
				List<Map<CombatEntityAPI,Float>> targetFacingMapList = new ArrayList<>();
				if(engine.getCustomData().get("targetFacingMapList")!=null)
				{
					targetFacingMapList = (List<Map<CombatEntityAPI,Float>>) engine.getCustomData().get("targetFacingMapList");
				}
				targetFacingMapList.add(targetFacingMap);
				engine.getCustomData().put("targetFacingMapList",targetFacingMapList);
				List<Map<CombatEntityAPI,Float>> landingDistanceMapList = new ArrayList<>();
				if(engine.getCustomData().get("landingDistanceMapList")!=null)
				{
					landingDistanceMapList = (List<Map<CombatEntityAPI,Float>>) engine.getCustomData().get("landingDistanceMapList");
				}
				landingDistanceMapList.add(landingDistanceMap);
				engine.getCustomData().put("landingDistanceMapList",landingDistanceMapList);
				List<Map<CombatEntityAPI,Float>> angleMapList = new ArrayList<>();
				if(engine.getCustomData().get("angleMapList")!=null)
				{
					angleMapList = (List<Map<CombatEntityAPI,Float>>) engine.getCustomData().get("angleMapList");
					angleMapList.add(angleMap);
				}
				engine.getCustomData().put("angleMapList",angleMapList);
				*/
				if(!isPluginExist){
					LayeredRenderingPlugin layeredRenderingPlugin = new LayeredRenderingPlugin();
					engine.addLayeredRenderingPlugin(layeredRenderingPlugin);
					isPluginExist=true;
				}

				if(target.getOwner()!=projectile.getOwner()) {
					//增加某船的陆战队员情况
					int nowShipMarines = 0;
					if(engine.getCustomData().get(((ShipAPI) target).getId()+"_nowMarines")!=null)
					{
						nowShipMarines = (int) engine.getCustomData().get(((ShipAPI) target).getId()+"_nowMarines");
					}
					nowShipMarines += addMarines;
					engine.getCustomData().put(((ShipAPI) target).getId()+"_nowMarines",nowShipMarines);
					engine.getCustomData().remove(((ShipAPI) target).getId() + "_text");
				}
			}
		}
	}

	class LayeredRenderingPlugin implements CombatLayeredRenderingPlugin
	{
		public LayeredRenderingPlugin() {

		}

		@Override
		public void init(CombatEntityAPI entity) {

		}

		@Override
		public void cleanup() {

		}

		@Override
		public boolean isExpired() {
			return false;
		}

		@Override
		public void advance(float amount) {
			for (CombatEntityAPI landing : speedMap.keySet()) {
				//速度相等
				CombatEntityAPI target = speedMap.get(landing);
				landing.getVelocity().set(target.getVelocity());
				landing.setFacing(landingFacingMap.get(landing)+(target.getFacing() - targetFacingMap.get(target)));
				landing.getLocation().set(MathUtils.getPoint(target.getLocation(),landingDistanceMap.get(landing),angleMap.get(landing)+(target.getFacing() - targetFacingMap.get(target))));
			}
		}

		@Override
		public EnumSet<CombatEngineLayers> getActiveLayers() {
			return EnumSet.of(CombatEngineLayers.ABOVE_SHIPS_LAYER);
		}

		@Override
		public float getRenderRadius() {
			return 100000;
		}

		@Override
		public void render(CombatEngineLayers layer, ViewportAPI viewport) {
		}
	}
}
