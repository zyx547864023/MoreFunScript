package real_combat.combat;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.fleet.FleetMemberType;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.mission.FleetSide;
import data.scripts.Magic_modPlugin;
import data.scripts.plugins.MagicCampaignTrailPlugin;
import data.scripts.util.MagicAnim;
import data.scripts.util.MagicCampaign;
import data.scripts.util.MagicUI;
import org.intellij.lang.annotations.MagicConstant;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.vector.Vector2f;
import real_combat.util.RC_Util;

import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RC_MonsterBallEveryFrameCombatPlugin implements EveryFrameCombatPlugin {

    public static final String RC_MONSTERBALL_ID = "reaper_torp_ball_sec";
    //用于存储每个船的当前占领值
    private Map<ShipAPI,Float> occupyMap = new HashMap();
    //经过时间间隔
    private Map<ShipAPI,Float> timeMap = new HashMap();

    public void advance(float amount, List<InputEventAPI> events) {
        //if (Global.getCombatEngine().isPaused()) return;
        CombatEngineAPI engine = Global.getCombatEngine();
        //如果不是模拟训练
        if(!(engine.isSimulation()||engine.isMission()))
        {
            //设置武器弹药
            List<ShipAPI> shipList = engine.getShips();
            for (ShipAPI s:shipList)
            {
                if(!s.isFighter()){
                    List<WeaponAPI> weaponList = s.getAllWeapons();
                    for (WeaponAPI w:weaponList) {
                        if("monster_ball_shooter".equals(w.getSpec().getWeaponId())){
                            int maxAmmo = 0;
                            if(s.getHullSize()== ShipAPI.HullSize.FRIGATE)
                            {
                                maxAmmo = 1;
                            }
                            else if(s.getHullSize()== ShipAPI.HullSize.DESTROYER)
                            {
                                maxAmmo = 2;
                            }
                            else if(s.getHullSize()== ShipAPI.HullSize.CRUISER)
                            {
                                maxAmmo = 3;
                            }
                            else if(s.getHullSize()== ShipAPI.HullSize.CAPITAL_SHIP)
                            {
                                maxAmmo = 4;
                            }
                            if(w.getAmmo()>maxAmmo)
                            {
                                w.setAmmo(maxAmmo);
                            }
                        }
                    }
                }
            }
        }

        ViewportAPI viewport = engine.getViewport();
        float alphaMult = viewport.getAlphaMult();
        if(alphaMult <= 0f) return;

        GL11.glMatrixMode(GL11.GL_PROJECTION);
        GL11.glPushMatrix();
        GL11.glLoadIdentity();
        GL11.glOrtho(viewport.getLLX(), viewport.getLLX() + viewport.getVisibleWidth(), viewport.getLLY(),
                viewport.getLLY() + viewport.getVisibleHeight(), -1,
                1);
        GL11.glMatrixMode(GL11.GL_MODELVIEW);
        GL11.glPushMatrix();
        GL11.glLoadIdentity();
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glEnable(GL11.GL_LINE_SMOOTH);
        GL11.glHint(GL11.GL_LINE_SMOOTH_HINT, GL11.GL_NICEST);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glTranslatef(0.01f, 0.01f, 0);

        List<ShipAPI> shipList = engine.getShips();
        int count = 0;
        if(engine.getShips()==null)
            return;
        try {
            for (ShipAPI s : shipList) {
                if (!s.isFighter() && s.isAlive()) {
                    Global.getLogger(this.getClass()).info(1);
                    //当前有多少陆战队
                    int nowMarines = 0;
                    if (engine.getCustomData().get(s.getId() + "_nowMarines") != null) {
                        nowMarines = (int) engine.getCustomData().get(s.getId() + "_nowMarines");
                    }
                    Global.getLogger(this.getClass()).info(2);
                    //占领值
                    float occupy = 0;
                    float occupyLength = 0;
                    if (occupyMap.get(s) != null) {
                        occupy = occupyMap.get(s);
                        if (occupy < 0) {
                            occupy = 0;
                        }
                        float crew = s.getHullSpec().getMinCrew();
                        if(crew==0)
                        {
                            if(s.getHullSize() == ShipAPI.HullSize.FRIGATE)
                            {
                                crew = 30;
                            }
                            else if(s.getHullSize() == ShipAPI.HullSize.DESTROYER)
                            {
                                crew = 80;
                            }
                            else if(s.getHullSize() == ShipAPI.HullSize.CRUISER)
                            {
                                crew = 250;
                            }
                            else if(s.getHullSize() == ShipAPI.HullSize.CAPITAL_SHIP)
                            {
                                crew = 800;
                            }
                        }
                        //占领值 变化 根据 陆战队原数量*10/船只必备船员数量 * amount
                        if (nowMarines * 10 > crew) {
                            occupy += nowMarines * 10 / crew * amount;
                        } else {
                            if (nowMarines > 0) {
                                occupy -= crew / nowMarines / 10 * amount;
                            } else {
                                occupy -= crew / 10 * amount;
                            }
                        }
                        if (occupy == 0) {
                            occupyLength = 0;
                        } else {
                            occupyLength = occupy / 100;
                        }
                        if (occupyLength < 0) {
                            occupyLength = 0;
                        }
                        if (occupyLength > 1) {
                            occupyLength = 1;
                        }
                        int red = Math.round((1 - occupyLength) * 255);
                        int green = Math.round(occupyLength * 153);
                        if (s.getOwner() == 0) {
                            red = Math.round(occupyLength * 255);
                            green = Math.round((1 - occupyLength) * 153);
                        }
                        try {
                            Color COLOR = new Color(red, green, 0, 100);
                            if (s!=null)
                            if (!RC_Util.hide(engine) && s.isAlive()) {
                                drawArc(COLOR, viewport.getAlphaMult(), 361f * occupyLength, s.getLocation(), s.getCollisionRadius(), 0f, 0f, 0f, 0f, 10 / viewport.getViewMult());
                                //Global.getLogger(this.getClass()).info(occupyLength);
                            }
                        } catch (Exception e) {
                            Global.getLogger(this.getClass()).info(e);
                        }
                    }
                    Global.getLogger(this.getClass()).info(3);
                    if (!Global.getCombatEngine().isPaused()) {
                        occupyMap.put(s, occupy);
                    }

                    if (occupyLength >= 0.8) {
                        s.giveCommand(ShipCommand.HOLD_FIRE, null, 0);
                    }
                    Global.getLogger(this.getClass()).info(4);
                    if (occupyLength >= 1) {
                        if (!s.isDrone()&&s.isAlive()&&!s.isShipWithModules()&&!s.isStationModule()) {
                            FleetSide fleetSide = FleetSide.PLAYER;
                            if (s.getOwner() == 0) {
                                fleetSide = FleetSide.ENEMY;
                            }
                            FleetMemberAPI member = Global.getFactory().createFleetMember(FleetMemberType.SHIP, s.getVariant());
                            member.setOwner(fleetSide.ordinal());
                            member.getCrewComposition().addCrew(member.getNeededCrew());
                            ShipAPI newShip = Global.getCombatEngine().getFleetManager(fleetSide)
                                    .spawnFleetMember(member, s.getLocation(), s.getFacing(), 0f);
                            newShip.setHitpoints(s.getHitpoints());
                            newShip.getFluxTracker().setCurrFlux(s.getCurrFlux());
                            newShip.getFluxTracker().setHardFlux(s.getHardFluxLevel());
                            newShip.setCRAtDeployment(s.getCurrentCR());
                            newShip.setCurrentCR(s.getCurrentCR());
                            newShip.setOwner(fleetSide.ordinal());
                            newShip.getShipAI().forceCircumstanceEvaluation();

                            newShip.getVelocity().set(s.getVelocity());
                            s.getVelocity().set(new Vector2f());
                            //s.setHitpoints(0);

                            List<ShipEngineControllerAPI.ShipEngineAPI> shipEngines = s.getEngineController().getShipEngines();
                            List<ShipEngineControllerAPI.ShipEngineAPI> newShipEngines = newShip.getEngineController().getShipEngines();
                            for (int e = 0; e < shipEngines.size(); e++) {
                                ShipEngineControllerAPI.ShipEngineAPI ne = newShipEngines.get(e);
                                ShipEngineControllerAPI.ShipEngineAPI oe = shipEngines.get(e);
                                ne.setHitpoints(oe.getHitpoints());
                                if (oe.isDisabled()) {
                                    ne.disable();
                                }
                            }
                            //newShipEngines.clear();
                            //newShipEngines.addAll(shipEngines);
                            Global.getLogger(this.getClass()).info(5);
                            List<WeaponAPI> shipWeapons = s.getAllWeapons();
                            List<WeaponAPI> newShipWeapons = newShip.getAllWeapons();
                            for (int w = 0; w < shipWeapons.size(); w++) {
                                WeaponAPI nw = newShipWeapons.get(w);
                                WeaponAPI ow = shipWeapons.get(w);
                                nw.setCurrHealth(ow.getCurrHealth());
                                nw.setAmmo(ow.getAmmo());
                                nw.setCurrAngle(ow.getCurrAngle());
                                nw.setRemainingCooldownTo(ow.getCooldownRemaining());
                                if (ow.isDisabled()) {
                                    nw.disable();
                                }
                            }

                            //newShipWeapons.clear();
                            //newShipWeapons.addAll(shipWeapons);
                            Global.getLogger(this.getClass()).info(6);
                            float[][] grid = s.getArmorGrid().getGrid();
                            for (int x = 0; x < grid.length; x++)
                                for (int y = 0; y < grid[x].length; y++) {
                                    newShip.getArmorGrid().setArmorValue(x, y, grid[x][y]);
                                }

                            //转移粘贴对象
                            List<Map<CombatEntityAPI, CombatEntityAPI>> speedMapList = (List<Map<CombatEntityAPI, CombatEntityAPI>>) engine.getCustomData().get("speedMapList");
                            for (Map<CombatEntityAPI, CombatEntityAPI> speedMap : speedMapList) {
                                for (CombatEntityAPI landing : speedMap.keySet()) {
                                    if (speedMap.get(landing) != null) {
                                        CombatEntityAPI target = speedMap.get(landing);
                                        if (target == s) {
                                            speedMap.put(landing, newShip);
                                            List<Map<CombatEntityAPI, Float>> targetFacingMapList = (List<Map<CombatEntityAPI, Float>>) engine.getCustomData().get("targetFacingMapList");
                                            for (Map<CombatEntityAPI, Float> targetFacingMap : targetFacingMapList) {
                                                targetFacingMap.put(newShip, newShip.getFacing());
                                            }
                                            List<Map<CombatEntityAPI, Float>> landingDistanceMapList = (List<Map<CombatEntityAPI, Float>>) engine.getCustomData().get("landingDistanceMapList");
                                            for (Map<CombatEntityAPI, Float> landingDistanceMap : landingDistanceMapList) {
                                                landingDistanceMap.put(landing, MathUtils.getDistance(newShip.getLocation(), landing.getLocation()));
                                            }
                                            List<Map<CombatEntityAPI, Float>> angleMapList = (List<Map<CombatEntityAPI, Float>>) engine.getCustomData().get("angleMapList");
                                            for (Map<CombatEntityAPI, Float> angleMap : angleMapList) {
                                                angleMap.put(landing, VectorUtils.getAngle(newShip.getLocation(), landing.getLocation()));
                                            }
                                        }
                                    }
                                }
                            }
                            Global.getLogger(this.getClass()).info(7);
                            if (!(engine.isSimulation() || engine.isMission())) {
                                if (Global.getSector() != null) {
                                    //如果是敌人抢玩家船
                                    if (fleetSide == FleetSide.ENEMY) {
                                        if (Global.getSector().getPlayerFleet() != null) {
                                            CampaignFleetAPI playerFleet = Global.getSector().getPlayerFleet();
                                            playerFleet.getFleetData().removeFleetMember(s.getFleetMember());
                                            if (playerFleet != null)
                                                if (playerFleet.getBattle() != null)
                                                    if (playerFleet.getBattle().getNonPlayerSide() != null) {
                                                        List<CampaignFleetAPI> fleetList = playerFleet.getBattle().getNonPlayerSide();
                                                        for (CampaignFleetAPI f : fleetList) {
                                                            //if(f.getFleetData().getMembersListCopy().size()>1) {
                                                            f.getFleetData().addFleetMember(newShip.getFleetMember());
                                                            break;
                                                            //}
                                                        }
                                                    }
                                        }
                                    } else {
                                        if (Global.getSector().getPlayerFleet() != null) {
                                            CampaignFleetAPI playerFleet = Global.getSector().getPlayerFleet();
                                            playerFleet.getFleetData().addFleetMember(newShip.getFleetMember());
                                            //回收陆战队
                                            playerFleet.getCargo().addMarines(nowMarines);
                                            if (playerFleet != null)
                                                if (playerFleet.getBattle() != null)
                                                    if (playerFleet.getBattle().getNonPlayerSide() != null) {
                                                        List<CampaignFleetAPI> fleetList = playerFleet.getBattle().getNonPlayerSide();
                                                        for (CampaignFleetAPI f : fleetList) {
                                                            //if(f.getFleetData().getMembersListCopy().size()>1) {
                                                            f.getFleetData().removeFleetMember(s.getFleetMember());
                                                            //}
                                                            if (f.getFleetData().getMembersListCopy().size() == 0) {
                                                                playerFleet.getCargo().addAll(f.getCargo());
                                                            }
                                                        }
                                                    }
                                        }
                                    }
                                }
                            }
                            Global.getCombatEngine().removeObject(s);
                            Global.getLogger(this.getClass()).info(8);
                        }
                    }

                    if (nowMarines > 0) {
                        //时间
                        float timer = 0;
                        if (timeMap.get(s) != null) {
                            timer = timeMap.get(s);
                            if (timer > 5) {
                                timer = 0;
                                //根据陆战队员和对方的人数和攻略进度进行陆战队的减员速度
                                nowMarines -= s.getHullSpec().getMinCrew() / 10 * (1 - occupyLength);
                                if (nowMarines < 0) {
                                    nowMarines = 0;
                                }
                                engine.getCustomData().put(s.getId() + "_nowMarines", nowMarines);
                            } else {
                                timer += amount;
                            }
                        }
                        if (!Global.getCombatEngine().isPaused()) {
                            timeMap.put(s, timer);
                        }
                    }
                    Global.getLogger(this.getClass()).info(9);
                }
                count++;
                if(count==shipList.size())
                {
                    break;
                }
            }

            GL11.glDisable(GL11.GL_BLEND);
            GL11.glMatrixMode(GL11.GL_MODELVIEW);
            GL11.glPopMatrix();
            GL11.glMatrixMode(GL11.GL_PROJECTION);
            GL11.glPopMatrix();
            GL11.glPopAttrib();
        }
        catch (Exception e)
        {
            Global.getLogger(this.getClass()).info(e);
        }
    }

    public void renderInUICoords(ViewportAPI viewport) {
    }

    public void init(CombatEngineAPI engine) {

    }

    public void renderInWorldCoords(ViewportAPI viewport) {

    }

    public void processInputPreCoreControls(float amount, List<InputEventAPI> events) {

    }

    private void drawArc(Color color, float alpha, float angle, Vector2f loc, float radius, float aimAngle, float aimAngleTop, float x, float y, float thickness){
        GL11.glLineWidth(thickness);
        GL11.glColor4ub((byte) color.getRed(), (byte) color.getGreen(), (byte) color.getBlue(), (byte)Math.max(0, Math.min(Math.round(alpha * 255f), 255)) );
        GL11.glBegin(GL11.GL_LINE_STRIP);
        for(int i = 0; i < Math.round(angle); i++){
            GL11.glVertex2f(
                    loc.x + (radius * (float)Math.cos(Math.toRadians(aimAngleTop + i)) + x * (float)Math.cos(Math.toRadians(aimAngle - 90f)) - y * (float)Math.sin(Math.toRadians(aimAngle - 90f))),
                    loc.y + (radius * (float)Math.sin(Math.toRadians(aimAngleTop + i)) + x * (float)Math.sin(Math.toRadians(aimAngle - 90f)) + y * (float)Math.cos(Math.toRadians(aimAngle - 90f)))
            );
        }
        GL11.glEnd();
    }
}
