package de.exbio.reposcapeweb.db.entities.edges;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSetter;
import de.exbio.reposcapeweb.db.entities.ids.PairId;
import de.exbio.reposcapeweb.db.entities.RepoTrialEdge;
import de.exbio.reposcapeweb.utils.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.*;
import java.io.Serializable;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;


/**
 * Entity class for the RepoTrial edge "ProteinInPathway" (protein_in_pathway in db).
 * The attributes primaryDomainIds of {@link ProteinInPathway#sourceDomainId} and {@link ProteinInPathway#targetDomainId} contain the ids of the {@link de.exbio.reposcapeweb.db.entities.nodes.Protein} (source) and a {@link de.exbio.reposcapeweb.db.entities.nodes.Pathway} it plays a role in (target).
 * These primaryDomainIds from RepoTrial are converted to numeric node ids autogenerated on insertion of the nodes into the db.
 * Further the "type" attribute is also not included in the database, having only one possible value.
 * The class extends the {@link RepoTrialEdge} abstract class, for some methods used during import of the different RepoTrial entities.
 *
 * @author Andreas Maier
 */
@Entity
@Table(name="protein_in_pathway")
public class ProteinInPathway extends RepoTrialEdge implements Serializable {
    @Transient
    @JsonIgnore
    private final Logger log = LoggerFactory.getLogger(ProteinInPathway.class);

    @JsonIgnore
    @EmbeddedId
    private PairId id;

    @Transient
    @JsonIgnore
    public final static HashSet<String> attributes = new HashSet<>(Arrays.asList("targetDomainId", "type", "sourceDomainId"));

    @Transient
    private String targetDomainId;
    @Transient
    private String sourceDomainId;

    public ProteinInPathway() {
    }

    public static String[] getListAttributes() {
        return new String[]{ "sourceId","targetId", "type"};
    }

    @Override
    public HashMap<String, Object> getAsMap() {
        HashMap<String,Object> values = new HashMap<>();
        values.put("targetDomainId",getTargetDomainId());
        values.put("sourceDomainId",getSourceDomainId());
        values.put("sourceId",id.getId1());
        values.put("targetId",id.getId2());
        values.put("type",getType());
        return values;
    }

    @Override
    public HashMap<String, Object> getAsMap(HashSet<String> attributes) {
        HashMap<String,Object> values = new HashMap<>();
        getAsMap().forEach((k,v)->{
            if(attributes.contains(k))
                values.put(k,v);
        });
        return values;
    }

    public String getTargetDomainId() {
        return targetDomainId;
    }

    public String getSourceDomainId() {
        return sourceDomainId;
    }

    @JsonGetter
    public String getType() {
        return "ProteinEncodedBy";
    }

    @JsonSetter
    public void setType(String type) {
    }


    public void setValues(ProteinInPathway other) {
        this.sourceDomainId = other.sourceDomainId;
        this.targetDomainId = other.targetDomainId;
    }

    @Override
    public PairId getPrimaryIds() {
        return id;
    }

    public void setId(PairId id) {
        this.id = id;
    }

    @Override
    public Pair<String, String> getIdsToMap() {
        return new Pair<>(sourceDomainId, targetDomainId);
    }

    public void setTargetDomainId(String targetDomainId) {
        this.targetDomainId = targetDomainId;
    }

    public void setSourceDomainId(String sourceDomainId) {
        this.sourceDomainId = sourceDomainId;
    }
}
