package at.ac.univie.mminf.qskos4j;

import at.ac.univie.mminf.qskos4j.issues.*;
import at.ac.univie.mminf.qskos4j.issues.clusters.ClustersResult;
import at.ac.univie.mminf.qskos4j.issues.clusters.DisconnectedConceptClusters;
import at.ac.univie.mminf.qskos4j.issues.concepts.AuthoritativeConcepts;
import at.ac.univie.mminf.qskos4j.issues.concepts.InvolvedConcepts;
import at.ac.univie.mminf.qskos4j.issues.concepts.OrphanConcepts;
import at.ac.univie.mminf.qskos4j.issues.count.*;
import at.ac.univie.mminf.qskos4j.issues.cycles.HierarchicalCycles;
import at.ac.univie.mminf.qskos4j.issues.labels.InconsistentPrefLabelFinder;
import at.ac.univie.mminf.qskos4j.issues.labels.LexicalRelations;
import at.ac.univie.mminf.qskos4j.issues.labels.OverlappingLabelsFinder;
import at.ac.univie.mminf.qskos4j.issues.labels.NonDisjointLabelsFinder;
import at.ac.univie.mminf.qskos4j.issues.labels.util.LabelConflict;
import at.ac.univie.mminf.qskos4j.issues.labels.util.ResourceLabelsCollector;
import at.ac.univie.mminf.qskos4j.issues.language.LanguageCoverage;
import at.ac.univie.mminf.qskos4j.issues.language.OmittedOrInvalidLanguageTags;
import at.ac.univie.mminf.qskos4j.issues.outlinks.BrokenLinks;
import at.ac.univie.mminf.qskos4j.issues.outlinks.HttpURIs;
import at.ac.univie.mminf.qskos4j.issues.outlinks.MissingOutLinks;
import at.ac.univie.mminf.qskos4j.issues.outlinks.NonHttpResources;
import at.ac.univie.mminf.qskos4j.result.Result;
import at.ac.univie.mminf.qskos4j.result.custom.*;
import at.ac.univie.mminf.qskos4j.result.general.CollectionResult;
import at.ac.univie.mminf.qskos4j.result.general.ExtrapolatedCollectionResult;
import at.ac.univie.mminf.qskos4j.util.Pair;
import at.ac.univie.mminf.qskos4j.util.graph.NamedEdge;
import at.ac.univie.mminf.qskos4j.util.progress.IProgressMonitor;
import at.ac.univie.mminf.qskos4j.util.vocab.VocabRepository;
import org.jgrapht.graph.DirectedMultigraph;
import org.openrdf.OpenRDFException;
import org.openrdf.model.Resource;
import org.openrdf.model.URI;
import org.openrdf.repository.Repository;
import org.openrdf.repository.sparql.SPARQLRepository;
import org.openrdf.rio.RDFFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.*;
import java.util.Collections;

/**
 * Main class intended for easy interaction with qSKOS. On instantiation an in-memory ("local") repository 
 * containing the passed controlled vocabulary is created which can be queried by calling the methods of this class. 
 * 
 * @author christian
 *
 */
public class QSkos {

	private final Logger logger = LoggerFactory.getLogger(QSkos.class);

    /**
     * Delay to avoid flooding "external" sources. This is used, e.g., when dereferencing lots of links or sending
     * many queryies to a SPARQL endpoint
     */
    private final static int EXT_ACCESS_MILLIS = 1500;
	
	private Collection<Repository> otherRepositories = new HashSet<Repository>();

	private VocabRepository vocabRepository;
	private IProgressMonitor progressMonitor;
	private String baseURI;
	private Integer extAccessDelayMillis = EXT_ACCESS_MILLIS;
	private Float randomSubsetSize_percent;
	
	private CollectionResult<URI> involvedConcepts, authoritativeConcepts;
	private DirectedMultigraph<Resource, NamedEdge> hierarchyGraph;
    private Collection<Issue> issuesToTest = Collections.EMPTY_LIST;

