package vu.daniel;

import org.semanticweb.elk.owlapi.ElkReasonerFactory;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.reasoner.NodeSet;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.semanticweb.owlapi.reasoner.OWLReasonerFactory;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.*;
import java.util.stream.Collectors;

public class Methods {


    static OWLOntologyManager loadManager() { return OWLManager.createOWLOntologyManager(); }

    static OWLReasonerFactory loadReasonerFactory() { return new ElkReasonerFactory(); }

    static OWLOntology loadOntology(String fileIn, OWLOntologyManager manager) throws OWLOntologyCreationException {
        File file = new File(fileIn);
        return manager.loadOntologyFromOntologyDocument(file);
    }

//    static OWLOntology loadClusteredOntology(
//            String fileIn,
//            OWLOntology originalOntology,
//            OWLOntologyManager manager,
//            OWLReasonerFactory reasonerFactory) throws OWLOntologyCreationException {
//
//        File file = new File(fileIn);
//        OWLOntology clusteredOntology = manager.loadOntologyFromOntologyDocument(file);
//
//        //create an empty ontology to put "thinned" clusters
//        OWLOntology filteredClusteredOntology = manager.createOntology();
//        OWLDataFactory dataFactory = manager.getOWLDataFactory();
//
//
//        OWLReasoner reasoner = reasonerFactory.createReasoner(clusteredOntology);
//
//        //get classes of original ontology
//        Set<OWLClass> originalClasses = originalOntology.classesInSignature().collect(Collectors.toSet());
//
//        //retrieve just the clusters
//        Set<OWLClass> newClusters = clusteredOntology.classesInSignature()
//                .filter(owlClass -> !originalClasses.contains(owlClass))
//                .collect(Collectors.toSet());
//
//        //adds tbox axioms
//        clusteredOntology.logicalAxioms().forEach(axiom -> {
//            if (axiom.classesInSignature().anyMatch(newClusters::contains)) {
//                manager.addAxiom(filteredClusteredOntology, axiom);
//            }
//        });
//
//        //filter classes and put into new ontology
//        clusteredOntology.classesInSignature()
//                .filter(owlClass -> !originalClasses.contains(owlClass))
//                .forEach(cluster -> {
//                        //cluster class definition
//                        manager.addAxiom(filteredClusteredOntology, dataFactory.getOWLDeclarationAxiom(cluster));
//
//                        //retrieve all members
//                        NodeSet<OWLNamedIndividual> instances = reasoner.getInstances(cluster, false);
//
//                        //abox and declaration axioms
//                        instances.entities().forEach(individual -> {
//                            manager.addAxiom(filteredClusteredOntology, dataFactory.getOWLDeclarationAxiom(individual));
//                            OWLClassAssertionAxiom assertion = dataFactory.getOWLClassAssertionAxiom(cluster, individual);
//                            manager.addAxiom(filteredClusteredOntology, assertion);
//                        });
//                    });
//
//        reasoner.dispose();
//
//        return filteredClusteredOntology;
//    }



    static Map<OWLClass, Set<OWLNamedIndividual>> findIndividuals(
            OWLOntology ontology,
            Set<OWLClass> targetClasses,
            OWLReasonerFactory reasonerFactory) {

        // create reasoner
        OWLReasoner reasoner = reasonerFactory.createReasoner(ontology);

        Map<OWLClass, Set<OWLNamedIndividual>> clusterDict = new HashMap<>();

        System.out.println("Nr of clusters: " + targetClasses.size());

        //fetch each class in a stream, iterate over stream
        targetClasses.forEach(cluster -> {

            //retrieve all members of the class, inferred and asserted
            Set<OWLNamedIndividual> individualsSet = (reasoner.getInstances(cluster, false)).entities().collect(Collectors.toSet());

            //add to map
            clusterDict.put(cluster, individualsSet);
        });
        reasoner.dispose();
        return clusterDict;
    }


    /**
     * Calculate the Jaccard index (intersection over union)
     * @param individuals1 first set of individuals
     * @param individuals2 second set of individuals
     * @return Jaccard index
     */
    static double calculateOverlap(Set<OWLNamedIndividual> individuals1, Set<OWLNamedIndividual> individuals2) {
        if (individuals1.isEmpty() && individuals2.isEmpty()) {
            return -1.0; // if both sets are empty, flag as -1
        }
        if (individuals1.isEmpty() ^ individuals2.isEmpty()) {
            return 0.0;
        }

        Set<OWLNamedIndividual> intersection = new HashSet<>(individuals1);
        intersection.retainAll(individuals2);

        int union = individuals1.size() + individuals2.size() - intersection.size();

        return (double) intersection.size() / union;
    }

