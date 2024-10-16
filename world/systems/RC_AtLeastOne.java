package real_combat.world.systems;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.impl.campaign.DerelictShipEntityPlugin;
import com.fs.starfarer.api.impl.campaign.ids.Entities;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import com.fs.starfarer.api.impl.campaign.procgen.NameGenData;
import com.fs.starfarer.api.impl.campaign.procgen.NebulaEditor;
import com.fs.starfarer.api.impl.campaign.procgen.ProcgenUsedNames;
import com.fs.starfarer.api.impl.campaign.procgen.StarAge;
import com.fs.starfarer.api.impl.campaign.procgen.themes.BaseThemeGenerator;
import com.fs.starfarer.api.impl.campaign.procgen.themes.SalvageSpecialAssigner;
import com.fs.starfarer.api.impl.campaign.terrain.HyperspaceTerrainPlugin;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.api.util.WeightedRandomPicker;

import java.util.List;
import java.util.Random;

public class RC_AtLeastOne {
    protected Random random = new Random();
    public void generate(SectorAPI sector) {
        for (StarSystemAPI system:sector.getStarSystems()) {
            //只有主星的随机加一个行星
            if(system.getPlanets().size()<=1&&system.getStar()!=null) {
                Boolean hasMarket = false;
                for (SectorEntityToken s:system.getAllEntities()) {
                    if (s.getMarket()!=null) {
                        hasMarket = true;
                        break;
                    }
                }
                if (hasMarket) {continue;}
                List<BaseThemeGenerator.OrbitGap> gaps = BaseThemeGenerator.findGaps(system.getCenter(), 6000, 20000, 800);
                float orbitRadius = 7000;
                if (!gaps.isEmpty()) {
                    orbitRadius = (gaps.get(0).start + gaps.get(0).end) * 0.5f;
                }

                float orbitDays = orbitRadius / (20f + random.nextFloat() * 5f);
                float radius = 100f + random.nextFloat() * 50f;
                float angle = random.nextFloat() * 360f;
                PlanetSpecAPI planetSpec = pickStar(system.getAge());
                if (planetSpec==null) {continue;}
                String type = planetSpec.getPlanetType();
                ProcgenUsedNames.NamePick namePick = ProcgenUsedNames.pickName(NameGenData.TAG_PLANET, null, null);
                String name = namePick.nameWithRomanSuffixIfAny;
                PlanetAPI planet = system.addPlanet(Misc.genUID(), system.getStar(), name, type, angle, radius, orbitRadius, orbitDays);
                if (planet==null) {continue;}
                //StarSystemGenerator.addOrbitingEntities(system,system.getStar(),system.getAge(),0,1,orbitRadius,3,false);

                BaseThemeGenerator.StarSystemData data = new BaseThemeGenerator.StarSystemData();
                WeightedRandomPicker<String> derelictShipFactions = new WeightedRandomPicker<String>(random);
                derelictShipFactions.add(Factions.PIRATES);
                WeightedRandomPicker<String> hulls = new WeightedRandomPicker<String>(random);
                hulls.add("RC_spider_queen", 1f);
                //残骸
                addShipGraveyard(data, planet, derelictShipFactions, hulls);
                for (BaseThemeGenerator.AddedEntity ae : data.generated) {
                    SalvageSpecialAssigner.assignSpecials(ae.entity, true);
                }
                cleanup(system);
            }
        }
        /*
        //create a star system
        StarSystemAPI system = sector.createStarSystem("Peach Garden");
        //set its location
        system.getLocation().set(-14200f, -10000f);
        //set background image
        system.setBackgroundTextureFilename("graphics/backgrounds/background6.jpg");

        //the star
        PlanetAPI pg_Star = system.initStar("TaoHuaYuan", "star_yellow", 600f, 350f);

        //background light color
        system.setLightColor(new Color(255, 185, 50));

        //make asteroid belt surround it
        system.addAsteroidBelt(pg_Star, 100, 2200f, 150f, 180, 360, Terrain.ASTEROID_BELT, "");

        //a new planet for people
        PlanetAPI yuanming = system.addPlanet("cmc_planet_yuanming", pg_Star, I18nUtil.getStarSystemsString("planet_name_yuanming"), "terran", 215, 120f, 4500f, 365f);

        //a new market for planet
        MarketAPI yuanmingMarket = RCWorldGen.addMarketplace("cmc", yuanming, null
                , yuanming.getName(), 6,
                new ArrayList<>(
                        Arrays.asList(
                                Conditions.POPULATION_7, // population
                                Conditions.HABITABLE,
                                Conditions.FARMLAND_BOUNTIFUL,
                                Conditions.MILD_CLIMATE,
                                Conditions.RUINS_WIDESPREAD
                        )),
                new ArrayList<>(
                        Arrays.asList(
                                Submarkets.GENERIC_MILITARY,
                                Submarkets.SUBMARKET_BLACK,
                                Submarkets.SUBMARKET_OPEN,
                                Submarkets.SUBMARKET_STORAGE
                        )),
                new ArrayList<>(
                        Arrays.asList(
                                Industries.POPULATION,
                                Industries.MEGAPORT,
                                Industries.STARFORTRESS_MID,
                                Industries.FARMING,
                                Industries.PATROLHQ,
                                Industries.ORBITALWORKS,
                                Industries.WAYSTATION,
                                Industries.TECHMINING,
                                Industries.HEAVYBATTERIES
                        )),
                0.3f,
                false,
                true);
        //make a custom description which is specified in descriptions.csv
        yuanming.setCustomDescriptionId("cmc_planet_yuanming");
        //yuanmingMarket.setAdmin(People.getPerson());
        //Misc.getAICoreAdminPlugin(Commodities.ALPHA_CORE).createPerson()

        Industry yuanmingOrbitalWorks = yuanmingMarket.getIndustry(Industries.ORBITALWORKS);
        //give the orbital works a gamma core
        yuanmingOrbitalWorks.setAICoreId(Commodities.GAMMA_CORE);
        // give it a nanoforge
        yuanmingOrbitalWorks.setSpecialItem(new SpecialItemData(Items.CORRUPTED_NANOFORGE, null));
        // and apply its effects
        InstallableItemEffect itemEffect = ItemEffectsRepo.ITEM_EFFECTS.get(Items.CORRUPTED_NANOFORGE);
        itemEffect.apply(yuanmingOrbitalWorks);

        PlanetAPI mingyue = system.addPlanet("cmc_planet_mingyue", yuanming, I18nUtil.getStarSystemsString("planet_name_mingyue"), "barren", 90, 60, 1000f, 30);

        MarketAPI mingyueMarket = RCWorldGen.addMarketplace("cmc", mingyue, null
                , mingyue.getName(), 6,
                new ArrayList<>(
                        Arrays.asList(
                                Conditions.POPULATION_5,
                                Conditions.ORE_MODERATE,
                                Conditions.RARE_ORE_RICH,
                                Conditions.VOLATILES_PLENTIFUL
                        )),
                new ArrayList<>(
                        Arrays.asList(
                                Submarkets.SUBMARKET_BLACK,
                                Submarkets.SUBMARKET_OPEN,
                                Submarkets.SUBMARKET_STORAGE
                        )),
                new ArrayList<>(
                        Arrays.asList(
                                Industries.POPULATION,
                                Industries.SPACEPORT,
                                Industries.MINING,
                                Industries.HEAVYINDUSTRY,
                                Industries.LIGHTINDUSTRY,
                                Industries.REFINING
                        )),
                0.3f,
                true,
                true);
        mingyue.setCustomDescriptionId("cmc_planet_mingyue");
        Industry mingyegHeavyIndustry = mingyueMarket.getIndustry(Industries.HEAVYINDUSTRY);
        // give it a corrupted nanoforge
        mingyegHeavyIndustry.setSpecialItem(new SpecialItemData(Items.CORRUPTED_NANOFORGE, null));
        // and apply its effects
        itemEffect.apply(mingyegHeavyIndustry);

        // generates hyperspace destinations for in-system jump points
        system.autogenerateHyperspaceJumpPoints(true, true);
        // Debris
        DebrisFieldParams params = new DebrisFieldParams(
                150f, // field radius - should not go above 1000 for performance reasons
                1f, // density, visual - affects number of debris pieces
                10000000f, // duration in days
                0f); // days the field will keep generating glowing pieces
        params.source = DebrisFieldSource.MIXED;
        params.baseSalvageXP = 500; // base XP for scavenging in field
        SectorEntityToken debris = Misc.addDebrisField(system, params, StarSystemGenerator.random);
        SalvageSpecialAssigner.assignSpecialForDebrisField(debris);

        // makes the debris field always visible on map/sensors and not give any xp or notification on being discovered
        debris.setSensorProfile(null);
        debris.setDiscoverable(null);

        // makes it discoverable and give 200 xp on being found
        // sets the range at which it can be detected (as a sensor contact) to 2000 units
        // commented out.
        debris.setDiscoverable(true);
        debris.setDiscoveryXP(200f);
        debris.setSensorProfile(1f);
        debris.getDetectedRangeMod().modifyFlat("gen", 2000);

        debris.setCircularOrbit(pg_Star, 45 + 10, 1600, 250);
        //Finally cleans up hyperspace

        cleanup(system);
        */
    }