	/**
	 * Constructs a QSkos object and initializes it with content from the passed RDF vocabulary.
	 * 
	 * @param rdfFile rdfFile a file holding a SKOS vocabulary
	 * @throws OpenRDFException if errors when initializing local repository 
	 * @throws IOException if problems occur reading the passed vocabulary file
	 */
	public QSkos(File rdfFile) 
		throws OpenRDFException, IOException
	{
		this(rdfFile, null, null);
	}
	
	/**
	 * Constructs a QSkos object and initializes it with content from the passed RDF vocabulary and
	 * explicitly stating the RDF serialization format.
	 * 
	 * @param rdfFile rdfFile a file holding a SKOS vocabulary
	 * @param dataFormat RDF serialization format of the passed vocabulary
	 * @throws OpenRDFException if errors when initializing temporal repository
	 * @throws IOException if problems occur reading the passed vocabulary file
	 */
	public QSkos(File rdfFile,
		RDFFormat dataFormat) 
		throws OpenRDFException, IOException
	{
		this(rdfFile, null, dataFormat);
	}
	
	private QSkos(File rdfFile,
		String baseURI,
		RDFFormat dataFormat) 
		throws OpenRDFException, IOException 
	{
		logger.info("initializing vocabulary from file '" +rdfFile.getName()+ "'...");
		
		vocabRepository = new VocabRepository(rdfFile, baseURI, dataFormat);
		this.baseURI = baseURI;
	}

	private void addAllIssues() {
        issuesToTest.clear();

        InvolvedConcepts involvedConcepts = new InvolvedConcepts();
        AuthoritativeConcepts authoritativeConcepts = new AuthoritativeConcepts(involvedConcepts, baseURI);
        HttpURIs httpURIs = new HttpURIs();

        addIssue(involvedConcepts);
        addIssue(authoritativeConcepts);
        addIssue(new OrphanConcepts(involvedConcepts));
        addIssue(new MissingOutLinks(authoritativeConcepts));
        addIssue(new LexicalRelations(involvedConcepts));
        addIssue(new SemanticRelations());
        addIssue(new AggregationRelations());
        addIssue(new ConceptSchemes());
        addIssue(new at.ac.univie.mminf.qskos4j.issues.count.Collections());
        addIssue(httpURIs);
        addIssue(new DisconnectedConceptClusters(involvedConcepts));
        addIssue(new MissingOutLinks(authoritativeConcepts));
        addIssue(new HierarchicalCycles());
        addIssue(new NonHttpResources());
        addIssue(new OmittedOrInvalidLanguageTags());
        addIssue(new LanguageCoverage(involvedConcepts));

        BrokenLinks brokenLinks = new BrokenLinks(httpURIs);
        brokenLinks.setSubsetSize(randomSubsetSize_percent);
        brokenLinks.setExtAccessDelayMillis(extAccessDelayMillis);
        addIssue(brokenLinks);

	}

    private void addIssue(Issue issue) {
        issuesToTest.add(issue);
        issue.setVocabRepository(vocabRepository);
        issue.setProgressMonitor(progressMonitor);
    }

    public void invokeAllIssues() {
        addAllIssues();

        for (Issue issue : issuesToTest) {
            //issue.getResult();
        }
    }


    /*
	private DirectedMultigraph<Resource, NamedEdge> getHierarchyGraph() 
		throws OpenRDFException
	{
		if (hierarchyGraph == null) {
			hierarchyGraph = new HierarchyGraph(vocabRepository).createGraph();
		}
		return hierarchyGraph;
	}
	*/

	/**
	 * Finds resources not defined in the SKOS ontology (
	 * <a href="https://github.com/cmader/qSKOS/wiki/Quality-Issues#wiki-Undefined_SKOS_Resources">Undefined SKOS Resources</a>
	 * ).
	 * 
	 * @throws OpenRDFException
	public CollectionResult<URI> findUndefinedSkosResources() throws OpenRDFException {
		return new SkosTermsChecker(vocabRepository).findUndefinedSkosResources();
	}
     */

