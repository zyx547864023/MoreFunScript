package real_combat.scripts.campaign;

import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.econ.SubmarketAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.fleet.FleetMemberType;
import com.fs.starfarer.api.impl.campaign.DModManager;
import com.fs.starfarer.api.impl.campaign.ids.Submarkets;
import com.fs.starfarer.api.util.IntervalUtil;
import com.fs.starfarer.api.util.Misc;
import data.scripts.util.UW_Util;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import org.lazywizard.lazylib.MathUtils;

/* Based on Nicke535's work with heavy modifications */
public class UW_MarketRiggerScript implements EveryFrameScript {

    private static final Map<String, MarketRiggerData> RIGGER_DATA = new HashMap<>();

    static {
        Map<String, ShipReplacerData> highRestrictionRigger = new HashMap<>();
        highRestrictionRigger.put(Submarkets.SUBMARKET_OPEN, new ShipReplacerData(0.75f, 2, 4));
        highRestrictionRigger.put(Submarkets.SUBMARKET_BLACK, new ShipReplacerData(0.5f, 1, 3));
        highRestrictionRigger.put(Submarkets.GENERIC_MILITARY, new ShipReplacerData(0.25f, 0, 0));
        highRestrictionRigger.put("ii_ebay", new ShipReplacerData(0.5f, 1, 3));
        highRestrictionRigger.put("uw_scrapyard_submarket", new ShipReplacerData(0.5f, 1, 3));

        RIGGER_DATA.put("uw_infernus", new MarketRiggerData(Arrays.asList("uw_renegade", "uw_dragon", "uw_amalgam"), highRestrictionRigger));
    }

    /* Counts in seconds */
    private final IntervalUtil shortTracker = new IntervalUtil(1f, 1.5f);

    /* Counts in days */
    private final IntervalUtil longTracker = new IntervalUtil(29f, 31f);

    /* Updates once every longTracker period */
    private final List<String> marketsToManipulate = new ArrayList<>();

    private final Set<String> retainedMembers = new HashSet<>();

    private final Random rand = new Random();

    @Override
    public void advance(float amount) {
        SectorAPI sector = Global.getSector();
        if (sector == null) {
            return;
        }

        float longAmount = Misc.getDays(amount);
        if (sector.isPaused()) {
            longAmount = 0f;
        }

        longTracker.advance(longAmount);
        shortTracker.advance(amount);

        if (longTracker.intervalElapsed()) {
            marketsToManipulate.clear();
            retainedMembers.clear();
            for (MarketAPI market : sector.getEconomy().getMarketsCopy()) {
                if (!market.isHidden()) {
                    marketsToManipulate.add(market.getId());
                }
            }
        }

        if (shortTracker.intervalElapsed()) {
            for (String marketID : marketsToManipulate) {
                MarketAPI market = sector.getEconomy().getMarket(marketID);
                if (market == null) {
                    continue;
                }

                for (SubmarketAPI submarket : market.getSubmarketsCopy()) {
                    String submarketID = submarket.getSpecId();
                    CargoAPI cargo = submarket.getCargo();
                    List<FleetMemberAPI> toDelete = new ArrayList<>();
                    for (FleetMemberAPI member : cargo.getMothballedShips().getMembersInPriorityOrder()) {
                        if (retainedMembers.contains(member.getId())) {
                            continue;
                        }

                        String hullID = UW_Util.getNonDHullId(member.getHullSpec());
                        MarketRiggerData riggerData = RIGGER_DATA.get(hullID);
                        if (riggerData != null) {
                            ShipReplacerData replacerData = riggerData.replacementData.get(submarketID);
                            if (replacerData == null) {
                                replacerData = new ShipReplacerData(0f, 0, 0);
                            }

                            if ((float) Math.random() < replacerData.replacementChance) {
                                List<String> variantList = riggerData.replacementHulls;
                                String variantID = variantList.get(MathUtils.getRandomNumberInRange(0, variantList.size() - 1));
                                variantID += "_Hull";

                                cargo.addMothballedShip(FleetMemberType.SHIP, variantID, null);

                                toDelete.add(member);
                            } else {
                                int DMods = MathUtils.getRandomNumberInRange(replacerData.minDMods, replacerData.maxDMods);
                                if (DMods > 0) {
                                    DModManager.setDHull(member.getVariant());
                                    DModManager.addDMods(member, true, DMods, rand);
                                }
                                retainedMembers.add(member.getId());
                            }
                        }
                    }

                    for (FleetMemberAPI member : toDelete) {
                        cargo.getMothballedShips().removeFleetMember(member);
                    }
                }
            }
        }
    }

    @Override
    public boolean isDone() {
        return false;
    }

    /* Run when paused to even further limit the small grace-periods the player might find the wrong ships in */
    @Override
    public boolean runWhilePaused() {
        return true;
    }

    static class MarketRiggerData {

        final List<String> replacementHulls;
        final Map<String, ShipReplacerData> replacementData;

        MarketRiggerData(List<String> replacementHulls, Map<String, ShipReplacerData> replacementData) {
            this.replacementHulls = replacementHulls;
            this.replacementData = replacementData;
        }
    }

    static class ShipReplacerData {

        final float replacementChance;
        final int minDMods;
        final int maxDMods;

        ShipReplacerData(float replacementChance, int minDMods, int maxDMods) {
            this.replacementChance = replacementChance;
            this.minDMods = minDMods;
            this.maxDMods = maxDMods;
        }
    }
}