    //Learning from Tart scripts
    //Clean nearby Nebula(nearby system)
    private void cleanup(StarSystemAPI system) {
        HyperspaceTerrainPlugin plugin = (HyperspaceTerrainPlugin) Misc.getHyperspaceTerrain().getPlugin();
        NebulaEditor editor = new NebulaEditor(plugin);
        float minRadius = plugin.getTileSize() * 2f;

        float radius = system.getMaxRadiusInHyperspace();
        editor.clearArc(system.getLocation().x, system.getLocation().y, 0, radius + minRadius * 0.5f, 0, 360f);
        editor.clearArc(system.getLocation().x, system.getLocation().y, 0, radius + minRadius, 0, 360f, 0.25f);
    }

    public PlanetSpecAPI pickStar(StarAge age) {
        WeightedRandomPicker<PlanetSpecAPI> picker = new WeightedRandomPicker<PlanetSpecAPI>(random);
        for (PlanetSpecAPI spec : Global.getSettings().getAllPlanetSpecs()) {
            if (spec.isStar()) continue;
            String id = spec.getPlanetType();
            picker.add(spec, 1f);
        }
        return picker.pick();
    }

    public void addShipGraveyard(BaseThemeGenerator.StarSystemData data, SectorEntityToken focus, WeightedRandomPicker<String> factions,
                                 WeightedRandomPicker<String> hulls) {
        int numShips = random.nextInt(9) + 3;
        //numShips = 12;
        if (BaseThemeGenerator.DEBUG) System.out.println("    Adding ship graveyard (" + numShips + " ships)");

        WeightedRandomPicker<Float> bands = new WeightedRandomPicker<Float>(random);
        for (int i = 0; i < numShips + 5; i++) {
            bands.add(new Float(140 + i * 20), (i + 1) * (i + 1));
        }

        for (int i = 0; i < numShips; i++) {
            float radius = bands.pickAndRemove();

            DerelictShipEntityPlugin.DerelictShipData params = DerelictShipEntityPlugin.createRandom(factions.pick(), null, random, DerelictShipEntityPlugin.getDefaultSModProb());
            if (hulls != null && !hulls.isEmpty()) {
                params = DerelictShipEntityPlugin.createHull(hulls.pickAndRemove(), random, DerelictShipEntityPlugin.getDefaultSModProb());
            }
            if (params != null) {
                CustomCampaignEntityAPI entity = (CustomCampaignEntityAPI) BaseThemeGenerator.addSalvageEntity(random,
                        focus.getContainingLocation(),
                        Entities.WRECK, Factions.NEUTRAL, params);
                entity.setDiscoverable(true);
                float orbitDays = radius / (5f + random.nextFloat() * 10f);
                entity.setCircularOrbit(focus, random.nextFloat() * 360f, radius, orbitDays);
                if (BaseThemeGenerator.DEBUG) System.out.println("      Added ship: " +
                        ((DerelictShipEntityPlugin)entity.getCustomPlugin()).getData().ship.variantId);

                BaseThemeGenerator.AddedEntity added = new BaseThemeGenerator.AddedEntity(entity, null, Entities.WRECK);
                data.generated.add(added);
            }
        }
    }
}
