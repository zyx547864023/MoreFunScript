package real_combat.combat;


import com.fs.graphics.H;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.ModSpecAPI;
import com.fs.starfarer.api.SettingsAPI;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.fleet.CrewCompositionAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.fleet.FleetMemberType;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.impl.campaign.ids.HullMods;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.mission.FleetSide;
import com.fs.starfarer.api.util.Misc;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.vector.Vector2f;
import real_combat.entity.RC_AnchoredEntity;
import real_combat.shipsystems.scripts.RC_TransAmSystem;
import real_combat.util.RC_Util;

import java.awt.*;
import java.io.IOException;
import java.util.*;
import java.util.List;

public class RC_MonsterBallEveryFrameCombatPlugin implements EveryFrameCombatPlugin {
    private final static String ID = "RC_MonsterBallLayeredRenderingPlugin";
    private final static String MONSTER_BALL_ON_HIT_EFFECT = "MONSTER_BALL_ON_HIT_EFFECT";
    public static final String MONSTER_BALL_SHOOTER = "monster_ball_shooter";
    public static final String MONSTER_BALL_SHOOTER_BIG = "monster_ball_shooter_big";
    private final static String ANCHORED_ENTITY_LIST="ANCHORED_ENTITY_LIST";
    private final static String NOW_MARINES = "NOW_MARINES";
    private final static String TEXT = "TEXT";
    private final static Color GREY = new Color(100, 100, 100, 50);
    private final static String SPRITE_NAME = "graphics/icons/hullsys/fortress_shield.png";
    private final static float FONT_SIZE = 50f;
    private final static float FLASH_FREQUENCY = 5f;
    private final static float FLASH_DURATION = 10f;
    private final static float MARINES_TO_CREW = 10f;
    private final static float MARINES_REFRESH_TIME = 10f;
    private final static String IS_CATCH = "IS_CATCH";
    private final static String ZIGGURAT = "ziggurat";
    private CombatEngineAPI engine = Global.getCombatEngine();
    private SpriteAPI sprite  = Global.getSettings().getSprite("graphics/missiles/shuttle.png");
    //用于存储每个船的当前占领值
    private Map<ShipAPI,Float> occupyMap = new HashMap();
    //经过时间间隔
    private Map<ShipAPI,Float> timeMap = new HashMap();
    private List<RC_AnchoredEntity> anchoredEntityList = new ArrayList<>();

    /**
     * 先读这个mod使用的是白名单还是黑名单
     * 默认每艘船都有描述，把描述id和modid关联起来
     *
     */
    private static final String SHIPS_BLACKLIST = "data/config/seize/seize_ships_blacklist.csv";
    private static final String SHIPS_WHITELIST = "data/config/seize/seize_ships_whitelist.csv";
    private static final String DESCRIPTIONS = "data/strings/descriptions.csv";
    private static boolean initialized = false;

    protected static Map<String,Set<String>> black = new HashMap<>();
    protected static Map<String,Set<String>> white = new HashMap<>();
    protected static Map<String,String> shipFromMod = new HashMap<>();

    public static void reloadSettings(){
        SettingsAPI settings = Global.getSettings();
        List<ModSpecAPI> mods = settings.getModManager().getEnabledModsCopy();
        for (ModSpecAPI mod : mods) {
            JSONArray descriptionsData;
            JSONArray blackListData;
            JSONArray whiteListData;
            try {
                descriptionsData = settings.loadCSV(DESCRIPTIONS, mod.getId());
                if (descriptionsData.length()!=0) {
                    packOnlyShip(shipFromMod,descriptionsData,mod.getId());

                    whiteListData = settings.loadCSV(SHIPS_WHITELIST, mod.getId());
                    if (whiteListData.length()!=0) {
                        Set<String> set = pack(whiteListData);
                        white.put(mod.getId(),set);
                    }
                    else {
                        blackListData = settings.loadCSV(SHIPS_BLACKLIST, mod.getId());
                        if (blackListData.length() != 0) {
                            Set<String> set = pack(blackListData);
                            black.put(mod.getId(),set);
                        }
                    }
                }
            } catch (Exception e) {
                continue;
            }
        }
        initialized = true;
    }

