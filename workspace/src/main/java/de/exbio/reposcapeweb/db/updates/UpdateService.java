package de.exbio.reposcapeweb.db.updates;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import de.exbio.reposcapeweb.db.entities.RepoTrialEntity;
import de.exbio.reposcapeweb.db.entities.nodes.*;
import de.exbio.reposcapeweb.db.services.*;
import de.exbio.reposcapeweb.utils.FileUtils;
import de.exbio.reposcapeweb.utils.ReaderUtils;
import de.exbio.reposcapeweb.utils.RepoTrialUtils;
import de.exbio.reposcapeweb.utils.StringUtils;
import org.apache.tomcat.jni.Proc;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.expression.Operation;
import org.springframework.stereotype.Service;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.util.*;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

@Service
public class UpdateService {
    private final Logger log = LoggerFactory.getLogger(UpdateService.class);

    private final Environment env;
    private final ObjectMapper objectMapper;

    private final DisorderService disorderService;
    private final DrugService drugService;
    private final GeneService geneService;
    private final PathwayService pathwayService;
    private final ProteinService proteinService;

    @Autowired
    public UpdateService(Environment environment, DrugService drugService, ObjectMapper objectMapper, PathwayService pathwayService, DisorderService disorderService, GeneService geneService, ProteinService proteinService) {
        this.env = environment;
        this.drugService = drugService;
        this.objectMapper = objectMapper;
        this.pathwayService = pathwayService;
        this.disorderService=disorderService;
        this.geneService=geneService;
        this.proteinService=proteinService;
    }


    public boolean executeDataUpdate() {
        String url = env.getProperty("url.api.db");
        File cacheDir = new File(env.getProperty("path.db.cache"));
        try {
            log.info("Executing DB update.");
            update(url, cacheDir);
        } catch (Exception e) {
            log.error("Update could not be executed correctly: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Terminated because of update failure caused by" + e.getClass().toString().substring(5));
        }
        return true;
    }

    private void update(String url, File cacheDir) {
        log.debug("Executing Database update from " + url + " to cache-directory " + cacheDir.getAbsolutePath());

        HashMap<String, Collection> collections = new HashMap<>();

        File updateDir = new File(cacheDir, "update_" + System.currentTimeMillis());
        updateDir.deleteOnExit();
        String fileType = env.getProperty("file.collections.filetype");
        fileType = fileType.charAt(0) == '.' ? fileType : '.' + fileType;

        log.info("Downloading database files!");
        downloadUpdates(url, updateDir, fileType, collections);
        updateDB(collections);
        overrideOldData(cacheDir, collections);

        updateDir.delete();
    }

    private void updateDB(HashMap<String, Collection> collections) {
        //TODO implement
        //TODO just for dev
        reformatCollections(collections);

        identifyUpdates(collections);
    }

    private <T extends RepoTrialEntity> EnumMap<UpdateOperation, HashMap<String, T>> runUpdates(Class<T> valueType, String attributeDefinition, Collection c) {
        EnumMap<UpdateOperation, HashMap<String, T>> updates = new EnumMap<>(UpdateOperation.class);
        if (!readUpdates(c, updates, valueType))
            importInsertions(c.getFile(), updates, valueType);
        return updates;
    }

    private void identifyUpdates(HashMap<String, Collection> collections) {
        collections.forEach((k, c) -> {
            try {
                String attributeDefinition = ReaderUtils.getUrlContent(new URL(env.getProperty("url.api.db") + k + "/attributes"));
                boolean updateSuccessful = true;

                switch (k) {
                    case "drug": {
                        if (updateSuccessful = Drug.validateFormat(getAttributeNames(attributeDefinition)))
                            updateSuccessful = drugService.submitUpdates(runUpdates(Drug.class, attributeDefinition, c));
                        break;
                    }
                    case "pathway": {
                        if (updateSuccessful = Pathway.validateFormat(getAttributeNames(attributeDefinition)))
                            updateSuccessful = pathwayService.submitUpdates(runUpdates(Pathway.class, attributeDefinition, c));
                        break;
                    }
                    case "disorder": {
                        if (updateSuccessful = Disorder.validateFormat(getAttributeNames(attributeDefinition)))
                            updateSuccessful = disorderService.submitUpdates(runUpdates(Disorder.class, attributeDefinition, c));
                        break;
                    }
                    case "gene": {
                        if (updateSuccessful = Gene.validateFormat(getAttributeNames(attributeDefinition)))
                            updateSuccessful = geneService.submitUpdates(runUpdates(Gene.class, attributeDefinition, c));
                        break;
                    }
                    case "protein": {
                        if (updateSuccessful = Protein.validateFormat(getAttributeNames(attributeDefinition)))
                            updateSuccessful = proteinService.submitUpdates(runUpdates(Protein.class, attributeDefinition, c));
                        break;
                    }
                    //TODO validate edges and create statistics
                }
                if (updateSuccessful)
                    log.warn("Update execution for " + k + ": Success!");
                else
                    log.debug("Update execution for " + k + ": Error!");
            } catch (MalformedURLException e) {
                e.printStackTrace();
            }
        });

    }

