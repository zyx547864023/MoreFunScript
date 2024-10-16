package real_combat.hullmods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.PlanetAPI;
import com.fs.starfarer.api.campaign.ai.FleetAssignmentDataAPI;
import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.ids.Industries;
import com.fs.starfarer.api.util.IntervalUtil;
import com.fs.starfarer.api.util.Misc;
import org.lazywizard.lazylib.MathUtils;

public class PlusMoney extends BaseHullMod {
    private final float REVENUE_STREAM=1000;
    private final IntervalUtil tic = new IntervalUtil (10,10);

    @Override
    public void advanceInCampaign(FleetMemberAPI member, float amount) {
        //skip if mothballed
        if(member.isMothballed())return;
        tic.advance(amount);
        if(tic.intervalElapsed()){
            if (
                    member.getFleetData() != null
                            && member.getFleetData().getFleet() != null
                            && member.getFleetCommander().isPlayer()
            ){
                //nothing in hyperspace
                if(member.getFleetData().getFleet().isInHyperspace())return;
                int light=0;
                int population=0;
                for (PlanetAPI p : member.getFleetData().getFleet().getContainingLocation().getPlanets()){
                    if(p.getMarket()!=null) {
                        if (p.isStar()
                                ||p.getMarket().hasIndustry(Industries.ORBITALSTATION)
                                ||p.getMarket().hasIndustry(Industries.BATTLESTATION)
                                ||p.getMarket().hasIndustry(Industries.STARFORTRESS)
                                ||p.getMarket().hasIndustry(Industries.WAYSTATION)
                        ) {
                            light++;
                            population+=Math.max(p.getMarket().getSize()-4,0);
                        }
                    }
                }
                if(light>0 && population>0){
                    if (member.getFleetData().getFleet().getInteractionTarget()!=null) {
                        member.getFleetData().getFleet().addFloatingText(member.getFleetData().getFleet().getInteractionTarget().getName(), Misc.getTextColor(), 1f);
                    }
                    if (member.getFleetData().getFleet().getAI()!=null)
                    for (FleetAssignmentDataAPI d:member.getFleetData().getFleet().getAssignmentsCopy()) {
                        member.getFleetData().getFleet().addFloatingText(d.getActionText(), Misc.getTextColor(), 1f);
                    }
                    float credits= population*REVENUE_STREAM*MathUtils.getRandomNumberInRange(0.8f,1.1f);
                    Global.getSector().getPlayerFleet().getCargo().getCredits().add(credits);
                    member.getFleetData().getFleet().addFloatingText("+"+(int)credits, Misc.getTextColor(), 1f);
                }
            }
        }
    }
}