    public static Set<String> pack(JSONArray listData){
        Set<String> set = new HashSet<>();
        for (int i = 0; i < listData.length(); i++) {
            try {
                JSONObject row = listData.getJSONObject(i);
                String id = row.getString("id");
                //每个白名单来自于哪个mod
                set.add(id);
            } catch (Exception e) {

            }
        }
        return set;
    }

    public static void packOnlyShip(Map<String,String> shipFormMod, JSONArray listData,String modId){
        for (int i = 0; i < listData.length(); i++) {
            try {
                JSONObject row = listData.getJSONObject(i);
                String id = row.getString("id");
                String type = row.getString("type");
                if (!(id == null || type == null || id.equals("") || type.equals(""))) {
                    if ("SHIP".equals(type)) {
                        //每个描述
                        shipFormMod.put(id,modId);
                    }
                }
            } catch (Exception e) {

            }
        }
    }

    public void getOut(CombatEntityAPI s){

    }

    public void init(CombatEngineAPI engine) {

    }

    public void setAmmo(ShipAPI s)
    {
        List<WeaponAPI> weaponList = s.getAllWeapons();
        for (WeaponAPI w : weaponList) {
            if (MONSTER_BALL_SHOOTER.equals(w.getSpec().getWeaponId())) {
                int maxAmmo = 0;
                if (s.getHullSize() == ShipAPI.HullSize.FRIGATE) {
                    maxAmmo = 1;
                } else if (s.getHullSize() == ShipAPI.HullSize.DESTROYER) {
                    maxAmmo = 2;
                } else if (s.getHullSize() == ShipAPI.HullSize.CRUISER) {
                    maxAmmo = 3;
                } else if (s.getHullSize() == ShipAPI.HullSize.CAPITAL_SHIP) {
                    maxAmmo = 4;
                }
                if (w.getAmmo() > maxAmmo) {
                    w.setAmmo(maxAmmo);
                }
            } else if (MONSTER_BALL_SHOOTER_BIG.equals(w.getSpec().getWeaponId())) {
                int maxAmmo = 0;
                if (s.getHullSize() == ShipAPI.HullSize.FRIGATE) {
                    maxAmmo = 2;
                } else if (s.getHullSize() == ShipAPI.HullSize.DESTROYER) {
                    maxAmmo = 4;
                } else if (s.getHullSize() == ShipAPI.HullSize.CRUISER) {
                    maxAmmo = 6;
                } else if (s.getHullSize() == ShipAPI.HullSize.CAPITAL_SHIP) {
                    maxAmmo = 8;
                }
                if (w.getAmmo() > maxAmmo) {
                    w.setAmmo(maxAmmo);
                }
            }
        }
    }

