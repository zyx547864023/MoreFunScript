package real_combat.weapons;

import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.impl.PlayerFleetPersonnelTracker;
import com.fs.starfarer.api.impl.campaign.ids.Commodities;
import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.util.vector.Vector2f;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.listeners.ApplyDamageResultAPI;
import real_combat.entity.RC_AnchoredEntity;

import java.util.*;
import java.util.List;

public class RC_MonsterBallOnHitEffect implements OnHitEffectPlugin {
	private final static String ID="MONSTER_BALL_ON_HIT_EFFECT";
	private final static String ANCHORED_ENTITY_LIST="ANCHORED_ENTITY_LIST";
	private final static String NOW_MARINES = "NOW_MARINES";
	private final static String TEXT = "TEXT";
	private List<RC_AnchoredEntity> anchoredEntityList = new ArrayList<>();
	public void onHit(DamagingProjectileAPI projectile, CombatEntityAPI target,
					  Vector2f point, boolean shieldHit, ApplyDamageResultAPI damageResult, CombatEngineAPI engine) {
		int nowMarines = 9999;
		int addMarines = 20;
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
				ShipAPI ship = (ShipAPI) target;
				//相对位置
				float targetFace = target.getFacing();
				float landingFace = projectile.getFacing();
				float relativeAngle = MathUtils.getShortestRotation(targetFace,landingFace);
				RC_AnchoredEntity a = new RC_AnchoredEntity(target,point,relativeAngle);
				if(engine.getCustomData().get(ANCHORED_ENTITY_LIST)!=null)
				{
					anchoredEntityList = (List<RC_AnchoredEntity>) engine.getCustomData().get(ANCHORED_ENTITY_LIST);
				}
				anchoredEntityList.add(a);
				engine.getCustomData().put(ANCHORED_ENTITY_LIST,anchoredEntityList);

				if(target.getOwner()!=projectile.getOwner()) {
					//增加某船的陆战队员情况
					int nowShipMarines = 0;
					if(ship.getCustomData().get(ID+NOW_MARINES)!=null)
					{
						nowShipMarines = (int) ship.getCustomData().get(ID+NOW_MARINES);
					}
					nowShipMarines += addMarines;
					ship.setCustomData(ID+NOW_MARINES,nowShipMarines);
					ship.getCustomData().remove(ID+TEXT);
				}
			}
		}
	}
}
