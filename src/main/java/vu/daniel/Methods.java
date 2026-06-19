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

    static OWLOntology loadClusteredOntology(
            String fileIn,
            OWLOntology originalOntology,
            OWLOntologyManager manager,
            OWLReasonerFactory reasonerFactory) throws OWLOntologyCreationException {

        File file = new File(fileIn);
        OWLOntology clusteredOntology = manager.loadOntologyFromOntologyDocument(file);

        //create an empty ontology to put "thinned" clusters
        OWLOntology filteredClusteredOntology = manager.createOntology();
        OWLDataFactory dataFactory = manager.getOWLDataFactory();


        OWLReasoner reasoner = reasonerFactory.createReasoner(clusteredOntology);

        //get classes of original ontology
        Set<OWLClass> originalClasses = originalOntology.classesInSignature().collect(Collectors.toSet());

        //filter classes and put into new ontology
        clusteredOntology.classesInSignature()
                .filter(owlClass -> !originalClasses.contains(owlClass))
                .forEach(cluster -> {
                    //cluster class definition
                    manager.addAxiom(filteredClusteredOntology, dataFactory.getOWLDeclarationAxiom(cluster));

                    //retrieve all members
                    NodeSet<OWLNamedIndividual> instances = reasoner.getInstances(cluster, false);

                    instances.entities().forEach(individual -> {
                        //declare individual and add class assertion
                        manager.addAxiom(filteredClusteredOntology, dataFactory.getOWLDeclarationAxiom(individual));
                        OWLClassAssertionAxiom assertion = dataFactory.getOWLClassAssertionAxiom(cluster, individual);
                        manager.addAxiom(filteredClusteredOntology, assertion);
                    });
                });

        reasoner.dispose();

        return filteredClusteredOntology;
    }



    static Map<OWLClass, Set<OWLNamedIndividual>> findIndividuals(
            OWLOntology ontology,
            OWLReasonerFactory reasonerFactory) {

        // create reasoner
        OWLReasoner reasoner = reasonerFactory.createReasoner(ontology);

        Map<OWLClass, Set<OWLNamedIndividual>> clusterDict = new HashMap<>();

        int nr_cluster = ontology.classesInSignature().toList().size();
        System.out.println("Nr of clusters: " + nr_cluster);

        //fetch each class in a stream, iterate over stream
        ontology.classesInSignature().forEach(cluster -> {

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
        int skippedCount = 0;
        int belowThreshold = 0;
        int skippedThing = 0;

        ArrayList<Double> overlapValuesTotal = new ArrayList<>();
        ArrayList<Double> overlapValuesMatches = new ArrayList<>();


        //loop through each class of the ontology
        for (Map.Entry<OWLClass, Set<OWLNamedIndividual>> entry1 : map_original.entrySet()) {
            OWLClass c1 = entry1.getKey();
            Set<OWLNamedIndividual> individuals1 = entry1.getValue();

            if (Objects.equals(c1.getIRI().getShortForm(), "Thing")) {
                skippedThing++;
                continue;
            }
            //if the class of the og ont is empty, skip to the next one, count it
            //if the class of the og ont is the Thing class, skip, count it

            //for remembering best cluster match for class
            //loop over each cluster
            for (Map.Entry<OWLClass,Set<OWLNamedIndividual>> entry2 : map_clustered.entrySet()) {
                OWLClass c2 = entry2.getKey();
                Set<OWLNamedIndividual> individuals2 = entry2.getValue();

                if (Objects.equals(c2.getIRI().getShortForm(), "Thing")) {
                    skippedThing++;
                    continue;
                }
                //if the class of the clustered ont is empty, skip to the next one, count it
                //if the class of the clustered ont is the Thing class, skip, count it

                double overlap = calculateOverlap(individuals1, individuals2);
                //calculate current overlap between the (inferred + asserted) individuals of the class and cluster

                overlapValuesTotal.add(overlap);
                //if the Current overlap is larger than the Best overlap so far

                if (overlap >= threshold) {
                    System.out.printf("Overlap: %.2f \n %s (Ont1) <---> %s (Ont2)\n",
                            overlap, c1.getIRI().getShortForm(), c2.getIRI().getShortForm());
                    matchCount++;
            //check if the BEST cluster can be counted as a match

                    overlapValuesMatches.add(overlap);

                    //write to detailed CSV
                    detailsWriter.printf("%s,%s,%s,%.4f\n",
                            ontID,c1.getIRI().getShortForm(),c2.getIRI().getShortForm(),overlap);

                } else if (overlap == -1.0) {
                    skippedCount++;
                } else if (overlap < threshold) {
                    belowThreshold++;
                }
            }
        }


        if (matchCount > 0) {
            System.out.println("Total matches found: " + matchCount);
        } else {
            System.out.println("No matches found above the threshold.");
        }

        System.out.println("Skipped (empty): " + skippedCount);
        System.out.println("Skipped (below threshold): "+ belowThreshold+"\n");
        System.out.println("Skipped (Thing class): "+ skippedThing+"\n");

        double avgOverlapMatches = calculateAvgOverlap(overlapValuesMatches);
        System.out.printf("Average Overlap value (matches): %.4f\n", avgOverlapMatches);
        double avgOverlapTotal = calculateAvgOverlap(overlapValuesTotal);
        System.out.printf("Average Overlap value (total): %.4f\n", avgOverlapTotal);



        //return object to add to Summary CSV
        return new Main.ComparisonResults(matchCount,skippedCount,belowThreshold,skippedThing,avgOverlapMatches,avgOverlapTotal);
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

    static double calculateAvgOverlap(ArrayList<Double> overlapValues) {
        double total = 0;
        for (double value : overlapValues) {
            total = total + value;
        }
        return total/overlapValues.size();
    }
}
