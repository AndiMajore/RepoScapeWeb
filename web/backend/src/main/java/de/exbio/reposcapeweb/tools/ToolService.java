package de.exbio.reposcapeweb.tools;

import de.exbio.reposcapeweb.communication.cache.Graph;
import de.exbio.reposcapeweb.communication.cache.Graphs;
import de.exbio.reposcapeweb.communication.jobs.Job;
import de.exbio.reposcapeweb.communication.jobs.JobRequest;
import de.exbio.reposcapeweb.communication.jobs.JobResult;
import de.exbio.reposcapeweb.configs.DBConfig;
import de.exbio.reposcapeweb.db.services.edges.ProteinInteractsWithProteinService;
import de.exbio.reposcapeweb.db.services.nodes.GeneService;
import de.exbio.reposcapeweb.db.services.nodes.ProteinService;
import de.exbio.reposcapeweb.utils.*;
import org.apache.tomcat.util.http.fileupload.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.zeroturnaround.zip.ZipUtil;

import java.io.*;
import java.nio.file.Files;
import java.util.*;

@Service
public class ToolService {

    private final Environment env;
    private final ProteinInteractsWithProteinService interactionService;
    private final ProteinService proteinService;
    private final GeneService geneService;

    private File scriptDir;
    private File dataDir;
    private File executor;
    private File bicon;
    private File diamond;
    private File trustrank;
    private File centrality;
    private File must;

    private HashMap<String, File> tempDirs = new HashMap<>();

    private Logger log = LoggerFactory.getLogger(ToolService.class);

    @Autowired
    public ToolService(ProteinService proteinService, GeneService geneService, Environment env, ProteinInteractsWithProteinService interactionService) {
        this.env = env;
        this.interactionService = interactionService;
        this.proteinService = proteinService;
        this.geneService = geneService;
        dataDir = new File(env.getProperty("path.external.cache"));
        scriptDir = new File(env.getProperty("path.scripts.dir"));
        validateEnvironment();
    }

    private void validateEnvironment() {
        envValidate("python3", "--version");
    }

