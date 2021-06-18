package de.exbio.reposcapeweb.communication.jobs;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.exbio.reposcapeweb.tools.ToolService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.EnumMap;
import java.util.HashMap;

@Service
public class JobCache {

    private EnumMap<ToolService.Tool, HashMap<Integer, String>> cache = new EnumMap<>(ToolService.Tool.class);
    private ObjectMapper objectMapper;

    @Autowired
    public JobCache(ObjectMapper objectMapper){
        this.objectMapper=objectMapper;
    }

    public String getCached(Job j) {
        if (!cache.containsKey(j.getMethod())) {
            cache.put(j.getMethod(), new HashMap<>());
            addToCache(j);
            return null;
        }
        String jid = cache.get(j.getMethod()).get(j.hashCode());
        if (jid != null)
            return jid;
        addToCache(j);
        return null;
    }

    private void addToCache(Job j) {
        cache.get(j.getMethod()).put(j.hashCode(), j.getJobId());
    }


}
