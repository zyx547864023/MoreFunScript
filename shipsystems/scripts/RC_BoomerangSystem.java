package real_combat.shipsystems.scripts;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.fleet.FleetMemberType;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.util.IntervalUtil;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lazywizard.lazylib.combat.CombatUtils;
import org.lazywizard.lazylib.combat.entities.SimpleEntity;
import org.lwjgl.util.vector.Vector2f;
import real_combat.ai.RC_ModulesFighterAI;
import real_combat.entity.RC_AnchoredEntity;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * 分体之后飞机移动过去 ，播放起飞动画
 * 合体之后把飞机存起来，分体的时候再放出去
 *
 */

public class RC_BoomerangSystem extends BaseShipSystemScript {
    public static final float MIN_DISTANCE = 5f;
    private static String ID = "RC_TrinityForceSystem";
    private String CONVERTED_FIGHTERBAY = "converted_fighterbay";
    private static String COLLISION_RADIUS = "COLLISION_RADIUS";
    private static String MOTHERSHIP = "MOTHERSHIP";
    private static String ORIGINAL = "ORIGINAL";
    private static String ARC = "ARC";
    private static String IS_MERGE = "IS_MERGE";
    private final static String ANCHORED_ENTITY_LIST="ANCHORED_ENTITY_LIST";
    private final static String SPRITE_NAME = "graphics/icons/hullsys/fortress_shield.png";

    private final static float THICKNESS = 10f;
    private final static Color FRINGE = new Color(20, 88, 143, 218);
    private final static Color CORE = new Color(61, 59, 59, 218);
    private boolean init = false;
    private ShipAPI ship;

    List<ShipAPI> newShips = new ArrayList<>();
    private Stage stage = Stage.DOWN;
    public static enum Stage {
        TURN,
        OUT,
        BACK,
        DOWN
    }
    private RC_BoomerangeSystemCombatPlugin plugin = null;
    /***
     * @param stats
     * @param id
     * @param state
     * @param effectLevel
     */
    @Override
    public void apply(MutableShipStatsAPI stats, String id, State state, float effectLevel) {
        CombatEngineAPI engine = Global.getCombatEngine();
        if (engine.isPaused()) {return;}
        ship = (ShipAPI) stats.getEntity();
        if (ship == null) {
            return;
        }
        try {
            if (stage == Stage.DOWN) {
                stage = Stage.TURN;
            }
            if (plugin == null) {
                plugin = new RC_BoomerangeSystemCombatPlugin();
                engine.addPlugin(plugin);
            }
        }
        catch (Exception e)
        {
            Global.getLogger(this.getClass()).info(e.getMessage());
        }
    }

    /***
     * 解除F之后把船还原回来
     * 当船离开主船一定距离以后重置碰撞
     * 敌人数量为零时自动解除
     * @param stats
     * @param id
     */
    @Override
    public void unapply(MutableShipStatsAPI stats, String id) {
        CombatEngineAPI engine = Global.getCombatEngine();
        if(engine.isPaused()) {return;}
        if (ship == null) {
            return;
        }
        try {

        }
        catch (Exception e)
        {
            Global.getLogger(this.getClass()).info(e);
        }
    }

    public StatusData getStatusData(int index, State state, float effectLevel) {
        if (index == 0) {
            if (ship!=null) {
                if (ship.getCustomData().get(ID + IS_MERGE) != null) {
                    if ((boolean) ship.getCustomData().get(ID + IS_MERGE)) {
                        return new StatusData("分体！", false);
                    } else {
                        return new StatusData("合体！", false);
                    }
                }
            }
        }
        return null;
    }

    @Override
    public String getInfoText(ShipSystemAPI system, ShipAPI ship) {
        if (system.getState() != ShipSystemAPI.SystemState.IDLE) {return null;}
        //如果想要合体然后幅能大于1/2
        if (ship.getCustomData().get(ID + IS_MERGE)!=null) {
            if (ship.getFluxTracker().getCurrFlux() > ship.getMaxFlux() / 2 && !(boolean) ship.getCustomData().get(ID + IS_MERGE)) {
                return "幅能不足";
            }
        }
        return null;
    }

    @Override
    public boolean isUsable(ShipSystemAPI system, ShipAPI ship) {
        //如果想要合体然后幅能大于1/2
        if (ship.getCustomData().get(ID + IS_MERGE)!=null) {
            if (ship.getFluxTracker().getCurrFlux() > ship.getMaxFlux() / 2 && !(boolean) ship.getCustomData().get(ID + IS_MERGE)) {
                return false;
            }
        }
        return true;
    }

