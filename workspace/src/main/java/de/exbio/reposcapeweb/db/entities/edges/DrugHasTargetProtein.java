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
import java.util.HashSet;
import java.util.List;


/**
 * Entity class for a transitive edge "DrugHasTargetProtein" (drug_has_target_protein in db).
 * <i>These entries are derived by combining {@link DrugHasTargetGene} and {@link ProteinEncodedBy} for giving the opportunity for better data discovery.</i>
 * The attributes primaryDomainIds of {@link DrugHasTargetProtein#sourceDomainId} and {@link DrugHasTargetProtein#targetDomainId} contain the ids of the {@link de.exbio.reposcapeweb.db.entities.nodes.Drug} (source) potentially having an effect on a {@link de.exbio.reposcapeweb.db.entities.nodes.Protein} (target).
 * These primaryDomainIds from RepoTrial are converted to numeric node ids autogenerated on insertion of the nodes into the db.
 * Further the "type" attribute is also not included in the database, having only one possible value.
 * The class extends the {@link RepoTrialEdge} abstract class, for some methods used during import of the different RepoTrial entities.
 *
 * @author Andreas Maier
 */
@Entity
@Table(name = "drug_has_target_protein")
public class DrugHasTargetProtein extends RepoTrialEdge implements Serializable {

    @Transient
    @JsonIgnore
    private final Logger log = LoggerFactory.getLogger(DrugHasTargetProtein.class);

    @JsonIgnore
    @EmbeddedId
    private PairId id;

    @Transient
    @JsonIgnore
    public final static HashSet<String> attributes = new HashSet<>(Arrays.asList("targetDomainId", "type", "sourceDomainId", "actions", "databases"));

    @Transient
    private String targetDomainId;
    @Transient
    private String sourceDomainId;

    private String actions;

    private String sourceDatabases;

    public DrugHasTargetProtein() {
    }

    public String getTargetDomainId() {
        return targetDomainId;
    }

    public String getSourceDomainId() {
        return sourceDomainId;
    }

    @JsonGetter
    public String getType() {
        return "DrugHasTarget";
    }

    @JsonSetter
    public void setType(String type) {
    }

    public List<String> getActions() {
        return StringUtils.stringToList(actions);
    }

    public void setActions(List<String> actions) {
        this.actions = StringUtils.listToString(actions);
    }

    public List<String> getDatabases() {
        return StringUtils.stringToList(sourceDatabases);
    }

    public void setDatabases(List<String> databases) {
        this.sourceDatabases = StringUtils.listToString(databases);
    }

    public void setValues(DrugHasTargetProtein other) {
        this.sourceDomainId = other.sourceDomainId;
        this.targetDomainId = other.targetDomainId;
        this.actions = other.actions;
        this.sourceDatabases = other.sourceDatabases;
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
