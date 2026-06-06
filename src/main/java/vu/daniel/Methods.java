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
            NodeSet<OWLNamedIndividual> instances = reasoner.getInstances(cluster, false);
            //convert stream to set for saving to map
            Set<OWLNamedIndividual> individualsSet = instances.entities().collect(Collectors.toSet());

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

        Set<OWLNamedIndividual> intersection = new HashSet<>(individuals1);
        intersection.retainAll(individuals2);

        //|AuB| = |A| + |B| - |A n B|
        int union = (individuals1.size() + individuals2.size()) - intersection.size();

        return (double) intersection.size() / union;
    }

    static Main.ComparisonResults compareOntologies(
            String ontID,
            Map<OWLClass, Set<OWLNamedIndividual>> map_original,
            Map<OWLClass, Set<OWLNamedIndividual>> map_clustered,
            double threshold,
            PrintWriter detailsWriter) {

        System.out.println("\n --- Matching clusters w/ threshold >= " + threshold+" ---\n");

        int matchCount = 0;
        int skippedCount = 0;
        int belowThreshold = 0;


        for (Map.Entry<OWLClass, Set<OWLNamedIndividual>> entry1 : map_original.entrySet()) {
            OWLClass c1 = entry1.getKey();
            Set<OWLNamedIndividual> individuals1 = entry1.getValue();


            for (Map.Entry<OWLClass,Set<OWLNamedIndividual>> entry2 : map_clustered.entrySet()) {
                OWLClass c2 = entry2.getKey();
                Set<OWLNamedIndividual> individuals2 = entry2.getValue();


                double overlap = calculateOverlap(individuals1, individuals2);


                if (overlap >= threshold) {
                    System.out.printf("Overlap: %.2f \n %s (Ont1) <---> %s (Ont2)\n",
                            overlap, c1.getIRI().getShortForm(), c2.getIRI().getShortForm());
                    matchCount++;

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
        System.out.println("Skipped (below threshold: "+ belowThreshold+"\n");


        //return object to add to Summary CSV
        return new Main.ComparisonResults(matchCount,skippedCount,belowThreshold);
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