    private class RC_BoomerangeSystemCombatPlugin implements EveryFrameCombatPlugin {

        @Override
        public void init(CombatEngineAPI engine) {

        }

        @Override
        public void processInputPreCoreControls(float amount, List<InputEventAPI> events) {

        }

        @Override
        public void advance(float amount, List<InputEventAPI> events) {
            CombatEngineAPI engine = Global.getCombatEngine();
            try {
                if (!engine.isPaused()) {
                    CombatFleetManagerAPI manager = engine.getFleetManager(ship.getOwner());
                    int alive = 0;
                    for (ShipAPI m : ship.getChildModulesCopy()) {

                            alive++;
                            if (stage == Stage.TURN) {
                                //复制一艘船
                                ShipAPI newShip = null;
                                if (alive > newShips.size()) {
                                    FleetMemberAPI newFleetMember = Global.getFactory().createFleetMember
                                            (FleetMemberType.SHIP, m.getVariant());
                                    newFleetMember.setOwner(ship.getOwner());
                                    newFleetMember.getCrewComposition().addCrew(newFleetMember.getNeededCrew());

                                    float height = engine.getMapHeight();
                                    Vector2f newLocation = new Vector2f(ship.getCollisionRadius() * 4, ship.getOwner() == 0 ? height / -2f - 20000f - ship.getCollisionRadius() * 4 : height / 2f + 20000f + ship.getCollisionRadius() * 4);
                                    newShip = manager.spawnFleetMember(newFleetMember, newLocation, m.getFacing(), 0f);

                                    copyShip(ship.getOwner(), newShip, m);

                                    newShips.add(newShip);
                                    m.setCustomData(COLLISION_RADIUS, m.getCollisionClass());
                                    m.setShipAI(null);
                                    newShip.setShipAI(null);

                                    //子船原来的船关联
                                    newShip.setCustomData(ORIGINAL, m);
                                    newShip.setCustomData(COLLISION_RADIUS, newShip.getCollisionClass());

                                } else {
                                    for (ShipAPI n : newShips) {
                                        if (m.equals(n.getCustomData().get(ORIGINAL))) {
                                            newShip = n;

                                            newShip.setAlphaMult(1);
                                            newShip.setPhased(false);
                                            newShip.setCollisionClass(CollisionClass.NONE);

                                        }
                                    }
                                }
                                //给一个力
                                //CombatUtils.applyForce(newShip, ship.getFacing(), 1000);
                                m.setPhased(true);
                                m.setAlphaMult(0);
                                m.setShipAI(null);
                                m.setCollisionClass(CollisionClass.NONE);
                                newShip.getLocation().set(m.getLocation());
                                if (newShip.getAngularVelocity() < 720) {
                                    newShip.setAngularVelocity(newShip.getAngularVelocity() + amount * 720);
                                } else {
                                    //够速度了发射出去
                                    stage = Stage.OUT;
                                    newShip.getVelocity().set(MathUtils.getPoint(new Vector2f(0,0), newShip.getMaxSpeed()*10, VectorUtils.getAngle(ship.getLocation(), newShip.getLocation())));
                                }
                            } else if (Stage.OUT == stage) {
                                //距离远了之后把碰撞判定加回来
                                ShipAPI newShip = null;
                                for (ShipAPI n : newShips) {
                                    if (m.equals(n.getCustomData().get(ORIGINAL))) {
                                        newShip = n;
                                        newShip.setAngularVelocity(720);
                                        float nowDistance = MathUtils.getDistance(newShip.getLocation(), ship.getLocation());
                                        if (nowDistance > newShip.getCollisionRadius() + ship.getCollisionRadius()) {
                                            CollisionClass collisionClass = (CollisionClass) newShip.getCustomData().get(COLLISION_RADIUS);
                                            newShip.setCollisionClass(collisionClass);
                                        }

                                        //收回来
                                        if (nowDistance > 1000) {
                                            stage = Stage.BACK;

                                        }
                                    }
                                }
                            } else if (stage == Stage.BACK) {
                                //这里要有一个变速 慢快慢
                                ShipAPI newShip = null;
                                for (ShipAPI n : newShips) {
                                    if (m.equals(n.getCustomData().get(ORIGINAL))) {
                                        newShip = n;
                                        float nowDistance = MathUtils.getDistance(newShip.getLocation(), ship.getLocation());
                                        if (nowDistance > newShip.getCollisionRadius() + ship.getCollisionRadius()) {
                                            newShip.setAngularVelocity(720);
                                            newShip.getVelocity().set(MathUtils.getPoint(new Vector2f(), newShip.getMaxSpeed() * 10, VectorUtils.getAngle(newShip.getLocation(), ship.getLocation())));
                                        } else {
                                            newShip.setCollisionClass(CollisionClass.NONE);
                                            newShip.getLocation().set(m.getLocation());
                                            newShip.getVelocity().set(new Vector2f(0,0));
                                            //距离足够
                                            if (Math.abs(newShip.getAngularVelocity() - amount * 720) > 10) {
                                                newShip.setAngularVelocity(newShip.getAngularVelocity() - amount * 720);
                                            } else {
                                                newShip.setAngularVelocity(0);
                                                stage = Stage.DOWN;
                                            }
                                        }
                                    }
                                }
                            } else if (stage == Stage.DOWN) {
                                //这里要有一个变速 慢快慢
                                ShipAPI newShip = null;
                                for (ShipAPI n : newShips) {
                                    if (m.equals(n.getCustomData().get(ORIGINAL))) {
                                        newShip = n;
                                        if (m.getFacing()!=ship.getFacing()) {
                                            m.setFacing(newShip.getFacing());
                                        }
                                        if (m.isPhased()) {
                                            copyShip(ship.getOwner(), m, newShip);
                                            m.resetDefaultAI();
                                            m.setAlphaMult(1);
                                            m.setPhased(false);
                                            CollisionClass collisionClass = (CollisionClass) m.getCustomData().get(COLLISION_RADIUS);
                                            m.setCollisionClass(collisionClass);
                                            newShip.setAlphaMult(0);
                                            newShip.setPhased(true);
                                            newShip.setCollisionClass(CollisionClass.NONE);
                                            float height = engine.getMapHeight();
                                            float width = engine.getMapWidth();
                                            Vector2f newLocation = new Vector2f(ship.getOwner() == 0 ? -width * 4 : width * 4, ship.getOwner() == 0 ? -height * 4 : height * 4);
                                            //newShip.getLocation().set(newLocation);
                                            if (ship.getSystem().isActive()) {
                                                ship.getSystem().setCooldownRemaining(0);
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
            }
            catch (Exception e){
                Global.getLogger(this.getClass()).info(e.getMessage());
            }
        }

        @Override
        public void renderInWorldCoords(ViewportAPI viewport) {

        }

        @Override
        public void renderInUICoords(ViewportAPI viewport) {

        }

        private void copyShip(int owner, ShipAPI newShip, ShipAPI s) {
            CombatEngineAPI engine = Global.getCombatEngine();
            newShip.setHitpoints(s.getHitpoints());
            newShip.getFluxTracker().setCurrFlux(s.getCurrFlux());
            newShip.getFluxTracker().setHardFlux(s.getMinFlux());
            newShip.setCRAtDeployment(s.getCRAtDeployment());
            newShip.setCurrentCR(s.getCurrentCR());
            newShip.setOwner(owner);
            if (newShip.getShipAI()!=null) {
                newShip.getShipAI().forceCircumstanceEvaluation();
            }

            if (newShip.getSystem() != null) {
                newShip.getSystem().setCooldownRemaining(s.getSystem().getCooldownRemaining());
            }
            ShieldAPI shield = s.getShield();
            if (shield!=null)
            {
                if(shield.isOn())
                {
                    newShip.getShield().toggleOn();
                    //newShip.getShield().setActiveArc(shield.getActiveArc());报错
                }
                newShip.getShield().setArc(shield.getArc());
            }
            newShip.setAngularVelocity(s.getAngularVelocity());
            //newShip.getVelocity().set(s.getVelocity());
            s.getVelocity().set(new Vector2f());
            s.setPhased(true);
            s.setCollisionClass(CollisionClass.NONE);

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

            float[][] grid = s.getArmorGrid().getGrid();
            for (int x = 0; x < grid.length; x++) {
                for (int y = 0; y < grid[x].length; y++) {
                    newShip.getArmorGrid().setArmorValue(x, y, grid[x][y]);
                }
            }

            //没有效果不知道干啥用
            newShip.syncWithArmorGridState();
            newShip.syncWeaponDecalsWithArmorDamage();

            newShip.getLocation().set(s.getLocation());

            //转移粘贴对象
            if(engine.getCustomData().get(ANCHORED_ENTITY_LIST)!=null) {
                List<RC_AnchoredEntity> myAnchoredEntityList = (List<RC_AnchoredEntity>) engine.getCustomData().get(ANCHORED_ENTITY_LIST);
                for (RC_AnchoredEntity m:myAnchoredEntityList)
                {
                    if(m.getAnchor().equals(s))
                    {
                        m.reanchor(newShip,m.getLocation());
                    }
                }
            }
        }
    }
}