    static ComparisonResults compareOntologies(
            String ontID,
            Map<OWLClass, Set<OWLNamedIndividual>> map_original,
            Map<OWLClass, Set<OWLNamedIndividual>> map_clustered,
            double threshold,
            PrintWriter detailsWriter) {

        System.out.println("\n --- Matching clusters w/ threshold >= " + threshold+" ---\n");

        //instantiate counters for various conditions
        int matchCount = 0;
        int skippedClassEmpty = 0;
        int skippedClusterEmpty = 0;
        int belowThreshold = 0;
        int skippedClassThing = 0;
        int skippedClusterThing = 0;

        ArrayList<Double> overlapValuesBestOverlapTotal = new ArrayList<>();
        ArrayList<Double> overlapValuesBestMatches = new ArrayList<>();


        //loop through each class of the ontology
        for (Map.Entry<OWLClass, Set<OWLNamedIndividual>> entry1 : map_original.entrySet()) {
            OWLClass c1 = entry1.getKey();
            Set<OWLNamedIndividual> individuals1 = entry1.getValue();


            //if the class of the og ont is empty, skip to the next one, count it
            if (individuals1.isEmpty()){ skippedClassEmpty++; continue; }
            //if the class of the og ont is the Thing class, skip, count it
            if (Objects.equals(c1.getIRI().getShortForm(),"Thing")) { skippedClassThing++; continue; }

            //for remembering best cluster match for class
            double bestOverlap = 0;
            OWLClass bestClusterMatch = null;

            //loop over each cluster
            for (Map.Entry<OWLClass,Set<OWLNamedIndividual>> entry2 : map_clustered.entrySet()) {
                OWLClass c2 = entry2.getKey();
                Set<OWLNamedIndividual> individuals2 = entry2.getValue();

                //if the class of the clustered ont is empty, skip to the next one, count it
                if (individuals2.isEmpty()) { skippedClusterEmpty++; continue; }
                //if the class of the clustered ont is the Thing class, skip, count it
                if (Objects.equals(c2.getIRI().getShortForm(),"Thing")) { skippedClusterThing++; continue; }

                //calculate current overlap between the (inferred + asserted) individuals of the class and cluster
                double currentOverlap = calculateOverlap(individuals1, individuals2);

                //if the Current overlap is larger than the Best overlap so far
                if (currentOverlap > bestOverlap) {
                    bestOverlap = currentOverlap;
                    if(bestClusterMatch==null){
                        System.out.println("\u001B[42mbetter match found FLAG"+"\u001B[0m"+" Current: NULL ---> "+"Better: "+ c2.getIRI().getShortForm());
                        bestClusterMatch = c2;
                    } else {
                        System.out.println("\u001B[42mbetter match found FLAG"+"\u001B[0m"+" Current: "+ bestClusterMatch.getIRI().getShortForm()+" ---> "+"Better: "+ c2.getIRI().getShortForm());
                        bestClusterMatch = c2;
                    }
                }
            }

            //check if the BEST cluster can be counted as a match
            if (bestOverlap >= threshold) {
                System.out.printf("Overlap: %.2f \n %s (Ont1) <---> %s (Ont2)\n",
                        bestOverlap, c1.getIRI().getShortForm(), bestClusterMatch.getIRI().getShortForm());
                matchCount++;

                overlapValuesBestMatches.add(bestOverlap);

                //write to detailed CSV
                detailsWriter.printf("%s,%s,%s,%.4f\n",
                        ontID,c1.getIRI().getShortForm(),bestClusterMatch.getIRI().getShortForm(),bestOverlap);

            } else if (bestOverlap < threshold) {
                belowThreshold++;
            }
            overlapValuesBestOverlapTotal.add(bestOverlap);
        }


        if (matchCount > 0) {
            System.out.println("Total matches found: " + matchCount);
        } else {
            System.out.println("No matches found above the threshold.");
        }

        System.out.println("Skipped (empty class): " + skippedClassEmpty+"\n");
        System.out.println("Skipped (empty cluster): " + skippedClusterEmpty+"\n");
        System.out.println("Skipped (below threshold): "+ belowThreshold+"\n");
        System.out.println("Skipped (Thing class): "+ skippedClassThing+"\n");
        System.out.println("Skipped (Thing cluster): "+ skippedClusterThing+"\n");

        double avgOverlapMatches = calculateAvgOverlap(overlapValuesBestMatches);
        System.out.printf("Average Overlap value (matches): %.4f\n", avgOverlapMatches);
        double avgOverlapTotal = calculateAvgOverlap(overlapValuesBestOverlapTotal);
        System.out.printf("Average Overlap value (total): %.4f\n", avgOverlapTotal);


        //return object to add to Summary CSV
        return new ComparisonResults(matchCount,belowThreshold,skippedClassEmpty,skippedClusterEmpty,skippedClassThing,skippedClusterThing,avgOverlapMatches,avgOverlapTotal);
    }


