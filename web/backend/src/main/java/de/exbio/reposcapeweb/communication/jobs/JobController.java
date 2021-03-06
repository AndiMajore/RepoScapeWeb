package de.exbio.reposcapeweb.communication.jobs;

import de.exbio.reposcapeweb.communication.cache.Graph;
import de.exbio.reposcapeweb.communication.cache.Graphs;
import de.exbio.reposcapeweb.communication.controller.SocketController;
import de.exbio.reposcapeweb.communication.reponses.WebGraphService;
import de.exbio.reposcapeweb.db.history.HistoryController;
import de.exbio.reposcapeweb.db.services.nodes.DrugService;
import de.exbio.reposcapeweb.db.services.nodes.GeneService;
import de.exbio.reposcapeweb.db.services.nodes.ProteinService;
import de.exbio.reposcapeweb.filter.NodeFilter;
import de.exbio.reposcapeweb.tools.ToolService;
import de.exbio.reposcapeweb.utils.Pair;
import de.exbio.reposcapeweb.utils.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.Files;
import java.util.*;

@Service
public class JobController {

    private final WebGraphService graphService;
    private final ToolService toolService;
    private final JobQueue jobQueue;
    private final JobRepository jobRepository;
    private final SocketController socketController;
    private final GeneService geneService;
    private final ProteinService proteinService;
    private final DrugService drugService;
    private final HistoryController historyController;
    private final JobCache jobCache;

    private HashMap<String, Job> jobs = new HashMap<>();
    private HashMap<String, LinkedList<String>> jobWaitingForResults = new HashMap<>();
    private Logger log = LoggerFactory.getLogger(JobController.class);

    @Autowired
    public JobController(HistoryController historyController, DrugService drugService, ProteinService proteinService, GeneService geneService, WebGraphService graphService, ToolService toolService, JobQueue jobQueue, JobRepository jobRepository, SocketController socketController, JobCache jobCache) {
        this.jobCache = jobCache;
        this.graphService = graphService;
        this.toolService = toolService;
        this.jobQueue = jobQueue;
        this.jobRepository = jobRepository;
        this.socketController = socketController;
        this.geneService = geneService;
        this.proteinService = proteinService;
        this.drugService = drugService;
        this.historyController = historyController;
    }


    private Job createJob(JobRequest req) {
        String id = UUID.randomUUID().toString();
        while (jobs.containsKey(id))
            id = UUID.randomUUID().toString();
        Job j = new Job(id, req);
        jobs.put(id, j);
        return j;
    }

    public void importJobsHistory() {
        jobRepository.findAll().forEach(j -> jobs.put(j.getJobId(), j));
    }


    public Job registerJob(JobRequest req) {
        Job j = createJob(req);

        j.addParam("experimentalOnly", req.experimentalOnly);

        String[] params = new String[]{"nodesOnly", "addInteractions", "n", "alpha", "type", "approved", "direct"};
        for (String param : params)
            if (req.getParams().containsKey(param))
                j.addParam(param, req.getParams().get(param));

        j.addParam("pcutoff", req.getParams().containsKey("pcutoff") ? Math.pow(10, Double.parseDouble(req.getParams().get("pcutoff"))) : 1);
        j.addParam("topX", req.getParams().containsKey("topX") ? Integer.parseInt(req.getParams().get("topX")) : Integer.MAX_VALUE);
        j.addParam("elements", req.getParams().containsKey("elements") && req.getParams().get("elements").startsWith("t"));
        Graph g = graphService.getCachedGraph(req.graphId);
        try {
            prepareJob(j, req, g);
        } catch (Exception e) {
            log.error("Error on job submission");
            j.setStatus(Job.JobState.ERROR);
            e.printStackTrace();
            return j;
        }
        if (!j.getState().equals(Job.JobState.DONE) && !j.getState().equals(Job.JobState.WAITING))
            queue(j);
        else
            jobRepository.save(j);
        return j;
    }

    private void queue(Job j) {
        save(j);
        jobQueue.addJob(j);
    }

    private void save(Job j) {
        jobRepository.save(j);
    }

    private void prepareJob(Job j, JobRequest req, Graph g) {
        if (g == null & req.selection) {
            g = graphService.createGraphFromIds(req.getParams().get("type"), req.nodes, j.getUserId());
            req.graphId = g.getId();
            j.setBasisGraph(g.getId());
        }
        if (j.getMethod().equals(ToolService.Tool.BICON))
            prepareExpressionFile(req);

        String command = createCommand(j, req);
        j.setCommand(command);

        if (!j.getMethod().equals(ToolService.Tool.BICON) && req.nodes != null && req.nodes.size() > 0) {
            j.setSeeds(req.nodes);
            try {
                Job sameJob = jobs.get(jobCache.getCached(j));
                switch (sameJob.getState()) {
                    case DONE -> {
                        copyResults(j.getJobId(), sameJob, false);
                        return;
                    }
                    case EXECUTING, INITIALIZED, QUEUED, NOCHANGE -> {
                        if (!this.jobWaitingForResults.containsKey(sameJob.getJobId()))
                            this.jobWaitingForResults.put(sameJob.getJobId(), new LinkedList<>());
                        this.jobWaitingForResults.get(sameJob.getJobId()).add(j.getJobId());
                        j.setStatus(Job.JobState.WAITING);
                        return;
                    }
                }
            } catch (NullPointerException ignore) {
            }
        }

        prepareFiles(j, req, g);
    }

