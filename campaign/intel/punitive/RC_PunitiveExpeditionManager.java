package real_combat.campaign.intel.punitive;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.econ.Industry;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import com.fs.starfarer.api.impl.campaign.ids.Industries;
import com.fs.starfarer.api.impl.campaign.ids.MemFlags;
import com.fs.starfarer.api.impl.campaign.intel.punitive.PunitiveExpeditionIntel;
import com.fs.starfarer.api.impl.campaign.intel.punitive.PunitiveExpeditionManager;
import com.fs.starfarer.api.util.WeightedRandomPicker;
import org.json.JSONObject;
import org.lazywizard.lazylib.MathUtils;

import java.util.List;

public class RC_PunitiveExpeditionManager extends PunitiveExpeditionManager {

    private MarketAPI market;

    public RC_PunitiveExpeditionManager(MarketAPI market, Industry industry) {
        this.market = market;
    }
    @Override
    public void createExpedition(com.fs.starfarer.api.impl.campaign.intel.punitive.PunitiveExpeditionManager.PunExData curr, Integer fpOverride) {

        JSONObject json = curr.faction.getCustom().optJSONObject(Factions.CUSTOM_PUNITIVE_EXPEDITION_DATA);
        if (json == null) return;

        boolean canBombard = json.optBoolean("canBombard", false);

        List<com.fs.starfarer.api.impl.campaign.intel.punitive.PunitiveExpeditionManager.PunExReason> reasons = getExpeditionReasons(curr);
        WeightedRandomPicker<com.fs.starfarer.api.impl.campaign.intel.punitive.PunitiveExpeditionManager.PunExReason> reasonPicker = new WeightedRandomPicker<com.fs.starfarer.api.impl.campaign.intel.punitive.PunitiveExpeditionManager.PunExReason>(curr.random);
        for (com.fs.starfarer.api.impl.campaign.intel.punitive.PunitiveExpeditionManager.PunExReason r : reasons) {
            reasonPicker.add(r, r.weight);
        }
        com.fs.starfarer.api.impl.campaign.intel.punitive.PunitiveExpeditionManager.PunExReason reason = reasonPicker.pick();
        if (reason == null) return;
        reason.type = PunitiveExpeditionManager.PunExType.ANTI_COMPETITION;
        if (market.getIndustry(Industries.ORBITALWORKS).getAllSupply().size()==0) {
            return;
        }
        reason.commodityId = market.getIndustry(Industries.ORBITALWORKS).getAllSupply().get(MathUtils.getRandomNumberInRange(0,market.getIndustry(Industries.ORBITALWORKS).getAllSupply().size()-1)).getCommodityId();
        MarketAPI target = market;
        if (target == null) return;

        WeightedRandomPicker<MarketAPI> picker = new WeightedRandomPicker<MarketAPI>(curr.random);
        for (MarketAPI market : Global.getSector().getEconomy().getMarketsInGroup(null)) {
            if (market.getFaction() == curr.faction &&
                    market.getMemoryWithoutUpdate().getBoolean(MemFlags.MARKET_MILITARY)) {
                picker.add(market, market.getSize());
            }
        }

        MarketAPI from = picker.pick();
        if (from == null) return;

        com.fs.starfarer.api.impl.campaign.intel.punitive.PunitiveExpeditionManager.PunExGoal goal = null;
        Industry industry = null;
        if (reason.type == com.fs.starfarer.api.impl.campaign.intel.punitive.PunitiveExpeditionManager.PunExType.ANTI_FREE_PORT) {
            goal = com.fs.starfarer.api.impl.campaign.intel.punitive.PunitiveExpeditionManager.PunExGoal.RAID_SPACEPORT;
            if (canBombard && curr.numSuccesses >= 2) {
                goal = com.fs.starfarer.api.impl.campaign.intel.punitive.PunitiveExpeditionManager.PunExGoal.BOMBARD;
            }
        } else if (reason.type == com.fs.starfarer.api.impl.campaign.intel.punitive.PunitiveExpeditionManager.PunExType.TERRITORIAL) {
            if (canBombard || true) {
                goal = com.fs.starfarer.api.impl.campaign.intel.punitive.PunitiveExpeditionManager.PunExGoal.BOMBARD;
            } else {
            }
        } else {
            goal = com.fs.starfarer.api.impl.campaign.intel.punitive.PunitiveExpeditionManager.PunExGoal.RAID_PRODUCTION;
            if (reason.commodityId == null || curr.numSuccesses >= 1) {
                goal = com.fs.starfarer.api.impl.campaign.intel.punitive.PunitiveExpeditionManager.PunExGoal.RAID_SPACEPORT;
            }
            if (canBombard && curr.numSuccesses >= 2) {
                goal = com.fs.starfarer.api.impl.campaign.intel.punitive.PunitiveExpeditionManager.PunExGoal.BOMBARD;
            }
        }

        industry = target.getIndustry(Industries.ORBITALWORKS);
        float fp = 50 + curr.threshold * 0.5f;
        fp = Math.max(50, fp - 50);

        if (fpOverride != null) {
            fp = fpOverride;
        }

        float totalAttempts = 0f;
        for (com.fs.starfarer.api.impl.campaign.intel.punitive.PunitiveExpeditionManager.PunExData d : data.values()) {
            totalAttempts += d.numAttempts;
        }

        float extraMult = 0f;
        if (totalAttempts <= 2) {
            extraMult = 0f;
        } else if (totalAttempts <= 4) {
            extraMult = 1f;
        } else if (totalAttempts <= 7) {
            extraMult = 2f;
        } else if (totalAttempts <= 10) {
            extraMult = 3f;
        } else {
            extraMult = 4f;
        }

        float orgDur = extraMult * 10f + (extraMult * 5f) * (float) Math.random();

        curr.intel = new PunitiveExpeditionIntel(from.getFaction(), from, target, fp, orgDur,
                goal, industry, reason);
        if (curr.intel.isDone()) {
            curr.intel = null;
            timeout = orgDur + MIN_TIMEOUT + curr.random.nextFloat() * (MAX_TIMEOUT - MIN_TIMEOUT);
            return;
        }
        timeout=0;
        numSentSinceTimeout++;

        curr.numAttempts++;
        curr.anger = 0f;
        curr.threshold *= 2f;
        if (curr.threshold > MAX_THRESHOLD) {
            curr.threshold = MAX_THRESHOLD;
        }
    }
}