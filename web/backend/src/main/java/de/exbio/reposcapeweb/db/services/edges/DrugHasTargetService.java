package de.exbio.reposcapeweb.db.services.edges;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.exbio.reposcapeweb.db.entities.edges.DrugHasTargetGene;
import de.exbio.reposcapeweb.db.entities.edges.DrugHasTargetProtein;
import de.exbio.reposcapeweb.db.entities.ids.PairId;
import de.exbio.reposcapeweb.db.repositories.edges.DrugHasTargetGeneRepository;
import de.exbio.reposcapeweb.db.repositories.edges.DrugHasTargetProteinRepository;
import de.exbio.reposcapeweb.db.services.nodes.DrugService;
import de.exbio.reposcapeweb.db.services.nodes.ProteinService;
import de.exbio.reposcapeweb.db.updates.UpdateOperation;
import de.exbio.reposcapeweb.utils.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class DrugHasTargetService {
    private final Logger log = LoggerFactory.getLogger(DrugHasTargetService.class);

    private final DrugHasTargetProteinRepository drugHasTargetProteinRepository;
    private final DrugHasTargetGeneRepository drugHasTargetGeneRepository;

    private final DrugService drugService;

    private final ProteinService proteinService;

    private final boolean directed = true;
    private final HashMap<Integer, HashSet<Integer>> proteinEdgesTo = new HashMap<>();
    private final HashMap<Integer, HashSet<Integer>> proteinEdgesFrom = new HashMap<>();
    private final HashMap<Integer, HashSet<Integer>> geneEdgesTo = new HashMap<>();
    private final HashMap<Integer, HashSet<Integer>> geneEdgesFrom = new HashMap<>();

    private final DataSource dataSource;
    private final String clearQuery = "DELETE FROM drug_has_target_gene";
    private final String generationQuery = "INSERT IGNORE INTO drug_has_target_gene SELECT drug_has_target_protein.id_1, protein_encoded_by.id_2 FROM protein_encoded_by INNER JOIN drug_has_target_protein ON protein_encoded_by.id_1=drug_has_target_protein.id_2;";
    private PreparedStatement clearPs = null;
    private PreparedStatement generationPs = null;

    @Autowired
    public DrugHasTargetService(DrugService drugService, ProteinService proteinService, DrugHasTargetProteinRepository drugHasTargetProteinRepository, DrugHasTargetGeneRepository drugHasTargetGeneRepository, DataSource dataSource) {
        this.drugService = drugService;
        this.drugHasTargetProteinRepository = drugHasTargetProteinRepository;
        this.drugHasTargetGeneRepository = drugHasTargetGeneRepository;
        this.proteinService = proteinService;
        this.dataSource = dataSource;
    }


    public boolean submitUpdates(EnumMap<UpdateOperation, HashMap<PairId, DrugHasTargetProtein>> updates) {

        if (updates == null)
            return false;

        if (updates.containsKey(UpdateOperation.Deletion))
            drugHasTargetProteinRepository.deleteAll(drugHasTargetProteinRepository.findDrugHasTargetsByIdIn(updates.get(UpdateOperation.Deletion).keySet().stream().map(o -> (PairId) o).collect(Collectors.toSet())));

        LinkedList<DrugHasTargetProtein> toSave = new LinkedList(updates.get(UpdateOperation.Insertion).values());
        int insertCount = toSave.size();
        if (updates.containsKey(UpdateOperation.Alteration)) {
            HashMap<PairId, DrugHasTargetProtein> toUpdate = updates.get(UpdateOperation.Alteration);

            drugHasTargetProteinRepository.findDrugHasTargetsByIdIn(new HashSet<>(toUpdate.keySet().stream().map(o -> (PairId) o).collect(Collectors.toSet()))).forEach(d -> {
                d.setValues(toUpdate.get(d.getPrimaryIds()));
                toSave.add(d);
            });
        }
        drugHasTargetProteinRepository.saveAll(toSave);
        log.debug("Updated drug_has_target(_protein) table: " + insertCount + " Inserts, " + (updates.containsKey(UpdateOperation.Alteration) ? updates.get(UpdateOperation.Alteration).size() : 0) + " Changes, " + (updates.containsKey(UpdateOperation.Deletion) ? updates.get(UpdateOperation.Deletion).size() : 0) + " Deletions identified!");
        log.debug("Deriving entries for drug_has_target_gene.");
        if (!generateGeneEntries())
            return false;
        log.debug("Derived " + drugHasTargetGeneRepository.count() + " drug -> gene relations.");
        return true;
    }

    public boolean generateGeneEntries() {
        log.debug("Generating entries for drug_has_target_gene from drug_has_target(_protein).");
        try (Connection con = dataSource.getConnection()) {
            clearPs = con.prepareCall(clearQuery);
            generationPs = con.prepareStatement(generationQuery);
            clearPs.executeUpdate();
            generationPs.executeUpdate();
        } catch (SQLException throwables) {
            throwables.printStackTrace();
            return false;
        }
        return true;
    }


    public Iterable<DrugHasTargetGene> findAllGenes() {
        return drugHasTargetGeneRepository.findAll();
    }

    public void importEdges() {
        findAllGenes().forEach(edge -> {
            importGeneEdge(edge.getPrimaryIds());
        });
        findAllGenes().forEach(edge -> {
            importGeneEdge(edge.getPrimaryIds());
        });

        findAllProteins().forEach(edge -> {
            importProteinEdge(edge.getPrimaryIds());
        });
        findAllProteins().forEach(edge -> {
            importProteinEdge(edge.getPrimaryIds());
        });
    }

    private void importGeneEdge(PairId edge) {
        if (!geneEdgesFrom.containsKey(edge.getId1()))
            geneEdgesFrom.put(edge.getId1(), new HashSet<>());
        geneEdgesFrom.get(edge.getId1()).add(edge.getId2());

        if (!geneEdgesTo.containsKey(edge.getId2()))
            geneEdgesTo.put(edge.getId2(), new HashSet<>());
        geneEdgesTo.get(edge.getId2()).add(edge.getId1());
    }

    public boolean isGeneEdgeFrom(PairId edge) {
        return isGeneEdgeFrom(edge.getId1(), edge.getId2());
    }

    public boolean isGeneEdgeFrom(int id1, int id2) {
        try {
            return geneEdgesFrom.get(id1).contains(id2);
        } catch (NullPointerException e) {
            return false;
        }
    }

    public HashSet<Integer> getGeneEdgesTo(int id) {
        return geneEdgesTo.get(id);
    }

    public boolean isGeneEdgeTo(int id1, int id2) {
        try {
            return geneEdgesTo.get(id1).contains(id2);
        } catch (NullPointerException e) {
            return false;
        }
    }

    public HashSet<Integer> getGeneEdgesFrom(int id) {
        return geneEdgesTo.get(id);
    }


    public Iterable<DrugHasTargetProtein> findAllProteins() {
        return drugHasTargetProteinRepository.findAll();
    }

    private void importProteinEdge(PairId edge) {
        if (!proteinEdgesFrom.containsKey(edge.getId1()))
            proteinEdgesFrom.put(edge.getId1(), new HashSet<>());
        proteinEdgesFrom.get(edge.getId1()).add(edge.getId2());

        if (!proteinEdgesTo.containsKey(edge.getId2()))
            proteinEdgesTo.put(edge.getId2(), new HashSet<>());
        proteinEdgesTo.get(edge.getId2()).add(edge.getId1());
    }


    public boolean isProteinEdgeFrom(PairId edge) {
        return isProteinEdgeFrom(edge.getId1(), edge.getId2());
    }

    public boolean isProteinEdgeFrom(int id1, int id2) {
        try {
            return proteinEdgesFrom.get(id1).contains(id2);
        } catch (NullPointerException e) {
            return false;
        }
    }

    public HashSet<Integer> getProteinEdgesTo(int id) {
        return proteinEdgesTo.get(id);
    }

    public boolean isProteinEdgeTo(int id1, int id2) {
        try {
            return proteinEdgesTo.get(id1).contains(id2);
        } catch (NullPointerException e) {
            return false;
        }
    }

    public HashSet<Integer> getProteinEdgesFrom(int id) {
        return proteinEdgesTo.get(id);
    }


    public PairId mapIds(Pair<String, String> ids) {
        return new PairId(drugService.map(ids.getFirst()), proteinService.map(ids.getSecond()));
    }

    public Iterable<DrugHasTargetGene> getGenes(Collection<PairId> ids) {
        return drugHasTargetGeneRepository.findDrugHasTargetsByIdIn(ids);
    }


    public Iterable<DrugHasTargetProtein> getProteins(Collection<PairId> ids) {
        return drugHasTargetProteinRepository.findDrugHasTargetsByIdIn(ids);
    }
}