    private <T extends RepoTrialEntity> boolean readUpdates(Collection c, EnumMap<UpdateOperation, HashMap<String, T>> updates, Class<T> valueType) {
        File cached = RepoTrialUtils.getCachedFile(c, env.getProperty("path.db.cache"));
        if (!cached.exists())
            return false;
        ProcessBuilder pb = new ProcessBuilder("diff", cached.getAbsolutePath(), c.getFile().getAbsolutePath());

        pb.redirectErrorStream(true);
        Process p = null;
        HashMap<String, T> ins = new HashMap<>();
        HashMap<String, T> upd = new HashMap<>();
        HashMap<String, T> dels = new HashMap<>();
        updates.put(UpdateOperation.Alteration, upd);
        updates.put(UpdateOperation.Deletion, dels);
        updates.put(UpdateOperation.Insertion, ins);

        try {
            p = pb.start();
            BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String line = "";
            while ((line = br.readLine()) != null) {
                char startC = line.charAt(0);
                if (startC == '>' | startC == '<') {
                    int start = line.charAt(2) == '[' ? 3 : 2;
                    T d = objectMapper.readValue(line.substring(start), valueType);
                    if (startC == '<') {
                        dels.put(d.getPrimaryId(), d);
                    } else {
                        ins.put(d.getPrimaryId(), d);
                    }
                }

            }

            HashSet<String> overlap = new HashSet<>();
            dels.keySet().forEach(id -> {
                if (ins.containsKey(id))
                    overlap.add(id);
            });

            overlap.forEach(id -> {
                upd.put(id, ins.get(id));
                dels.remove(id);
                ins.remove(id);
            });

            p.waitFor();
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
        return true;
    }

    public <T extends RepoTrialEntity> void importInsertions(File updateFile, EnumMap<UpdateOperation, HashMap<String, T>> updates, Class<T> valueType) {
        log.debug("Importing insertions only from " + updateFile);
        HashMap<String, T> inserts = new HashMap<>();
        updates.put(UpdateOperation.Insertion, inserts);
        BufferedReader br = ReaderUtils.getBasicReader(updateFile);
        String line = "";
        boolean first = true;
        try {
            while ((line = br.readLine()) != null) {
                if (first) {
                    first = false;
                    if (line.charAt(0) == '[')
                        line = line.substring(1);
                }
                try {
                    T d = objectMapper.readValue(line, valueType);
                    inserts.put(d.getPrimaryId(), d);
                } catch (MismatchedInputException e) {
                    e.printStackTrace();
                    log.error("Malformed input line in " + updateFile.getName() + ": " + line);
                }
                if (line.charAt(line.length() - 1) == ']')
                    break;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

//    private HashMap<String,String> getAttributes(String values){
//        HashMap<String,String> map = new HashMap<>();
//        System.out.println(StringUtils.split(values, ",\""));
//    }

    private HashSet<String> getAttributeNames(String content) {
        HashSet<String> attributes = new HashSet<>();
        StringUtils.split(content.substring(1, content.length() - 2), ",").forEach(a -> attributes.add(a.substring(1, a.length() - 1)));
        return attributes;
    }

    private void reformatCollections(HashMap<String, Collection> collections) {
        collections.values().forEach(col -> {
            FileUtils.formatJson(col.getFile());
            BufferedReader br = ReaderUtils.getBasicReader(col.getFile());
            int count = 0;
            String line = "";
            try {
                while ((line = br.readLine()) != null) {
                    count++;
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
//            }
            int officialCount = getCountFromDetails(col.getName());
            log.debug(col.getName() + " contains " + count + " entries!");
            if (count != officialCount) {
                log.error("Entry count for " + col.getName() + " does not match to official number from repotrial (" + count + " vs " + officialCount + ")");
                throw new RuntimeException("Error while validating the entitiy counts. Maybe file format has changed.");
            }
        });
        log.info("Validation of entity count in cached files!");
    }

    private int getCountFromDetails(String name) {
        try {
            BufferedReader br = ReaderUtils.getBufferedReader(new URL(env.getProperty("url.api.db") + name + "/details"));
            String line = "";
            while ((line = br.readLine()) != null) {
                for (String l : StringUtils.split(line, ","))
                    if (l.startsWith("\"count"))
                        return Integer.parseInt(l.substring(8));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return -1;
    }

    private void overrideOldData(File cacheDir, HashMap<String, Collection> collections) {
        collections.forEach((k, v) -> {
            try {
                log.debug("Moving " + v.getFile().toPath() + " to " + new File(cacheDir, v.getFile().getName()).toPath());
                v.setFile(Files.move(v.getFile().toPath(), new File(cacheDir, v.getFile().getName()).toPath(), REPLACE_EXISTING).toFile());
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    private void downloadUpdates(String api, File destDir, String fileType, HashMap<String, Collection> collections) {
        //TODO let downloads happen in background
        destDir.mkdirs();

        prepareCollections(env.getProperty("file.collections.nodes"), collections, true);
        prepareCollections(env.getProperty("file.collections.edges"), collections, false);

        collections.forEach((k, v) -> v.setFile(FileUtils.download(createUrl(api, k), createFile(destDir, k, fileType))));

        //TODO just for dev
//        collections.forEach((k, v) -> v.setFile(RepoTrialUtils.getCachedFile(v, fileType, env.getProperty("path.db.cache"))));

    }

    private String createUrl(String api, String k) {
        return api + k + "/all";
    }

    private File createFile(File destDir, String k, String fileType) {
        return new File(destDir, k + fileType);
    }

    private void prepareCollections(String file, HashMap<String, Collection> collections, boolean typeNode) {
        BufferedReader br = ReaderUtils.getBasicReader(file);
        String line = "";
        try {
            while ((line = br.readLine()) != null) {
                if (line.charAt(0) == '#')
                    continue;
                Collection c = typeNode ? new Node(line) : new Edge(line);
                collections.put(c.getName(), c);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


}