    private void prepareExpressionFile(JobRequest req) {
        String data = req.getParams().get("exprData");
        if (data.indexOf(',') > -1)
            data = StringUtils.split(data, ',').get(1);
        final boolean[] header = {true};
        char tab = '\t';
        char n = '\n';
        StringBuilder mapped = new StringBuilder();
        StringUtils.split(new String(Base64.getDecoder().decode(data)), '\n').forEach(line -> {
                    if (line.length() == 0 || line.charAt(0) == '!') {
                        return;
                    }
                    if (header[0]) {
                        mapped.append(line).append(n);
                        header[0] = false;
                        return;
                    }
                    LinkedList<String> split = StringUtils.split(line, "\t");
                    String entrez = split.get(0);
                    if (entrez.charAt(0) == '"')
                        entrez = entrez.substring(1, entrez.length() - 1);

                    if (!entrez.startsWith("entrez."))
                        entrez = "entrez." + entrez;
                    Integer id = geneService.getDomainToIdMap().get(entrez);
                    if (id != null) {
                        mapped.append(id);
                        split.subList(1, split.size()).forEach(e -> mapped.append(tab).append(e));
                        mapped.append(n);
                    }
                }
        );
        req.getParams().put("exprData", mapped.toString());
    }

    private void prepareFiles(Job j, JobRequest req, Graph g) {
        HashMap<Integer, Pair<String, String>> domainMap;
        if (req.getParams().get("type").equals("gene")) {
            domainMap = geneService.getIdToDomainMap();
        } else {
            domainMap = proteinService.getIdToDomainMap();
        }
        toolService.prepareJobFiles(j, req, g, domainMap);
    }

    private String createCommand(Job j, JobRequest req) {
        return toolService.createCommand(j, req);
    }


    public void finishJob(String id) {
        Job j = jobs.get(id);
        try {
            HashMap<Integer, HashMap<String, Integer>> domainIds = new HashMap<>();
            domainIds.put(Graphs.getNode("gene"), geneService.getDomainToIdMap());
            domainIds.put(Graphs.getNode("protein"), proteinService.getDomainToIdMap());
            domainIds.put(Graphs.getNode("drug"), drugService.getDomainToIdMap());
            jobQueue.finishJob(j);
            toolService.getJobResults(j, domainIds);
            if ((j.getResult().getNodes().isEmpty()) & (!j.getParams().containsKey("nodesOnly") || !j.getParams().get("nodesOnly").equals("true"))) {
                j.setDerivedGraph(j.getBasisGraph());
            } else
                graphService.applyModuleJob(j);
        } catch (Exception e) {
            log.error("Error on finishing job: " + id);
            e.printStackTrace();
            j.setStatus(Job.JobState.ERROR);
        }
        try {
            toolService.clearDirectories(j, historyController.getJobPath(j));
        } catch (Exception e) {
            log.error("Error on finishing job: " + id);
            e.printStackTrace();
            j.setStatus(Job.JobState.ERROR);
        }
        save(j);
        socketController.setJobUpdate(j);
        checkWaitingJobs(j);
    }

    private void checkWaitingJobs(Job j) {
        if (jobWaitingForResults.containsKey(j.getJobId()))
            jobWaitingForResults.get(j.getJobId()).forEach(jid -> copyResults(jid, j, true));
    }

    private void copyResults(String cloneJid, Job j, boolean notify) {
        Job dependent = jobs.get(cloneJid);
        dependent.setStatus(Job.JobState.DONE);
        log.info("Finished " + dependent.getMethod().name() + " job " + dependent.getJobId() + "of user " + dependent.getUserId() + "! (Cloned from " + j.getJobId() + " to finish)");
        dependent.setDerivedGraph(graphService.cloneGraph(j.getDerivedGraph(), dependent.getUserId(), dependent.getBasisGraph()));
        dependent.setResultFile(true);
        try {
            Files.copy(historyController.getJobPath(j).toPath(), historyController.getJobPath(dependent).toPath());
        } catch (IOException e) {
            e.printStackTrace();
        }
        dependent.setUpdate(j.getUpdate());
        if (notify)
            socketController.setJobUpdate(dependent);
    }


    public File getDownload(String id) {
        return historyController.getJobPath(jobs.get(id));
    }


    public HashMap<String, Pair<String, Pair<Job.JobState, ToolService.Tool>>> getJobGraphStatesAndTypes(String user) {
        HashMap<String, Pair<String, Pair<Job.JobState, ToolService.Tool>>> stateMap = new HashMap<>();
        jobs.values().stream().filter(j -> j.getUserId().equals(user)).forEach(j -> stateMap.put(j.getDerivedGraph(), new Pair<>(j.getJobId(), new Pair<>(j.getState(), j.getMethod()))));
        return stateMap;
    }

    public LinkedList<HashMap<String, Object>> getJobGraphStatesAndTypes(String user, String gid) {
        LinkedList<HashMap<String, Object>> stateMap = new LinkedList<>();
        jobs.values().stream().filter(j -> j.getUserId().equals(user)).forEach(j -> {
            if ((gid == null || j.getBasisGraph().equals(gid)) | !j.getState().equals(Job.JobState.DONE)) {
                stateMap.add(j.toMap());
            }
        });
        return stateMap;
    }

}
