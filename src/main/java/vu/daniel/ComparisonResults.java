package vu.daniel;

public class ComparisonResults {
    int matches;
    int belowThreshold;
    int skippedClassEmpty;
    int skippedClusterEmpty;
    int skippedClassThing;
    int skippedClusterThing;
    double avgOverlapMatches;
    double avgOverlapTotal;

    ComparisonResults(int matches, int belowThreshold, int skippedClassEmpty, int skippedClusterEmpty, int skippedClassThing, int skippedClusterThing, double avgOverlapMatches, double avgOverlapTotal) {
        this.matches = matches;
        this.belowThreshold = belowThreshold;
        this.skippedClassEmpty =skippedClassEmpty;
        this.skippedClusterEmpty =skippedClusterEmpty;
        this.skippedClassThing = skippedClassThing;
        this.skippedClusterThing = skippedClusterThing;
        this.avgOverlapMatches = avgOverlapMatches;
        this.avgOverlapTotal = avgOverlapTotal;
    }
}
