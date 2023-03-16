package real_combat.constant;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.MissileAPI;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipSystemAPI;
import com.fs.starfarer.api.impl.combat.*;
import data.shipsystems.scripts.AmmoFeedStats;
import data.shipsystems.scripts.HighEnergyFocusStats;
import data.shipsystems.scripts.MicroBurnStats;

import java.awt.*;
import java.util.HashMap;
import java.util.Map;

public class RC_ComboConstant {
    //等离子爆裂驱动器
    public static final String SKILL_MICROBURN = "WWW";
    //熵放大器
    public static final String SKILL_ENTROPYAMPLIFIER1 = "AAD";
    public static final String SKILL_ENTROPYAMPLIFIER2 = "DDA";
    //相位传送器
    //public static final String SKILL_PHASETELEPORTER1 = "WSAD";
    //public static final String SKILL_PHASETELEPORTER2 = "WSDA";
    //高能聚焦系统
    //public static final String SKILL_HIGHENERGYFOCUS1 = "ADAD";
    //public static final String SKILL_HIGHENERGYFOCUS2 = "DADA";
    //加速填弹器
    //public static final String SKILL_AMMOFEED1 = "WSWS";
    //public static final String SKILL_AMMOFEED2 = "SWSW";
    //量子干扰,acausaldisruptor
    public static final String SKILL_ACAUSALDISRUPTOR1 = "WWS";
    public static final String SKILL_ACAUSALDISRUPTOR2 = "SSW";
    //熵抑制器
    public static final String SKILL_DAMPER_OMEGA = "SSS";
    //EMP 发生器
    //public static final String SKILL_EMP1 = "ADAD";
    //public static final String SKILL_EMP2 = "DADA";
    //时流之壳
    public static final String SKILL_TEMPORALSHELL1 = "WSW";
    public static final String SKILL_TEMPORALSHELL2 = "SWS";


    public static final Map SKILL = new HashMap();
    static {
        SKILL.put(SKILL_MICROBURN,new MicroBurnStats());
        SKILL.put(SKILL_ENTROPYAMPLIFIER1, new EntropyAmplifierStats());
        //SKILL.put(SKILL_HIGHENERGYFOCUS1,new HighEnergyFocusStats());
        //SKILL.put(SKILL_AMMOFEED1,new AmmoFeedStats());

        SKILL.put(SKILL_ENTROPYAMPLIFIER2, new EntropyAmplifierStats());
        //SKILL.put(SKILL_HIGHENERGYFOCUS2,new HighEnergyFocusStats());
        //SKILL.put(SKILL_AMMOFEED2,new AmmoFeedStats());

        SKILL.put(SKILL_ACAUSALDISRUPTOR1, new AcausalDisruptorStats());
        SKILL.put(SKILL_ACAUSALDISRUPTOR2, new AcausalDisruptorStats());
        SKILL.put(SKILL_DAMPER_OMEGA, new DamperFieldOmegaStats());
        SKILL.put(SKILL_TEMPORALSHELL1, new TemporalShellStats());
        SKILL.put(SKILL_TEMPORALSHELL2, new TemporalShellStats());
    }

    public static final Map SKILL_CHINESE = new HashMap();
    static {
        SKILL_CHINESE.put(SKILL_MICROBURN,"等离子爆裂驱动器");
        SKILL_CHINESE.put(SKILL_ENTROPYAMPLIFIER1,"熵放大器");
        //SKILL_CHINESE.put(SKILL_PHASETELEPORTER1,"相位传送器");
        //SKILL_CHINESE.put(SKILL_HIGHENERGYFOCUS1,"高能聚焦系统");
        //SKILL_CHINESE.put(SKILL_AMMOFEED1,"加速填弹器");

        SKILL_CHINESE.put(SKILL_ENTROPYAMPLIFIER2,"熵放大器");
        //SKILL_CHINESE.put(SKILL_PHASETELEPORTER2,"相位传送器");
        //SKILL_CHINESE.put(SKILL_HIGHENERGYFOCUS2,"高能聚焦系统");
        //SKILL_CHINESE.put(SKILL_AMMOFEED2,"加速填弹器");

        SKILL_CHINESE.put(SKILL_ACAUSALDISRUPTOR1, "量子干扰");
        SKILL_CHINESE.put(SKILL_ACAUSALDISRUPTOR2, "量子干扰");
        SKILL_CHINESE.put(SKILL_DAMPER_OMEGA, "熵抑制器");
        SKILL_CHINESE.put(SKILL_TEMPORALSHELL1, "时流之壳");
        SKILL_CHINESE.put(SKILL_TEMPORALSHELL2, "时流之壳");
    }
}