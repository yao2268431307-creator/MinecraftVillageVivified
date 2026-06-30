package com.livingvillages.adapter.data;

import com.livingvillages.core.data.*;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import java.util.*;

/**
 * NBT-persisted VillageStateStore. Serializes all 11 fields.
 */
public class NbtVillageStateStore extends SavedData implements VillageStateStore {

    private static final String DATA_NAME = "livingvillages_data";

    private List<VillageRecord> villages = new ArrayList<>();
    private List<ClusterRecord> clusters = new ArrayList<>();
    private Map<String, ClusterName> clusterNames = new HashMap<>();
    private Map<UUID, String> villageNames = new HashMap<>();
    private List<EdgeRecord> edges = new ArrayList<>();
    private Map<String, Map<String, Double>> prices = new HashMap<>();
    private Map<UUID, Map<String, Integer>> warehouses = new HashMap<>();
    private List<CaravanState> caravans = new ArrayList<>();
    private Map<UUID, Integer> villageLevels = new HashMap<>();
    private Map<UUID, Map<String, Long>> accumulatedConsumption = new HashMap<>();
    private Map<UUID, String> specialities = new HashMap<>();

    // ── SavedData ──

    public static NbtVillageStateStore load(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(
            new Factory<>(NbtVillageStateStore::new, NbtVillageStateStore::fromNbt, null), DATA_NAME);
    }

    @SuppressWarnings("unchecked")
    private static NbtVillageStateStore fromNbt(CompoundTag root, HolderLookup.Provider provider) {
        NbtVillageStateStore s = new NbtVillageStateStore();
        // villages
        ListTag vlist = root.getList("villages", Tag.TAG_COMPOUND);
        for (int i = 0; i < vlist.size(); i++) {
            CompoundTag t = vlist.getCompound(i);
            s.villages.add(new VillageRecord(
                t.getUUID("id"),
                new Vec3i(t.getInt("x"), t.getInt("y"), t.getInt("z")),
                t.getString("biome"),
                t.getInt("beds"),
                t.getLong("tick"),
                t.getBoolean("placed")));
        }
        // clusters
        ListTag clist = root.getList("clusters", Tag.TAG_COMPOUND);
        for (int i = 0; i < clist.size(); i++) {
            CompoundTag t = clist.getCompound(i);
            List<UUID> members = new ArrayList<>();
            for (Tag tag : t.getList("members", Tag.TAG_INT_ARRAY))
                members.add(NbtUtils.loadUUID(tag));
            s.clusters.add(new ClusterRecord(t.getString("id"), members,
                t.getUUID("center"), t.getBoolean("named"), t.getBoolean("edgesBuilt")));
        }
        // clusterNames
        CompoundTag cnTag = root.getCompound("clusterNames");
        for (String key : cnTag.getAllKeys()) {
            CompoundTag ct = cnTag.getCompound(key);
            Map<UUID, String> sats = new HashMap<>();
            CompoundTag satTag = ct.getCompound("satellites");
            for (String sk : satTag.getAllKeys()) sats.put(UUID.fromString(sk), satTag.getString(sk));
            s.clusterNames.put(key, new ClusterName(ct.getString("name"), ct.getString("town"), sats));
        }
        // villageNames
        CompoundTag vnTag = root.getCompound("villageNames");
        for (String key : vnTag.getAllKeys()) s.villageNames.put(UUID.fromString(key), vnTag.getString(key));
        // edges
        ListTag elist = root.getList("edges", Tag.TAG_COMPOUND);
        for (int i = 0; i < elist.size(); i++) {
            CompoundTag et = elist.getCompound(i);
            List<Vec3i> path = new ArrayList<>();
            for (Tag pt : et.getList("path", Tag.TAG_COMPOUND)) {
                CompoundTag pc = (CompoundTag) pt;
                path.add(new Vec3i(pc.getInt("x"), pc.getInt("y"), pc.getInt("z")));
            }
            s.edges.add(new EdgeRecord(et.getString("from"), et.getString("to"), path,
                EdgeType.valueOf(et.getString("type"))));
        }
        // prices
        CompoundTag pTag = root.getCompound("prices");
        for (String cid : pTag.getAllKeys()) {
            CompoundTag inner = pTag.getCompound(cid);
            Map<String, Double> map = new HashMap<>();
            for (String item : inner.getAllKeys()) map.put(item, inner.getDouble(item));
            s.prices.put(cid, map);
        }
        // warehouses
        CompoundTag wTag = root.getCompound("warehouses");
        for (String vid : wTag.getAllKeys()) {
            CompoundTag inner = wTag.getCompound(vid);
            Map<String, Integer> map = new HashMap<>();
            for (String item : inner.getAllKeys()) map.put(item, inner.getInt(item));
            s.warehouses.put(UUID.fromString(vid), map);
        }
        // caravans
        ListTag cvlist = root.getList("caravans", Tag.TAG_COMPOUND);
        for (int i = 0; i < cvlist.size(); i++) {
            CompoundTag ct = cvlist.getCompound(i);
            Map<String, Integer> cargo = new HashMap<>();
            CompoundTag cg = ct.getCompound("cargo");
            for (String k : cg.getAllKeys()) cargo.put(k, cg.getInt(k));
            s.caravans.add(new CaravanState(ct.getUUID("id"), ct.getString("from"), ct.getString("to"),
                ct.getInt("pathIdx"), ct.getInt("fuel"), cargo,
                CaravanPhase.valueOf(ct.getString("phase"))));
        }
        // levels, consumption, specialities (simple maps)
        s.villageLevels = readIntMap(root.getCompound("levels"));
        s.specialities = readStringMap(root.getCompound("specialities"));
        s.accumulatedConsumption = readLongMapMap(root.getCompound("consumption"));
        return s;
    }

