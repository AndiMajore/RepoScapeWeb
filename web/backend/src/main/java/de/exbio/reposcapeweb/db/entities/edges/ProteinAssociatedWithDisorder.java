package de.exbio.reposcapeweb.db.entities.edges;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSetter;
import de.exbio.reposcapeweb.db.entities.RepoTrialEdge;
import de.exbio.reposcapeweb.db.entities.ids.PairId;
import de.exbio.reposcapeweb.utils.Pair;
import de.exbio.reposcapeweb.utils.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.Transient;
import java.io.Serializable;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;


/**
 * Entity class for the RepoTrial edge "ProteinAssociatedWithDisorder" (protein_associated_with_disorder in db).
 * The attributes primaryDomainIds of {@link ProteinAssociatedWithDisorder#sourceDomainId} and {@link ProteinAssociatedWithDisorder#targetDomainId} contain the ids of the {@link de.exbio.reposcapeweb.db.entities.nodes.Protein} (source) and a identified association with a {@link de.exbio.reposcapeweb.db.entities.nodes.Disorder} (target).
 * These primaryDomainIds from RepoTrial are converted to numeric node ids autogenerated on insertion of the nodes into the db.
 * Further the "type" attribute is also not included in the database, having only one possible value.
 * The class extends the {@link RepoTrialEdge} abstract class, for some methods used during import of the different RepoTrial entities.
 *
 * @author Andreas Maier
 */
@Entity
@Table(name = "protein_associated_with_disorder")
public class ProteinAssociatedWithDisorder extends RepoTrialEdge implements Serializable {

    @Transient
    @JsonIgnore
    private final Logger log = LoggerFactory.getLogger(ProteinAssociatedWithDisorder.class);

    @JsonIgnore
    @EmbeddedId
    private PairId id;


    @Transient
    private String targetDomainId;
    @Transient
    private String sourceDomainId;
    @Transient
    private String nodeOne;
    @Transient
    private String nodeTwo;


    private String score;

    private String assertedBy;

    public static String[] getListAttributes() {
        return new String[]{"id","node1","node2","score", "assertedBy"};
    }

    @Transient
    @JsonIgnore
    public final static String[] allAttributes = new String[]{"id","sourceId","targetId","node1","node2", "sourceDomainId","targetDomainId", "score", "assertedBy","type"};

    @Transient
    @JsonIgnore
    public final static String[] allAttributeTypes = new String[]{"numeric","numeric","numeric","","", "","", "numeric", "array",""};


    @Transient
    @JsonIgnore
    public final static Boolean[] idAttributes = new Boolean[]{true, true, true,false,false, true, true, false, false, false};

    @Override
    public HashMap<String, Object> getAsMap() {
        HashMap<String,Object> values = new HashMap<>();
        values.put("targetDomainId",targetDomainId);
        values.put("sourceDomainId",sourceDomainId);
        values.put("sourceId",id.getId1());
        values.put("targetId",id.getId2());
        values.put("node1",nodeOne);
        values.put("node2",nodeTwo);
        values.put("score",score);
        values.put("assertedBy",getAssertedBy());
        values.put("type",getType());
        values.put("id",id.getId1()+"-"+id.getId2());
        return values;
    }

    public void setNodeNames(String node1, String node2){
        nodeOne=node1;
        nodeTwo=node2;
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

    public ProteinAssociatedWithDisorder() {
    }

    public String getTargetDomainId() {
        return targetDomainId;
    }

    public String getSourceDomainId() {
        return sourceDomainId;
    }

    @JsonGetter
    public String getType() {
        return "GeneAssociatedWithDisorder";
    }

    public String getScore() {
        return score;
    }

    @JsonSetter
    public void setType(String type) {
    }

    public List<String> getAssertedBy() {
        return StringUtils.stringToList(assertedBy);
    }

    public void setAssertedBy(List<String> assertedBy) {
        this.assertedBy = StringUtils.listToString(assertedBy);
    }

    public void setValues(ProteinAssociatedWithDisorder other) {
        this.sourceDomainId = other.sourceDomainId;
        this.targetDomainId = other.targetDomainId;
        this.assertedBy = other.assertedBy;
        this.score = other.score;
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
