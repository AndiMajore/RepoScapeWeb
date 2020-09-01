package de.exbio.reposcapeweb.db.entities.edges;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSetter;
import de.exbio.reposcapeweb.db.entities.ids.PairId;
import de.exbio.reposcapeweb.db.entities.nodes.RepoTrialEdge;
import de.exbio.reposcapeweb.utils.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.Transient;
import java.io.Serializable;
import java.util.Arrays;
import java.util.HashSet;

@Entity
@Table(name="protein_encoded_by")
public class ProteinEncodedBy extends RepoTrialEdge implements Serializable {
    @Transient
    @JsonIgnore
    private final Logger log = LoggerFactory.getLogger(ProteinEncodedBy.class);

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

    public ProteinEncodedBy() {
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


    public void setValues(ProteinEncodedBy other) {
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
}