    @Override
    public CompoundTag save(CompoundTag root, HolderLookup.Provider provider) {
        // villages
        ListTag vlist = new ListTag();
        for (VillageRecord v : villages) {
            CompoundTag t = new CompoundTag();
            t.putUUID("id", v.id()); t.putInt("x", v.position().x()); t.putInt("y", v.position().y());
            t.putInt("z", v.position().z()); t.putString("biome", v.biomeCategory());
            t.putInt("beds", v.bedCount()); t.putLong("tick", v.firstSeenTick());
            t.putBoolean("placed", v.placed()); vlist.add(t);
        }
        root.put("villages", vlist);
        // clusters
        ListTag clist = new ListTag();
        for (ClusterRecord c : clusters) {
            CompoundTag t = new CompoundTag();
            t.putString("id", c.id()); t.putUUID("center", c.centerVillageId());
            t.putBoolean("named", c.isNamed()); t.putBoolean("edgesBuilt", c.edgesBuilt());
            ListTag mlist = new ListTag();
            for (UUID mid : c.memberVillageIds()) mlist.add(NbtUtils.createUUID(mid));
            t.put("members", mlist); clist.add(t);
        }
        root.put("clusters", clist);
        // names
        CompoundTag cn = new CompoundTag();
        for (var e : clusterNames.entrySet()) {
            CompoundTag ct = new CompoundTag();
            ct.putString("name", e.getValue().clusterName());
            ct.putString("town", e.getValue().centerTownName());
            CompoundTag st = new CompoundTag();
            e.getValue().satelliteNames().forEach((k, v) -> st.putString(k.toString(), v));
            ct.put("satellites", st); cn.put(e.getKey(), ct);
        }
        root.put("clusterNames", cn);
        CompoundTag vn = new CompoundTag();
        villageNames.forEach((k, v) -> vn.putString(k.toString(), v));
        root.put("villageNames", vn);
        // edges
        ListTag elist = new ListTag();
        for (EdgeRecord e : edges) {
            CompoundTag et = new CompoundTag();
            et.putString("from", e.fromClusterId()); et.putString("to", e.toClusterId());
            et.putString("type", e.type().name());
            ListTag plist = new ListTag();
            for (Vec3i p : e.path()) { CompoundTag pc = new CompoundTag(); pc.putInt("x", p.x());
                pc.putInt("y", p.y()); pc.putInt("z", p.z()); plist.add(pc); }
            et.put("path", plist); elist.add(et);
        }
        root.put("edges", elist);
        // prices
        CompoundTag pt = new CompoundTag();
        for (var e : prices.entrySet()) {
            CompoundTag inner = new CompoundTag();
            e.getValue().forEach(inner::putDouble); pt.put(e.getKey(), inner);
        }
        root.put("prices", pt);
        // warehouses
        CompoundTag wt = new CompoundTag();
        for (var e : warehouses.entrySet()) {
            CompoundTag inner = new CompoundTag();
            e.getValue().forEach(inner::putInt); wt.put(e.getKey().toString(), inner);
        }
        root.put("warehouses", wt);
        // caravans
        ListTag cvlist = new ListTag();
        for (CaravanState c : caravans) {
            CompoundTag ct = new CompoundTag();
            ct.putUUID("id", c.caravanId()); ct.putString("from", c.fromClusterId());
            ct.putString("to", c.toClusterId()); ct.putInt("pathIdx", c.currentPathIndex());
            ct.putInt("fuel", c.fuelTicks()); ct.putString("phase", c.phase().name());
            CompoundTag cg = new CompoundTag(); c.cargo().forEach(cg::putInt); ct.put("cargo", cg);
            cvlist.add(ct);
        }
        root.put("caravans", cvlist);
        // simple maps
        writeIntMap(root, "levels", villageLevels);
        writeStringMap(root, "specialities", specialities);
        writeLongMapMap(root, "consumption", accumulatedConsumption);
        return root;
    }