	/**
	 * Finds concepts with more than one preferred label (
	 * <a href="https://github.com/cmader/qSKOS/wiki/Quality-Issues#wiki-Inconsistent_Preferred_Labels">Inconsistent Preferred Labels</a>
	 * ).
	 * 
	 * @throws OpenRDFException
	public CollectionResult<LabelConflict> findInconsistentPrefLabels() throws OpenRDFException {
		return new InconsistentPrefLabelFinder(vocabRepository, resourceLabelsCollector).findInconsistentPrefLabels();
	}
     */

	/**
	 * Finds concepts having identical entries for prefLabel, altLabel or hiddenLabel (
	 * <a href="https://github.com/cmader/qSKOS/wiki/Quality-Issues#wiki-Disjoint_Labels_Violation-2">Disjoint Labels Violation</a>
	 * ).
	 * 
	 * @throws OpenRDFException
	public CollectionResult<LabelConflict> findDisjointLabelsViolations() throws OpenRDFException {
		return new NonDisjointLabelsFinder(vocabRepository, resourceLabelsCollector).findDisjointLabelsViolations();
	}
     */

	/**
	 * Finds all <a href="https://github.com/cmader/qSKOS/wiki/Quality-Issues#wiki-Valueless_Associative_Relations">Valueless Associative Relations</a>.
	 * 
	 * @throws OpenRDFException
	public Result<Collection<Pair<URI>>> findValuelessAssociativeRelations() throws OpenRDFException {
		return new ValuelessAssocRelationsResult(
            redundantAssociativeRelationsFinder.findValuelessAssociativeRelations(),
            vocabRepository);
	}
     */

	/**
	 * Finds concepts having the same preferred labels (
	 * <a href="https://github.com/cmader/qSKOS/wiki/Quality-Issues#wiki-Overlapping_Labels">Overlapping Labels</a>
	 * ).
	 * 
	 * @throws OpenRDFException
	public CollectionResult<LabelConflict> findOverlappingLabels() throws OpenRDFException {
		OverlappingLabelsFinder overlappingLabelsFinder = new OverlappingLabelsFinder(vocabRepository);
		overlappingLabelsFinder.setProgressMonitor(progressMonitor);
		return overlappingLabelsFinder.findOverlappingLabels(findAuthoritativeConcepts().getData());
	}
     */

	/**
	 * Finds concepts that aren't referred by other vocabularies on the Web (
	 * <a href="https://github.com/cmader/qSKOS/wiki/Quality-Issues#wiki-Missing_InLinks">Missing In-Links</a>
	 * ).
	 * 
	 * @throws OpenRDFException
	public CollectionResult<URI> findMissingInLinks() throws OpenRDFException
	{
		InLinkFinder inLinkFinder = new InLinkFinder(
			vocabRepository, 
			otherRepositories);
		inLinkFinder.setProgressMonitor(progressMonitor);

        return inLinkFinder.findMissingInLinks(
            findAuthoritativeConcepts().getData(),
            randomSubsetSize_percent,
            extAccessDelayMillis);
	}
     */

	/**
	 * Finds <a href="https://github.com/cmader/qSKOS/wiki/Quality-Issues#wiki-Unidirectionally_Related_Concepts">Unidirectionally Related Concepts</a>.
	 * 
	 * @throws OpenRDFException
	public UnidirRelResourcesResult findUnidirectionallyRelatedConcepts() throws OpenRDFException {
		return new InverseRelationsChecker(vocabRepository).findUnidirectionallyRelatedConcepts();
	}
     */

	/**
	 * Finds <a href="https://github.com/cmader/qSKOS/wiki/Quality-Issues#wiki-Solely_Transitively_Related_Concepts">Solely Transitively Related Concepts</a>.
	 * 
	 * @throws OpenRDFException
	public CollectionResult<Pair<URI>> findSolelyTransitivelyRelatedConcepts() throws OpenRDFException {
		return new SolitaryTransitiveRelationsFinder(vocabRepository).findSolelyTransitivelyRelatedConcepts();
	}
     */

	/**
	 * Finds concepts lacking documentation information (
	 * <a href="https://github.com/cmader/qSKOS/wiki/Quality-Issues#wiki-Undocumented_Concepts">Undocumented Concepts</a>
	 * ).
	 * 
	 * @throws OpenRDFException
	public CollectionResult<Resource> findUndocumentedConcepts() throws OpenRDFException
	{
		UndocumentedConceptsChecker docCovChecker = 
			new UndocumentedConceptsChecker(vocabRepository);
		docCovChecker.setProgressMonitor(progressMonitor);
		return docCovChecker.findUndocumentedConcepts(findAuthoritativeConcepts().getData());
	}
     */

