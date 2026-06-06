package vu.daniel;

import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.reasoner.OWLReasonerFactory;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;

import vu.daniel.Methods.*;
import vu.daniel.PathNames;


public class Main {

    //class for formatting experiment results
    static class ComparisonResults{
        int matches;
        int skipped;
        int belowThreshold;

        ComparisonResults(int matches, int skipped, int belowThreshold) {
            this.matches = matches;
            this.skipped = skipped;
            this.belowThreshold = belowThreshold;
        }
    }

    static void main() throws IOException {
        PathNames paths = new PathNames();

        //IDs of ontologies to be compared
        ArrayList<String> ontIds = Methods.extractOntIDs(paths.getOntologyIDsPath()+"fileorder.txt");


        //set threshold for comparison
        double threshold = 0.5;

        int counter = 0;


        try(PrintWriter summaryWriter = new PrintWriter(new FileWriter("comparison_summary.csv"));
            PrintWriter detailsWriter = new PrintWriter(new FileWriter("comparison_details.csv"))){

            summaryWriter.println("Ontology_ID,Original_Clusters_Count,Clustered_Clusters_Count,Matches_Found,Skipped_Empty,Below_Threshold");
            detailsWriter.println("Ontology_ID,Original_Cluster,Clustered_Cluster,Overlap_Score");




            for(String id: ontIds) {
                try {
                    String originalPath = paths.getOriginalPath() + id;
                    String clusteredPath = paths.getClusteredPath()+id+".clustering.owl";

                    OWLOntologyManager manager = Methods.loadManager();
                    OWLReasonerFactory factory = Methods.loadReasonerFactory();

                    //checking that files are present
                    File origFileCheck = new File(originalPath);
                    File clustFileCheck = new File(clusteredPath);


                    if(!origFileCheck.exists()) {
                        System.err.println("Original ontology not found: " + originalPath);
                        continue;
                    }
                    if (!clustFileCheck.exists()) {
                        System.err.println("Clustered ontology not found: " + clusteredPath);
                        if (origFileCheck.exists()) {
                            System.err.println("Clustering failed (timed out/invalid axioms)");
                            summaryWriter.printf("%s,-,-,-,-,-\n", id);
                            continue;
                        }
                        continue;
                    }

                    System.out.println("\n--- Processing ontology id: "+id+ " ---");


                    //load onts
                    System.out.println("Loading ontologies...");
                    OWLOntology originalOntology = Methods.loadOntology(originalPath, manager);
                    OWLOntology clusteredOntology = Methods.loadClusteredOntology(clusteredPath, originalOntology, manager, factory);


                    //extract individuals
                    System.out.println("Extracting individuals from original ontology...");
                    Map<OWLClass, Set<OWLNamedIndividual>> map_original = Methods.findIndividuals(originalOntology, factory);

                    System.out.println("Extracting individuals from clustered ontology...");
                    Map<OWLClass, Set<OWLNamedIndividual>> map_clusters = Methods.findIndividuals(clusteredOntology, factory);


                    //add to summary csv
                    ComparisonResults results = Methods.compareOntologies(id, map_original, map_clusters, threshold, detailsWriter);

                    summaryWriter.printf("%s,%d,%d,%d,%d,%d\n",
                            id,map_original.size(),map_clusters.size(),results.matches,results.skipped,results.belowThreshold);

                    counter++;
                    System.out.println("Progress: "+ counter + "/"+ ontIds.size());

                } catch (Exception e) {
                    System.err.println("Error processing ID "+id+": "+ e.getMessage());
                }

            }
        }

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



        System.out.println("\n\n---FINISHED---");
    }




}