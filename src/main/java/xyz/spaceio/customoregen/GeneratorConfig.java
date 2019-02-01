package xyz.spaceio.customoregen;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class GeneratorConfig {
    public List<GeneratorItem> itemList = new ArrayList<GeneratorItem>();
    public Set<String> enabledWorlds = new HashSet<>();
    public Set<String> worldguardRegions = new HashSet<>();
    public String permission = ";";
    public int unlock_islandLevel = 0;
}
