package real_combat.campaign;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.BattleAPI;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.DModManager;
import com.fs.starfarer.api.impl.campaign.FleetEncounterContext;
import com.fs.starfarer.api.loading.VariantSource;
import com.fs.starfarer.api.util.Misc;

import java.util.List;
import java.util.Random;

public class RC_FleetEncounterContext extends FleetEncounterContext {

    @Override
    public List<FleetMemberAPI> getRecoverableShips(BattleAPI battle, CampaignFleetAPI winningFleet, CampaignFleetAPI otherFleet) {
        List<FleetMemberAPI> result = super.getRecoverableShips(battle, winningFleet, otherFleet);

        if (Misc.isPlayerOrCombinedContainingPlayer(otherFleet)) {
            return result;
        }
        DataForEncounterSide winnerData = getDataFor(winningFleet);

        float playerContribMult = computePlayerContribFraction();
        List<FleetMemberData> enemyCasualties = winnerData.getEnemyCasualties();

        for (FleetMemberData data : enemyCasualties) {
            if (Misc.isUnboardable(data.getMember())) {
                continue;
            }
            if ((data.getStatus() != Status.DISABLED) && (data.getStatus() != Status.DESTROYED)) {
                continue;
            }
            /* Don't double-add */
            if (result.contains(data.getMember())) {
                continue;
            }
            if (getStoryRecoverableShips().contains(data.getMember())) {
                continue;
            }
            if (playerContribMult > 0f) {
                data.getMember().setCaptain(Global.getFactory().createPerson());

                ShipVariantAPI variant = data.getMember().getVariant();
                variant = variant.clone();
                variant.setSource(VariantSource.REFIT);
                variant.setOriginalVariant(null);
                data.getMember().setVariant(variant, false, true);

                Random dModRandom = new Random(1000000 * data.getMember().getId().hashCode() + Global.getSector().getPlayerBattleSeed());
                dModRandom = Misc.getRandom(dModRandom.nextLong(), 5);
                DModManager.addDMods(data, false, Global.getSector().getPlayerFleet(), dModRandom);
                if (DModManager.getNumDMods(variant) > 0) {
                    DModManager.setDHull(variant);
                }

                prepareShipForRecovery(data.getMember(), false, false, false, 1, 1, getSalvageRandom());

                getStoryRecoverableShips().add(data.getMember());
            }
        }

        return result;
    }
}