    private void envValidate(String component, String versionCommand) {
        StringBuffer sb = new StringBuffer();
        try {
            ProcessUtils.executeProcessWait(new ProcessBuilder("which", component), sb);
        } catch (IOException | InterruptedException e) {
            log.error(component + " not found in environment. Make sure it is installed properly!");
            throw new RuntimeException("Missing dependency: " + component);
        }
        StringBuffer version = new StringBuffer();
        try {
            ProcessUtils.executeProcessWait(new ProcessBuilder(component, versionCommand), version);
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
        log.info("Using " + component + ": " + sb.toString().strip() + " [version: " + version.toString().strip() + "]");
    }


    public void validateTools() {
        validateJobExecutor();

        prepareDiamond();
        prepareBicon();
        prepareTrustRank();
        prepareCentrality();
        prepareMust();

    }

    private void validateJobExecutor() {
        executor = new File(env.getProperty("jobs.scripts.executor"));
        if (!executor.exists()) {
            log.error("JobExecutor executable was not found in config.");
            new RuntimeException("Missing reference.");
        }
    }

    private void prepareDiamond() {
        log.info("Setting up DIAMOnD");
        File diamondSetUpScript = new File(scriptDir, "diamond_setup.sh");
        diamond = new File(env.getProperty("path.tool.diamond"));
        log.info("Diamond path=" + diamond.getAbsolutePath());
        if (!diamond.exists()) {
            log.error("DIAMOnD executable was not found in config. Please make sure it is referenced correctly!");
            new RuntimeException("Missing reference.");
        }
    }

    private void prepareBicon() {
        log.info("Setting up BiCon");
        bicon = new File(env.getProperty("path.tool.bicon"));
        log.info("BiCon path=" + bicon.getAbsolutePath());
        if (!bicon.exists()) {
            log.error("BiCon executable was not found in config. Please make sure it is referenced correctly!");
            new RuntimeException("Missing reference.");
        }
    }

    private void prepareTrustRank() {
        log.info("Setting up TrustRank");
        trustrank = new File(env.getProperty("path.tool.trustrank"));
        log.info("TrustRank path=" + trustrank.getAbsolutePath());
        if (!trustrank.exists()) {
            log.error("TrustRank executable was not found in config. Please make sure it is referenced correctly!");
            new RuntimeException("Missing reference.");
        }
    }

    private void prepareCentrality() {
        log.info("Setting up Centrality");
        centrality = new File(env.getProperty("path.tool.centrality"));
        log.info("Centrality path=" + centrality.getAbsolutePath());
        if (!centrality.exists()) {
            log.error("Centrality executable was not found in config. Please make sure it is referenced correctly!");
            new RuntimeException("Missing reference.");
        }
    }

    private void prepareMust() {
        log.info("Setting up Must");
        must = new File(env.getProperty("path.tool.must"));
        log.info("Must path=" + must.getAbsolutePath());
        if (!must.exists()) {
            log.error("Must executable was not found in config. Please make sure it is referenced correctly!");
            new RuntimeException("Missing reference.");
        }
    }

    public void createInteractionFiles() {
        File ggi_all = new File(dataDir, "gene_gene_interaction_all.pairs");
        File ggi_exp = new File(dataDir, "gene_gene_interaction_exp.pairs");
        try (BufferedWriter bw_all = WriterUtils.getBasicWriter(ggi_all); BufferedWriter bw_exp = WriterUtils.getBasicWriter(ggi_exp)) {
            interactionService.getGenes().forEach((id1, map) -> map.forEach((id, vals) -> {
                        try {
                            bw_all.write(geneService.map(id1) + "\t" + geneService.map(id.getId2()) + "\n");
                            if (vals.second) {
                                bw_exp.write(geneService.map(id1) + "\t" + geneService.map(id.getId2()) + "\n");
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    })
            );
        } catch (IOException e) {
            e.printStackTrace();
        }


        File ppi_all = new File(dataDir, "protein_protein_interaction_all.pairs");
        File ppi_exp = new File(dataDir, "protein_protein_interaction_exp.pairs");
        try (BufferedWriter bw_all = WriterUtils.getBasicWriter(ppi_all); BufferedWriter bw_exp = WriterUtils.getBasicWriter(ppi_exp)) {
            interactionService.getProteins().forEach((id1, map) -> map.forEach((id, vals) -> {
                try {
                    bw_all.write(proteinService.map(id1) + "\t" + proteinService.map(id.getId2()) + "\n");
                    if (vals.second) {
                        bw_exp.write(proteinService.map(id1) + "\t" + proteinService.map(id.getId2()) + "\n");
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }));

        } catch (IOException e) {
            e.printStackTrace();
        }

        File rankCache = new File(dataDir, "ranking_files");
        if (rankCache.exists())
            rankCache.delete();
        rankCache.mkdir();
        try {
            ProcessUtils.executeProcessWait(new ProcessBuilder("python3", env.getProperty("jobs.scripts.ranking_preparation"), dataDir.getAbsolutePath(), rankCache.getAbsolutePath()));
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }


    public File getTempDir(String jobId) {
        if (!tempDirs.containsKey(jobId))
            try {
                File f = Files.createTempDirectory(new File("/tmp").toPath(), "nedrex_job_" + jobId).toFile();
                tempDirs.put(jobId, f);
            } catch (IOException e) {
                e.printStackTrace();
            }
        return tempDirs.get(jobId);
    }

    @Async
    public void removeTempDir(String jobId) {
        File wd = getTempDir(jobId);
        if (wd.exists()) {
            try {
                FileUtils.deleteDirectory(wd);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }


    public String createCommand(Job job, JobRequest request) {
        String command = "localhost:"+env.getProperty("server.port")+env.getProperty("server.servlet.context-path")+"/api/finishedJob?id=" + job.getJobId() + " " + getTempDir(job.getJobId()).getAbsolutePath() + " ";
        LinkedList<File> neededFiles = new LinkedList<>();
        switch (job.getMethod()) {
            case DIAMOND: {
                File interactions = (request.getParams().get("type").equals("gene") ? new File(dataDir, "gene_gene_interaction_" + (request.experimentalOnly ? "exp" : "all") + ".pairs") : new File(dataDir, "protein_protein_interaction_" + (request.experimentalOnly ? "exp" : "all") + ".pairs"));
                neededFiles.add(interactions);
                command += "diamond " +
                        diamond.getAbsolutePath() + " " +
                        interactions.getName() + " " +
                        "seeds.list " +
                        request.getParams().get("n") + " " + request.getParams().get("alpha");
                break;
            }
            case BICON: {
                File interactions = new File(dataDir, "gene_gene_interaction_" + (request.experimentalOnly ? "exp" : "all") + ".pairs");
                neededFiles.add(interactions);
                command += "bicon " + bicon.getAbsolutePath() + " exprFile " + interactions.getName() + " genes.txt " + request.getParams().get("lg_min") + " " + request.getParams().get("lg_max");
                break;
            }
            case MUST: {
                File interactions = (request.getParams().get("type").equals("gene") ? new File(dataDir, "gene_gene_interaction_" + (request.experimentalOnly ? "exp" : "all") + ".pairs") : new File(dataDir, "protein_protein_interaction_" + (request.experimentalOnly ? "exp" : "all") + ".pairs"));
                neededFiles.add(interactions);
                command += "must " +
                        must.getAbsolutePath() + " " +
                        interactions.getName() + " " +
                        "seeds.list edges.list nodes.list " +
                        request.getParams().get("penalty") + " " +
                        request.getParams().get("maxit") + (request.getParams().get("multiple").equals("true") ? " " + request.getParams().get("trees") : "");
                break;
            }
            case TRUSTRANK: {
                File interactions = request.getParams().get("type").equals("gene") ? new File(dataDir, "ranking_files/GGDr_" + (request.experimentalOnly ? "exp" : "all") + ".gt") : new File(dataDir, "ranking_files/PPDr_all.gt");
                neededFiles.add(interactions);
                command += "trustrank " + trustrank.getAbsolutePath() + " " +
                        interactions.getName() +
                        " seeds.list " +
                        request.getParams().get("damping") +
                        (request.getParams().get("direct").charAt(0) == 't' ? " Y" : " N") +
                        (request.getParams().get("approved").charAt(0) == 't' ? " Y" : " N");
                break;
            }
            case CENTRALITY: {
                File interactions = request.getParams().get("type").equals("gene") ? new File(dataDir, "ranking_files/GGDr_" + (request.experimentalOnly ? "exp" : "all") + ".gt") : new File(dataDir, "ranking_files/PPDr_all.gt");
                neededFiles.add(interactions);
                command += "centrality " + centrality.getAbsolutePath() + " " +
                        interactions.getName() +
                        " seeds.list" +
                        (request.getParams().get("direct").charAt(0) == 't' ? " Y" : " N") +
                        (request.getParams().get("approved").charAt(0) == 't' ? " Y" : " N");
                break;
            }
        }
        prepareTempDir(job, neededFiles);
        return command;
    }

    private void prepareTempDir(Job job, LinkedList<File> neededFiles) {
        File dir = getTempDir(job.getJobId());
        neededFiles.forEach(f -> {
            try {
                Files.copy(f.toPath(), new File(dir, f.getName()).toPath());
            } catch (IOException e) {
                e.printStackTrace();
            }
        });

    }

    public void prepareJobFiles(Job job, JobRequest req, Graph g, HashMap<Integer, Pair<String, String>> domainMap) {
        switch (job.getMethod()) {
            case DIAMOND, MUST, TRUSTRANK, CENTRALITY: {
                File seed = new File(getTempDir(job.getJobId()), "seeds.list");
                if (req.selection)
                    writeSeedFile(req.params.get("type"), seed, req.nodes, domainMap);
                else
                    writeSeedFile(req.params.get("type"), seed, g, domainMap);
                break;
            }
            case BICON: {
                File exprFile = new File(getTempDir(job.getJobId()), "exprFile");
                boolean header = true;
                try (BufferedWriter bw = WriterUtils.getBasicWriter(exprFile)) {
                    for (String line : StringUtils.split(req.getParams().get("exprData"), "\n")) {
                        bw.write((header ? "" : "entrez.") + line + "\n");
                        if (header)
                            header = false;
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
                break;
            }
//            case TRUSTRANK, CENTRALITY: {
//                File seed = new File(getTempDir(job.getJobId()), "seeds.list");
//                writeSeedFile(req.params.get("type"), seed, req.ids, domainMap);
//                break;
//            }
        }

    }

    private void writeSeedFile(String type, File file, Graph g, HashMap<Integer, Pair<String, String>> domainMap) {
        writeSeedFile(type, file, g.getNodes().get(Graphs.getNode(type)).keySet(), domainMap);
    }

    private void writeSeedFile(String type, File file, Collection nodeIds, HashMap<Integer, Pair<String, String>> domainMap) {
        BufferedWriter bw = WriterUtils.getBasicWriter(file);
        nodeIds.forEach(node -> {
            try {
                bw.write(domainMap.get(node).getFirst() + "\n");
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        try {
            bw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public Process executeJob(String command) {
        try {
            return Runtime.getRuntime().exec(executor.getAbsolutePath() + " " + command);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public void getJobResults(Job j, HashMap<Integer, HashMap<String, Integer>> domainMaps) throws IOException {
        JobResult result = new JobResult();
        j.setResult(result);
        HashMap<Integer, HashMap<String, Object>> nodes = new HashMap<>();
        HashMap<Integer, HashMap<Integer, HashMap<String, Object>>> edges = new HashMap<>();
        switch (j.getMethod()) {
            case DIAMOND: {
                for (File f : getTempDir(j.getJobId()).listFiles()) {
                    if (f.getName().endsWith(".txt")) {
                        nodes = readDiamondResults(f, Double.parseDouble(j.getParams().get("pcutoff")), domainMaps.get(Graphs.getNode(j.getTarget())));
                    }
                }
                break;
            }
            case MUST: {
                HashMap<String, Integer> domainMap = domainMaps.get(Graphs.getNode(j.getTarget()));
                try (BufferedReader br = ReaderUtils.getBasicReader(new File(getTempDir(j.getJobId()), "nodes.list"))) {
                    String line = br.readLine();
                    while ((line = br.readLine()) != null) {
                        LinkedList<String> split = StringUtils.split(line, "\t");
                        int id = domainMaps.get(Graphs.getNode(j.getTarget())).get(split.get(0));
                        nodes.put(id, new HashMap<>());
                        nodes.get(id).put("participation_number", split.get(1));
                    }
                }
                try (BufferedReader br = ReaderUtils.getBasicReader(new File(getTempDir(j.getJobId()), "edges.list"))) {
                    String line = br.readLine();
                    while ((line = br.readLine()) != null) {
                        LinkedList<String> split = StringUtils.split(line, "\t");
                        int id1 = domainMap.get(split.get(0));
                        int id2 = domainMap.get(split.get(1));
                        if (id2 < id1) {
                            int tmp = id1;
                            id1 = id2;
                            id2 = tmp;
                        }
                        if (!edges.containsKey(id1))
                            edges.put(id1, new HashMap<>());
                        if (!edges.get(id1).containsKey(id2))
                            edges.get(id1).put(id2, new HashMap<>());

                        HashMap<String, Object> e = edges.get(id1).get(id2);

                        e.put("memberOne", split.get(0));
                        e.put("memberTwo", split.get(1));
                        e.put("participation_number", Integer.parseInt(split.get(2)));
                    }
                }
                break;
            }
            case BICON: {
                try (BufferedReader br = ReaderUtils.getBasicReader(new File(getTempDir(j.getJobId()), "genes.txt"))) {
                    String line = "";
                    while ((line = br.readLine()) != null) {
                        nodes.put(domainMaps.get(Graphs.getNode("gene")).get(line), null);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    throw e;
                }
                break;
            }
            case TRUSTRANK, CENTRALITY: {
                for (File f : getTempDir(j.getJobId()).listFiles()) {
                    if (!f.getName().equals("seeds.list") && !f.getName().endsWith(".gt")) {
                        nodes = readTrustRankResults(f, domainMaps, j.getTarget());
                    }
                }
                break;
            }
        }
        result.setNodes(nodes);
        result.setEdges(edges);
    }

    private HashMap<Integer, HashMap<String, Object>> readTrustRankResults(File f, HashMap<Integer, HashMap<String, Integer>> domainMap, String target) {
        HashMap<Integer, HashMap<String, Object>> out = new HashMap<>();
        HashMap<String, Integer> drugMap = domainMap.get(Graphs.getNode("drug"));
        HashMap<String, Integer> seedMap = domainMap.get(Graphs.getNode(target));
        try (BufferedReader br = ReaderUtils.getBasicReader(f)) {
            String line = br.readLine();
            while ((line = br.readLine()) != null) {
                LinkedList<String> split = StringUtils.split(line, "\t");
                if (split.size() == 2) {
                    int id = drugMap.get(split.getFirst());
                    out.put(id, new HashMap<>());
                    out.get(id).put("score", Double.parseDouble(split.get(1)));
                } else {
                    out.put(seedMap.get(line), null);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return out;
    }


    public void clearDirectories(Job j, File dest) throws Exception {
        File result = null;
        switch (j.getMethod()) {
            case DIAMOND: {
                for (File f : getTempDir(j.getJobId()).listFiles()) {
                    if (f.getName().endsWith(".txt")) {
                        result = f;
                        break;
                    }
                }
                break;
            }
            case BICON: {
                result = new File(getTempDir(j.getJobId()), "genes.txt");
                break;
            }
            case TRUSTRANK, CENTRALITY: {
                for (File f : getTempDir(j.getJobId()).listFiles()) {
                    if (!f.getName().equals("seeds.list") && !f.getName().endsWith(".gt")) {
                        result = f;
                        break;
                    }
                }
                break;
            }
            case MUST: {
                File tmp = getTempDir(j.getJobId());
                Arrays.stream(tmp.listFiles()).forEach(f->{
                    if(!f.getName().equals("edges.list") && !f.getName().equals("nodes.list"))
                        f.delete();
                });

                if (tmp.list().length > 0) {
                    result = new File(tmp, "results.zip");
                    ZipUtil.packEntries(tmp.listFiles(), result);
                }
            }
        }
        try {
            if (result.exists())
                j.setResultFile(true);
            dest.getParentFile().mkdirs();
            Files.move(result.toPath(), dest.toPath());
        } catch (IOException | NullPointerException e) {
            getTempDir(j.getJobId()).delete();
            e.printStackTrace();
            throw new Exception("Missing results exception");
        }
        removeTempDir(j.getJobId());
    }

    private HashMap<Integer, HashMap<String, Object>> readDiamondResults(File f, double cutoff, HashMap<String, Integer> domainMap) {
        HashMap<Integer, HashMap<String, Object>> results = new HashMap<>();
        try {
            BufferedReader br = ReaderUtils.getBasicReader(f);
            String line = "";
            while ((line = br.readLine()) != null) {
                if (line.charAt(0) == '#')
                    continue;
                LinkedList<String> attrs = StringUtils.split(line, "\t");
                double p_val = Double.parseDouble(attrs.get(2));
                if (p_val < cutoff) {
                    int id = domainMap.get(attrs.get(1));
                    results.put(id, new HashMap<>());
                    results.get(id).put("p_hyper", p_val);
                    results.get(id).put("rank", Integer.parseInt(attrs.get(0)));
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return results;
    }

    public void createGraphmlFromFS(File wd, File graphml) {
        try {
            ProcessUtils.executeProcessWait(new ProcessBuilder("python3", new File(scriptDir, "tablesToGraphml.py").getAbsolutePath(), wd.getAbsolutePath(), graphml.getAbsolutePath()));
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
        if (!graphml.exists()) {
            log.error(graphml.getAbsolutePath() + " should hav been created but wasn't!");
        }
    }

    public void createThumbnail(File graphml, File thumb) {
        thumb.getParentFile().mkdirs();
        try {
            ProcessUtils.executeProcessWait(new ProcessBuilder("python3", new File(scriptDir, "computeLayouting.py").getAbsolutePath(), graphml.getAbsolutePath(), "1", thumb.getAbsolutePath(), DBConfig.getConfFile().getAbsolutePath()));
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void createLayout(File graphml, File layout) {
        layout.getParentFile().mkdirs();
        try {
            ProcessUtils.executeProcessWait(new ProcessBuilder("python3", new File(scriptDir, "computeLayouting.py").getAbsolutePath(), graphml.getAbsolutePath(), "0", layout.getAbsolutePath(),DBConfig.getConfFile().getAbsolutePath()));
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    public enum Tool {
        DIAMOND, BICON, TRUSTRANK, CENTRALITY, MUST
    }
}
