package de.exbio.reposcapeweb.db.services.edges;

import de.exbio.reposcapeweb.db.entities.edges.DrugHasIndication;
import de.exbio.reposcapeweb.db.entities.ids.PairId;
import de.exbio.reposcapeweb.db.repositories.edges.DrugHasIndicationRepository;
import de.exbio.reposcapeweb.db.services.nodes.DisorderService;
import de.exbio.reposcapeweb.db.services.nodes.DrugService;
import de.exbio.reposcapeweb.db.updates.UpdateOperation;
import de.exbio.reposcapeweb.utils.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.stream.Collectors;

@Service
public class DrugHasIndicationService {

    private final Logger log = LoggerFactory.getLogger(DrugHasIndicationService.class);

    private final DrugHasIndicationRepository drugHasIndicationRepository;

    private final DrugService drugService;

    private final DisorderService disorderService;

    @Autowired
    public DrugHasIndicationService(DrugService drugService,  DisorderService disorderService, DrugHasIndicationRepository drugHasIndicationRepository){
        this.drugService=drugService;
        this.drugHasIndicationRepository=drugHasIndicationRepository;
        this.disorderService=disorderService;
    }


    public boolean submitUpdates(EnumMap<UpdateOperation, HashMap<PairId, DrugHasIndication>> updates) {

        if (updates == null)
            return false;

        if (updates.containsKey(UpdateOperation.Deletion))
            drugHasIndicationRepository.deleteAll(drugHasIndicationRepository.findDrugHasIndicationsByIdIn(updates.get(UpdateOperation.Deletion).keySet().stream().map(o->(PairId)o).collect(Collectors.toSet())));

        LinkedList<DrugHasIndication> toSave = new LinkedList(updates.get(UpdateOperation.Insertion).values());
        int insertCount = toSave.size();
        if (updates.containsKey(UpdateOperation.Alteration)) {
            HashMap<PairId, DrugHasIndication> toUpdate = updates.get(UpdateOperation.Alteration);

            drugHasIndicationRepository.findDrugHasIndicationsByIdIn(new HashSet<>(toUpdate.keySet().stream().map(o->(PairId)o).collect(Collectors.toSet()))).forEach(d -> {
                d.setValues(toUpdate.get(d.getPrimaryIds()));
                toSave.add(d);
            });
        }
        drugHasIndicationRepository.saveAll(toSave);
        log.debug("Updated drug_has_indication table: " + insertCount + " Inserts, " + (updates.containsKey(UpdateOperation.Alteration) ? updates.get(UpdateOperation.Alteration).size() : 0) + " Changes, " + (updates.containsKey(UpdateOperation.Deletion) ? updates.get(UpdateOperation.Deletion).size() : 0) + " Deletions identified!");
        return true;
    }


    public PairId mapIds(Pair<String,String> ids) {
        return new PairId(drugService.map(ids.getFirst()),disorderService.map(ids.getSecond()));
    }

}