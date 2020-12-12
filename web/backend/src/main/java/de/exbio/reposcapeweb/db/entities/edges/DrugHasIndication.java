package de.exbio.reposcapeweb.db.entities.edges;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSetter;
import de.exbio.reposcapeweb.db.entities.RepoTrialEdge;
import de.exbio.reposcapeweb.db.entities.ids.PairId;
import de.exbio.reposcapeweb.utils.Pair;
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


/**
 * Entity class for the RepoTrial edge "DrugHasIndication" (drug_has_indication in db).
 * The attributes primaryDomainIds of {@link DrugHasIndication#sourceDomainId} and {@link DrugHasIndication#targetDomainId} contain the ids of the {@link de.exbio.reposcapeweb.db.entities.nodes.Drug} (source) having an indication to be effective against a {@link de.exbio.reposcapeweb.db.entities.nodes.Disorder} (target).
 * These primaryDomainIds from RepoTrial are converted to numeric node ids autogenerated on insertion of the nodes into the db.
 * Further the "type" attribute is also not included in the database, having only one possible value.
 * The class extends the {@link RepoTrialEdge} abstract class, for some methods used during import of the different RepoTrial entities.
 *
 * @author Andreas Maier
 */
@Entity
@Table(name = "drug_has_indication")
public class DrugHasIndication extends RepoTrialEdge implements Serializable {

    @Transient
    @JsonIgnore
    private final Logger log = LoggerFactory.getLogger(DrugHasIndication.class);

    @JsonIgnore
    @EmbeddedId
    private PairId id;

    @Transient
    @JsonIgnore
    public final static HashSet<String> attributes = new HashSet<>(Arrays.asList( "sourceDomainId","targetDomainId","type"));

    @Transient
    @JsonIgnore
    public final static String[] allAttributes = new String[]{"id","sourceId","targetId","node1","node2","sourceDomainId","targetDomainId","type"};

    @Transient
    @JsonIgnore
    public final static String[] allAttributeTypes = new String[]{"numeric","numeric","numeric","","","","",""};

    @Transient
    @JsonIgnore
    public final static Boolean[] idAttributes = new Boolean[]{true, true, true, false,false,true, true, false};



    @Transient
    private String targetDomainId;
    @Transient
    private String sourceDomainId;

    @Transient
    private String nodeOne;
    @Transient
    private String nodeTwo;

    public DrugHasIndication() {
    }

    public String getTargetDomainId() {
        return targetDomainId;
    }

    public String getSourceDomainId() {
        return sourceDomainId;
    }

    @JsonGetter
    public String getType() {
        return "DrugHasIndication";
    }

    @JsonSetter
    public void setType(String type) {
    }

    public static String[] getListAttributes() {
        return new String[]{ "id","node1","node2"};
    }

    @Override
    public HashMap<String, Object> getAsMap() {
        HashMap<String,Object> values = new HashMap<>();
        values.put("targetDomainId",getTargetDomainId());
        values.put("sourceDomainId",getSourceDomainId());
        values.put("sourceId",id.getId1());
        values.put("targetId",id.getId2());
        values.put("node1",nodeOne);
        values.put("node2",nodeTwo);
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


    public void setValues(DrugHasIndication other) {
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
