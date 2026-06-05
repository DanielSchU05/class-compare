package vu.daniel;

import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.reasoner.OWLReasonerFactory;

import java.util.Map;
import java.util.Set;

public class OldMethods {

// in main:

//    //load ontology manager
//    OWLOntologyManager manager = loadManager();
//    OWLReasonerFactory factory = loadReasonerFactory();
//
//    //load original ontology
//        System.out.println("Loading original ontology...");
//    String originalPath = "/Users/daniel/Desktop/BP/Real_ontologies/ore_ont_5769.owl";
//    OWLOntology originalOntology = loadOntology(originalPath, manager);
//
//    //load clustered ontologies
//        System.out.println("Loading original ontology/ontologies...");
//    String clusteredPath1 = "/Users/daniel/Desktop/BP/Clustering_results/5769/clustering-result-5769-1-1.owl";
// //        String clusteredPath2 = "/Users/daniel/Desktop/BP/Clustering_results/5769/clustering-result-5769-1-2.owl";
//
//    OWLOntology clusteredOntology1 = loadClusteredOntology(clusteredPath1, originalOntology, manager, factory);
// //        OWLOntology clusteredOntology2 = loadClusteredOntology(clusteredPath2, originalOntology, manager, factory);
//
//    //retrieve individuals
//        System.out.println("Extracting individuals from original ontology...");
//    Map<OWLClass, Set<OWLNamedIndividual>> map_original= findIndividuals(originalOntology, factory);
//        System.out.println("Extracting individuals from clustered ontology...");
//    Map<OWLClass, Set<OWLNamedIndividual>> map1_clusters = findIndividuals(clusteredOntology1, factory);
//
//
//
//    compareOntologies(map_original, map1_clusters, threshold);


//        System.out.println("Map OG");
//        //testing
//        for (Map.Entry<OWLClass, Set<OWLNamedIndividual>> entry : map_original.entrySet()) {
//            Set<OWLNamedIndividual> individuals_set = entry.getValue();
//            if (individuals_set.isEmpty()) {
//                continue;
//            }
//            System.out.println("Key: " + entry.getKey().getIRI().getShortForm() + " -- Size: " + individuals_set.size());
//
//        }
//
//        System.out.println("Map 1: ");
//        //testing
//        for (Map.Entry<OWLClass, Set<OWLNamedIndividual>> entry : map1_clusters.entrySet()) {
//            Set<OWLNamedIndividual> individuals_set = entry.getValue();
//            System.out.println("Key: " + entry.getKey().getIRI().getShortForm() + " -- Size: " + individuals_set.size());
//
//
//        }






// findCLusteredIndividuals method (before implementing loadClusteredOntology)

//    static Map<OWLClass, Set<OWLNamedIndividual>> findClusteredIndividuals(
//            OWLOntology clusteredOntology,
//            OWLOntology originalOntology,
//            OWLOntologyManager manager,
//            OWLReasonerFactory reasonerFactory) {
//
//        // create reasoner
//        OWLReasoner reasoner = reasonerFactory.createReasoner(clusteredOntology);
//
//        //original ontology to filter the classes from clustered ontology
//        Set<OWLClass> originalClasses = originalOntology.classesInSignature().collect(Collectors.toSet());
//
//
//        Map<OWLClass, Set<OWLNamedIndividual>> clusterDict = new HashMap<>();
//
//        int nr_clustered_cluster = clusteredOntology.classesInSignature().toList().size();
//        int nr_og_cluster = originalOntology.classesInSignature().toList().size();
//        int difference = nr_clustered_cluster - nr_og_cluster;
//        System.out.println("Nr of clustered clusters: " + difference);
//
//        //fetch each class in a stream, iterate over stream
//        clusteredOntology.classesInSignature()
//                .filter(owlClass -> !originalClasses.contains(owlClass))
//                .forEach(cluster -> {
//
//                //retrieve all members of the class, inferred and asserted
//                NodeSet<OWLNamedIndividual> instances = reasoner.getInstances(cluster, false);
//                //convert stream to set for saving to map
//                Set<OWLNamedIndividual> individualsSet = instances.entities().collect(Collectors.toSet());
//
//                //add to map
//                clusterDict.put(cluster, individualsSet);
//                });
//        reasoner.dispose();
//        return clusterDict;
//    }





//Old file comparison (no individuals, just axioms)

//    static void compareOWLFiles_old() throws OWLOntologyCreationException {
//        //load ont files
//        File file1 = new File("/Users/daniel/Desktop/BP/Clustering_results/clustering-result-12452-3-3.owl"); // user inputs
//        File file2 = new File("/Users/daniel/Desktop/BP/Clustering_results/clustering-result-12452-5-5.owl"); // "/Users/daniel/Desktop/BP/Real_ontologies/ore_ont_12721.owl"
//
//        OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
//
//        //load as ontologies
//        OWLOntology ontology1 = manager.loadOntologyFromOntologyDocument(file1);
//        OWLOntology ontology2 = manager.loadOntologyFromOntologyDocument(file2);
//
//        System.out.println("Ontology 1 ID: " + ontology1.getOntologyID());
//        System.out.println("Ontology 2 ID: " + ontology2.getOntologyID());
//
//        Set<OWLAxiom> axioms1 = ontology1.getAxioms();
//        Set<OWLAxiom> axioms2 = ontology2.getAxioms();
//
//        System.out.println("Ontology 1 # of axioms: " + axioms1.size());
//        System.out.println("Ontology 2 # of axioms: " + axioms2.size());
//
//        //ensure finding axioms unique to ont1 and ont2
//        Set<OWLAxiom> uniqueOnt1 = new java.util.HashSet<>(axioms1);
//        uniqueOnt1.removeAll(axioms2);
//
//        Set<OWLAxiom> uniqueOnt2 = new java.util.HashSet<>(axioms2);
//        uniqueOnt2.removeAll(axioms1);
//
//        Iterator itr1 = axioms1.iterator();
//        Iterator itr2 = axioms2.iterator();
//
//        System.out.println("\n    Ontology 1 unique axioms: " + uniqueOnt1.size());
// //        while (itr1.hasNext()) {
// //            System.out.println(itr1.next());
// //        }
//
//        System.out.println("\n    Ontology 2 unique axioms: " + uniqueOnt2.size());
// //        while (itr2.hasNext()) {
// //            System.out.println(itr1.next());
// //        }
//
//
//        if (uniqueOnt1.isEmpty() && uniqueOnt2.isEmpty()) {
//            System.out.println("Both ontologies contain same axioms");
//        }
//        else {
//            System.out.println("Ontologies have different axioms");
//        }
//
//    }




// old compareOntologies() method without csv export


//    static void compareOntologies(
//            Map<OWLClass, Set<OWLNamedIndividual>> map1,
//            Map<OWLClass, Set<OWLNamedIndividual>> map2,
//            double threshold) {
//
//        System.out.println("\n --- Matching clusters w/ threshold >= " + threshold+" ---");
//
//        int matchCount = 0;
//        int skippedCount = 0;
//        int belowThreshold = 0;
//
//
//        for (Map.Entry<OWLClass, Set<OWLNamedIndividual>> entry1 : map1.entrySet()) {
//            OWLClass c1 = entry1.getKey();
//            Set<OWLNamedIndividual> individuals1 = entry1.getValue();
//
//
//            for (Map.Entry<OWLClass,Set<OWLNamedIndividual>> entry2 : map2.entrySet()) {
//                OWLClass c2 = entry2.getKey();
//                Set<OWLNamedIndividual> individuals2 = entry2.getValue();
//
//
//                double overlap = calculateOverlap(individuals1, individuals2);
//
//
//                //add these stats to writer
//                if (overlap >= threshold) {
//                    System.out.printf("Overlap: %.2f \n %s (Ont1) <---> %s (Ont2)\n",
//                            overlap, c1.getIRI().getShortForm(), c2.getIRI().getShortForm());
//                    matchCount++;
//                } else if (overlap == -1.0) {
//                    skippedCount++;
//                } else if (overlap < threshold) {
//                    belowThreshold++;
//                }
//            }
//        }
//
//
//        if (matchCount > 0) {
//            System.out.println("Total matches found: " + matchCount);
//        } else {
//            System.out.println("No matches found above the threshold.");
//        }
//
//        System.out.println("\nSkipped (empty): " + skippedCount);
//        System.out.println("Skipped (below threshold: "+ belowThreshold);
//
//
//    }





}
