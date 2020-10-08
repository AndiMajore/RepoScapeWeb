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
 * Entity class for the RepoTrial edge "DisorderComorbidWithDisorder" (disorder_comorbid_with_disorder in db).
 * The attributes primaryDomainIds of {@link DisorderComorbidWithDisorder#memberOne} and {@link DisorderComorbidWithDisorder#memberTwo} describe two source ids of undirected connected {@link de.exbio.reposcapeweb.db.entities.nodes.Disorder} instances by "comorbidity".
 * These primaryDomainIds from RepoTrial are converted to numeric node ids autogenerated on insertion of the nodes into the db.
 * Further the "type" attribute is also not included in the database, having only one possible value.
 * The class extends the {@link RepoTrialEdge} abstract class, for some methods used during import of the different RepoTrial entities.
 *
 * @author Andreas Maier
 */
@Entity
@Table(name = "disorder_comorbid_with_disorder")
public class DisorderComorbidWithDisorder extends RepoTrialEdge implements Serializable {

    @Transient
    @JsonIgnore
    private final Logger log = LoggerFactory.getLogger(DisorderComorbidWithDisorder.class);

    @JsonIgnore
    @EmbeddedId
    private PairId id;

    @Transient
    @JsonIgnore
    public final static HashSet<String> attributes = new HashSet<>(Arrays.asList("rr21", "type", "rr12", "memberTwo", "rrGeoMean", "memberOne", "phiCor"));

    @Transient
    private String memberOne;
    @Transient
    private String memberTwo;
    private Float phiCor;
    private Float rr12;
    private Float rr21;
    private Float rrGeoMean;


    public DisorderComorbidWithDisorder() {
    }

    public String getMemberOne() {
        return memberOne;
    }

    public String getMemberTwo() {
        return memberTwo;
    }

    public Float getPhiCor() {
        return phiCor;
    }

    public Float getRr12() {
        return rr12;
    }

    public Float getRr21() {
        return rr21;
    }

    public Float getRrGeoMean() {
        return rrGeoMean;
    }

    @JsonGetter
    public String getType() {
        return "DisorderComorbidWithDisorder";
    }

    @JsonSetter
    public void setType(String type) {
    }

    public void setValues(DisorderComorbidWithDisorder other) {
        this.memberOne = other.memberOne;
        this.memberTwo = other.memberTwo;
        this.phiCor = other.phiCor;
        this.rr12 = other.rr12;
        this.rr21 = other.rr21;
        this.rrGeoMean = other.rrGeoMean;
    }

    public static String[] getListAttributes() {
        return new String[]{"memberOne", "memberTwo",  "phiCor","rrGeoMean","type","rr12","rr21"};
    }

    @Override
    public HashMap<String, String> getAsMap() {
        HashMap<String,String> values = new HashMap<>();
        values.put("rr21",rr21+"");
        values.put("rr12",rr12+"");
        values.put("type",getType());
        values.put("memberOne",memberOne);
        values.put("memberTwo",memberTwo);
        values.put("rrGeoMean",rrGeoMean+"");
        values.put("phiCor",phiCor+"");
        return values;
    }

    @Override
    public HashMap<String, String> getAsMap(HashSet<String> attributes) {
        HashMap<String,String> values = new HashMap<>();
        getAsMap().forEach((k,v)->{
            if(attributes.contains(k))
                values.put(k,v);
        });
        return values;
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