    // ── Helpers ──
    private static Map<UUID, Integer> readIntMap(CompoundTag tag) {
        Map<UUID, Integer> m = new HashMap<>();
        for (String k : tag.getAllKeys()) m.put(UUID.fromString(k), tag.getInt(k));
        return m;
    }
    private static Map<UUID, String> readStringMap(CompoundTag tag) {
        Map<UUID, String> m = new HashMap<>();
        for (String k : tag.getAllKeys()) m.put(UUID.fromString(k), tag.getString(k));
        return m;
    }
    private static Map<UUID, Map<String, Long>> readLongMapMap(CompoundTag tag) {
        Map<UUID, Map<String, Long>> m = new HashMap<>();
        for (String k : tag.getAllKeys()) {
            CompoundTag inner = tag.getCompound(k);
            Map<String, Long> im = new HashMap<>();
            for (String ik : inner.getAllKeys()) im.put(ik, inner.getLong(ik));
            m.put(UUID.fromString(k), im);
        }
        return m;
    }
    private static void writeIntMap(CompoundTag root, String key, Map<UUID, Integer> map) {
        CompoundTag t = new CompoundTag(); map.forEach((k, v) -> t.putInt(k.toString(), v)); root.put(key, t);
    }
    private static void writeStringMap(CompoundTag root, String key, Map<UUID, String> map) {
        CompoundTag t = new CompoundTag(); map.forEach((k, v) -> t.putString(k.toString(), v)); root.put(key, t);
    }
    private static void writeLongMapMap(CompoundTag root, String key, Map<UUID, Map<String, Long>> map) {
        CompoundTag t = new CompoundTag();
        map.forEach((k, v) -> { CompoundTag inner = new CompoundTag(); v.forEach(inner::putLong); t.put(k.toString(), inner); });
        root.put(key, t);
    }

    // ── VillageStateStore getters/setters ──
    @Override public List<VillageRecord> getVillages() { return Collections.unmodifiableList(villages); }
    @Override public void setVillages(List<VillageRecord> v) { villages = new ArrayList<>(v); setDirty(); }
    @Override public List<ClusterRecord> getClusters() { return Collections.unmodifiableList(clusters); }
    @Override public void setClusters(List<ClusterRecord> c) { clusters = new ArrayList<>(c); setDirty(); }
    @Override public Map<String, ClusterName> getClusterNames() { return Collections.unmodifiableMap(clusterNames); }
    @Override public void setClusterNames(Map<String, ClusterName> n) { clusterNames = new HashMap<>(n); setDirty(); }
    @Override public Map<UUID, String> getVillageNames() { return Collections.unmodifiableMap(villageNames); }
    @Override public void setVillageNames(Map<UUID, String> n) { villageNames = new HashMap<>(n); setDirty(); }
    @Override public List<EdgeRecord> getInterClusterEdges() { return Collections.unmodifiableList(edges); }
    @Override public void setInterClusterEdges(List<EdgeRecord> e) { edges = new ArrayList<>(e); setDirty(); }
    @Override public Map<String, Map<String, Double>> getPrices() { return Collections.unmodifiableMap(prices); }
    @Override public void setPrices(Map<String, Map<String, Double>> p) { prices = new HashMap<>(); p.forEach((k, v) -> prices.put(k, new HashMap<>(v))); setDirty(); }
    @Override public Map<UUID, Map<String, Integer>> getWarehouses() { return Collections.unmodifiableMap(warehouses); }
    @Override public void setWarehouses(Map<UUID, Map<String, Integer>> w) { warehouses = new HashMap<>(); w.forEach((k, v) -> warehouses.put(k, new HashMap<>(v))); setDirty(); }
    @Override public List<CaravanState> getCaravanStates() { return Collections.unmodifiableList(caravans); }
    @Override public void setCaravanStates(List<CaravanState> s) { caravans = new ArrayList<>(s); setDirty(); }
    @Override public Map<UUID, Integer> getVillageLevels() { return Collections.unmodifiableMap(villageLevels); }
    @Override public void setVillageLevels(Map<UUID, Integer> l) { villageLevels = new HashMap<>(l); setDirty(); }
    @Override public Map<UUID, Map<String, Long>> getAccumulatedConsumption() { return Collections.unmodifiableMap(accumulatedConsumption); }
    @Override public void setAccumulatedConsumption(Map<UUID, Map<String, Long>> c) { accumulatedConsumption = new HashMap<>(); c.forEach((k, v) -> accumulatedConsumption.put(k, new HashMap<>(v))); setDirty(); }
    @Override public Map<UUID, String> getSpecialities() { return Collections.unmodifiableMap(specialities); }
    @Override public void setSpecialities(Map<UUID, String> s) { specialities = new HashMap<>(s); setDirty(); }
    @Override public void markDirty() { setDirty(); }
}