	/**
	 * Finds top concepts that have broader concepts (
	 * <a href="https://github.com/cmader/qSKOS/wiki/Quality-Issues#wiki-Top_Concepts_Having_Broader_Concepts">Top Concepts Having Broader Concepts</a>
	 * ).
	 * 
	 * @throws OpenRDFException
	public CollectionResult<URI> findTopConceptsHavingBroaderConcepts() throws OpenRDFException {
		return new ConceptSchemeChecker(vocabRepository).findTopConceptsHavingBroaderConcepts();
	}
     */

	/**
	 * Finds <a href="https://github.com/cmader/qSKOS/wiki/Quality-Issues#wiki-Relation_Clashes">Associative vs. Hierarchical Relation Clashes</a>.
	 *
	 * @throws OpenRDFException
	public CollectionResult<Pair<URI>> findRelationClashes() throws OpenRDFException {
		SkosReferenceIntegrityChecker skosReferenceIntegrityChecker = new SkosReferenceIntegrityChecker(vocabRepository);
		skosReferenceIntegrityChecker.setProgressMonitor(progressMonitor);
		return skosReferenceIntegrityChecker.findRelationClashes(getHierarchyGraph());
	}
     */

	/**
	 * Finds <a href="https://github.com/cmader/qSKOS/wiki/Quality-Issues#wiki-Mapping_Clashes">Exact vs. Associative and Hierarchical Mapping Clashes</a>.
	 * 
	 * @throws OpenRDFException
	public CollectionResult<Pair<URI>> findMappingClashes() throws OpenRDFException {
		return new SkosReferenceIntegrityChecker(vocabRepository).findMappingClashes();
	}
     */

	/**
	 * Set an IProgressMonitor that is notified on changes in the evaluation progress for every managed issues.
	 * @param progressMonitor monitor instance to be notified
	 */
	public void setProgressMonitor(IProgressMonitor progressMonitor) {
        this.progressMonitor = progressMonitor;
	}

	/**
	 * Adds a SPARQL endpoint for estimation of in-links.
	 * 
	 * @param endpointUrl SPARL endpoint URL
	 */
	public void addSparqlEndPoint(String endpointUrl) {
		otherRepositories.add(new SPARQLRepository(endpointUrl));
	}
	
	/**
	 * Adds the repository containing the vocabulary that's about to test to the list of
	 * other repositories. This is only useful for in-link testing purposes.
	 */
	public void addRepositoryLoopback() {
		otherRepositories.add(vocabRepository.getRepository());
	}
	
	/**
	 * Sets a delay time in milliseconds that must pass between accessing an external resource. This is intended to
     * avoid flooding of, e.g., vocabulary hosts or SPARQL endpoints with HTTP requests.
	 * 
	 * @param delayMillis delay time in milliseconds
	 */
	public void setExtAccessDelayMillis(int delayMillis) {
		extAccessDelayMillis = delayMillis;
	}
	
	/**
	 * Some methods in this class support investigating only a subset of the vocabulary and extrapolate the results
	 * to shorten evaluation time. Works for, e.g., finding broken links. 
	 * 
	 * @param subsetSizePercent percentage of the total resources to investigate.
	 */
	public void setSubsetSize(Float subsetSizePercent) {
		randomSubsetSize_percent = subsetSizePercent;
	}
	
	/**
	 * If this is called, the local repository is complemented with SKOS lexical labels inferred from SKOSXL definitions 
	 * as described in the SKOS <a href="http://www.w3.org/TR/skos-reference/#S55">reference document</a> by the axioms
	 * S55-S57
	 * 
	 * @throws OpenRDFException if errors when initializing local repository
	 */
	public void enableSkosXlSupport() throws OpenRDFException {
		logger.info("inferring SKOSXL triples");
		vocabRepository.enableSkosXlSupport();
	}
		
}