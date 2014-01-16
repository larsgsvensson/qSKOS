package at.ac.univie.mminf.qskos4j.cmd;

import at.ac.univie.mminf.qskos4j.issues.Issue;
import at.ac.univie.mminf.qskos4j.result.Result;
import org.openrdf.OpenRDFException;
import org.openrdf.model.URI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;


class ReportCollector {

    private final Logger logger = LoggerFactory.getLogger(ReportCollector.class);

    private Collection<Issue> issues;
    private String reportFileName;
    private boolean includeSummary;

    public ReportCollector(Collection<Issue> issues, String reportFileName, boolean includeSummary) {
        this.issues = issues;
        this.reportFileName = reportFileName;
        this.includeSummary = includeSummary;
    }

    void outputIssuesReport(boolean shouldWriteGraphs)
        throws IOException, OpenRDFException
    {
        File reportFile = createReportFile();
        BufferedWriter reportWriter = new BufferedWriter(new FileWriter(reportFile));

        processIssues();

        String reportSummary = "";
        if (includeSummary) {
            reportSummary = createReportSummary();
            logger.info("\n" +reportSummary);
        }
        writeReportHeader(reportWriter, reportFile, reportSummary);
        writeReportBody(reportWriter, reportFile, shouldWriteGraphs);

        reportWriter.close();
    }

    private void writeReportHeader(BufferedWriter reportWriter,
                                   File reportFile,
                                   String reportSummary) throws IOException
    {
        String issuedDate = new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss Z").format(new Date());
        String fileName = reportFile.getAbsolutePath();
        reportWriter.write("This is the quality report of file " +fileName+ ", generated by qSKOS on " +issuedDate);
        reportWriter.newLine();
        reportWriter.newLine();
        reportWriter.write(reportSummary);
    }

    private void processIssues() throws OpenRDFException {
        int issueNumber = 0;
        Iterator<Issue> issueIt = issues.iterator();
        while (issueIt.hasNext()) {
            Issue issue = issueIt.next();
            issueNumber++;

            logger.info("Processing issue " + issueNumber + " of " + issues.size() + " (" + issue.getName() + ")");
            issue.getResult();

        }

        logger.info("Report complete!");
    }

    private String createReportSummary() throws IOException, OpenRDFException {
        StringBuffer summary = new StringBuffer();
        summary.append("* Summary of Quality Issue Occurrences:\n");

        for (Issue issue : issues) {
            summary.append(issue.getName() + ": " + prepareOccurrenceText(issue) + "\n");
        }

        summary.append("\n");
        return summary.toString();
    }

    private String prepareOccurrenceText(Issue issue) throws OpenRDFException {
        String occurrenceText = "";
        if (issue.getResult().isProblematic()) {
            occurrenceText = "FAIL";
            try {
                String occurrenceCount = Long.toString(issue.getResult().occurrenceCount());
                occurrenceText += " (" +occurrenceCount+ ")";
            }
            catch (UnsupportedOperationException e) {
                // ignore this
            }
        }
        else {
            occurrenceText = "OK (no potential problems found)";
        }

        return occurrenceText;
    }

    private void writeReportBody(BufferedWriter reportWriter,
                                 File reportFile,
                                 boolean shouldWriteGraphs)
        throws IOException, OpenRDFException
    {
        reportWriter.write("* Detailed coverage of each Quality Issue:\n\n");
        Iterator<Issue> issueIt = issues.iterator();
        while (issueIt.hasNext()) {
            Issue issue = issueIt.next();

            writeTextReport(issue, reportWriter);

            if (issueIt.hasNext()) {
                reportWriter.newLine();
            }

            if (shouldWriteGraphs) {
                writeGraphFiles(issue, getDotFilesPath(reportFile));
            }
        }
    }

    private File createReportFile() throws IOException {
        File file = new File(reportFileName);
        file.createNewFile();
        return file;
    }

    private void writeTextReport(Issue issue, BufferedWriter writer)
        throws IOException, OpenRDFException
    {
        writer.write(createIssueHeader(issue));
        writer.newLine();
        issue.getResult().generateReport(writer, Result.ReportFormat.TXT, Result.ReportStyle.SHORT);

        writer.newLine();
        issue.getResult().generateReport(writer, Result.ReportFormat.TXT, Result.ReportStyle.EXTENSIVE);

        writer.newLine();
        writer.flush();
    }

    private String createIssueHeader(Issue issue) {
        String header = "--- " +issue.getName();
        URI weblink = issue.getWeblink();
        header += "\nDescription: " +issue.getDescription();

        if (weblink != null) {
            header += "\nDetailed information: " +weblink.stringValue();
        }
        return header;
    }

    private String getDotFilesPath(File reportFile) {
        String absolutePath = reportFile.getAbsolutePath();
        return absolutePath.substring(0, absolutePath.lastIndexOf(File.separator));
    }

    private void writeGraphFiles(Issue issue, String dotFilesPath) throws IOException, OpenRDFException {
        BufferedWriter graphFileWriter = new BufferedWriter(new FileWriter(dotFilesPath + issue.getId() + ".dot"));
        issue.getResult().generateReport(graphFileWriter, Result.ReportFormat.DOT);
        graphFileWriter.close();
    }

}
