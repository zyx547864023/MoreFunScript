package real_combat.hullmods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.loading.WingRole;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import real_combat.ai.RC_FighterAI;
import real_combat.ai.RC_BomberAI;
import real_combat.shipsystems.scripts.RC_AsteroidArm;

import java.awt.*;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 合体船的速度计算
 */
public class RC_SpiderCore extends BaseHullMod {
    public static String ID = "RC_SpiderCore";
    @Override
    public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {
        if (ship.getChildModulesCopy()!=null) {
            List<String> slotIds = ship.getVariant().getModuleSlots();
            for (String s : slotIds) {
                ShipVariantAPI variant = ship.getVariant().getModuleVariant(s);
                //扩编改为内置
                variant.removePermaMod("RC_TrinityForceModule");
                variant.removePermaMod("converted_fighterbay");
            }
        }
    }

    @Override
    public void applyEffectsBeforeShipCreation(HullSize hullSize,
                                               MutableShipStatsAPI stats, String id) {
    }

    @Override
    public void advanceInCampaign(FleetMemberAPI member, float amount) {

    }

    @Override
    public void advanceInCombat(final ShipAPI ship, float amount) {
        Set<CombatEntityAPI> hulkList = new HashSet<>();
        CombatEngineAPI engine = Global.getCombatEngine();
        if (engine.getCustomData().get("HULK_LIST")!=null) {
            hulkList = (Set<CombatEntityAPI>) engine.getCustomData().get("HULK_LIST");
        }
        Set<CombatEntityAPI> removeSet = new HashSet<>();
        for (CombatEntityAPI h:hulkList) {
            if (h.getCustomData().get(RC_AsteroidArm.WHO_CATCH)!=null) {
                if (h.getHitpoints()<=0) {
                    ShipAPI catched = (ShipAPI) h.getCustomData().get(RC_AsteroidArm.WHO_CATCH);
                    catched.getCustomData().remove(RC_BomberAI.ID+RC_BomberAI.CATCHED);
                    h.removeCustomData(RC_AsteroidArm.WHO_CATCH);
                    removeSet.add(h);
                    continue;
                }
                ShipAPI catched = (ShipAPI) h.getCustomData().get(RC_AsteroidArm.WHO_CATCH);
                h.getLocation().set(catched.getLocation());
                h.setFacing((float)catched.getCustomData().get(RC_BomberAI.ID+RC_BomberAI.CATCHED)+ship.getFacing());
            }
        }
        hulkList.remove(removeSet);
        engine.getCustomData().put("HULK_LIST",hulkList);
        if (!ship.isPullBackFighters()) {
            for (FighterWingAPI w : ship.getAllWings()) {
                if (WingRole.FIGHTER.equals(w.getRole())) {
                    for (ShipAPI f:w.getWingMembers()) {
                        if (f.getCustomData().get(RC_FighterAI.ID)==null&&f.isAlive()) {
                            f.setCustomData(RC_FighterAI.ID,f.getShipAI());
                            f.setShipAI(new RC_FighterAI(f));
                        }
                    }
                }
            }
        }
        else {
            for (FighterWingAPI w : ship.getAllWings()) {
                if (WingRole.BOMBER.equals(w.getRole())) {
                    for (ShipAPI f : w.getWingMembers()) {
                        if (f.getCustomData().get(RC_BomberAI.ID) == null && f.isAlive() && f.getCustomData().get(RC_BomberAI.ID + RC_BomberAI.CATCHED) == null) {
                            int ammo = 0;
                            for (WeaponAPI wp : f.getAllWeapons()) {
                                if (wp.usesAmmo()) {
                                    ammo += wp.getAmmo();
                                }
                            }
                            if (ammo > 0) {
                                f.setCustomData(RC_BomberAI.ID, f.getShipAI());
                                f.setShipAI(new RC_BomberAI(f));
                            }
                        }
                    }
                }
            }
        }
        for (ShipAPI m : ship.getChildModulesCopy()) {
            if (m.getCustomData().get(ID)==null) {
                m.setCustomData(ID,ship);
            }
            m.setPullBackFighters(ship.isPullBackFighters());
            if (!m.isPullBackFighters()) {
                for (FighterWingAPI w : m.getAllWings()) {
                    if (WingRole.FIGHTER.equals(w.getRole())) {
                        for (ShipAPI f:w.getWingMembers()) {
                            if (f.getCustomData().get(RC_FighterAI.ID)==null&&f.isAlive()) {
                                f.setCustomData(RC_FighterAI.ID,f.getShipAI());
                                f.setShipAI(new RC_FighterAI(f));
                            }
                        }
                    }
                }
            }
            else {
                for (FighterWingAPI w : ship.getAllWings()) {
                    if (WingRole.BOMBER.equals(w.getRole())) {
                        for (ShipAPI f : w.getWingMembers()) {
                            if (f.getCustomData().get(RC_BomberAI.ID) == null && f.isAlive() && f.getCustomData().get(RC_BomberAI.ID + RC_BomberAI.CATCHED) == null) {
                                int ammo = 0;
                                for (WeaponAPI wp : f.getAllWeapons()) {
                                    if (wp.usesAmmo()) {
                                        ammo += wp.getAmmo();
                                    }
                                }
                                if (ammo > 0) {
                                    f.setCustomData(RC_BomberAI.ID, f.getShipAI());
                                    f.setShipAI(new RC_BomberAI(f));
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    @Override
    public boolean isApplicableToShip(ShipAPI ship) {
        //ship.getCaptain();
        return true;
    }
    @Override
    public String getUnapplicableReason(ShipAPI ship) {
        return null;
    }
    @Override
    public void addPostDescriptionSection(TooltipMakerAPI tooltip, ShipAPI.HullSize hullSize, ShipAPI ship, float width, boolean isForModSpec) {
        float pad = 5f;
        Color h = Misc.getHighlightColor();
//        Color g = Misc.getGrayColor();
//        Color bad = Misc.getNegativeHighlightColor();
        Color good = Misc.getPositiveHighlightColor();

        TooltipMakerAPI text = tooltip.beginImageWithText("graphics/ships/RC/RC_drone_borer.png", 64f);
        tooltip.addSectionHeading("额外描述", Alignment.TMID, pad * 2f);
            text = tooltip.beginImageWithText("graphics/ships/RC/RC_bloody.png", 32f);
            text.addPara("%s", pad, new Color(155, 155, 255, 255), "1");
            text.addPara("%s", pad, h, "4");
            tooltip.addImageWithText(0);
            /*
            text = tooltip.beginImageWithText("graphics/ships/RC/RC_copy.png", 32f);
            text.addPara("%s", 0, new Color(155, 155, 255, 255), "2");
            text.addPara("%s--", 0, h, "5");
            tooltip.addImageWithText(0);
            text = tooltip.beginImageWithText("graphics/ships/RC/RC_drone_borer.png", 32f);
            text.addPara("%s", 0, new Color(155, 155, 255, 255), "3");
            text.addPara("%s--", 0, h, "6");
            tooltip.addImageWithText(0);
             */
            float tableWidth = width - 5f;//表格宽度
            tooltip.beginTable(
                    Misc.getBasePlayerColor(),
                    Misc.getDarkPlayerColor(),
                    Misc.getBrightPlayerColor(),
                    20f,
                    //从这往下可以按照格式添加列
                    "第一列", tableWidth * 0.35f,
                    "第二列", tableWidth * 0.25f,
                    "第三列", tableWidth * 0.4f);
            //添加一行数据，颜色后面的参数数量需要与表格列数一至
            tooltip.addRow(new Color(255, 211, 154), DamageType.KINETIC.getDisplayName(),
                     "%",
                     "%");
            //添加表格到额外说明
            tooltip.addTable("N/A", 0, pad);
    }
}
