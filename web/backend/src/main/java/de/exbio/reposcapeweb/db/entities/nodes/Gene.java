package de.exbio.reposcapeweb.db.entities.nodes;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSetter;
import de.exbio.reposcapeweb.db.entities.RepoTrialNode;
import de.exbio.reposcapeweb.filter.FilterEntry;
import de.exbio.reposcapeweb.filter.FilterKey;
import de.exbio.reposcapeweb.filter.FilterType;
import de.exbio.reposcapeweb.utils.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.*;
import java.util.*;

@Entity
@Table(name = "genes")
public class Gene extends RepoTrialNode {


    @Transient
    @JsonIgnore
    private final Logger log = LoggerFactory.getLogger(Disorder.class);

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @JsonIgnore
    private int id;
    @Transient
    @JsonIgnore
    public static final HashSet<String> attributes = new HashSet<>(Arrays.asList("displayName", "type", "domainIds", "primaryDomainId", "geneType", "symbols", "approvedSymbol", "synonyms", "description", "chromosome", "mapLocation"));

    @Column(nullable = false)
    private String primaryDomainId;
    private String domainIds;
    @Column(nullable = false)
    private String displayName;
    @Column(columnDefinition = "TEXT")
    private String synonyms;
    private String approvedSymbol;
    private String symbols;
    private String description;
    private String chromosome;
    private String mapLocation;
    private String geneType;

    public static String[] getListAttributes() {
        return new String[]{"displayName", "primaryDomainId", "geneType", "approvedSymbol", "synonyms", "chromosome", "mapLocation"};
    }

    @Override
    public HashMap<String, String> getAsMap() {
        HashMap<String,String> values = new HashMap<>();
        values.put("displayName",getDisplayName());
        values.put("type",getType());
        values.put("domainIds",domainIds);
        values.put("synonyms",synonyms);
        values.put("geneType",geneType);
        values.put("symbols",symbols);
        values.put("approvedSymbol",approvedSymbol);
        values.put("description",description);
        values.put("chromosome",chromosome);
        values.put("mapLocation",mapLocation);
        values.put("primaryDomainId",primaryDomainId);
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


    public String getPrimaryDomainId() {
        return primaryDomainId;
    }

    @JsonGetter
    public List<String> getDomainIds() {
        return StringUtils.stringToList(domainIds);
    }

    @JsonSetter
    public void setDomainIds(List<String> domainIds) {
        this.domainIds = StringUtils.listToString(domainIds);
    }

    public String getDisplayName() {
        return displayName;
    }

    @JsonGetter
    public List<String> getSynonyms() {
        return StringUtils.stringToList(synonyms);
    }

    @JsonSetter
    public void setSynonyms(List<String> synonyms) {
        this.synonyms = StringUtils.listToString(synonyms);
    }

    public String getApprovedSymbol() {
        return approvedSymbol;
    }

    @JsonGetter
    public List<String> getSymbols() {
        return StringUtils.stringToList(symbols);
    }

    @JsonSetter
    public void setSymbols(List<String> symbols) {
        this.symbols = StringUtils.listToString(symbols);
    }

    public String getDescription() {
        return description;
    }

    public String getChromosome() {
        return chromosome;
    }

    public String getMapLocation() {
        return mapLocation;
    }

    public String getGeneType() {
        return geneType;
    }

    @JsonGetter
    public String getType() {
        return "Gene";
    }

    @JsonSetter
    public void setType(String type) {
    }

    public void setValues(Gene other) {
        this.symbols = other.symbols;
        this.domainIds = other.domainIds;
        this.approvedSymbol = other.approvedSymbol;
        this.chromosome = other.chromosome;
        this.description = other.description;
        this.displayName = other.displayName;
        this.geneType = other.geneType;
        this.mapLocation = other.mapLocation;
        this.primaryDomainId = other.primaryDomainId;
        this.synonyms = other.synonyms;

    }


    @Override
    public int getId() {
        return id;
    }

    @Override
    public String getPrimaryId() {
        return getPrimaryDomainId();
    }


    @Override
    public String getUniqueId() {
        return getPrimaryId();
    }

    @Override
    public EnumMap<FilterType, Map<FilterKey, FilterEntry>> toUniqueFilter() {
        EnumMap<FilterType, Map<FilterKey, FilterEntry>> map = new EnumMap<>(FilterType.class);

        FilterEntry ids = new FilterEntry(displayName, FilterType.DOMAIN_ID, id);


        map.put(FilterType.DOMAIN_ID, new HashMap<>());

        if (!getDomainIds().contains(primaryDomainId))
            try {
                primaryDomainId.charAt(0);
                map.get(FilterType.DOMAIN_ID).put(new FilterKey(primaryDomainId), ids);
            } catch (NullPointerException | IndexOutOfBoundsException ignore) {
            }

        getDomainIds().forEach(id -> map.get(FilterType.DOMAIN_ID).put(new FilterKey(id), ids));

        map.put(FilterType.DISPLAY_NAME, new HashMap<>());
        map.get(FilterType.DISPLAY_NAME).put(new FilterKey(displayName), new FilterEntry(displayName, FilterType.DISPLAY_NAME, id));

        if(!displayName.equals(approvedSymbol) & !getSymbols().contains(approvedSymbol)){
            map.put(FilterType.SYMBOLS, new HashMap<>());
            FilterEntry symbolEntry = new FilterEntry(displayName,FilterType.SYMBOLS,id);
            getSymbols().stream().filter(s->!s.equals(displayName) | !s.equals(approvedSymbol)).forEach(s->map.get(FilterType.SYMBOLS).put(new FilterKey(s),symbolEntry));
        }


        FilterEntry syns = new FilterEntry(displayName, FilterType.SYNONYM, id);
        map.put(FilterType.SYNONYM, new HashMap<>());
        getSynonyms().forEach(syn -> {
            if ( !displayName.equals(syn))
                map.get(FilterType.SYNONYM).put(new FilterKey(syn), syns);
        });


        return map;
    }

    @Override
    public EnumMap<FilterType, Map<FilterKey, FilterEntry>> toDistinctFilter() {
        return new EnumMap<>(FilterType.class);
    }

}
