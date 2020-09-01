package de.exbio.reposcapeweb.db.entities.edges;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSetter;
import de.exbio.reposcapeweb.db.entities.ids.PairId;
import de.exbio.reposcapeweb.db.entities.nodes.RepoTrialEdge;
import de.exbio.reposcapeweb.utils.Pair;
import de.exbio.reposcapeweb.utils.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.*;
import java.io.Serializable;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

@Entity
@Table(name="protein_interacts_with_protein")
public class ProteinInteractsWithProtein extends RepoTrialEdge implements Serializable {


    @Transient
    @JsonIgnore
    private final Logger log = LoggerFactory.getLogger(ProteinInteractsWithProtein.class);

    @JsonIgnore
    @EmbeddedId
    private PairId id;

    @Transient
    @JsonIgnore
    public final static HashSet<String> attributes = new HashSet<>(Arrays.asList("type", "methods", "databases", "memberTwo", "evidenceTypes", "memberOne"));

    @Transient
    private String memberOne;
    @Transient
    private String memberTwo;
    @Column(columnDefinition = "TEXT")
    private String methods;

    private String sourceDatabases;
    private String evidenceTypes;

    public ProteinInteractsWithProtein() {
    }


    public String getMemberOne() {
        return memberOne;
    }

    public String getMemberTwo() {
        return memberTwo;
    }

    @JsonGetter
    public String getType() {
        return "ProteinInteractsWithProtein";
    }

    @JsonSetter
    public void setType(String type) {
    }

    public List<String> getMethods() {
        return StringUtils.stringToList(methods);
    }

    public void setMethods(List<String> methods) {
        this.methods = StringUtils.listToString(methods);
    }

    public List<String> getDatabases() {
        return StringUtils.stringToList(sourceDatabases);
    }

    public void setDatabases(List<String> databases) {
        this.sourceDatabases = StringUtils.listToString(databases);
    }

    public List<String> getEvidenceTypes() {
        return StringUtils.stringToList(evidenceTypes);
    }

    public void setEvidenceTypes(List<String> evidenceTypes) {
        this.evidenceTypes = StringUtils.listToString(evidenceTypes);
    }

    public void setValues(ProteinInteractsWithProtein other) {
       this.sourceDatabases=other.sourceDatabases;
       this.evidenceTypes=other.evidenceTypes;
       this.memberOne=other.memberOne;
       this.memberTwo=other.memberTwo;
       this.methods=other.methods;
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
        return new Pair<>(memberOne, memberTwo);
    }
}