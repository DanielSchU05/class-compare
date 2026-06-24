package vu.daniel;

import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.reasoner.OWLReasonerFactory;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;


public class Main {

    static void main() throws IOException {
        //measure code runtime
        long startTime = System.nanoTime();

        PathNames paths = new PathNames();

        //IDs of ontologies to be compared
        ArrayList<String> ontIds = Methods.extractOntIDs(paths.getOntologyIDsPath()+"fileorder.txt");
        ArrayList<String> usedOntIds = new ArrayList<>(ontIds);

        //set threshold for comparison
        double threshold = 0.75;

        int counter = 0;


        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm");

        String timestamp = LocalDateTime.now().format(formatter);


        try(PrintWriter summaryWriterPrecision = new PrintWriter(new FileWriter(paths.getSummaryWriterPath()+"comparison_summary-"+threshold+"-precision_"+timestamp+".csv"));
            PrintWriter summaryWriterRecall = new PrintWriter(new FileWriter(paths.getSummaryWriterPath()+"comparison_summary-"+threshold+"-recall_"+timestamp+".csv"));
            PrintWriter detailsWriter = new PrintWriter(new FileWriter(paths.getDetailsWriterPath()+"comparison_details-"+threshold+"_"+timestamp+".csv"))){

            String csv_header = "Ontology_ID,Threshold,Total_Classes,Total_Clusters,Matches_Found,Metric_Score,Avg_Overlap_Matches,Avg_Overlap_Total,Logical_Equivalence_Ratio,Total_Original_Classes,Total_Classes_Equivalent_to_Cluster,Skipped_Class_Empty,Skipped_Cluster_Empty,Skipped_Class_Thing,Skipped_Cluster_Thing,Below_Threshold";

            //Clustered --> Original (For each cluster, which 1 single class matches it best)
            summaryWriterPrecision.println(csv_header);

            //Original --> Clustered (how much of the original is covered in the clusters)
            summaryWriterRecall.println(csv_header);

            detailsWriter.println("Ontology_ID,Original_Class,Clustered_Cluster,Overlap_Score");




            for(String id: ontIds) {
                try {
                    String originalPath = paths.getOriginalPath() + id;
                    String clusteredPath = paths.getClusteredPath()+id+".clustering.owl";

                    //new managers and factories are created with each loop, otherwise memory leaks/increased memory usage occurs
                    OWLOntologyManager manager = Methods.loadManager();
                    OWLReasonerFactory factory = Methods.loadReasonerFactory();

                    //checking that files are present
                    File origFileCheck = new File(originalPath);
                    File clustFileCheck = new File(clusteredPath);

                    System.out.println("===================================================");
                    System.out.println("--- Processing ontology id: "+id+ " ---");
                    System.out.println("===================================================");
                    System.out.flush();
                    Thread.sleep(10);

                    if(!origFileCheck.exists()) {
                        System.err.println("Original ontology not found: " + originalPath);
                        System.err.flush();
                        Thread.sleep(10);
                        continue;
                    }
                    if (!clustFileCheck.exists()) {
                        System.err.println("Clustered ontology not found: " + clusteredPath);
                        if (origFileCheck.exists()) {
                            System.err.println("Clustering failed (timed out/invalid axioms)");

                            summaryWriterPrecision.printf("%s,%.2f,-,-,-,-,-,-,-,-,-,-,-,-,-,-\n", id,threshold);
                            summaryWriterRecall.printf("%s,%.2f,-,-,-,-,-,-,-,-,-,-,-,-,-,-\n", id,threshold);
                            System.err.flush();

                            usedOntIds.remove(id);
                            continue;
                        }
                        System.err.flush();
                        Thread.sleep(10);
                        continue;
                    }

                    //load onts
                    System.out.println("Loading ontologies...");
                    System.out.println("Loading original...");
                    OWLOntology originalOntology = Methods.loadOntology(originalPath, manager);
                    System.err.flush();
                    System.out.flush();
                    Thread.sleep(10);

                    System.out.println("Loading clustered...");
                    OWLOntology clusteredOntology = Methods.loadOntology(clusteredPath, manager);
                    System.err.flush();
                    System.out.flush();
                    Thread.sleep(10);



                    //get individuals to see if there is an abox, otherwise it will skip
                    Set<OWLClass> originalClasses = originalOntology.classesInSignature().collect(Collectors.toSet());

                    Set<OWLClass> filteredClusters = clusteredOntology.classesInSignature().collect(Collectors.toSet());
                    filteredClusters.removeAll(originalClasses);

                    //extract individuals
                    System.out.println("\nExtracting individuals from original ontology...");
                    Map<OWLClass, Set<OWLNamedIndividual>> map_original = Methods.findIndividuals(originalOntology, originalClasses, factory);

                    System.out.println("Extracting individuals from clustered ontology...");
                    Map<OWLClass, Set<OWLNamedIndividual>> map_clusters = Methods.findIndividuals(clusteredOntology, filteredClusters, factory);


                    double[] logicRatio = Methods.calculateLogicalEquivalence(originalOntology,clusteredOntology,factory);
                    System.err.flush();
                    System.out.flush();
                    Thread.sleep(10);


                    //Calculate Precision (Clustered -> Original) (how many of the clustered is correct)
                    System.out.println("\n  ===== Calculating Precision =====");
                    ComparisonResults resultsPrecision = Methods.compareOntologies(id, map_clusters, map_original, threshold, detailsWriter, "Precision");

                    //Calculate Recall (Original -> Clustered) (how many of the correct were found in cluster)
                    System.out.println("\n  ===== Calculating Recall =====");
                    ComparisonResults resultsRecall = Methods.compareOntologies(id, map_original, map_clusters, threshold, detailsWriter, "Recall");

                    double precisionScore;
                    if (map_clusters.isEmpty()) { precisionScore = 0.0; } else { precisionScore = (double) resultsPrecision.matches /map_clusters.size(); }

                    double recallScore;
                    if (map_original.isEmpty()) { recallScore = 0.0; } else { recallScore = (double) resultsRecall.matches /map_original.size(); }


                    //add results to PRECISION csv
                    summaryWriterPrecision.printf("%s,%.2f,%d,%d,%d,%.4f,%.4f,%.4f,%.4f,%.0f,%.0f,%d,%d,%d,%d,%d\n",
                            id, threshold, map_original.size(), map_clusters.size(),
                            resultsPrecision.matches, precisionScore, resultsPrecision.avgOverlapMatches, resultsPrecision.avgOverlapTotal,
                            logicRatio[0], logicRatio[1], logicRatio[2],
                            resultsPrecision.skippedClassEmpty, resultsPrecision.skippedClusterEmpty, resultsPrecision.skippedClassThing, resultsPrecision.skippedClusterThing, resultsPrecision.belowThreshold);

                    //add results to RECALL csv
                    summaryWriterRecall.printf("%s,%.2f,%d,%d,%d,%.4f,%.4f,%.4f,%.4f,%.0f,%.0f,%d,%d,%d,%d,%d\n",
                            id, threshold, map_original.size(), map_clusters.size(),
                            resultsRecall.matches, recallScore, resultsRecall.avgOverlapMatches, resultsRecall.avgOverlapTotal,
                            logicRatio[0], logicRatio[1], logicRatio[2],
                            resultsRecall.skippedClassEmpty, resultsRecall.skippedClusterEmpty, resultsRecall.skippedClassThing, resultsRecall.skippedClusterThing, resultsRecall.belowThreshold);


                    counter++;
                    System.out.println("Progress: "+ counter + "/"+ ontIds.size()+"\n\n\n");
                    System.err.flush();
                    System.out.flush();
                    Thread.sleep(10);


                } catch (Exception e) {
                    System.err.println("Error processing ID "+id+": "+ e.getMessage());
                }
            }
        }

//        try {
//            FileWriter usedOnts = new FileWriter("used_Ontology_IDs.txt");
//            for (String id : usedOntIds) {
//                usedOnts.write(id+"\n");
//            }
//            usedOnts.close();
//            System.out.println("Successfully wrote used ontology IDs.");
//        } catch (IOException e) {
//            System.err.println("Error occurred while printing used ontology IDs");
//            Thread.sleep(10);
//            System.err.flush();
//            e.printStackTrace();
//        }


        System.out.println("\n--- Finished processing batch ---\n");








//        //load ontology manager
//        OWLOntologyManager manager = loadManager();
//        OWLReasonerFactory factory = loadReasonerFactory();
//
//        //load original ontology
//        System.out.println("Loading original ontology...");
//        String originalPath = "/Users/daniel/Desktop/BP/Clustering-Results-Koopmann/Zenodo submission/experiment results/results family ontology/family-benchmark_rich_background.owl";
//        OWLOntology originalOntology = loadOntology(originalPath, manager);
//
//        //load clustered ontologies
//        System.out.println("Loading original ontology/ontologies...");
//        String clusteredPath1 = "/Users/daniel/Desktop/BP/Clustering-Results-Koopmann/Zenodo submission/experiment results/results family ontology/clustering-result.owl";
// //        String clusteredPath2 = "/Users/daniel/Desktop/BP/Clustering_results/5769/clustering-result-5769-1-2.owl";
//
//        OWLOntology clusteredOntology1 = loadClusteredOntology(clusteredPath1, originalOntology, manager, factory);
// //        OWLOntology clusteredOntology2 = loadClusteredOntology(clusteredPath2, originalOntology, manager, factory);
//
//        //retrieve individuals
//        System.out.println("Extracting individuals from original ontology...");
//        Map<OWLClass, Set<OWLNamedIndividual>> map_original= findIndividuals(originalOntology, factory);
//        System.out.println("Extracting individuals from clustered ontology...");
//        Map<OWLClass, Set<OWLNamedIndividual>> map1_clusters = findIndividuals(clusteredOntology1, factory);
//
//
//
//        compareOntologies(map_original, map1_clusters, threshold);


        long estimatedTime = System.nanoTime()-startTime;
        long timeSeconds = TimeUnit.NANOSECONDS.toSeconds(estimatedTime);
        long hours = timeSeconds/3600;
        long mins = (timeSeconds%3600)/60;
        long secs = timeSeconds%60;

        System.out.printf("Ran for %d:%d:%d",hours,mins,secs);


        System.out.println("\n\n---FINISHED---");
    }
}