    static double calculateAvgOverlap(ArrayList<Double> overlapValues) {
        double total = 0;
        for (double value : overlapValues) {
            total = total + value;
        }
        return total/overlapValues.size();
    }



    //return: double array [ratio of clustered|=original, ratio of original|=clustered]
    static double calculateLogicalEntailment(OWLOntology originalOntology, OWLOntology clusteredOntology, OWLReasonerFactory factory) throws InterruptedException {
        System.out.println("\n --- Calculating logical equivalence ---\n");


        // checking entailment clustered |= original
        System.out.println("Checking clustered |= original...");
        OWLReasoner clusteredReasoner = factory.createReasoner(clusteredOntology);
        if(!clusteredReasoner.isConsistent()) {
            System.err.println("Clustered ontology is inconsistent. Skipping...");
            clusteredReasoner.dispose();
            return Double.NaN;
        }
        System.err.flush();
        System.out.flush();
        Thread.sleep(2);

        int originalAxiomsTotal = 0;
        int originalAxiomsEntailed = 0;

        List<OWLLogicalAxiom> originalAxioms = originalOntology.logicalAxioms().toList();
        for (OWLLogicalAxiom axiom : originalAxioms) {
            originalAxiomsTotal++;
            try {
                if(clusteredReasoner.isEntailed(axiom)) {
                    originalAxiomsEntailed++;
                }
            } catch (Exception e) {
                //ignore what ELK doesn't support
            }
        }
        clusteredReasoner.dispose();




//        //checking entailment original |= clustered
//        System.out.println("Checking original |= clustered...");
//        OWLReasoner originalReasoner = factory.createReasoner(originalOntology);
//        if(!originalReasoner.isConsistent()) {
//            System.err.println("Original ontology is inconsistent. Skipping...");
//
//            if (originalAxiomsTotal==0){
//                //-1.0 means no axioms
//                return new double[]{0, Double.NaN};
//            } else {
//                return new double[]{(double) originalAxiomsEntailed/originalAxiomsTotal, Double.NaN};
//            }
//        }

//        int clusteredAxiomsTotal = 0;
//        int clusteredAxiomsEntailed = 0;
//
//        Set<OWLEntity> originalSignature = originalOntology.signature().collect(Collectors.toSet());
//
//        List<OWLLogicalAxiom> clusteredAxioms = clusteredOntology.logicalAxioms().toList();
//
//        for (OWLLogicalAxiom axiom: clusteredAxioms) {
//            Set<OWLEntity> axiomSignature = axiom.signature().collect(Collectors.toSet());
//
//            try {
//                if(originalSignature.contains(axiomSignature)) {
//                    clusteredAxiomsTotal++;
//                    if(originalReasoner.isEntailed(axiom)) {
//                        clusteredAxiomsEntailed++;
//                    }
//                }
//            } catch (Exception e) {
//                //ignore unsupported
//            }
//        }
//
//        originalReasoner.dispose();




        double originalByClustered=0;

        if (originalAxiomsTotal!=0) {
            originalByClustered = (double) originalAxiomsEntailed/originalAxiomsTotal;
        }



//        if (clusteredAxiomsTotal==0) {
//            clusteredByOriginal = -1;
//        } else {
//            clusteredByOriginal = (double) clusteredAxiomsEntailed/clusteredAxiomsTotal;
//        }

        System.out.printf("\nOriginal axioms preservered in Clustered ontology: %d/%d (%.2f%%)\n", originalAxiomsEntailed,originalAxiomsTotal, originalByClustered*100);
//        System.out.printf("Clustered axioms in Original ontology: %d/%d (%.2f%%)\n", clusteredAxiomsEntailed,clusteredAxiomsTotal, clusteredByOriginal*100);

        return originalByClustered;

    }




    static ArrayList<String> extractOntIDs(String fileIn) {
        File file = new File(fileIn);

        ArrayList<String> ontIDs = new ArrayList<>();

        try(Scanner reader = new Scanner(file)) {
            while(reader.hasNext()) {
                ontIDs.add(reader.nextLine());
            }
        } catch (FileNotFoundException e) {
            System.err.println("File not found.");
            e.printStackTrace();
        }

        return ontIDs;
    }

}
