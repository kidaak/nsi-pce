package net.es.nsi.pce.pf.api;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Path {
    private List<StpPair> stpPairs = new ArrayList<>();

    public Path() {
    }

    public Path(StpPair... stpPairs) {
        this.stpPairs.addAll(Arrays.asList(stpPairs));
    }

    public List<StpPair> getStpPairs() {
        return stpPairs;
    }

    public void addStpPair(StpPair pair) {
        this.stpPairs.add(pair);
    }
}
