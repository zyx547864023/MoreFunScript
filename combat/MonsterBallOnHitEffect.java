package real_combat.combat;

import com.fs.starfarer.api.campaign.CampaignEventListener;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.FleetEncounterContextPlugin;
import com.fs.starfarer.api.campaign.IntelDataAPI;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.fleet.FleetMemberType;
import com.fs.starfarer.api.impl.campaign.ids.Stats;
import com.fs.starfarer.api.impl.combat.DisintegratorEffect;
import com.fs.starfarer.api.mission.FleetSide;
import org.lazywizard.lazylib.combat.CombatUtils;
import org.lwjgl.util.vector.Vector2f;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.listeners.ApplyDamageResultAPI;
import com.fs.starfarer.api.util.Misc;

import java.util.EnumSet;
import java.util.List;

public class MonsterBallOnHitEffect implements OnHitEffectPlugin {

	public static float DAMAGE = 250;

	private CombatEntityAPI target = null;
	private boolean isPluginExist = false;

	public void onHit(DamagingProjectileAPI projectile, CombatEntityAPI target,
					  Vector2f point, boolean shieldHit, ApplyDamageResultAPI damageResult, CombatEngineAPI engine) {
		if (!shieldHit && target instanceof ShipAPI) {
			if(target.getOwner()!=projectile.getOwner()) {
				this.target = target;
				ShipAPI ship = ((ShipAPI) target);
				if(!ship.isFighter()) {
					/*
					if(!isPluginExist){
						LayeredRenderingPlugin layeredRenderingPlugi = new LayeredRenderingPlugin(projectile.getOwner());
						engine.addLayeredRenderingPlugin(layeredRenderingPlugi);
						isPluginExist=true;
					}
					 */
					FleetMemberAPI member = Global.getFactory().createFleetMember( FleetMemberType.SHIP, ship.getVariant());
					member.setOwner(FleetSide.PLAYER.ordinal());
					member.getCrewComposition().addCrew(member.getNeededCrew());
					ShipAPI newShip = Global.getCombatEngine().getFleetManager(FleetSide.PLAYER)
							.spawnFleetMember(member, ship.getLocation(), ship.getFacing(), 0f);
					newShip.setCRAtDeployment(ship.getCurrentCR());
					newShip.setCurrentCR(ship.getCurrentCR());
					newShip.setOwner(FleetSide.PLAYER.ordinal());
					newShip.getShipAI().forceCircumstanceEvaluation();

					CampaignFleetAPI playerFleet = Global.getSector().getPlayerFleet();
					playerFleet.getFleetData().addFleetMember(newShip.getFleetMember());
					//ship.getMutableStats().getDynamic().getMod(Stats.INDIVIDUAL_SHIP_RECOVERY_MOD).modifyFlat(ship.getId(), 0f);
					try {
						List<CampaignFleetAPI> fleetList = playerFleet.getBattle().getNonPlayerSide();
						for (CampaignFleetAPI f : fleetList) {
							//if(f.getFleetData().getMembersListCopy().size()>1) {
								f.getFleetData().removeFleetMember(ship.getFleetMember());
							//}
						}
					}catch (Exception e) {
						Global.getLogger(this.getClass()).info(e);
					}
					Global.getCombatEngine().removeObject(target);
				}
			}
		}
	}

	class LayeredRenderingPlugin implements CombatLayeredRenderingPlugin
	{
		float time = 0f;
		int owner = 0;

		public LayeredRenderingPlugin(int owner) {
			this.owner = owner;
		}

		@Override
		public void init(CombatEntityAPI entity) {

		}

		@Override
		public void cleanup() {

		}

		@Override
		public boolean isExpired() {
			if(time>5f)
			{
				return true;
			}
			return false;
		}

		@Override
		public void advance(float amount) {
			time+=amount;
			if(time<=5f)
			{
				target.setOwner(0);
			} else {
				target.setOwner(1);
			}
		}

		@Override
		public EnumSet<CombatEngineLayers> getActiveLayers() {
			return null;
		}

		@Override
		public float getRenderRadius() {
			return 0;
		}

		@Override
		public void render(CombatEngineLayers layer, ViewportAPI viewport) {

		}
	}
}