    public float getCrew(ShipAPI s)
    {
        CrewCompositionAPI crewComposition = s.getFleetMember().getCrewComposition();
        float crew = crewComposition.getCrew();
        //float crew = s.getHullSpec().getMinCrew();
        if (crew==0)
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
            crewComposition.setCrew(crew);
        }
        return crew;
    }

    public float getMult(ShipAPI s, float nowMarines)
    {
        //一个乘数
        float mult = getMultWithOutMarines(s);
        float crew = getCrew(s);
        //陆战队船员数量
        mult += (nowMarines * MARINES_TO_CREW - crew)/crew;
        return mult;
    }

    public static float getMultWithOutMarines(ShipAPI s)
    {
        //一个乘数
        float mult = 0;
        //船的战备 战备越低加成越多 小于40开始
        mult += 1-s.getCurrentCR();
        //船的血量 血量越低加成越多
        mult += 1-s.getHitpoints()/s.getMaxHitpoints();
        //船的装甲 装甲越低加成越多
        float nowArmor = 0;
        int count = 0;
        float[][] grid = s.getArmorGrid().getGrid();
        for (int x = 0; x < grid.length; x++) {
            for (int y = 0; y < grid[x].length; y++) {
                nowArmor += s.getArmorGrid().getArmorValue(x, y);
                count++;
            }
        }
        float maxArmor = count * s.getArmorGrid().getArmorRating()/15f;
        if(maxArmor!=0) {
            mult += 1 - nowArmor / maxArmor;
        }
        //船的幅能 幅能越高加成越多
        float nowFlux = s.getCurrFlux();
        float maxFlux = s.getMaxFlux();
        if (maxFlux!=0) {
            mult += (nowFlux - (maxFlux / 2)) / (maxFlux / 2);
        }
        //引擎是否宕机 引擎宕机越多加载越多
        count = 0;
        List<ShipEngineControllerAPI.ShipEngineAPI> shipEngines = s.getEngineController().getShipEngines();
        for (int e = 0; e < shipEngines.size(); e++) {
            ShipEngineControllerAPI.ShipEngineAPI oe = shipEngines.get(e);
            if (oe.isDisabled()) {
                count++;
            }
        }
        if(shipEngines.size()!=0)
        {
            mult += 1 - (shipEngines.size()-count)/shipEngines.size();
        }
        //武器是否宕机 武器宕机越多加成越多
        count = 0;
        List<WeaponAPI> shipWeapons = s.getAllWeapons();
        for (int w = 0; w < shipWeapons.size(); w++) {
            WeaponAPI ow = shipWeapons.get(w);
            if (ow.isDisabled()) {
                count++;
            }
        }
        if(shipWeapons.size()!=0) {
            mult += 1 - (shipWeapons.size()-count)/ shipWeapons.size();
        }
        //是否过载 过载加成
        if(s.getFluxTracker().isOverloadedOrVenting())
        {
            mult = mult*2;
        }
        return mult;
    }

    public float getMarines(ShipAPI s, float crew)
    {
        float mult = getMultWithOutMarines(s);
        float marines = ( 1 - mult ) * crew / MARINES_TO_CREW;
        return marines;
    }

    private void drawArc(Color color, float alpha, float angle, Vector2f loc, float radius, float aimAngle, float aimAngleTop, float x, float y, float thickness){
        GL11.glPushMatrix();
        //禁用纹理
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        SpriteAPI sprite = Global.getSettings().getSprite("fs_beamsystem", "fs_bs_2");
        sprite.bindTexture();
        //color = new Color(color.getRed(),color.getGreen(),color.getBlue(),255);
        //抗锯齿 移动镜头的时候不会出现奇怪的锯齿
        GL11.glEnable(GL11.GL_LINE_SMOOTH);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

        GL11.glLineWidth(thickness / engine.getViewport().getViewMult());
        GL11.glColor4ub((byte) color.getRed(), (byte) color.getGreen(), (byte) color.getBlue(), (byte)Math.max(0, Math.min(Math.round(alpha * 255f), 255)) );
        GL11.glBegin(GL11.GL_LINE_STRIP);
        for(double i = 0; i < Math.round(angle); i=i+0.1){

            GL11.glTexCoord2f(
                    loc.x + (radius * (float)Math.cos(Math.toRadians(aimAngleTop + i)) + x * (float)Math.cos(Math.toRadians(aimAngle - 90f)) - y * (float)Math.sin(Math.toRadians(aimAngle - 90f))),
                    loc.y + (radius * (float)Math.sin(Math.toRadians(aimAngleTop + i)) + x * (float)Math.sin(Math.toRadians(aimAngle - 90f)) + y * (float)Math.cos(Math.toRadians(aimAngle - 90f)))
            );

            GL11.glVertex2f(
                    loc.x + (radius * (float)Math.cos(Math.toRadians(aimAngleTop + i)) + x * (float)Math.cos(Math.toRadians(aimAngle - 90f)) - y * (float)Math.sin(Math.toRadians(aimAngle - 90f))),
                    loc.y + (radius * (float)Math.sin(Math.toRadians(aimAngleTop + i)) + x * (float)Math.sin(Math.toRadians(aimAngle - 90f)) + y * (float)Math.cos(Math.toRadians(aimAngle - 90f)))
            );
        }
        GL11.glEnd();
        GL11.glPopMatrix();
    }

    private void copyShip(FleetSide fleetSide,ShipAPI newShip, ShipAPI s,int nowMarines,List<ShipAPI> removeShip)
    {
        CombatEngineAPI engine = Global.getCombatEngine();
        newShip.setHitpoints(s.getHitpoints());
        newShip.getFluxTracker().setCurrFlux(s.getCurrFlux());
        newShip.getFluxTracker().setHardFlux(s.getMinFlux());
        newShip.setCRAtDeployment(s.getCRAtDeployment());
        newShip.setCurrentCR(s.getCurrentCR());
        newShip.setOwner(fleetSide.ordinal());
        if (newShip.getShipAI()!=null) {
            newShip.getShipAI().forceCircumstanceEvaluation();
        }


        if(newShip.getSystem()!=null){
            newShip.getSystem().setCooldownRemaining(s.getSystem().getCooldownRemaining());
            //newShip.setShipSystemDisabled(true);
        }

        if(!s.getVariant().hasHullMod(HullMods.AUTOMATED)) {
            if (!Float.isNaN(s.getFleetMember().getCrewComposition().getCrew())) {
                newShip.getFleetMember().getCrewComposition().setCrew(s.getFleetMember().getCrewComposition().getCrew());
            }
        }
        newShip.setAngularVelocity(s.getAngularVelocity());
        newShip.getVelocity().set(s.getVelocity());
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
        if (!(engine.isSimulation() || engine.isMission())) {
            if (Global.getSector() != null) {
                //如果是敌人抢玩家船
                if (fleetSide == FleetSide.ENEMY) {
                    if (Global.getSector().getPlayerFleet() != null) {
                        CampaignFleetAPI playerFleet = Global.getSector().getPlayerFleet();
                        playerFleet.getFleetData().removeFleetMember(s.getFleetMember());
                        if (playerFleet != null) {
                            if (playerFleet.getBattle() != null) {
                                if (playerFleet.getBattle().getNonPlayerSide() != null) {
                                    List<CampaignFleetAPI> fleetList = playerFleet.getBattle().getNonPlayerSide();
                                    for (CampaignFleetAPI f : fleetList) {
                                        f.getFleetData().addFleetMember(newShip.getFleetMember());
                                        break;
                                    }
                                }
                            }
                        }
                    }
                } else {
                    if (Global.getSector().getPlayerFleet() != null) {
                        CampaignFleetAPI playerFleet = Global.getSector().getPlayerFleet();
                        playerFleet.getFleetData().addFleetMember(newShip.getFleetMember());
                        //回收陆战队
                        playerFleet.getCargo().addMarines(nowMarines);
                        if (playerFleet != null) {
                            if (playerFleet.getBattle() != null) {
                                if (playerFleet.getBattle().getNonPlayerSide() != null) {
                                    List<CampaignFleetAPI> fleetList = playerFleet.getBattle().getNonPlayerSide();
                                    for (CampaignFleetAPI f : fleetList) {
                                        f.getFleetData().removeFleetMember(s.getFleetMember());
                                        if (f.getFleetData().getMembersListCopy().size() == 0) {
                                            playerFleet.getCargo().addAll(f.getCargo());
                                        }
                                    }
                                }
                                if (playerFleet.getBattle().getNonPlayerCombined() != null) {
                                    playerFleet.getBattle().getNonPlayerCombined().removeFleetMemberWithDestructionFlash(s.getFleetMember());
                                }
                            }
                        }
                    }
                }
            }
        }
        removeShip.add(s);
    }

    public int disableWeaponAndEngine(ShipAPI s) {
        int count = 0;
        List<ShipEngineControllerAPI.ShipEngineAPI> shipEngines = s.getEngineController().getShipEngines();
        for (int e = 0; e < shipEngines.size(); e++) {
            ShipEngineControllerAPI.ShipEngineAPI oe = shipEngines.get(e);
            if (!oe.isDisabled()) {
                oe.disable(true);
                count++;
            }
        }
        List<WeaponAPI> shipWeapons = s.getAllWeapons();
        for (int w = 0; w < shipWeapons.size(); w++) {
            WeaponAPI ow = shipWeapons.get(w);
            if (!ow.isDisabled()) {
                ow.disable(true);
                count++;
            }
        }
        s.setShipSystemDisabled(true);
        return count;
    }

    public float getOccupyLength(float occupy) {
        float occupyLength = 0;
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
        return occupyLength;
    }

    @Override
    public void processInputPreCoreControls(float amount, List<InputEventAPI> events) {

    }

    @Override
    public void advance(float amount, List<InputEventAPI> events) {
        if (engine==null) {return;}
        if (engine.isPaused()) {return;}
        try {
            //清理掉无用
            if (engine.getCustomData().get(ANCHORED_ENTITY_LIST) != null) {
                anchoredEntityList = (List<RC_AnchoredEntity>) engine.getCustomData().get(ANCHORED_ENTITY_LIST);
                List<RC_AnchoredEntity> removeList = new ArrayList<>();
                for (RC_AnchoredEntity m : anchoredEntityList) {
                    if (((ShipAPI) m.getAnchor()).isHulk()) {
                        removeList.add(m);
                    }
                }
                anchoredEntityList.removeAll(removeList);
            }

            //设置武器弹药
            List<ShipAPI> shipList = engine.getShips();
            List<ShipAPI> removeShip = new ArrayList<>();
            ShipAPI player = engine.getPlayerShip();
            /* 由于某些情况无法结束游戏
            List<FleetMemberAPI> removeFleetMember = new ArrayList<>();
            List<CampaignFleetAPI> fleetList = Global.getSector().getPlayerFleet().getBattle().getNonPlayerSide();
            if (Global.getSector().getPlayerFleet().getBattle().getNonPlayerCombined().getFleetData().getMembersListCopy().get(3).getHullSpec().getHullName().contains("伯劳鸟"))
            {
                Global.getSector().getPlayerFleet().getBattle().getNonPlayerCombined().getFleetData().removeFleetMember(Global.getSector().getPlayerFleet().getBattle().getNonPlayerCombined().getFleetData().getMembersListCopy().get(3));
            }
            if (Global.getSector().getPlayerFleet().getFleetData().getMembersListCopy().get(27).getHullSpec().getHullName().contains("岩块"))
            {
                Global.getSector().getPlayerFleet().getFleetData().removeFleetMember(Global.getSector().getPlayerFleet().getFleetData().getMembersListCopy().get(27));
            }
            */
            for (ShipAPI s : shipList) {
                //如果船存活
                if (!s.isFighter()&&s.isAlive()) {
                    if (!(engine.isSimulation() || engine.isMission())) {
                        setAmmo(s);
                    }
                    //当前有多少陆战队
                    int nowMarines = 0;
                    if (s.getCustomData().get(MONSTER_BALL_ON_HIT_EFFECT+NOW_MARINES) != null) {
                        nowMarines = (int) s.getCustomData().get(MONSTER_BALL_ON_HIT_EFFECT+NOW_MARINES);
                    }

                    float crew = getCrew(s);
                    float mult = getMult(s,nowMarines);
                    if(player!=null) {
                        if(s.equals(player.getShipTarget())) {
                            float needMarines = getMarines(s,crew);
                            engine.maintainStatusForPlayerShip(ID, SPRITE_NAME, "占领目标 持有陆战队：" + Global.getSector().getPlayerFleet().getCargo().getMarines(),"至少需要:"+ (int)needMarines + "名陆战队员 当前:" + nowMarines, true);
                        }
                    }

                    //占领值
                    float occupy = 0;
                    if (occupyMap.get(s) != null) {
                        occupy = occupyMap.get(s);
                        if (occupy < 0) {
                            occupy = 0;
                        }
                        if(nowMarines>0)
                        {
                            float oldOccupy = occupy;
                            occupy += mult*amount;
                            if(occupy>oldOccupy)
                            {
                                if(!"正在占领中".equals(s.getCustomData().get(MONSTER_BALL_ON_HIT_EFFECT + TEXT))) {
                                    engine.addFloatingText(s.getLocation(), "正在占领中", 50f, Color.GREEN, s,  FLASH_FREQUENCY, FLASH_DURATION);
                                    s.getCustomData().put(MONSTER_BALL_ON_HIT_EFFECT + TEXT, "正在占领中");
                                }
                            }
                            else {
                                if(!"舰船状态良好，登陆失败".equals(s.getCustomData().get(MONSTER_BALL_ON_HIT_EFFECT + TEXT))) {
                                    engine.addFloatingText(s.getLocation(), "舰船状态良好，登陆失败", 50f, Color.YELLOW, s,  FLASH_FREQUENCY, FLASH_DURATION);
                                    s.getCustomData().put(MONSTER_BALL_ON_HIT_EFFECT + TEXT, "舰船状态良好，登陆失败");
                                }
                            }
                        }
                        else if(occupy>0)
                        {
                            //状态越好占领值掉的掉的越快
                            if(mult<0) {
                                occupy -= (1 - occupy / 100) * amount * (-mult+1);
                            }
                            else {
                                occupy -= (1 - occupy / 100) * amount;
                            }
                            if(!"登陆小队已全灭，舰船正在恢复".equals(engine.getCustomData().get(MONSTER_BALL_ON_HIT_EFFECT + TEXT))) {
                                engine.addFloatingText(s.getLocation(), "登陆小队已全灭，舰船正在恢复", FONT_SIZE, Color.RED, s,  FLASH_FREQUENCY, FLASH_DURATION);
                                engine.getCustomData().put(MONSTER_BALL_ON_HIT_EFFECT + TEXT, "登陆小队已全灭，舰船正在恢复");
                            }
                        }
                    }
                    if (!engine.isPaused()) {
                        occupyMap.put(s, occupy);
                    }

                    float occupyLength = getOccupyLength(occupy);

                    MutableShipStatsAPI stats = s.getMutableStats();
                    //占领值大于0时候
                    if (occupyLength > 0) {
                        float percent = occupyLength * -100f;
                        //下降机动性、武器装填速度、引擎武器修复速度、护盾转向速度
                        stats.getMaxSpeed().modifyPercent(ID,percent);
                        stats.getMaxTurnRate().modifyPercent(ID,percent);
                        stats.getTurnAcceleration().modifyPercent(ID,percent);
                        stats.getAcceleration().modifyPercent(ID,percent);

                        stats.getBallisticAmmoRegenMult().modifyPercent(ID,percent);
                        stats.getEnergyAmmoRegenMult().modifyPercent(ID,percent);
                        stats.getMissileAmmoRegenMult().modifyPercent(ID,percent);

                        stats.getCombatEngineRepairTimeMult().modifyPercent(ID,percent);
                        stats.getCombatWeaponRepairTimeMult().modifyPercent(ID,percent);

                        stats.getShieldTurnRateMult().modifyPercent(ID,percent);

                        stats.getFighterRefitTimeMult().modifyPercent(ID,percent);
                    }
                    else
                    {
                        stats.getMaxSpeed().unmodifyPercent(ID);
                        stats.getMaxTurnRate().unmodifyPercent(ID);
                        stats.getTurnAcceleration().unmodifyPercent(ID);
                        stats.getAcceleration().unmodifyPercent(ID);

                        stats.getBallisticAmmoRegenMult().unmodifyPercent(ID);
                        stats.getEnergyAmmoRegenMult().unmodifyPercent(ID);
                        stats.getMissileAmmoRegenMult().unmodifyPercent(ID);

                        stats.getCombatEngineRepairTimeMult().unmodifyPercent(ID);
                        stats.getCombatWeaponRepairTimeMult().unmodifyPercent(ID);

                        stats.getShieldTurnRateMult().unmodifyPercent(ID);
                        stats.getFighterRefitTimeMult().unmodifyPercent(ID);
                    }

                    if (nowMarines > 0) {
                        //时间
                        float timer = 0;
                        if (timeMap.get(s) != null) {
                            timer = timeMap.get(s);
                            if (timer > MARINES_REFRESH_TIME) {
                                timer = 0;
                                //根据陆战队员和对方的人数和攻略进度进行陆战队的减员速度
                                float subMarines = getCrew(s) / MARINES_TO_CREW * (1 - occupyLength);
                                nowMarines -= subMarines;
                                if (nowMarines < 0) {
                                    nowMarines = 0;
                                }
                                s.setCustomData(MONSTER_BALL_ON_HIT_EFFECT + NOW_MARINES, nowMarines);
                                //敌方舰船人员削减
                                //马润减少的越多，船员减少的越少
                                float nowCrew = getCrew(s) - getCrew(s)/subMarines;
                                if(nowCrew < 1) {
                                    nowCrew = 1;
                                }
                                s.getFleetMember().getCrewComposition().setCrew(nowCrew);
                            } else {
                                timer += amount;
                            }
                        }
                        if (!engine.isPaused()) {
                            timeMap.put(s, timer);
                        }
                    }
                }
                //移除非法目标
                if(s.getShipTarget()!=null)
                {
                    if (removeShip.indexOf(s.getShipTarget())!=-1)
                    {
                        s.setShipTarget(null);
                    }
                }
            }

            for (ShipAPI s:occupyMap.keySet()) {
                if (s.isAlive()) {
                    float occupy = occupyMap.get(s);
                    float occupyLength = getOccupyLength(occupy);
                    if (occupyLength >= 1) {
                        int nowMarines = 0;
                        if (s.getCustomData().get(MONSTER_BALL_ON_HIT_EFFECT + NOW_MARINES) != null) {
                            nowMarines = (int) s.getCustomData().get(MONSTER_BALL_ON_HIT_EFFECT + NOW_MARINES);
                        }
                        boolean allowed = true;
                        //先找出自哪个mod
                        String hullId = s.getHullSpec().getHullId().replace("_default_D", "");
                        String modId = shipFromMod.get(hullId);
                        if (modId != null) {
                            Set<String> whiteSet = white.get(modId);
                            Set<String> blackSet = black.get(modId);
                            if (whiteSet != null) {
                                if (!whiteSet.contains(hullId)) {
                                    allowed = false;
                                }
                            } else if (blackSet != null) {
                                if (blackSet.contains(hullId)) {
                                    allowed = false;
                                }
                            }
                        }
                        //不是无人机不是空间站不是模块&&!s.getVariant().hasHullMod(HullMods.AUTOMATED)
                        if (s.isAlive() && !s.isStationModule() && !s.isStation() && !Misc.isUnboardable(s.getFleetMember()) && !hullId.contains(ZIGGURAT) && allowed) {
                            FleetSide fleetSide = FleetSide.PLAYER;
                            if (s.getOwner() == 0) {
                                fleetSide = FleetSide.ENEMY;
                            }
                            FleetMemberAPI member = Global.getFactory().createFleetMember(FleetMemberType.SHIP, s.getVariant());
                            member.setOwner(fleetSide.ordinal());
                            member.getCrewComposition().addCrew(member.getNeededCrew());
                            float height = engine.getMapHeight();
                            Vector2f newLocation = new Vector2f(s.getCollisionRadius() * 4, s.getOwner() == 0 ? height / -2f - 20000f - s.getCollisionRadius() * 4 : height / 2f + 20000f + s.getCollisionRadius() * 4);
                            ShipAPI newShip = Global.getCombatEngine().getFleetManager(fleetSide)
                                    .spawnFleetMember(member, newLocation, s.getFacing(), 0f);
                            //尝试捕捉模块船
                            try {
                                if (s.isShipWithModules()) {
                                    List<ShipAPI> newShips = newShip.getChildModulesCopy();
                                    List<ShipAPI> oldShips = s.getChildModulesCopy();
                                    for (int ss = 0; ss < newShips.size(); ss++) {
                                        ShipAPI ns = newShips.get(ss);
                                        ShipAPI os = oldShips.get(ss);
                                        if (os.isAlive()) {
                                            //拷贝
                                            int nm = 0;
                                            if (s.getCustomData().get(MONSTER_BALL_ON_HIT_EFFECT + NOW_MARINES) != null) {
                                                nm = (int) s.getCustomData().get(MONSTER_BALL_ON_HIT_EFFECT + NOW_MARINES);
                                            }
                                            copyShip(fleetSide, ns, os, nm, removeShip);
                                        } else {
                                            //删除
                                            removeShip.add(ns);
                                        }
                                    }
                                }
                            } catch (Exception e) {
                                Global.getLogger(this.getClass()).info(e);
                            }
                            copyShip(fleetSide, newShip, s, nowMarines, removeShip);
                            engine.addFloatingText(s.getLocation(), "占领成功", FONT_SIZE, Color.GREEN, s, FLASH_FREQUENCY, FLASH_DURATION);
                        } else {
                            /*
                            List<ShipAPI> isCatch = new ArrayList<>();
                            if(engine.getCustomData().get(IS_CATCH)!=null)
                            {
                                isCatch = (List<ShipAPI>)engine.getCustomData().get(IS_CATCH);
                            }
                            isCatch.add(s);
                            engine.getCustomData().put(IS_CATCH,isCatch);
                            */
                            int count = disableWeaponAndEngine(s);
                            if (count != 0) {
                                engine.addFloatingText(s.getLocation(), "占领成功", FONT_SIZE, Color.GREEN, s, FLASH_FREQUENCY, FLASH_DURATION);
                            }
                        }
                    }
                }
            }
            for(ShipAPI r:removeShip)
            {
                //先移动到屏幕外面然后炸掉
                engine.removeObject(r);
            }
        }
        catch (Exception e)
        {
            Global.getLogger(this.getClass()).info(e);
        }
    }

    @Override
    public void renderInWorldCoords(ViewportAPI viewport) {
        if(engine==null) {return;}
        if(engine.getCustomData().get(ANCHORED_ENTITY_LIST)!=null)
        {
            anchoredEntityList = (List<RC_AnchoredEntity>) engine.getCustomData().get(ANCHORED_ENTITY_LIST);
            for (RC_AnchoredEntity m:anchoredEntityList)
            {
                float targetFace = m.getAnchor().getFacing();
                float relativeAngle = m.getRelativeAngle();
                sprite.setAngle(relativeAngle+targetFace-90);
                sprite.renderAtCenter(m.getLocation().getX(), m.getLocation().getY());
            }
        }
        //渲染圈圈
        for (ShipAPI s:occupyMap.keySet()) {
            float occupy = occupyMap.get(s);
            float occupyLength = getOccupyLength(occupy);

            int red = Math.round((1 - occupyLength) * 255);
            int green = Math.round(occupyLength * 153);
            if (s.getOwner() == 0) {
                red = Math.round(occupyLength * 255);
                green = Math.round((1 - occupyLength) * 153);
            }
            try {
                Color COLOR = new Color(red, green, 0, 100);
                if (s != null) {
                    if (!RC_Util.hide(engine) && s.isAlive() && viewport != null) {
                        if (occupyLength > 0) {
                            drawArc(GREY, viewport.getAlphaMult(), 360f, s.getLocation(), s.getCollisionRadius(), 0f, 0f, 0f, 0f, 10);
                            drawArc(COLOR, viewport.getAlphaMult(), 360f * occupyLength, s.getLocation(), s.getCollisionRadius(), 0f, 0f, 0f, 0f, 10);
                        }
                    }
                }
            } catch (Exception e) {
                Global.getLogger(this.getClass()).info(e);
            }
        }
        //
    }

    @Override
    public void renderInUICoords(ViewportAPI viewport) {

    }
}